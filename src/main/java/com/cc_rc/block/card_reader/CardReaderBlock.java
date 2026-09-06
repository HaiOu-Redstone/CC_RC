package com.cc_rc.block.card_reader;

import com.cc_rc.Config;
import com.cc_rc.item.CardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;

/**
 * 刷卡机方块（A/B/C/D/E 五个等级，使用构造参数 grade 区分）
 *
 * 放置：仅墙面水平四方向（NORTH/SOUTH/EAST/WEST），碰撞箱 8x14x3（宽 8 / 高 14 / 厚 3，宽向左右各空 4 格）贴于墙
 * 状态：FACING（朝向，即玩家可见面朝向外）+ POWERED（on/off），默认 off
 * 交互：
 *   用正确等级的 CardItem 右键 → 刷卡机变为 on，向墙后方（贴墙的那侧）输出红石
 *     正确 = Config.isCardAccepted(readerGrade, card.getGrade())
 *     等级不足 / 不是 CardItem → 无效果（可添加错误提示音）
 * 红石：
 *   on 状态 → 向"后方"（贴墙面的反方向，即 FACING.getOpposite()，即墙所在方向）输出 15 信号
 *   （类似按钮：按钮按下向贴墙一侧输出红石）
 *   持续 Config.getCardReaderOnTicks() tick 后自动变回 off
 */
public class CardReaderBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /**
     * FACING 语义：刷卡机显示面朝向玩家的方向（=玩家视线正对显示面时，玩家的面向方向）。
     * wall（贴附墙面）= facing.getOpposite()。即：
     *   facing=NORTH  玩家站在方块北侧（z更小的位置），正面朝北；贴在 SOUTH 墙（z=16侧）→ SHAPE_SOUTH
     *   facing=SOUTH  玩家站在南侧，正面朝南；贴在 NORTH 墙（z=0侧）→ SHAPE_NORTH
     *   facing=EAST   玩家站在东侧，正面朝东；贴在 WEST 墙（x=0侧）→ SHAPE_WEST
     *   facing=WEST   玩家站在西侧，正面朝西；贴在 EAST 墙（x=16侧）→ SHAPE_EAST
     * 基准模型（y=0）：正面朝北，z 深度位于方块 0..3 区间 —— 对应 facing=SOUTH（贴在北墙，玩家从南看时需要y=180）
     * 因此在 blockstate 中 y-rot 映射：facing=NORTH→y=0（贴南墙，基准正面向北对玩家）
     *                                facing=SOUTH→y=180
     *                                facing=WEST→y=90
     *                                facing=EAST→y=270
     * 8x14x3 碰撞箱贴墙侧（宽 8 / 高 14 / 厚 3，宽向左右各空 4 格）：
     */

    // ==================================================================
    // ★★ 方向 / 位置 / 尺寸 可调参数区 —— 只改这一段，其余别动 ★★
    // 碰撞箱坐标范围 0~16（1 = 1/16 格）。X/Y 是平行墙面的两个轴，Z/X 视贴墙侧而定。
    //
    // --------- A. 尺寸与离墙间距 ---------
    // 平行墙面方向为"宽"（水平居中，左右各空留白），竖直方向为"高"，贴墙方向为"厚"。
    private static final double BOX_W_LO = 4;        // 面板宽度下边界（平行墙面）→ 宽 = 12-4 = 8 格（左右各空 4 格）
    private static final double BOX_W_HI = 12;       // 面板宽度上边界（平行墙面）→ 12
    private static final double BOX_H_LO = 1;        // 面板高度下边界 → 高 = 15-1 = 14 格（高度不变）
    private static final double BOX_H_HI = 15;       // 面板高度上边界 → 15
    private static final double BOX_THICKNESS = 3;   // 贴墙方向的厚度       → 3（3/16=0.1875 格）
    private static final double WALL_OFFSET = 0;     // 离墙间距（0=贴平墙；+1=离墙 1/16 格向外凸）
    //
    // --------- B. 整体贴墙侧翻转开关 ---------
    //   waste → OFF 表示 wall=NORTH 贴"坐标小侧"(z≈0~2)，wall=SOUTH 贴"坐标大侧"(z≈14~16)
    //   true 表示整体到对侧（N↔S、E↔W 翻转）
    //   ★若碰撞箱跑到了墙的对侧（该在墙左却挂在墙右），翻转此项即可★
    private static final boolean SHAPE_ON_BIG_SIDE = false;
    // ==================================================================

    public static final VoxelShape SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST;

    static {
        double wlo = BOX_W_LO, whi = BOX_W_HI, hlo = BOX_H_LO, hhi = BOX_H_HI, t = BOX_THICKNESS, o = WALL_OFFSET;
        if (SHAPE_ON_BIG_SIDE) {
            // wall 贴坐标大侧(z/x≈14~16)
            SHAPE_NORTH = Block.box(wlo, hlo, 16 - t - o, whi, hhi, 16 - o);
            SHAPE_SOUTH = Block.box(wlo, hlo, o, whi, hhi, o + t);
            SHAPE_EAST  = Block.box(o, hlo, wlo, o + t, hhi, whi);
            SHAPE_WEST  = Block.box(16 - t - o, hlo, wlo, 16 - o, hhi, whi);
        } else {
            // wall 贴坐标小侧(z/x≈0~2)
            SHAPE_NORTH = Block.box(wlo, hlo, o, whi, hhi, o + t);
            SHAPE_SOUTH = Block.box(wlo, hlo, 16 - t - o, whi, hhi, 16 - o);
            SHAPE_EAST  = Block.box(16 - t - o, hlo, wlo, 16 - o, hhi, whi);
            SHAPE_WEST  = Block.box(o, hlo, wlo, o + t, hhi, whi);
        }
    }

    // ==================================================================
    // ★★ 模型方向对照 —— 改模型方向请同步调整 blockstate JSON 的 y 值 ★★
    // 基准模型(JSON 内 y=0)正面朝北、元素 z 深度≈0~3。在 blockstates/card_reader_*.json 里：
    //   facing=north → y=0       facing=south → y=180       facing=east → y=270       facing=west → y=90
    //   ∠ 若某一面模型朝向反了，把对应状态 y 值 +180；左右错位则 east/west 的 y 互换(90↔270)。
    // ==================================================================

    private final char grade; // 'A' 'B' 'C' 'D' 'E'

    public CardReaderBlock(char grade, Properties properties) {
        super(properties);
        this.grade = grade;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    public char getGrade() {
        return grade;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        // FACING = 显示面朝玩家方向 == 墙面点击面 clickedFace。
        // 玩家从 clickedFace 方向点击 wallPos 的 clickedFace 表面 →
        // 放置 pos = wallPos.relative(clickedFace)，玩家在 pos 的 clickedFace 方向，屏幕朝 clickedFace 方向对玩家。
        if (clicked.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, clicked);
        }
        // fallback：玩家水平朝向（面朝 = 屏幕朝玩家方向）
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // wallDir = FACING.getOpposite()：碰撞箱贴在 wallDir 方向的墙面上
        return switch (state.getValue(FACING).getOpposite()) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default -> SHAPE_SOUTH;
        };
    }

    /**
     * 与原版 FaceAttachedHorizontalDirectionalBlock#getConnectedDirection 完全同一语义：
     * 配合 updateNeighbours 中 getOpposite() 使用后，wallPos = pos.relative(getConnectedDirection(state).getOpposite())
     * CardReader 事实 wallPos = pos.relative(FACING.getOpposite()) → getConnectedDirection = FACING 本身
     * （与 LeverBlock WALL case 返回 FACING 完全一致）
     */
    public static Direction getConnectedDirection(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof CardItem card)) {
            return InteractionResult.PASS;
        }

        char cardGrade = card.getGrade();
        if (Config.isCardAccepted(this.grade, cardGrade)) {
            // 刷卡成功 → 切换到 on，刷新计时
            BlockState next = state.setValue(POWERED, true);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
                    0.3F, 0.8F);
            updateNeighbours(next, level, pos);
            // 提交计时：到 on_ticks 后 tick() 触发 off
            level.scheduleTick(pos, this, Config.getCardReaderOnTicks());
            return InteractionResult.CONSUME;
        } else {
            // 卡等级不够 → 错误音（轻微）
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS,
                    0.2F, 1.2F);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, false);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS,
                    0.3F, 0.6F);
            updateNeighbours(next, level, pos);
        }
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        // LeverBlock字节码金标准：第二次刷新 pos.relative(getConnectedDirection(state).getOpposite())
        Direction connected = getConnectedDirection(state);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(connected.getOpposite()), this);
    }

    // ---------- 红石输出 ----------
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 向后方（贴墙方向）输出强红石信号（15），其他相邻方向输出弱信号。
     * 类似原版木/石按钮：按钮按下时按钮所在方块向所有方向给出弱信号，
     * 同时向贴墙方向给出强信号，使墙的另一侧的红石线/中继器被点亮。
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 原版ButtonBlock：弱信号全方向=15（侧邻红石线直接亮）
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 强信号只向"连接方向"输出（与参考方块 LeverBlock/ButtonBlock 一致）：
        // 红石信号相关方法的方向参数是"反"的——实际输出的是 direction.getOpposite() 方向，
        // 故条件写成 getConnectedDirection(state) == direction（即 FACING == direction），
        // 实际强信号流向 FACING.getOpposite() = 墙所在方向，从而强充能墙方块，
        // 使墙另一侧的红石线/中继器被点亮（信号穿墙）。
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(POWERED)) {
                updateNeighbours(state, level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
