package nya.tuyw.hugme.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import javafx.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nya.tuyw.hugme.HugMe;
import nya.tuyw.hugme.animation.HugAnimationEnum;
import nya.tuyw.hugme.network.HugRenderPayload;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = HugMe.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HugCommandHandler {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long REQUEST_TIMEOUT_S = 60;
    private static final short AnimTick = 20 * 6;

    private static final Map<Pair<UUID,UUID>, HugRequest> hugRequests = new ConcurrentHashMap<>();
    private static final Map<Pair<UUID,UUID>, HugStatus> hugStatuses = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hugme")
                .then(Commands.literal("acceptRequest")
                        .then(Commands.argument("sender", StringArgumentType.string())
                                .executes(context -> {
                                    String senderName = StringArgumentType.getString(context, "sender");
                                    ServerPlayer sender = context.getSource().getServer().getPlayerList().getPlayerByName(senderName);
                                    ServerPlayer receiver = context.getSource().getPlayerOrException();

                                    if (sender != null) {
                                        handleHugAcceptance(sender, receiver);
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("hugme.message.sendernotfound").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                })))
                .then(Commands.literal("rejectRequest")
                        .then(Commands.argument("sender", StringArgumentType.string())
                                .executes(context -> {
                                    String senderName = StringArgumentType.getString(context, "sender");
                                    ServerPlayer sender = context.getSource().getServer().getPlayerList().getPlayerByName(senderName);
                                    ServerPlayer receiver = context.getSource().getPlayerOrException();
                                    if (sender != null) {
                                        handleHugRejection(sender,receiver);
                                        return 1;
                                    }else {
                                        context.getSource().sendFailure(Component.translatable("hugme.message.sendernotfound").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                })))
        );
    }
    private static void handleHugRejection(ServerPlayer sender, ServerPlayer receiver) {
        Pair<UUID, UUID> key = new Pair<>(sender.getUUID(), receiver.getUUID());
        HugRequest request = hugRequests.get(key);

        if (request != null && request.receiverId().equals(receiver.getUUID())) {
            sender.sendSystemMessage(Component.translatable("hugme.message.sender_rejected", receiver.getName()).withStyle(ChatFormatting.RED));
            receiver.sendSystemMessage(Component.translatable("hugme.message.receiver_rejected", sender.getName()).withStyle(ChatFormatting.RED));
            hugRequests.remove(key);
        } else {
            receiver.sendSystemMessage(Component.translatable("hugme.message.invalid_request").withStyle(ChatFormatting.RED));
        }
    }

    private static void handleHugAcceptance(ServerPlayer sender, ServerPlayer receiver) {
        Pair<UUID, UUID> key = new Pair<>(sender.getUUID(), receiver.getUUID());

        HugRequest request = hugRequests.get(key);
        if (request != null) {
            if (hugStatuses.containsKey(key)) {
                sender.sendSystemMessage(Component.translatable("hugme.message.hug_in_progress_sender",receiver.getName().getString()).withStyle(ChatFormatting.RED));
                receiver.sendSystemMessage(Component.translatable("hugme.message.hug_in_progress_receiver",sender.getName().getString()).withStyle(ChatFormatting.RED));
                return;
            }
            sender.sendSystemMessage(Component.translatable("hugme.message.sender_accepted", receiver.getName().getString()).withStyle(ChatFormatting.GREEN));
            receiver.sendSystemMessage(Component.translatable("hugme.message.receiver_accepted", sender.getName().getString()).withStyle(ChatFormatting.GREEN));

            if (sender.serverLevel() != receiver.serverLevel()) {
                receiver.sendSystemMessage(Component.translatable("hugme.message.invalid_level", sender.getName().getString()).withStyle(ChatFormatting.RED));
                return;
            }
            if (receiver.distanceTo(sender) > 5) {
                receiver.sendSystemMessage(Component.translatable("hugme.message.too_far", sender.getName().getString()).withStyle(ChatFormatting.RED));
                return;
            }

            sender.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(receiver.getX(), receiver.getEyeY(), receiver.getZ()));

            Vec3 senderLookVec = sender.getLookAngle();
            double horizontalDistance = Math.sqrt(senderLookVec.x * senderLookVec.x + senderLookVec.z * senderLookVec.z);
            double offsetX = (senderLookVec.x / horizontalDistance) * 1.3;
            double offsetZ = (senderLookVec.z / horizontalDistance) * 1.3;

            double receiverX = sender.getX() + offsetX;
            double receiverY = sender.getY();
            double receiverZ = sender.getZ() + offsetZ;

            receiver.teleportTo(receiverX, receiverY, receiverZ);
            receiver.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(sender.getX(), sender.getEyeY(), sender.getZ()));

            sendRenderInfoToNearbyPlayers(sender, receiver);

            hugRequests.remove(key);
        } else {
            receiver.sendSystemMessage(Component.translatable("hugme.message.invalid_request").withStyle(ChatFormatting.RED));
        }
    }

    private static void sendRenderInfoToNearbyPlayers(ServerPlayer sender, ServerPlayer receiver) {
        List<ServerPlayer> nearbyPlayers = getNearbyPlayers(sender);
        HugStatus status = new HugStatus(sender.getUUID(), receiver.getUUID(), sender.position(), receiver.position(), true, AnimTick, nearbyPlayers);
        hugStatuses.put(new Pair<>(sender.getUUID(), receiver.getUUID()), status);

        for (ServerPlayer serverPlayer : nearbyPlayers) {
            PacketDistributor.sendToPlayer(serverPlayer, new HugRenderPayload(sender.getStringUUID(), receiver.getStringUUID(), false, new Random().nextInt(HugAnimationEnum.values().length)));
        }
    }

    private static List<ServerPlayer> getNearbyPlayers(ServerPlayer sender) {
        List<ServerPlayer> players = new ArrayList<>();
        ServerLevel level = sender.serverLevel();

        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (player.level().dimension() == level.dimension()) {
                    double distance = player.distanceTo(sender);
                    if (distance < 64) {
                        players.add(player);
                    }
                }
            }
        }
        return players;
    }

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        for (Map.Entry<Pair<UUID, UUID>, HugStatus> entry : hugStatuses.entrySet()) {
            HugStatus status = entry.getValue();
            if (!status.isActive()){hugStatuses.remove(new Pair<>(status.getSenderId(),status.getReceiverId()));}
            if (status.isActive() && status.getTicksRemaining() > 0) {
                doLock(status);
                status.decrementTicks();
                if (status.getTicksRemaining() == 0) {
                    endHug(status);
                }
            }
        }
    }
    private static void doLock(HugStatus status){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer sender = null;
        ServerPlayer receiver = null;
        if (server != null) {
            sender = server.getPlayerList().getPlayer(status.getSenderId());
            receiver = server.getPlayerList().getPlayer(status.getReceiverId());
        }
        if (sender != null && receiver != null) {
            sender.teleportTo(status.getSenderPos().x,status.getSenderPos().y,status.getSenderPos().z);
            receiver.teleportTo(status.getReceiverPos().x,status.getReceiverPos().y,status.getReceiverPos().z);
        }
    }
    private static void endHug(HugStatus status) {
        UUID senderId = status.getSenderId();
        UUID receiverId = status.getReceiverId();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer sender = null;
        ServerPlayer receiver = null;
        if (server != null) {
            sender = server.getPlayerList().getPlayer(senderId);
            receiver = server.getPlayerList().getPlayer(receiverId);
        }
        if (sender != null && receiver != null) {
            for (ServerPlayer player : status.getNearbyPlayers()) {
                PacketDistributor.sendToPlayer(player, new HugRenderPayload(sender.getStringUUID(), receiver.getStringUUID(), true, 0));
            }
            hugStatuses.remove(new Pair<>(senderId, receiverId));
        }
    }

    public static boolean startHugRequest(ServerPlayer sender, ServerPlayer receiver) {
        Pair<UUID, UUID> key = new Pair<>(sender.getUUID(), receiver.getUUID());
        if (hugRequests.get(key) != null){
            sender.sendSystemMessage(Component.translatable("hugme.message.have_request").withStyle(ChatFormatting.RED));
            return false;
        }
        HugRequest request = new HugRequest(sender.getUUID(), receiver.getUUID());
        hugRequests.put(key, request);

        scheduler.schedule(() -> {
            if (hugRequests.remove(key) != null) {
                sender.sendSystemMessage(Component.translatable("hugme.message.request_expired",receiver.getName().getString()).withStyle(ChatFormatting.RED));
                receiver.sendSystemMessage(Component.translatable("hugme.message.request_expired",sender.getName().getString()).withStyle(ChatFormatting.RED));
            }
        }, REQUEST_TIMEOUT_S, TimeUnit.SECONDS);

        return true;
    }

    private static class HugStatus {
        private final UUID senderId;
        private final UUID receiverId;
        private final Vec3 senderPos;
        private final Vec3 receiverPos;
        private final List<ServerPlayer> nearbyPlayers;
        private boolean active;
        private short ticksRemaining;
        public HugStatus(UUID senderId, UUID receiverId, Vec3 senderPos, Vec3 receiverPos, boolean active, short ticksRemaining, List<ServerPlayer> nearbyPlayers) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.senderPos = senderPos;
            this.receiverPos = receiverPos;
            this.active = active;
            this.ticksRemaining = ticksRemaining;
            this.nearbyPlayers = nearbyPlayers;
        }
        public UUID getSenderId() {
            return senderId;
        }
        public UUID getReceiverId() {
            return receiverId;
        }
        public Vec3 getSenderPos() {
            return senderPos;
        }
        public Vec3 getReceiverPos() {
            return receiverPos;
        }
        public boolean isActive() {
            return active;
        }
        public void setActive(boolean active) {
            this.active = active;
        }
        public int getTicksRemaining() {
            return ticksRemaining;
        }
        public void decrementTicks() {
            this.ticksRemaining--;
        }
        public List<ServerPlayer> getNearbyPlayers() {
            return nearbyPlayers;
        }
    }
    public record HugRequest(UUID senderId, UUID receiverId) {}
}