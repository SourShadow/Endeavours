package dev.sourshadow.endeavoursships;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ShipAssembler {
    public static final TagKey<Block> SHIP_BLOCKS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(EndeavoursShips.MOD_ID, "ship_blocks"));

    private ShipAssembler() {
    }

    public static boolean assemble(ServerLevel level, BlockPos helmPos, Player player) {
        ScanResult scan = scan(level, helmPos);
        if (scan.error != null) {
            player.sendOverlayMessage(Component.translatable(scan.error));
            return false;
        }

        List<ItemStack> cargo = collectCargo(level, scan.positions);
        if (cargo == null) {
            player.sendOverlayMessage(Component.translatable("message.endeavours_ships.too_much_cargo"));
            return false;
        }

        double centerX = (scan.minX + scan.maxX + 1) / 2.0;
        double centerZ = (scan.minZ + scan.maxZ + 1) / 2.0;
        List<ShipBlock> shipBlocks = new ArrayList<>(scan.positions.size());
        for (BlockPos pos : scan.positions) {
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            Optional<CompoundTag> data = Optional.empty();
            if (blockEntity != null) {
                CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
                if (blockEntity instanceof Container) {
                    tag.remove("Items");
                }
                data = Optional.of(tag);
            }

            int x2 = (int)Math.round((pos.getX() + 0.5 - centerX) * 2.0);
            int z2 = (int)Math.round((pos.getZ() + 0.5 - centerZ) * 2.0);
            shipBlocks.add(new ShipBlock(x2, pos.getY() - scan.minY, z2, state, data));
        }

        ShipEntity ship = EndeavoursShips.SHIP.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ship == null) {
            player.sendOverlayMessage(Component.translatable("message.endeavours_ships.assembly_failed"));
            return false;
        }
        ship.initialize(shipBlocks, cargo);
        ship.setInitialPos(centerX, scan.minY, centerZ);
        ship.setYRot(0.0F);
        if (!level.addFreshEntity(ship)) {
            player.sendOverlayMessage(Component.translatable("message.endeavours_ships.assembly_failed"));
            return false;
        }

        for (BlockPos pos : scan.positions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                container.clearContent();
            }
        }
        for (BlockPos pos : scan.positions) {
            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
        }

        player.sendOverlayMessage(Component.translatable("message.endeavours_ships.assembled", shipBlocks.size()));
        player.startRiding(ship);
        return true;
    }

    private static List<ItemStack> collectCargo(ServerLevel level, Set<BlockPos> positions) {
        List<ItemStack> cargo = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        cargo.add(stack.copy());
                        if (cargo.size() > 54) {
                            return null;
                        }
                    }
                }
            }
        }
        return cargo;
    }

    private static ScanResult scan(ServerLevel level, BlockPos helmPos) {
        int maximum = ShipConfig.MAX_BLOCKS.getAsInt();
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(helmPos.immutable());

        int minX = helmPos.getX();
        int minY = helmPos.getY();
        int minZ = helmPos.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;
        int helms = 0;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (found.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(SHIP_BLOCKS)) {
                continue;
            }
            found.add(pos);
            if (found.size() > maximum) {
                return ScanResult.error("message.endeavours_ships.too_many_blocks");
            }
            if (state.is(EndeavoursShips.SHIP_HELM)) {
                helms++;
            }

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!found.contains(next) && level.getBlockState(next).is(SHIP_BLOCKS)) {
                    queue.addLast(next.immutable());
                }
            }
        }

        if (found.size() < ShipConfig.MIN_BLOCKS.getAsInt()) {
            return ScanResult.error("message.endeavours_ships.too_few_blocks");
        }
        if (helms != 1) {
            return ScanResult.error("message.endeavours_ships.one_helm_required");
        }
        int maxDimension = ShipConfig.MAX_DIMENSION.getAsInt();
        if (maxX - minX + 1 > maxDimension || maxY - minY + 1 > maxDimension || maxZ - minZ + 1 > maxDimension) {
            return ScanResult.error("message.endeavours_ships.too_large");
        }

        Map<Long, Integer> columnBottoms = new HashMap<>();
        for (BlockPos pos : found) {
            long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
            columnBottoms.merge(column, pos.getY(), Math::min);
        }
        int waterColumns = 0;
        for (Map.Entry<Long, Integer> entry : columnBottoms.entrySet()) {
            BlockPos columnPos = BlockPos.of(entry.getKey());
            BlockPos bottom = new BlockPos(columnPos.getX(), entry.getValue(), columnPos.getZ());
            BlockState bottomState = level.getBlockState(bottom);
            BlockPos below = bottom.below();
            boolean water = bottomState.getFluidState().is(FluidTags.WATER)
                    || level.getFluidState(below).is(FluidTags.WATER)
                    || level.getFluidState(below.below()).is(FluidTags.WATER);
            if (water) {
                waterColumns++;
            } else {
                BlockState support = level.getBlockState(below);
                if (!support.isAir() && !support.canBeReplaced()) {
                    return ScanResult.error("message.endeavours_ships.grounded");
                }
            }
        }
        double ratio = waterColumns / (double)Math.max(1, columnBottoms.size());
        if (ratio < ShipConfig.WATER_CONTACT_RATIO.getAsDouble()) {
            return ScanResult.error("message.endeavours_ships.not_enough_water");
        }

        return new ScanResult(found, minX, minY, minZ, maxX, maxY, maxZ, null);
    }

    private record ScanResult(
            Set<BlockPos> positions,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            String error) {
        private static ScanResult error(String key) {
            return new ScanResult(Set.of(), 0, 0, 0, 0, 0, 0, key);
        }
    }
}
