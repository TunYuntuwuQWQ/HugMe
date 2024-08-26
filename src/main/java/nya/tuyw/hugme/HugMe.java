package nya.tuyw.hugme;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nya.tuyw.hugme.command.HugCommandHandler;
import nya.tuyw.hugme.item.HugTicketItem;
import org.slf4j.Logger;

import java.io.File;

@Mod(HugMe.MODID)
public class HugMe {
    public static final String MODID = "hugme";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items HUGME_ITEMS = DeferredRegister.createItems(HugMe.MODID);
    public static final DeferredItem HUG_TICKET = HUGME_ITEMS.registerItem("hug_ticket", HugTicketItem::new, new Item.Properties().stacksTo(64));

    public HugMe(IEventBus modEventBus, ModContainer modContainer) {
        HUGME_ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(this);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(HUG_TICKET);
    }

    @SubscribeEvent
    private void onServerStarting(ServerStartingEvent event) {
        HugCommandHandler.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (ServerLifecycleHooks.getCurrentServer() == null) return;
            File playerDataFile = new File(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), player.getUUID() + ".dat");
            if (!playerDataFile.exists()) {
                ItemStack itemStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hugme","hug_ticket")),16);
                if (!player.getInventory().add(itemStack)) {
                    player.drop(itemStack, false);
                }
            }
        }
    }
}
