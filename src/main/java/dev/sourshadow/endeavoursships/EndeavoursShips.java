package dev.sourshadow.endeavoursships;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(EndeavoursShips.MOD_ID)
public final class EndeavoursShips {
    public static final String MOD_ID = "endeavours_ships";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<ShipHelmBlock> SHIP_HELM = BLOCKS.registerBlock(
            "ship_helm",
            properties -> new ShipHelmBlock(properties
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredItem<?> SHIP_HELM_ITEM = ITEMS.registerSimpleBlockItem(SHIP_HELM);

    public static final DeferredHolder<EntityType<?>, EntityType<ShipEntity>> SHIP = ENTITY_TYPES.register(
            "ship",
            () -> EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "ship"))));

    public EndeavoursShips(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabContents);
        modEventBus.addListener(EndeavoursShipGameTests::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, ShipConfig.SPEC);
        LOGGER.info("Endeavours Ships is loading");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SHIP_HELM_ITEM);
        }
    }
}
