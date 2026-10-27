from pathlib import Path
import sys, json
root = Path(sys.argv[1]); java = root/'src/main/java/cz/endeavours/soulstone'
def update(path, old, new):
    p = root / path
    src = p.read_text()
    if old not in src:
        raise RuntimeError(f'Cannot patch {p}: missing {old[:60]!r}')
    p.write_text(src.replace(old, new))
update('build.gradle', "tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }", """tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
repositories {
    maven { name = 'TunaMods Glint & Glamour'; url = 'https://raw.githubusercontent.com/TunaMods/Glint-and-Glamour/26.1.2/mcmodsrepo' }
}
dependencies {
    jarJar(implementation('net.tunamods.customglint:glint-and-glamour-api')) {
        version { strictly '[1.7.0,2.0)'; prefer '1.7.0' }
    }
}
""")
update('gradle.properties', 'mod_version=0.1.0-dev', 'mod_version=0.2.1-dev')
update('src/main/resources/META-INF/neoforge.mods.toml','version="0.1.0-dev"', 'version="0.2.1-dev"')
p='src/main/java/cz/endeavours/soulstone/Soulstone.java'
update(p, 'import net.minecraft.core.registries.Registries;', 'import net.minecraft.core.registries.Registries;\nimport net.minecraft.world.item.CreativeModeTabs;\nimport net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;')
update(p, '        RECIPES.register(modBus);\n        NeoForge.EVENT_BUS.register(SoulstoneDeathEvents.class);', """        RECIPES.register(modBus);
        modBus.addListener(this::creativeTabs);
        NeoForge.EVENT_BUS.register(SoulstoneDeathEvents.class);
        NeoForge.EVENT_BUS.register(SoulstoneTooltip.class);
        NeoForge.EVENT_BUS.register(SoulstoneMigration.class);""")
update(p, '    public Soulstone(IEventBus modBus) {', """    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(ORE_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CRYSTAL.get());
            event.accept(SEAL.get());
        }
    }

    public Soulstone(IEventBus modBus) {""")
p='src/main/java/cz/endeavours/soulstone/SoulSealSmithingRecipe.java'
update(p,'import java.util.Optional;', 'import java.util.Optional;\nimport net.tunamods.customglint.common.CustomGlint;')
update(p, '        sealed.set(Soulstone.SOUL_STATE.get(), 1);', """        sealed.set(Soulstone.SOUL_STATE.get(), 1);
        CustomGlint.write(sealed, CustomGlint.VANILLA,
                new int[]{0xB3FFD65A, 0xC8FFBB38}, 0.75F, true, 1.0F, true);""")
p='src/main/java/cz/endeavours/soulstone/SoulstoneDeathEvents.java'
update(p, 'import java.util.*;', 'import java.util.*;\nimport net.tunamods.customglint.common.CustomGlint;')
update(p, '            copy.set(Soulstone.SOUL_STATE.get(), 2);', """            CustomGlint.remove(copy);
            copy.set(Soulstone.SOUL_STATE.get(), 2);""")
(java/'SoulstoneTooltip.java').write_text("""package cz.endeavours.soulstone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
public final class SoulstoneTooltip {
    private SoulstoneTooltip() {}
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getOrDefault(Soulstone.SOUL_STATE.get(), 0) != 1) return;
        if (event.getToolTip().isEmpty()) return;
        Component original = event.getToolTip().getFirst();
        event.getToolTip().set(0, original.copy().append(
                Component.translatable("endeavours_soulstone.soul_sealed").withStyle(ChatFormatting.GOLD)));
    }
}
""")
(java/'SoulstoneMigration.java').write_text("""package cz.endeavours.soulstone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tunamods.customglint.common.CustomGlint;

/** Give active seals made with v0.1 the same glint as newly made ones. */
public final class SoulstoneMigration {
    private SoulstoneMigration() {}
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if ((player.tickCount % 40) != 0) return;
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
            changed |= stamp(player.getInventory().getItem(i));
        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); ++i)
            changed |= stamp(player.getEnderChestInventory().getItem(i));
        if (changed) player.inventoryMenu.broadcastChanges();
    }
    private static boolean stamp(ItemStack item) {
        if (item.isEmpty() || item.getOrDefault(Soulstone.SOUL_STATE.get(), 0) != 1
                || CustomGlint.has(item)) return false;
        CustomGlint.write(item, CustomGlint.VANILLA,
                new int[]{0xB3FFD65A, 0xC8FFBB38}, 0.75F, true, 1.0F, true);
        return true;
    }
}
""")
for locale in ('en_us','cs_cz'):
    p=root/f'src/main/resources/assets/endeavours_soulstone/lang/{locale}.json'
    j=json.loads(p.read_text()); j['endeavours_soulstone.soul_sealed']=' (Soul Sealed)'
    p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n')
(root/'THIRD_PARTY_NOTICES.md').write_text('Glint & Glamour API (TunaMods and contributors) used under the MIT license. Source: https://github.com/TunaMods/Glint-and-Glamour\n')
print('Soulstone v0.2.0 patch applied')
