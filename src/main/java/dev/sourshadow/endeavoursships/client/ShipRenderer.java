package dev.sourshadow.endeavoursships.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sourshadow.endeavoursships.ShipBlock;
import dev.sourshadow.endeavoursships.ShipEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;

public final class ShipRenderer extends EntityRenderer<ShipEntity, ShipRenderState> {
    private final ItemModelResolver itemModelResolver;

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
        shadowRadius = 1.5F;
    }

    @Override
    public ShipRenderState createRenderState() {
        return new ShipRenderState();
    }

    @Override
    public void extractRenderState(ShipEntity entity, ShipRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.blocks.clear();
        BlockPos entityPos = entity.blockPosition();

        for (ShipBlock shipBlock : entity.shipBlocks()) {
            MovingBlockRenderState moving = new MovingBlockRenderState();
            BlockPos lightPos = BlockPos.containing(
                    entity.getX() + shipBlock.relativeX(),
                    entity.getY() + shipBlock.relativeY(),
                    entity.getZ() + shipBlock.relativeZ());
            moving.randomSeedPos = lightPos;
            moving.blockPos = lightPos;
            moving.blockState = shipBlock.state();
            if (entity.level() instanceof ClientLevel clientLevel) {
                moving.biome = clientLevel.getBiome(entityPos);
                moving.cardinalLighting = clientLevel.cardinalLighting();
                moving.lightEngine = clientLevel.getLightEngine();
            }
            ItemStackRenderState specialBlockItem = null;
            if (shipBlock.state().getRenderShape() != RenderShape.MODEL) {
                ItemStack stack = new ItemStack(shipBlock.state().getBlock().asItem());
                if (!stack.isEmpty()) {
                    specialBlockItem = new ItemStackRenderState();
                    itemModelResolver.updateForNonLiving(
                            specialBlockItem,
                            stack,
                            ItemDisplayContext.NONE,
                            entity);
                }
            }
            state.blocks.add(new ShipRenderState.RenderedShipBlock(
                    shipBlock.relativeX(),
                    shipBlock.relativeY(),
                    shipBlock.relativeZ(),
                    moving,
                    specialBlockItem));
        }
    }

    @Override
    public void submit(
            ShipRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        for (ShipRenderState.RenderedShipBlock shipBlock : state.blocks) {
            poseStack.pushPose();
            if (shipBlock.block().blockState.getRenderShape() == RenderShape.MODEL) {
                poseStack.translate(shipBlock.x() - 0.5, shipBlock.y(), shipBlock.z() - 0.5);
                submitNodeCollector.submitMovingBlock(poseStack, shipBlock.block());
            } else if (shipBlock.specialBlockItem() != null) {
                // Chests and banners use special item models because their world renderers require a stationary block entity.
                poseStack.translate(shipBlock.x(), shipBlock.y() + 0.5, shipBlock.z());
                shipBlock.specialBlockItem().submit(
                        poseStack,
                        submitNodeCollector,
                        state.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        state.outlineColor);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
