package nya.tuyw.hugme.animation;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nya.tuyw.hugme.HugMe;

@EventBusSubscriber(modid = HugMe.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AnimationManager {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ResourceLocation.fromNamespaceAndPath("hugme", "animations"), 42, (player) -> {
            if (player instanceof AbstractClientPlayer) {
                ModifierLayer<IAnimation> hugAnimation = new ModifierLayer<>();
                hugAnimation.addModifierBefore(new SpeedModifier(0.5F));
                return hugAnimation;
            }
            return null;
        });
    }

    @SuppressWarnings("all")
    public static void playHugAnimation(AbstractClientPlayer sender, AbstractClientPlayer receiver,HugAnimationEnum hugAnimationEnum) {
        if (sender == null) return;
        ModifierLayer<IAnimation> senderAnimation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(sender).get(ResourceLocation.fromNamespaceAndPath("hugme", "animations"));
        ModifierLayer<IAnimation> receiverAnimation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(receiver).get(ResourceLocation.fromNamespaceAndPath("hugme", "animations"));

        KeyframeAnimation sender_animation;
        KeyframeAnimationPlayer sender_animationPlayer = null;
        KeyframeAnimation receiver_animation;
        KeyframeAnimationPlayer receiver_animationPlayer = null;

        switch (hugAnimationEnum) {
            case NORMALHUG -> {
                sender_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_normal_sender"));
                sender_animationPlayer = new KeyframeAnimationPlayer(sender_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
                receiver_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_normal_receiver"));
                receiver_animationPlayer = new KeyframeAnimationPlayer(receiver_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
            }
            case FLYHUG -> { //动画未完成
                sender_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_touch_sender"));
                sender_animationPlayer = new KeyframeAnimationPlayer(sender_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
                receiver_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_touch_receiver"));
                receiver_animationPlayer = new KeyframeAnimationPlayer(receiver_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
            }
            case TOUCHHEADHUG -> {
                sender_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_touch_sender"));
                sender_animationPlayer = new KeyframeAnimationPlayer(sender_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
                receiver_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_touch_receiver"));
                receiver_animationPlayer = new KeyframeAnimationPlayer(receiver_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
            }
            default -> {
                sender_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_normal_sender"));
                sender_animationPlayer = new KeyframeAnimationPlayer(sender_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
                receiver_animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("hugme", "hug_normal_receiver"));
                receiver_animationPlayer = new KeyframeAnimationPlayer(receiver_animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                        .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowLeftItem(false).setShowRightArm(true).setShowRightItem(false));
            }
        }

        senderAnimation.setAnimation(sender_animationPlayer);
        receiverAnimation.setAnimation(receiver_animationPlayer);
    }
}

