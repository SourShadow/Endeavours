package dev.sourshadow.endeavoursships.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class ShipRenderState extends EntityRenderState {
    public float yRot;
    public final List<RenderedShipBlock> blocks = new ArrayList<>();

    public record RenderedShipBlock(
            double x,
            double y,
            double z,
            MovingBlockRenderState block,
            ItemStackRenderState specialBlockItem) {
    }
}
