package dev.sourshadow.endeavoursships;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime integration tests registered only when NeoForge enables GameTest. */
final class EndeavoursShipGameTests {
    private EndeavoursShipGameTests() {
    }

    static void register(RegisterGameTestsEvent event) {
        Identifier id = Identifier.fromNamespaceAndPath(EndeavoursShips.MOD_ID, "assemble_cargo_and_disassemble");
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(EndeavoursShips.MOD_ID, "default"));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true);
        event.registerTest(id, new ShipAssemblyTest(data));
    }

    private static final class ShipAssemblyTest extends GameTestInstance {
        private ShipAssemblyTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            BlockPos helm = new BlockPos(2, 3, 2);
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    helper.setBlock(x, 1, z, Blocks.WATER);
                    helper.setBlock(x, 2, z, Blocks.OAK_PLANKS);
                }
            }
            helper.setBlock(new BlockPos(1, 2, 1), Blocks.CHEST);
            helper.setBlock(helm, EndeavoursShips.SHIP_HELM.get());

            BlockEntity originalChest = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
            if (!(originalChest instanceof Container originalContainer)) {
                helper.fail("Test chest did not create a container");
                return;
            }
            originalContainer.setItem(5, new ItemStack(Items.COMPASS, 3));

            FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
            boolean assembled = ShipAssembler.assemble(helper.getLevel(), helper.absolutePos(helm), player);
            if (!assembled) {
                helper.fail("Valid test vessel failed to assemble");
                return;
            }
            BlockPos absoluteHelm = helper.absolutePos(helm);
            List<ShipEntity> ships = helper.getLevel().getEntitiesOfClass(
                    ShipEntity.class,
                    new AABB(absoluteHelm).inflate(8.0));
            if (ships.size() != 1) {
                helper.fail("Expected one assembled ship, found " + ships.size());
                return;
            }
            ShipEntity ship = ships.getFirst();
            if (ship.shipBlocks().size() != 10 || ship.getItem(0).getCount() != 3 || !ship.getItem(0).is(Items.COMPASS)) {
                helper.fail("Assembled ship did not preserve its blocks and cargo");
                return;
            }

            if (!ship.tryDisassemble(helper.getLevel(), player)) {
                helper.fail("Stationary test vessel failed to disassemble");
                return;
            }
            BlockState restoredHelm = helper.getBlockState(helm);
            BlockEntity restoredChest = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
            if (!restoredHelm.is(EndeavoursShips.SHIP_HELM)
                    || !(restoredChest instanceof Container restoredContainer)
                    || restoredContainer.countItem(Items.COMPASS) != 3) {
                helper.fail("Disassembly did not restore the helm and cargo");
                return;
            }
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            // Programmatically registered tests are never decoded; use the vanilla function codec as their type marker.
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Endeavours Ships integration test");
        }
    }
}
