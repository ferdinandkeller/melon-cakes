package dev.ferdinandkeller.meloncakes;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

public class MelonCakeBlock extends Block {
    public static final MapCodec<MelonCakeBlock> CODEC = createCodec(MelonCakeBlock::new);
    public static final int MAX_BITES = 3;
    public static final IntProperty BITES = Properties.BITES_MELON;

    private static final VoxelShape SHAPE_QUADRANT_1 = createCuboidShape(0, 0, 0, 8, 16, 8);
    private static final VoxelShape SHAPE_QUADRANT_2 = createCuboidShape(0, 0, 8, 8, 16, 16);
    private static final VoxelShape SHAPE_QUADRANT_3 = createCuboidShape(8, 0, 8, 16, 16, 16);
    private static final VoxelShape SHAPE_QUADRANT_4 = createCuboidShape(8, 0, 0, 16, 16, 8);
    private static final VoxelShape[] SHAPES_BY_BITES = {
        VoxelShapes.combineAndSimplify(
                VoxelShapes.empty(),
                VoxelShapes.union(SHAPE_QUADRANT_1, SHAPE_QUADRANT_2, SHAPE_QUADRANT_3, SHAPE_QUADRANT_4),
                BooleanBiFunction.OR
        ),
        VoxelShapes.combineAndSimplify(
                VoxelShapes.empty(),
                VoxelShapes.union(SHAPE_QUADRANT_2, SHAPE_QUADRANT_3, SHAPE_QUADRANT_4),
                BooleanBiFunction.OR
        ),
        VoxelShapes.combineAndSimplify(
                VoxelShapes.empty(),
                VoxelShapes.union(SHAPE_QUADRANT_3, SHAPE_QUADRANT_4),
                BooleanBiFunction.OR
        ),
        VoxelShapes.combineAndSimplify(
                VoxelShapes.empty(),
                VoxelShapes.union(SHAPE_QUADRANT_4),
                BooleanBiFunction.OR
        ),
    };

    @Override
    public MapCodec<MelonCakeBlock> getCodec() {
        return CODEC;
    }

    public MelonCakeBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(BITES, 0));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES_BY_BITES[state.get(BITES)];
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            if (tryEat(world, pos, state, player).isAccepted()) {
                return ActionResult.SUCCESS;
            }

            if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
                return ActionResult.CONSUME;
            }
        }

        return tryEat(world, pos, state, player);
    }

    protected static ActionResult tryEat(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!player.canConsume(false)) {
            return ActionResult.PASS;
        } else {
            player.incrementStat(Stats.EAT_CAKE_SLICE);
            player.getHungerManager().add(2, 0.1F);
            int i = (Integer)state.get(BITES);
            world.emitGameEvent(player, GameEvent.EAT, pos);
            if (i < MAX_BITES) {
                world.setBlockState(pos, state.with(BITES, i + 1), Block.NOTIFY_ALL);
            } else {
                world.removeBlock(pos, false);
                world.emitGameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            }
            return ActionResult.SUCCESS;
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }
}
