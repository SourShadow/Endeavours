package dev.sourshadow.endeavoursships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jspecify.annotations.Nullable;

public final class ShipEntity extends Boat implements Container, MenuProvider, IEntityWithComplexSpawn {
    private static final int CARGO_SLOTS = 54;
    private static final int MAX_NETWORK_BLOCKS = 8192;

    private final List<ShipBlock> shipBlocks = new ArrayList<>();
    private final NonNullList<ItemStack> cargo = NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
    private float shipWidth = 1.0F;
    private float shipHeight = 1.0F;
    private int waterlessTicks;

    public ShipEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> EndeavoursShips.SHIP_HELM_ITEM.get());
    }

    public void initialize(List<ShipBlock> blocks, List<ItemStack> cargoItems) {
        shipBlocks.clear();
        shipBlocks.addAll(blocks);
        clearContent();
        for (int i = 0; i < Math.min(cargo.size(), cargoItems.size()); i++) {
            cargo.set(i, cargoItems.get(i).copy());
        }
        recalculateDimensions();
    }

    public List<ShipBlock> shipBlocks() {
        return List.copyOf(shipBlocks);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(shipWidth, shipHeight);
    }

    private void recalculateDimensions() {
        if (shipBlocks.isEmpty()) {
            shipWidth = 1.0F;
            shipHeight = 1.0F;
        } else {
            double maxHorizontal = 1.0;
            int maxY = 0;
            for (ShipBlock block : shipBlocks) {
                maxHorizontal = Math.max(maxHorizontal, Math.max(Math.abs(block.relativeX()), Math.abs(block.relativeZ())) * 2.0 + 1.0);
                maxY = Math.max(maxY, block.relativeY());
            }
            shipWidth = (float)Math.min(maxHorizontal, ShipConfig.MAX_DIMENSION.getAsInt());
            shipHeight = Math.max(1.0F, maxY + 1.0F);
        }
        refreshDimensions();
    }

    @Override
    protected int getMaxPassengers() {
        return Mth.clamp(1 + shipBlocks.size() / 64, 2, 8);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        int row = index / 3;
        int column = index % 3 - 1;
        double x = column * Math.min(1.5, shipWidth / 4.0);
        double z = row * -1.25;
        return new Vec3(x, Math.min(shipHeight, 2.0F) + 0.15, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public void tick() {
        float oldYaw = getYRot();
        super.tick();

        if (!level().isClientSide()) {
            double sizeFactor = Math.max(1.0, Math.sqrt(Math.max(1, shipBlocks.size()) / 16.0));
            double maxSpeed = ShipConfig.MAX_SPEED.getAsDouble() / Math.sqrt(sizeFactor);
            Vec3 movement = getDeltaMovement();
            double horizontal = movement.horizontalDistance();
            if (horizontal > maxSpeed) {
                double scale = maxSpeed / horizontal;
                setDeltaMovement(movement.x * scale, movement.y, movement.z * scale);
            }

            float maxTurn = (float)(3.0 / sizeFactor);
            float yawDelta = Mth.wrapDegrees(getYRot() - oldYaw);
            setYRot(oldYaw + Mth.clamp(yawDelta, -maxTurn, maxTurn));

            if (hasWaterContact()) {
                waterlessTicks = 0;
            } else if (++waterlessTicks > 4) {
                Vec3 stopped = getDeltaMovement();
                setDeltaMovement(0.0, Math.min(0.0, stopped.y), 0.0);
            }
        }
    }

    private boolean hasWaterContact() {
        BlockPos center = BlockPos.containing(getX(), getBoundingBox().minY - 0.1, getZ());
        int radius = Math.max(1, Mth.ceil(shipWidth / 2.0F) - 1);
        for (int x = -radius; x <= radius; x += Math.max(1, radius)) {
            for (int z = -radius; z <= radius; z += Math.max(1, radius)) {
                if (level().isWaterAt(center.offset(x, 0, z)) || level().isWaterAt(center.offset(x, -1, z))) {
                    return true;
                }
            }
        }
        return level().isWaterAt(center) || level().isWaterAt(center.below());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) {
            if (player.getItemInHand(hand).is(EndeavoursShips.SHIP_HELM_ITEM.get())) {
                if (level() instanceof ServerLevel serverLevel) {
                    tryDisassemble(serverLevel, player);
                }
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    public boolean tryDisassemble(ServerLevel level, Player player) {
        if (getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
            player.sendOverlayMessage(Component.translatable("message.endeavours_ships.stop_before_disassembly"));
            return false;
        }

        int quarterTurns = Mth.floor((Mth.wrapDegrees(getYRot()) + 45.0F) / 90.0F) & 3;
        List<BlockPos> targets = new ArrayList<>(shipBlocks.size());
        for (ShipBlock block : shipBlocks) {
            BlockPos target = targetPosition(block, quarterTurns);
            BlockState existing = level.getBlockState(target);
            if (!existing.canBeReplaced() && !existing.isAir()) {
                player.sendOverlayMessage(Component.translatable("message.endeavours_ships.disassembly_obstructed"));
                return false;
            }
            targets.add(target);
        }

        ejectPassengers();
        Rotation rotation = Rotation.BY_ID.apply(quarterTurns);
        List<Container> restoredContainers = new ArrayList<>();
        for (int i = 0; i < shipBlocks.size(); i++) {
            ShipBlock shipBlock = shipBlocks.get(i);
            BlockPos target = targets.get(i);
            BlockState restoredState = shipBlock.state().rotate(rotation);
            level.setBlock(target, restoredState, Block.UPDATE_ALL);

            shipBlock.blockEntityData().ifPresent(tag -> {
                CompoundTag movedTag = tag.copy();
                movedTag.putInt("x", target.getX());
                movedTag.putInt("y", target.getY());
                movedTag.putInt("z", target.getZ());
                BlockEntity restored = BlockEntity.loadStatic(target, restoredState, movedTag, level.registryAccess());
                if (restored != null) {
                    level.setBlockEntity(restored);
                }
            });

            BlockEntity blockEntity = level.getBlockEntity(target);
            if (blockEntity instanceof Container container) {
                container.clearContent();
                restoredContainers.add(container);
            }
        }
        restoreCargo(restoredContainers);
        discard();
        player.sendOverlayMessage(Component.translatable("message.endeavours_ships.disassembled"));
        return true;
    }

    private BlockPos targetPosition(ShipBlock block, int quarterTurns) {
        double x = block.relativeX();
        double z = block.relativeZ();
        double rotatedX;
        double rotatedZ;
        switch (quarterTurns) {
            case 1 -> {
                rotatedX = -z;
                rotatedZ = x;
            }
            case 2 -> {
                rotatedX = -x;
                rotatedZ = -z;
            }
            case 3 -> {
                rotatedX = z;
                rotatedZ = -x;
            }
            default -> {
                rotatedX = x;
                rotatedZ = z;
            }
        }
        return BlockPos.containing(getX() + rotatedX, Math.floor(getY()) + block.relativeY(), getZ() + rotatedZ);
    }

    private void restoreCargo(List<Container> containers) {
        int cargoSlot = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize() && cargoSlot < cargo.size(); slot++) {
                while (cargoSlot < cargo.size() && cargo.get(cargoSlot).isEmpty()) {
                    cargoSlot++;
                }
                if (cargoSlot >= cargo.size()) {
                    return;
                }
                container.setItem(slot, cargo.get(cargoSlot));
                cargo.set(cargoSlot, ItemStack.EMPTY);
                cargoSlot++;
            }
            container.setChanged();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            // VehicleEntity normally discards creative-broken vehicles. A ship represents many real blocks,
            // so discarding it would silently delete the structure and its cargo.
            destroy(level, source);
            return true;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void destroy(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        ejectPassengers();
        for (ShipBlock block : shipBlocks) {
            ItemStack drop = new ItemStack(block.state().getBlock().asItem());
            if (!drop.isEmpty()) {
                spawnAtLocation(level, drop);
            }
        }
        for (ItemStack stack : cargo) {
            if (!stack.isEmpty()) {
                spawnAtLocation(level, stack.copy());
            }
        }
        kill(level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("ShipBlocks", ShipBlock.CODEC.listOf(), shipBlocks);
        ContainerHelper.saveAllItems(output, cargo);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        shipBlocks.clear();
        shipBlocks.addAll(input.read("ShipBlocks", ShipBlock.CODEC.listOf()).orElse(List.of()));
        ContainerHelper.loadAllItems(input, cargo);
        recalculateDimensions();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(shipBlocks.size());
        for (ShipBlock block : shipBlocks) {
            buffer.writeVarInt(block.relativeX2());
            buffer.writeVarInt(block.relativeY());
            buffer.writeVarInt(block.relativeZ2());
            buffer.writeVarInt(Block.getId(block.state()));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_NETWORK_BLOCKS) {
            throw new IllegalArgumentException("Invalid ship block count: " + count);
        }
        shipBlocks.clear();
        for (int i = 0; i < count; i++) {
            int x2 = buffer.readVarInt();
            int y = buffer.readVarInt();
            int z2 = buffer.readVarInt();
            BlockState state = Block.stateById(buffer.readVarInt());
            if (state == null) {
                state = Blocks.AIR.defaultBlockState();
            }
            shipBlocks.add(new ShipBlock(x2, y, z2, state, Optional.empty()));
        }
        recalculateDimensions();
    }

    @Override
    public int getContainerSize() {
        return cargo.size();
    }

    @Override
    public boolean isEmpty() {
        return cargo.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return cargo.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(cargo, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(cargo, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        cargo.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 144.0;
    }

    @Override
    public void clearContent() {
        Collections.fill(cargo, ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.endeavours_ships.ship_cargo");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }
}
