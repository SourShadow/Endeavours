package dev.sourshadow.endeavoursships;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record ShipBlock(int relativeX2, int relativeY, int relativeZ2, BlockState state, Optional<CompoundTag> blockEntityData) {
    public static final Codec<ShipBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x2").forGetter(ShipBlock::relativeX2),
            Codec.INT.fieldOf("y").forGetter(ShipBlock::relativeY),
            Codec.INT.fieldOf("z2").forGetter(ShipBlock::relativeZ2),
            BlockState.CODEC.fieldOf("state").forGetter(ShipBlock::state),
            CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(ShipBlock::blockEntityData)
    ).apply(instance, ShipBlock::new));

    public double relativeX() {
        return relativeX2 / 2.0;
    }

    public double relativeZ() {
        return relativeZ2 / 2.0;
    }
}
