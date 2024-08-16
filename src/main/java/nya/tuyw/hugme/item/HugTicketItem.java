package nya.tuyw.hugme.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nya.tuyw.hugme.command.HugCommandHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class HugTicketItem extends Item {

    public HugTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof Player targetPlayer) {
            if (!player.level().isClientSide) {
                if (HugCommandHandler.startHugRequest((ServerPlayer) player, (ServerPlayer) targetPlayer)) {
                    sendHugRequest(player, targetPlayer);
                    player.sendSystemMessage(Component.translatable("hugme.message.sendsuccess", targetPlayer.getName().getString()).withStyle(ChatFormatting.GREEN));
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hugme.hug_ticket.desc").withStyle(ChatFormatting.GRAY));
    }

    private void sendHugRequest(Player sender, Player target) {
        Component message = (Component.translatable("hugme.message.sendrequest", sender.getName()).withStyle(ChatFormatting.BLUE))
                .append(Component.translatable("hugme.message.acceptrequest").withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hugme acceptRequest " + sender.getName().getString()))))
                .append(Component.translatable("hugme.message.rejectrequest").withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hugme rejectRequest " + sender.getName().getString()))));

        target.sendSystemMessage(message);
    }
}
