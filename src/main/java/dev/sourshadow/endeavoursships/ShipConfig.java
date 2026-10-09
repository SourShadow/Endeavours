package dev.sourshadow.endeavoursships;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShipConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_BLOCKS = BUILDER
            .comment("Minimum number of connected ship blocks, including the helm.")
            .defineInRange("minimumBlocks", 8, 2, 256);

    public static final ModConfigSpec.IntValue MAX_BLOCKS = BUILDER
            .comment("Maximum number of connected blocks in one ship.")
            .defineInRange("maximumBlocks", 1024, 16, 8192);

    public static final ModConfigSpec.IntValue MAX_DIMENSION = BUILDER
            .comment("Maximum width, height, or length of an assembled ship.")
            .defineInRange("maximumDimension", 32, 4, 128);

    public static final ModConfigSpec.DoubleValue MAX_SPEED = BUILDER
            .comment("Maximum horizontal speed in blocks per tick for a small ship.")
            .defineInRange("maximumSpeed", 0.32, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue WATER_CONTACT_RATIO = BUILDER
            .comment("Required fraction of hull columns touching water during assembly.")
            .defineInRange("minimumWaterContactRatio", 0.25, 0.05, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ShipConfig() {
    }
}
