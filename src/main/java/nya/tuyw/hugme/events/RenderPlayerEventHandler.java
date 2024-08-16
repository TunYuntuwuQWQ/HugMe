package nya.tuyw.hugme.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nya.tuyw.hugme.HugMe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = HugMe.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class RenderPlayerEventHandler {
    private static final Minecraft client = Minecraft.getInstance();
    private static final Map<UUID, Pair<UUID,Boolean>> playerLockMap = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerRenderer renderer = event.getRenderer();
        float partialTicks = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        if (player instanceof AbstractClientPlayer renderPlayer) {
            UUID playerId = player.getUUID();
            if (playerLockMap.containsKey(playerId)) {
                Pair<UUID, Boolean> pair = playerLockMap.get(playerId);
                UUID targetPlayerUUID = pair.getLeft();
                if (targetPlayerUUID == null) return;
                if (client.level == null) return;
                AbstractClientPlayer targetPlayer = (AbstractClientPlayer) client.level.getPlayerByUUID(targetPlayerUUID);
                if (targetPlayer == null) return;
                float targetYaw = calculateYawToTarget(renderPlayer, targetPlayer);

                poseStack.pushPose();

                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(0, -1.5, 0);
                poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(targetYaw)));

                var animationPlayer = ((IAnimatedPlayer) renderPlayer).playerAnimator_getAnimation();
                animationPlayer.setTickDelta(partialTicks);
                if(animationPlayer.isActive()) {
                    Vec3f vec3d = animationPlayer.get3DTransform("body", TransformType.POSITION, Vec3f.ZERO);
                    poseStack.translate(vec3d.getX(), vec3d.getY() + 0.7, vec3d.getZ());
                    Vec3f vec3f = animationPlayer.get3DTransform("body", TransformType.ROTATION, Vec3f.ZERO);
                    poseStack.mulPose(Axis.ZP.rotation(vec3f.getZ()));
                    poseStack.mulPose(Axis.YP.rotation(vec3f.getY()));
                    poseStack.mulPose(Axis.XP.rotation(vec3f.getX()));
                    poseStack.translate(0, - 0.7d, 0);
                }

                poseStack.scale(1.0F, 1.0F, 1.0F);
                renderPlayerModel(renderer, renderPlayer, poseStack, buffer, packedLight, pair.getRight());

                poseStack.popPose();
                event.setCanceled(true);
            }
        }
    }

    private static void renderPlayerModel(PlayerRenderer renderer, AbstractClientPlayer entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean hideHead) {
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.young = entity.isBaby();
        model.head.visible = client.options.getCameraType() != CameraType.FIRST_PERSON || !hideHead || !(entity instanceof LocalPlayer);

        model.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        RenderType renderType = RenderType.entityCutoutNoCull(renderer.getTextureLocation(entity));

        model.renderToBuffer(poseStack, buffer.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY, -1);
    }

    private static float calculateYawToTarget(AbstractClientPlayer fromPlayer, AbstractClientPlayer toPlayer) {
        double deltaX = toPlayer.getX() - fromPlayer.getX();
        double deltaZ = toPlayer.getZ() - fromPlayer.getZ();
        return (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
    }

    public static void lockPlayers(AbstractClientPlayer sender, AbstractClientPlayer receiver) {
        playerLockMap.put(sender.getUUID(), new Pair<>(receiver.getUUID(),true));
        playerLockMap.put(receiver.getUUID(), new Pair<>(sender.getUUID(),false));
        HugMe.LOGGER.debug("Locked " + sender.getName().getString() + " and " + receiver.getName().getString());
    }

    public static void unlockPlayers(AbstractClientPlayer sender, AbstractClientPlayer receiver) {
        playerLockMap.remove(sender.getUUID());
        playerLockMap.remove(receiver.getUUID());
        HugMe.LOGGER.debug("Unlocked " + sender.getName().getString() + " and " + receiver.getName().getString());
    }
}