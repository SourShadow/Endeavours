package dev.sourshadow.endeavoursships.client;

import dev.sourshadow.endeavoursships.EndeavoursShips;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = EndeavoursShips.MOD_ID, value = Dist.CLIENT)
public final class EndeavoursShipsClient {
    private EndeavoursShipsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EndeavoursShips.SHIP.get(), ShipRenderer::new);
    }
}
