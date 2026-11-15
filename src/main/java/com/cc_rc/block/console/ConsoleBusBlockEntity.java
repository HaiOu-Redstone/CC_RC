package com.cc_rc.block.console;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 控制台总线方块实体：多方块结构的「唯一数据源」。
 *
 * 只保存结构元数据（状态、最小角、三边长度、格数、上次校验失败原因），
 * 成员方块由长方体范围推导，不需要成员列表。
 *
 * 三态：
 *  - UNFORMED 未成型：结构未校验或已失效；
 *  - PARTIAL  部分成型：方块结构合法，组件/装饰可自由编辑；
 *  - FULL     完全成型：锁定，组件与结构均不可改动，交互组件生效。
 *
 * 另外保存结构表面的文字列表（见 {@link TextEntry}）：文字坐标是相对该表面中心的
 * 像素偏移，因此结构尺寸变化时文字会随表面一起移动。
 *
 * 同步：结构或文字变化后 {@link #sync()} 同时刷新总线自身与全部成员的客户端数据。
 */
public class ConsoleBusBlockEntity extends BlockEntity {

    /** 单个结构允许的表面文字数量上限（防止异常数据拖慢渲染）。 */
    public static final int MAX_TEXTS = 256;

    /** applyTextEdit 的返回状态。 */
    public static final int TEXT_OK = 0;
    /** 结构尚未进入部分成型。 */
    public static final int TEXT_NOT_FORMED = 1;
    /** 结构已完全成型（锁定）。 */
    public static final int TEXT_LOCKED = 2;
    /** 点击位置不在结构表面范围内。 */
    public static final int TEXT_OUTSIDE = 3;
    /** 文字数量已达上限。 */
    public static final int TEXT_LIMIT = 4;

    /** 单个结构允许的表面组件数量上限。 */
    public static final int MAX_COMPS = 256;

    /** 组件操作返回状态（与 TEXT_* 分开，便于提示不同的失败原因）。 */
    public static final int COMP_OK = 0;
    public static final int COMP_NOT_FORMED = 1;
    public static final int COMP_LOCKED = 2;
    public static final int COMP_OUTSIDE = 3;
    public static final int COMP_LIMIT = 4;
    /** 该位置已被其它组件占用。 */
    public static final int COMP_DUP_POS = 5;
    /** 组件代号重复。 */
    public static final int COMP_DUP_CODE = 6;
    /** 组件代号格式非法（只允许英文字母 / 数字 / 下划线，1~32 位）。 */
    public static final int COMP_BAD_CODE = 7;
    /** 找不到对应组件（可能已被删除）。 */
    public static final int COMP_NOT_FOUND = 8;
    /** 操作条件不满足（例如钥匙与钥匙开关不匹配 / 组件已插着钥匙）。 */
    public static final int COMP_INVALID = 9;

    /** 单个结构允许的绘图图形数量上限（所有表面合计）。 */
    public static final int MAX_DRAW_SHAPES = 512;

    /** 绘图操作返回状态。 */
    public static final int DRAW_OK = 0;
    /** 结构尚未进入部分成型。 */
    public static final int DRAW_NOT_FORMED = 1;
    /** 结构已完全成型（锁定）。 */
    public static final int DRAW_LOCKED = 2;
    /** 坐标超出表面范围。 */
    public static final int DRAW_OUTSIDE = 3;
    /** 图形数量已达上限。 */
    public static final int DRAW_LIMIT = 4;
    /** 找不到对应图形（可能已被删除）。 */
    public static final int DRAW_NOT_FOUND = 5;
    /** 图形参数非法（未知类型 / 预设等）。 */
    public static final int DRAW_BAD_DATA = 6;

    /** 组件代号允许的字符集与长度。 */
    private static final java.util.regex.Pattern COMP_CODE_PATTERN =
            java.util.regex.Pattern.compile("[A-Za-z0-9_]{1,32}");

    /**
     * 变量槽位硬上限。
     *
     * 字节码地址使用 1 字节（0~255），因此最多 256 个槽位；
     * 实际可用数量由配置文件 console_program.max_vars 决定（默认 64）。
     */
    public static final int VAR_SLOTS_MAX = 256;
    /** 0 号槽位固定为「程序运行间隔」（只读，单位 tick，0 = 停止运行）。 */
    public static final int VAR_SLOT_PERIOD = 0;

    /** 当前配置允许的变量槽位总数（已夹紧到 2~256）。 */
    public static int varSlots() {
        return com.cc_rc.Config.getConsoleProgramMaxVars();
    }

    /** 运行间隔允许的最大值（tick），避免误输入极大值。 */
    public static final int MAX_PERIOD_TICKS = 72000;
    /** 变量名允许的字符集与长度（与组件代号保持一致）。 */
    private static final java.util.regex.Pattern VAR_NAME_PATTERN =
            java.util.regex.Pattern.compile("[A-Za-z0-9_]{1,32}");

    /** 变量操作返回状态。 */
    public static final int VAR_OK = 0;
    /** 槽位越界，或试图写入 0 号只读槽位。 */
    public static final int VAR_BAD_SLOT = 1;
    /** 该槽位已有变量。 */
    public static final int VAR_DUP_SLOT = 2;
    /** 变量名格式非法。 */
    public static final int VAR_BAD_NAME = 3;
    /** 变量名重复。 */
    public static final int VAR_DUP_NAME = 4;
    /** 绑定的组件不存在（可能已被删除）。 */
    public static final int VAR_COMP_NOT_FOUND = 5;
    /** 绑定的组件类型与变量类型不匹配。 */
    public static final int VAR_TYPE_MISMATCH = 6;
    /** 该变量只读（0 号槽位或按钮面板）。 */
    public static final int VAR_READ_ONLY = 7;
    /** 该槽位没有变量。 */
    public static final int VAR_NOT_FOUND = 8;
    /** 服务端校验失败 / 非法调用。 */
    public static final int VAR_INVALID = 9;
    /** 未绑定设备的变量使用了某个设备名称（会造成歧义）。 */
    public static final int VAR_NAME_IS_COMP = 10;
    /** 该变量仍被程序行引用，不能删除。 */
    public static final int VAR_IN_USE = 11;

    /** 程序行数量上限（固定 64 行，8 行一页共 8 页）。 */
    public static final int MAX_PROGRAM_LINES = 64;

    /** 程序保存返回状态（与变量返回码分开，提示不同的失败原因）。 */
    public static final int PROG_LIMIT = 20;
    /** 目标变量未定义 / 不可写。 */
    public static final int PROG_BAD_TARGET = 21;
    /** 行条件不是布尔变量。 */
    public static final int PROG_BAD_COND = 22;
    /** 表达式无效（引用未定义变量或运算类型非法）。 */
    public static final int PROG_BAD_EXPR = 23;
    /** 表达式类型与目标变量不一致。 */
    public static final int PROG_TYPE_MISMATCH = 24;
    /** 表达式嵌套深度超过上限。 */
    public static final int PROG_TOO_DEEP = 25;

    /** 配置加载返回状态（保存 / 加载「总线配置」时使用）。 */
    public static final int CFG_OK = 0;
    /** 文件内容不是合法的配置 JSON。 */
    public static final int CFG_BAD_JSON = 1;
    /** 结构未部分成型（完全成型或未成型都不能加载）。 */
    public static final int CFG_NOT_FORMED = 2;
    /** 结构尺寸与配置文件不一致。 */
    public static final int CFG_SIZE_MISMATCH = 3;
    /** 总线朝向与配置文件不一致（控制柜与结构的相对方向不同）。 */
    public static final int CFG_FACING_MISMATCH = 4;
    /** 组件 / 文字 / 变量 / 程序行数量超过上限。 */
    public static final int CFG_LIMIT = 5;
    /** 配置内容非法（类型、代号、变量引用等校验失败）。 */
    public static final int CFG_INVALID = 6;

    /** 变量类型：布尔与浮点两种。 */
    public enum VarType {
        BOOL,
        FLOAT
    }

    /**
     * 弧形仪表的可显示范围。
     *
     * 计算层保存原始浮点值，不做截断；渲染时把指针状态限制在这个范围内，
     * 允许玩家看出轻微超量程，又不会让指针跑出基座模型。
     */
    private static final long GAUGE_ARC_DISPLAY_MIN = -5L;
    private static final long GAUGE_ARC_DISPLAY_MAX = 105L;

    /** 同步指示器亮灯时允许的最大指针偏角（度，以正上方为 0）。 */
    private static final float SYNCHRO_LAMP_ANGLE_DEG = 15.0F;

    /** 警报器发声间隔（tick）：报警中且未静音时按该间隔重复播放占位音效。 */
    private static final long ALARM_SOUND_INTERVAL = 10L;

    /** 警报器按钮按下反馈的持续时长（tick）。 */
    private static final long ALARM_PRESS_TICKS = 5L;

    /** 成型状态。 */
    public enum State {
        UNFORMED,
        PARTIAL,
        FULL
    }

    private State formState = State.UNFORMED;
    /** 结构最小角（未成型时为 null）。 */
    private BlockPos origin;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private int memberCount;
    /** 上次校验失败原因（无错误时为 NONE）。 */
    private ConsoleStructureHelper.Error error = ConsoleStructureHelper.Error.NONE;
    /**
     * 识别到的结构颜色（成型时写入，未成型时为默认色）。
     * 同步到方块状态 COLOR 用于切换总线 / 辅助计算机贴图，并供低频巡检校验结构同色。
     */
    private ConsoleColor structureColor = ConsoleColor.DEFAULT;

    /**
     * 上一次成功成型时的结构范围（结构失效后仍保留）。
     *
     * 仅用于「重新成型」时把组件 / 表面文字的坐标换算到新参考系：
     * 表面坐标以表面中心为原点，结构扩大或缩减都会让中心移动，
     * 不做换算的话组件会跟着整体平移。失效（origin 清空）后仍保留这份范围，
     * 才能覆盖「先拆方块、再补方块重新成型」的情况。
     */
    private BlockPos lastOrigin;
    private int lastSizeX;
    private int lastSizeY;
    private int lastSizeZ;

    /** 结构表面文字列表（客户端用于渲染，服务端用于编辑）。 */
    private final List<TextEntry> texts = new ArrayList<>();
    /** 下一条文字的自增 id（保证编辑时能稳定引用同一条文字）。 */
    private int nextTextId = 1;

    /**
     * 每个表面的绘图图形（与文字 / 组件一样保存在总线里，随同步标签下发客户端）。
     * 图形自带图层号，绘制时按图层从低到高排列。
     */
    private final Map<Direction, List<ConsoleShape>> drawShapes = new EnumMap<>(Direction.class);
    /** 下一个绘图图形的自增 id。 */
    private int nextDrawId = 1;

    /** 结构表面组件列表（保存模型、代号、颜色与运行状态）。 */
    private final List<CompEntry> comps = new ArrayList<>();
    /** 下一个组件的自增 id。 */
    private int nextCompId = 1;
    /**
     * 安全按钮盖子的自动关闭时刻（组件 id → gameTime）。
     * 只存在于运行期（不写 NBT）：加载后按当前时间 + coverTicks 重建，
     * 避免存档关闭期间时间流逝导致重进世界时盖子立刻关闭。
     */
    private final Map<Integer, Long> coverDeadlines = new HashMap<>();

    /** 程序运行间隔（tick，0 = 停止）；对应变量表 0 号只读槽位。 */
    private float programPeriod = 0.0F;
    /** 变量定义表（下标即地址；0 号槽位保留给运行间隔，不存定义）。 */
    private final VarDef[] varDefs = new VarDef[VAR_SLOTS_MAX];
    /** 浮点变量值：变量表是最终存储，aux 只保存定义不保存值。 */
    private final float[] varFloats = new float[VAR_SLOTS_MAX];
    /** 布尔变量值。 */
    private final boolean[] varBools = new boolean[VAR_SLOTS_MAX];
    /** 程序行（下标即行号，null 表示空行；启用状态由行自身保存）。 */
    private final ProgramIR.Line[] programLines = new ProgramIR.Line[MAX_PROGRAM_LINES];
    /** 编译后的字节码（运行时缓存，不持久化；程序表或变量表变化后重新编译）。 */
    private ProgramVM.Compiled programCode;
    /** 程序表或变量表发生结构变化，需要重新编译。 */
    private boolean programDirty = true;
    /** 运行间隔计数器（服务端），累计到 period 后执行一次。 */
    private int programTimer;
    /** 影子数组：执行期只读写这里，执行完成后一次性提交回变量表与组件。 */
    private final float[] shadowFloats = new float[VAR_SLOTS_MAX];
    private final boolean[] shadowBools = new boolean[VAR_SLOTS_MAX];
    /** 最近一次执行的运行期警告（仅内存保存，并随方块实体同步给客户端显示）。 */
    private final List<ProgramVM.Warning> runWarnings = new ArrayList<>();
    /**
     * CC 接口的线程安全桥：Lua 线程只读写快照与操作队列，
     * 主线程每 tick 批量应用（见 {@link ConsoleCcBridge}）。
     */
    private final ConsoleCcBridge ccBridge = new ConsoleCcBridge();

    public ConsoleBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE_BUS_BE.get(), pos, state);
    }

    public State getFormState() {
        return this.formState;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    /** 识别到的结构颜色（未成型时为默认色）。 */
    public ConsoleColor getStructureColor() {
        return this.structureColor;
    }

    public int getSizeX() {
        return this.sizeX;
    }

    public int getSizeY() {
        return this.sizeY;
    }

    public int getSizeZ() {
        return this.sizeZ;
    }

    public int getMemberCount() {
        return this.memberCount;
    }

    public ConsoleStructureHelper.Error getError() {
        return this.error;
    }

    /** 结构表面文字列表（只读用途：渲染 / 客户端命中检测）。 */
    public List<TextEntry> getTexts() {
        return this.texts;
    }

    /** 按 id 查找文字，找不到返回 null。 */
    public TextEntry findText(int id) {
        for (TextEntry entry : this.texts) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    /** 结构表面组件列表（只读用途：渲染 / 客户端命中检测）。 */
    public List<CompEntry> getComps() {
        return this.comps;
    }

    /** 按 id 查找组件，找不到返回 null。 */
    public CompEntry findComp(int id) {
        for (CompEntry entry : this.comps) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    /** 某个表面的绘图图形列表（客户端渲染 / 命中检测；没有图形时返回空列表）。 */
    public List<ConsoleShape> getDrawShapes(Direction face) {
        List<ConsoleShape> list = this.drawShapes.get(face);
        return list == null ? List.of() : list;
    }

    /** 全部表面的绘图图形总数。 */
    public int countDrawShapes() {
        int total = 0;
        for (List<ConsoleShape> list : this.drawShapes.values()) {
            total += list.size();
        }
        return total;
    }

    /** 按 id 查找绘图图形，找不到返回 null。 */
    public ConsoleShape findDrawShape(int id) {
        for (List<ConsoleShape> list : this.drawShapes.values()) {
            for (ConsoleShape shape : list) {
                if (shape.id == id) {
                    return shape;
                }
            }
        }
        return null;
    }

    /**
     * 命中检测：返回该表面 (u, v) 处最上层的绘图图形。
     *
     * 图层数字大的优先；同一图层内后添加的优先。
     * 线段按「点到线段距离」判定，其它类型按包围盒判定（含 1 像素容差）。
     */
    public ConsoleShape findDrawShapeAt(Direction face, float u, float v) {
        List<ConsoleShape> list = this.drawShapes.get(face);
        if (list == null) {
            return null;
        }
        for (int layer = ConsoleShape.LAYERS - 1; layer >= 0; layer--) {
            for (int i = list.size() - 1; i >= 0; i--) {
                ConsoleShape shape = list.get(i);
                if (shape.layer == layer && drawHit(shape, u, v)) {
                    return shape;
                }
            }
        }
        return null;
    }

    /** 单个图形是否命中表面像素点。 */
    private static boolean drawHit(ConsoleShape shape, float u, float v) {
        float tolerance = 1.0F;
        if (shape.type == ConsoleShapeType.LINE) {
            return distanceToSegment(u, v, shape.u1, shape.v1, shape.u2, shape.v2)
                    <= shape.width / 2.0F + tolerance;
        }
        float[] bounds = shape.bounds();
        return u >= bounds[0] - tolerance && u <= bounds[2] + tolerance
                && v >= bounds[1] - tolerance && v <= bounds[3] + tolerance;
    }

    /** 点到线段的最短距离（像素）。 */
    private static float distanceToSegment(float px, float py, float x1, float y1,
                                           float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 0.0001F) {
            return (float) Math.hypot(px - x1, py - y1);
        }
        float t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        t = Math.max(0.0F, Math.min(1.0F, t));
        return (float) Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    /**
     * 新增一个绘图图形（服务端调用），成功时返回新图形（含 id），失败返回 null。
     *
     * 只允许在「部分成型」时编辑；坐标必须落在该表面矩形内。
     * 预设图案按服务端白名单校验，防止客户端引用任意贴图。
     */
    public ConsoleShape applyDrawAdd(Direction face, int layer, ConsoleShapeType type,
                                     int color, float width, float u1, float v1,
                                     float u2, float v2, String preset, float rotation,
                                     float scale) {
        if (this.level == null || this.level.isClientSide
                || this.formState != State.PARTIAL || this.origin == null
                || face == null || type == null) {
            return null;
        }
        if (countDrawShapes() >= MAX_DRAW_SHAPES) {
            return null;
        }
        Direction busFacing = getBusFacing();
        if (!ConsoleSurface.contains(face, busFacing, this.sizeX, this.sizeY, this.sizeZ, u1, v1)
                || !ConsoleSurface.contains(face, busFacing, this.sizeX, this.sizeY, this.sizeZ,
                u2, v2)) {
            return null;
        }
        ConsoleShape created = buildDrawShape(0, face, layer, type, color, width,
                u1, v1, u2, v2, preset, rotation, scale);
        if (created == null) {
            return null;
        }
        ConsoleShape shape = new ConsoleShape(this.nextDrawId++, face, created.layer,
                created.type, created.color, created.width, created.u1, created.v1,
                created.u2, created.v2, created.preset, created.rotation, created.scale);
        this.drawShapes.computeIfAbsent(face, key -> new ArrayList<>()).add(shape);
        sync();
        return shape;
    }

    /**
     * 整体修改一个绘图图形（类型 / 颜色 / 线宽 / 图层 / 两端点 / 预设参数）。
     *
     * 参数统一做范围校验：图层、线宽、旋转、倍率夹到允许区间；
     * 两端点夹到当前表面内（不因少量越界直接拒绝，便于在边缘调整）。
     */
    public int applyDrawEdit(int id, int layer, ConsoleShapeType type, int color, float width,
                             float u1, float v1, float u2, float v2,
                             String preset, float rotation, float scale) {
        if (this.level == null || this.level.isClientSide) {
            return DRAW_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return DRAW_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return DRAW_NOT_FORMED;
        }
        if (type == null) {
            return DRAW_BAD_DATA;
        }
        ConsoleShape existing = findDrawShape(id);
        if (existing == null) {
            return DRAW_NOT_FOUND;
        }
        List<ConsoleShape> list = this.drawShapes.get(existing.face);
        if (list == null) {
            return DRAW_NOT_FOUND;
        }

        Direction face = existing.face;
        Direction busFacing = getBusFacing();
        double halfW = ConsoleSurface.halfWidthPx(face, busFacing, this.sizeX, this.sizeY,
                this.sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(face, busFacing, this.sizeX, this.sizeY,
                this.sizeZ);
        float cu1 = clampPixel(u1, halfW);
        float cv1 = clampPixel(v1, halfH);
        float cu2 = clampPixel(u2, halfW);
        float cv2 = clampPixel(v2, halfH);

        ConsoleShape edited = buildDrawShape(id, face, layer, type, color, width,
                cu1, cv1, cu2, cv2, preset, rotation, scale);
        if (edited == null) {
            return DRAW_BAD_DATA;
        }
        list.set(list.indexOf(existing), edited);
        sync();
        return DRAW_OK;
    }

    /** 把一个绘图图形整体移动到新的锚点（第一点像素坐标），超出表面时自动夹紧。 */
    public int applyDrawSetAnchor(int id, float u, float v) {
        if (this.level == null || this.level.isClientSide) {
            return DRAW_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return DRAW_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return DRAW_NOT_FORMED;
        }
        ConsoleShape existing = findDrawShape(id);
        if (existing == null) {
            return DRAW_NOT_FOUND;
        }
        List<ConsoleShape> list = this.drawShapes.get(existing.face);
        if (list == null) {
            return DRAW_NOT_FOUND;
        }
        ConsoleShape moved = shiftDrawShape(existing, u - existing.u1, v - existing.v1,
                getBusFacing(), this.sizeX, this.sizeY, this.sizeZ);
        list.set(list.indexOf(existing), moved);
        sync();
        return DRAW_OK;
    }

    /** 删除一个绘图图形。 */
    public int applyDrawDelete(int id) {
        if (this.level == null || this.level.isClientSide) {
            return DRAW_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return DRAW_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return DRAW_NOT_FORMED;
        }
        ConsoleShape existing = findDrawShape(id);
        if (existing == null) {
            return DRAW_NOT_FOUND;
        }
        List<ConsoleShape> list = this.drawShapes.get(existing.face);
        if (list != null) {
            list.remove(existing);
        }
        sync();
        return DRAW_OK;
    }

    /** 构造（并校验）一个图形对象；未知预设返回 null。 */
    private static ConsoleShape buildDrawShape(int id, Direction face, int layer,
                                               ConsoleShapeType type, int color, float width,
                                               float u1, float v1, float u2, float v2,
                                               String preset, float rotation, float scale) {
        int clampedLayer = Math.max(0, Math.min(ConsoleShape.LAYERS - 1, layer));
        // 线宽 / 倍率支持一位小数：先夹到范围再量化到 0.1
        float clampedWidth = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, width)));
        int clampedColor = color & 0xFFFFFF;
        String presetId = "";
        float clampedRot = 0.0F;
        float clampedScale = ConsoleShape.MIN_SCALE;
        if (type == ConsoleShapeType.PRESET) {
            ConsoleDrawPresets.Preset def = ConsoleDrawPresets.byId(preset);
            if (def == null) {
                return null;
            }
            presetId = def.id();
            // 旋转角度归一化到 [0, 360)
            clampedRot = ConsoleShape.quantize(
                    (rotation % 360.0F + 360.0F) % 360.0F);
            clampedScale = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                    Math.min(def.maxScale(), scale)));
        }
        return new ConsoleShape(id, face, clampedLayer, type, clampedColor, clampedWidth,
                u1, v1, u2, v2, presetId, clampedRot, clampedScale);
    }

    /** 把像素坐标夹到 [-half, half] 内。 */
    private static float clampPixel(float value, double half) {
        return (float) Math.max(-half, Math.min(half, value));
    }

    /**
     * 把图形整体平移，并夹紧到当前表面范围内。
     * 用图形的像素包围盒计算允许的位移，避免图形被逐点夹紧而变形。
     */
    private static ConsoleShape shiftDrawShape(ConsoleShape shape, float du, float dv,
                                               Direction busFacing,
                                               int sizeX, int sizeY, int sizeZ) {
        double halfW = ConsoleSurface.halfWidthPx(shape.face, busFacing, sizeX, sizeY, sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(shape.face, busFacing, sizeX, sizeY, sizeZ);
        float[] bounds = shape.bounds();
        float clampedU = Math.max((float) -halfW - bounds[0],
                Math.min((float) halfW - bounds[2], du));
        float clampedV = Math.max((float) -halfH - bounds[1],
                Math.min((float) halfH - bounds[3], dv));
        return shape.translated(clampedU, clampedV);
    }

    /** 程序运行间隔（tick，0 = 停止）。 */
    public float getProgramPeriod() {
        return this.programPeriod;
    }

    /** CC 接口桥（外设读写变量 / 组件值使用）。 */
    public ConsoleCcBridge getCcBridge() {
        return this.ccBridge;
    }

    /** 读取指定槽位的变量定义；空槽位或越界返回 null。 */
    public VarDef getVarDef(int slot) {
        if (slot < 0 || slot >= VAR_SLOTS_MAX) {
            return null;
        }
        return this.varDefs[slot];
    }

    /** 指定槽位是否已定义变量（0 号槽位固定视为已定义）。 */
    public boolean isVarDefined(int slot) {
        if (slot == VAR_SLOT_PERIOD) {
            return true;
        }
        return slot > 0 && slot < VAR_SLOTS_MAX && this.varDefs[slot] != null;
    }

    /** 读取浮点变量值；0 号槽位返回运行间隔。 */
    public float getVarFloat(int slot) {
        if (slot == VAR_SLOT_PERIOD) {
            return this.programPeriod;
        }
        if (slot <= 0 || slot >= VAR_SLOTS_MAX) {
            return 0.0F;
        }
        return this.varFloats[slot];
    }

    /** 读取布尔变量值；0 号槽位不是布尔变量，固定返回 false。 */
    public boolean getVarBool(int slot) {
        if (slot <= 0 || slot >= VAR_SLOTS_MAX) {
            return false;
        }
        return this.varBools[slot];
    }

    /** 按名称查找变量槽位（忽略大小写），找不到返回 -1。 */
    public int findVarSlot(String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 1; i < VAR_SLOTS_MAX; i++) {
            VarDef def = this.varDefs[i];
            if (def != null && def.name.equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 设置程序运行间隔（服务端调用）。
     * 浮点值四舍五入取整；负值按 0 处理（0 表示停止运行），超过上限按上限处理。
     */
    public int applyProgramPeriod(float ticks) {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        float safe = Float.isFinite(ticks) ? ticks : 0.0F;
        int value = (int) Math.round(Math.max(0.0F, Math.min(MAX_PERIOD_TICKS, safe)));
        if (value == (int) this.programPeriod) {
            return VAR_OK;
        }
        this.programPeriod = value;
        sync();
        return VAR_OK;
    }

    /** 程序行数组（下标即行号，可为 null；只读用途）。 */
    public ProgramIR.Line[] getProgramLines() {
        return this.programLines;
    }

    /** 读取指定行号的程序行；空行或越界返回 null。 */
    public ProgramIR.Line getProgramLine(int index) {
        if (index < 0 || index >= MAX_PROGRAM_LINES) {
            return null;
        }
        return this.programLines[index];
    }

    /**
     * 变量查询视图：供程序 IR 做类型检查与文本显示。
     * 每次调用新建一个轻量对象，避免客户端持有方块实体内部数组。
     */
    public ProgramIR.Scope getProgramScope() {
        return new ProgramIR.Scope() {
            @Override
            public VarType typeOf(int slot) {
                VarDef def = getVarDef(slot);
                return def == null ? null : def.type;
            }

            @Override
            public String nameOf(int slot) {
                VarDef def = getVarDef(slot);
                return def == null ? "#" + slot : def.name;
            }
        };
    }

    /**
     * 保存整张程序表（服务端调用）。
     * 全量校验通过后才替换，任何一行出错都拒绝保存并返回对应错误码。
     */
    public int applyProgramSave(ProgramIR.Line[] lines) {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        if (this.formState == State.FULL) {
            return VAR_INVALID;
        }
        if (lines == null || lines.length > MAX_PROGRAM_LINES) {
            return PROG_LIMIT;
        }
        ProgramIR.Scope scope = getProgramScope();
        int slots = varSlots();
        for (int i = 0; i < lines.length; i++) {
            ProgramIR.Line line = lines[i];
            if (line == null) {
                continue;
            }
            switch (line.validate(scope, slots)) {
                case BAD_TARGET:
                    // 绑定按钮面板的变量状态只读，写回没有意义
                    VarDef target = getVarDef(line.targetSlot);
                    if (target != null && target.compId != 0) {
                        CompEntry comp = findComp(target.compId);
                        if (comp != null && !comp.type.supportsVariableWrite()) {
                            return PROG_BAD_TARGET;
                        }
                    }
                    return PROG_BAD_TARGET;
                case BAD_COND:
                    return PROG_BAD_COND;
                case BAD_EXPR:
                    return PROG_BAD_EXPR;
                case TYPE_MISMATCH:
                    return PROG_TYPE_MISMATCH;
                case TOO_DEEP:
                    return PROG_TOO_DEEP;
                default:
                    break;
            }
        }
        Arrays.fill(this.programLines, null);
        System.arraycopy(lines, 0, this.programLines, 0,
                Math.min(lines.length, MAX_PROGRAM_LINES));
        // 程序已变化：作废旧字节码与上一轮警告，下次执行前重新编译
        this.programDirty = true;
        this.programTimer = 0;
        this.runWarnings.clear();
        sync();
        return VAR_OK;
    }

    /** 指定变量是否被某行程序引用（目标 / 条件 / 表达式）。 */
    public boolean isVarReferenced(int slot) {
        for (ProgramIR.Line line : this.programLines) {
            if (line == null) {
                continue;
            }
            if (line.targetSlot == slot || line.condSlot == slot) {
                return true;
            }
            if (exprUsesSlot(line.expr, slot) || exprUsesSlot(line.condExpr, slot)) {
                return true;
            }
        }
        return false;
    }

    /** 递归检查表达式是否引用了指定变量槽位。 */
    private static boolean exprUsesSlot(ProgramIR.Expr expr, int slot) {
        if (expr == null) {
            return false;
        }
        if (expr.op == ProgramIR.Op.VAR && expr.slot == slot) {
            return true;
        }
        return exprUsesSlot(expr.left, slot) || exprUsesSlot(expr.right, slot);
    }

    /**
     * 在空槽位创建变量（服务端调用）。
     *
     * @param slot   目标槽位（1 ~ 63；0 号保留给运行间隔）
     * @param name   变量名（英文字母 / 数字 / 下划线，1~32 位，不区分大小写去重）
     * @param type   变量类型
     * @param compId 绑定的组件 id；0 表示自定义变量
     */
    public int applyVarCreate(int slot, String name, VarType type, int compId) {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        if (slot <= VAR_SLOT_PERIOD || slot >= varSlots()) {
            return VAR_BAD_SLOT;
        }
        if (this.varDefs[slot] != null) {
            return VAR_DUP_SLOT;
        }
        String normalized = name == null ? "" : name.trim();
        if (!VAR_NAME_PATTERN.matcher(normalized).matches()) {
            return VAR_BAD_NAME;
        }
        // 变量名不能以数字开头、也不能与 true / false 常量重名，
        // 否则程序行里的操作数无法区分「变量名」与「常量」
        char first = normalized.charAt(0);
        if ((first >= '0' && first <= '9')
                || normalized.equalsIgnoreCase("true") || normalized.equalsIgnoreCase("false")) {
            return VAR_BAD_NAME;
        }
        if (findVarSlot(normalized) >= 0) {
            return VAR_DUP_NAME;
        }
        if (type == null) {
            return VAR_INVALID;
        }
        // 按名称自动绑定设备：变量名与某个设备代号一致即视为绑定该设备，
        // 这样界面上只需要输入名称（带设备名补全），不需要额外的设备下拉框。
        // 圆盘记录仪还有第二名称：命中第二名称时绑定为「推送触发」布尔变量。
        CompEntry matched = null;
        boolean triggerMatch = false;
        // RGB 控制器的通道名（R / G / B）绑定为浮点变量
        boolean channelMatch = false;
        // 同步指示器的灯状态名（<代号>_b）绑定为只读布尔变量
        boolean lampMatch = false;
        // 警报器的静音状态名（<代号>_Q）绑定为只读布尔变量
        boolean muteMatch = false;
        for (CompEntry comp : this.comps) {
            if (comp.code.equalsIgnoreCase(normalized)) {
                matched = comp;
                triggerMatch = false;
                break;
            }
            if (comp.type.supportsHistory() && comp.code2.equalsIgnoreCase(normalized)) {
                matched = comp;
                triggerMatch = true;
                break;
            }
            if (comp.type == ConsoleCompType.RGB_COLOR && matchesChannel(normalized, comp)) {
                matched = comp;
                triggerMatch = false;
                channelMatch = true;
                break;
            }
            // 同步指示器的转速附加变量（<代号>_s）绑定为浮点变量
            if (comp.type.hasSpeedVariable() && isSynchroSpeedName(normalized, comp)) {
                matched = comp;
                triggerMatch = false;
                channelMatch = true;
                break;
            }
            // 同步指示器的灯亮状态附加变量（<代号>_b）绑定为布尔变量
            if (comp.type.hasLampVariable() && isSynchroLampName(normalized, comp)) {
                matched = comp;
                triggerMatch = false;
                lampMatch = true;
                break;
            }
            // 警报器的静音状态附加变量（<代号>_Q）绑定为布尔变量
            if (comp.type.hasMuteVariable() && isAlarmMuteName(normalized, comp)) {
                matched = comp;
                triggerMatch = false;
                muteMatch = true;
                break;
            }
        }
        if (matched != null) {
            // 客户端数据过期时可能传来别的组件 id，以名称匹配结果为准
            if (compId != 0 && compId != matched.id) {
                return VAR_COMP_NOT_FOUND;
            }
            compId = matched.id;
            if (triggerMatch) {
                // 第二名称固定为布尔触发量，用于控制本 tick 是否记录一笔
                type = VarType.BOOL;
            } else if (lampMatch) {
                // 灯状态附加变量固定为布尔量（只读输出）
                type = VarType.BOOL;
            } else if (muteMatch) {
                // 静音状态附加变量固定为布尔量（只读输出）
                type = VarType.BOOL;
            } else if (channelMatch) {
                // 颜色通道 / 转速附加变量固定为浮点量
                type = VarType.FLOAT;
            } else {
                VarType deviceType = matched.type.variableType();
                if (deviceType == null) {
                    // 名称属于不支持变量的设备（发信终端 / 圆盘记录仪），禁止占用以免歧义
                    return VAR_NAME_IS_COMP;
                }
                type = deviceType;
            }
        } else if (compId != 0) {
            return VAR_COMP_NOT_FOUND;
        }
        this.varDefs[slot] = new VarDef(slot, normalized, type, compId, triggerMatch);
        // 变量表结构变化：作废已编译字节码
        this.programDirty = true;
        // 新变量的初始值统一为 0 / false
        this.varFloats[slot] = 0.0F;
        this.varBools[slot] = false;
        // 绑定设备时用设备的当前值作为初值，打开界面即可看到设备真实状态
        // （触发变量没有设备侧状态，保持 false）
        if (compId != 0 && !triggerMatch) {
            CompEntry bound = findComp(compId);
            if (bound != null) {
                // 静音变量是只读输出，writeBackToVar 不会命中它，直接取组件当前静音状态
                if (muteMatch) {
                    this.varBools[slot] = bound.muted;
                } else {
                    writeBackToVar(bound);
                }
            }
        }
        sync();
        return VAR_OK;
    }

    /**
     * 删除变量（服务端调用）。
     * 程序系统接入后，这里还需要检查是否有程序行引用该变量。
     */
    public int applyVarDelete(int slot) {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        if (slot <= VAR_SLOT_PERIOD || slot >= varSlots()) {
            return VAR_BAD_SLOT;
        }
        if (this.varDefs[slot] == null) {
            return VAR_NOT_FOUND;
        }
        // 程序仍引用该变量时拒绝删除，避免程序静默失效
        if (isVarReferenced(slot)) {
            return VAR_IN_USE;
        }
        this.varDefs[slot] = null;
        this.varFloats[slot] = 0.0F;
        this.varBools[slot] = false;
        // 变量表结构变化：作废已编译字节码
        this.programDirty = true;
        sync();
        return VAR_OK;
    }

    /**
     * 变量表「保存」：写入玩家编辑后的值，并把绑定组件的变量一次性上传到组件。
     *
     * 运算过程中不写组件，只有玩家点击保存时才执行一次上传，方便测试
     * 「变量 ↔ 组件」的对应关系。数组长度至少为 {@link #varSlots()}；
     * 0 号槽位（运行间隔）忽略，由 {@link #applyProgramPeriod(float)} 单独设置。
     */
    public int applyVarValuesSave(float[] floats, boolean[] bools) {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        int slots = varSlots();
        if (floats == null || bools == null
                || floats.length < slots || bools.length < slots) {
            return VAR_INVALID;
        }
        boolean changed = false;
        for (int slot = 1; slot < slots; slot++) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                continue;
            }
            if (def.type == VarType.FLOAT) {
                float value = floats[slot];
                // 防御 NaN / Infinity，避免污染后续运算与渲染
                if (!Float.isFinite(value)) {
                    value = 0.0F;
                }
                if (this.varFloats[slot] != value) {
                    this.varFloats[slot] = value;
                    changed = true;
                }
            } else {
                if (this.varBools[slot] != bools[slot]) {
                    this.varBools[slot] = bools[slot];
                    changed = true;
                }
            }
            // 每次保存都强制上传一次（即使值没变），便于玩家测试组件响应
            changed |= uploadVarToComponent(def);
        }
        if (changed) {
            sync();
        }
        return VAR_OK;
    }

    /**
     * 把变量当前值写入其绑定的组件。
     *
     * @return 组件数据是否发生变化（需要同步）
     */
    private boolean uploadVarToComponent(VarDef def) {
        // 触发变量只表示「本 tick 是否推送」，没有对应的组件状态，不能反向写入组件
        if (def == null || def.compId == 0 || def.trigger) {
            return false;
        }
        CompEntry comp = findComp(def.compId);
        if (comp == null || !comp.type.supportsVariableWrite()) {
            return false;
        }
        // 同步指示器的转速附加变量只作为输入，不写指针位置
        if (isSynchroSpeedName(def.name, comp)) {
            return false;
        }
        // 同步指示器的灯状态附加变量是只读输出，不接受变量 → 组件写入
        if (isSynchroLampName(def.name, comp)) {
            return false;
        }
        // 警报器的静音状态附加变量是只读输出，不接受变量 → 组件写入
        if (isAlarmMuteName(def.name, comp)) {
            return false;
        }
        CompEntry updated;
        if (def.type == VarType.FLOAT) {
            // 只同步「当前值」，绝不在这里移位历史：
            // 圆盘记录仪的历史只由 flushPlotters（触发变量为 true）或 CC 的 push 写入
            updated = withFloatValue(comp, this.varFloats[def.slot]);
        } else {
            updated = withBoolValue(comp, this.varBools[def.slot]);
        }
        if (updated == comp) {
            return false;
        }
        int index = this.comps.indexOf(comp);
        if (index < 0) {
            return false;
        }
        this.comps.set(index, updated);
        return true;
    }

    /**
     * 生成写入一个历史采样后的组件副本（圆盘记录仪）。
     *
     * 语义与之前单独实现的圆盘记录仪一致：删除最后一位、其余整体后移，
     * 新值写入首位（索引 0 = 最新）。每次提交都产生新对象，保证「计算一轮记一笔」。
     */
    private static CompEntry withHistorySample(CompEntry comp, float value) {
        float safe = Float.isFinite(value) ? value : 0.0F;
        float[] history = new float[CompEntry.HISTORY_LEN];
        if (comp.history != null) {
            System.arraycopy(comp.history, 0, history, 1,
                    Math.min(comp.history.length, CompEntry.HISTORY_LEN - 1));
        }
        history[0] = safe;
        return new CompEntry(comp.id, comp.type, comp.face, comp.u, comp.v,
                comp.code, comp.color, comp.text, comp.state, safe,
                history, comp.decimals, comp.unit, comp.holdTicks, comp.scale, comp.code2,
                comp.links, comp.rgbR, comp.rgbG, comp.rgbB, comp.showOriginal,
                comp.speedVar, comp.lampVar, comp.coverTicks, comp.hasKey, comp.keyMode);
    }

    /**
     * 生成「只修改历史某一点」的组件副本（CC 接口 setPoint 使用）。
     * 修改最新一点（下标 0）时同步更新当前值，保持 value = 最新采样。
     */
    private static CompEntry withPoint(CompEntry comp, int index, float value) {
        float safe = Float.isFinite(value) ? value : 0.0F;
        float[] history = new float[CompEntry.HISTORY_LEN];
        if (comp.history != null) {
            System.arraycopy(comp.history, 0, history, 0,
                    Math.min(comp.history.length, CompEntry.HISTORY_LEN));
        }
        history[index] = safe;
        float current = index == 0 ? safe : comp.value;
        return new CompEntry(comp.id, comp.type, comp.face, comp.u, comp.v,
                comp.code, comp.color, comp.text, comp.state, current,
                history, comp.decimals, comp.unit, comp.holdTicks, comp.scale, comp.code2,
                comp.links, comp.rgbR, comp.rgbG, comp.rgbB, comp.showOriginal,
                comp.speedVar, comp.lampVar, comp.coverTicks, comp.hasKey, comp.keyMode);
    }

    /** 按代号查找组件（忽略大小写），找不到返回 null。 */
    private CompEntry findCompByCode(String code) {
        // 空名称不参与查找（未命名的 RGB 控制器）
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CompEntry comp : this.comps) {
            if (comp.code.equalsIgnoreCase(code)) {
                return comp;
            }
        }
        return null;
    }

    /**
     * 应用 CC 接口本 tick 的全部写操作（主线程）。
     *
     * 顺序遵守「辅助计算机 → CC 变量 / 组件出入 → 组件状态更新」：
     * 变量写入后立即上传其绑定组件（圆盘记录仪记一笔），组件写入直接生效，
     * 所有操作结算完最多只同步一次，连续多次 Lua 读写不会逐个消耗 tick。
     */
    private void applyCcOps() {
        List<ConsoleCcBridge.Op> ops = this.ccBridge.drain();
        if (ops.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (ConsoleCcBridge.Op op : ops) {
            switch (op.kind) {
                case ConsoleCcBridge.OP_VAR_FLOAT -> {
                    int slot = findVarSlot(op.key);
                    VarDef def = slot > 0 ? this.varDefs[slot] : null;
                    if (def == null || def.type != VarType.FLOAT) {
                        continue;
                    }
                    float value = (float) op.number;
                    if (!Float.isFinite(value)) {
                        continue;
                    }
                    boolean valueChanged = this.varFloats[slot] != value;
                    if (valueChanged) {
                        this.varFloats[slot] = value;
                        changed = true;
                    }
                    // 只有值变化才上传；圆盘记录仪的采样由推送触发变量或 CC 的 push 决定
                    // （见 clearPlotTriggers / flushPlotters）
                    if (valueChanged) {
                        changed |= uploadVarToComponent(def);
                    }
                }
                case ConsoleCcBridge.OP_VAR_BOOL -> {
                    int slot = findVarSlot(op.key);
                    VarDef def = slot > 0 ? this.varDefs[slot] : null;
                    if (def == null || def.type != VarType.BOOL) {
                        continue;
                    }
                    boolean value = op.number != 0.0D;
                    if (this.varBools[slot] == value) {
                        continue;
                    }
                    this.varBools[slot] = value;
                    changed = true;
                    changed |= uploadVarToComponent(def);
                }
                case ConsoleCcBridge.OP_COMP_FLOAT -> {
                    CompEntry comp = findCompByCode(op.key);
                    if (comp == null || !comp.type.supportsFloatVariable()) {
                        continue;
                    }
                    changed |= replaceComp(comp, withFloatValue(comp, (float) op.number));
                }
                case ConsoleCcBridge.OP_COMP_BOOL -> {
                    CompEntry comp = findCompByCode(op.key);
                    if (comp == null || !comp.type.supportsBoolVariable()
                            || !comp.type.supportsVariableWrite()) {
                        continue;
                    }
                    // RGB 控制器的布尔量表示启用 / 停用，而不是 state
                    if (comp.type == ConsoleCompType.RGB_COLOR) {
                        changed |= setRgbEnabled(comp, op.number != 0.0D);
                        continue;
                    }
                    changed |= replaceComp(comp, withBoolValue(comp, op.number != 0.0D));
                }
                case ConsoleCcBridge.OP_RGB_COLOR -> {
                    CompEntry comp = findCompByCode(op.key);
                    if (comp == null || comp.type != ConsoleCompType.RGB_COLOR) {
                        continue;
                    }
                    int packed = ((int) Math.round(op.number)) & 0xFFFFFF;
                    changed |= applyRgbColor(comp, packed);
                }
                case ConsoleCcBridge.OP_PLOT_PUSH -> {
                    CompEntry comp = findCompByCode(op.key);
                    if (comp == null || !comp.type.supportsHistory()) {
                        continue;
                    }
                    changed |= replaceComp(comp, withHistorySample(comp, (float) op.number));
                }
                case ConsoleCcBridge.OP_PLOT_SET -> {
                    CompEntry comp = findCompByCode(op.key);
                    if (comp == null || !comp.type.supportsHistory()
                            || op.index < 0 || op.index >= CompEntry.HISTORY_LEN) {
                        continue;
                    }
                    changed |= replaceComp(comp, withPoint(comp, op.index, (float) op.number));
                }
                default -> {
                }
            }
        }
        if (changed) {
            sync();
        }
    }

    /**
     * CC：设置 RGB 控制器的启用状态。
     * 注册了启用布尔变量时写变量（与程序调节同一份数据），否则改「显示原色」开关。
     */
    private boolean setRgbEnabled(CompEntry comp, boolean enabled) {
        int boolSlot = findRgbBoolSlot(comp.id);
        if (boolSlot > 0) {
            if (this.varBools[boolSlot] == enabled) {
                return false;
            }
            this.varBools[boolSlot] = enabled;
            return true;
        }
        return replaceComp(comp, CompEntry.copyWithShowOriginal(comp, !enabled));
    }

    /**
     * CC：直接设置 RGB 灯色（0xRRGGBB）。
     *
     * 已注册的通道变量同步写入，保持「界面颜色 = 变量值」是同一份数据；
     * 组件颜色也立刻更新，因此部分成型时也能直接看到效果；
     * 完全成型时随后的 applyRgbControllers 会用同一组变量值结算，结果一致。
     */
    private boolean applyRgbColor(CompEntry comp, int packed) {
        boolean changed = false;
        int slot = findRgbChannelSlot(comp, comp.rgbR);
        float r = (packed >> 16) & 0xFF;
        if (slot > 0 && this.varFloats[slot] != r) {
            this.varFloats[slot] = r;
            changed = true;
        }
        slot = findRgbChannelSlot(comp, comp.rgbG);
        float g = (packed >> 8) & 0xFF;
        if (slot > 0 && this.varFloats[slot] != g) {
            this.varFloats[slot] = g;
            changed = true;
        }
        slot = findRgbChannelSlot(comp, comp.rgbB);
        float b = packed & 0xFF;
        if (slot > 0 && this.varFloats[slot] != b) {
            this.varFloats[slot] = b;
            changed = true;
        }
        if (comp.color != packed) {
            changed |= replaceComp(comp, CompEntry.copyWithColor(comp, packed));
        }
        return changed;
    }

    /** 用新组件替换列表中的旧组件，返回是否成功（找不到旧组件返回 false）。 */
    private boolean replaceComp(CompEntry old, CompEntry updated) {
        if (updated == old) {
            return false;
        }
        int index = this.comps.indexOf(old);
        if (index < 0) {
            return false;
        }
        this.comps.set(index, updated);
        return true;
    }

    /** 主线程：把当前变量与组件状态发布给 CC 接口（Lua 线程只读快照）。 */
    private void publishCcSnapshot() {
        int slots = varSlots();
        Map<String, ConsoleCcBridge.VarInfo> vars = new HashMap<>();
        List<ConsoleCcBridge.VarInfo> varList = new ArrayList<>();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def == null) {
                continue;
            }
            String compCode = "";
            if (def.compId != 0) {
                CompEntry bound = findComp(def.compId);
                if (bound != null) {
                    // 触发变量对应设备第二名称，其余变量对应主代号
                    compCode = def.trigger ? bound.code2 : bound.code;
                }
            }
            ConsoleCcBridge.VarInfo info = new ConsoleCcBridge.VarInfo(i, def.name, def.type,
                    compCode,
                    def.type == VarType.FLOAT ? this.varFloats[i] : 0.0F,
                    def.type == VarType.BOOL && this.varBools[i]);
            vars.put(def.name.toLowerCase(Locale.ROOT), info);
            varList.add(info);
        }
        Map<String, ConsoleCcBridge.CompInfo> comps = new HashMap<>();
        List<ConsoleCcBridge.CompInfo> compList = new ArrayList<>();
        long now = this.level == null ? 0L : this.level.getGameTime();
        for (CompEntry comp : this.comps) {
            boolean boolValue;
            if (comp.type == ConsoleCompType.KEYPAD || comp.type == ConsoleCompType.BUTTON_X
                    || comp.type == ConsoleCompType.BUTTON_S) {
                boolValue = comp.isKeypadPressed(now);
            } else {
                boolValue = comp.type.supportsBoolVariable() && comp.state != 0L;
            }
            // RGB 控制器：发布实际灯色与启用状态（启用 = 注册了启用变量时取变量值，否则取「显示原色」的反值）
            int color = comp.color;
            boolean enabled = false;
            if (comp.type == ConsoleCompType.RGB_COLOR) {
                int boolSlot = findRgbBoolSlot(comp.id);
                enabled = boolSlot > 0 ? this.varBools[boolSlot] : !comp.showOriginal;
            }
            ConsoleCcBridge.CompInfo info = new ConsoleCcBridge.CompInfo(comp.id, comp.code,
                    comp.type, Float.isFinite(comp.value) ? comp.value : 0.0F, boolValue,
                    comp.state, comp.history, comp.decimals, comp.unit, color, enabled);
            // 未命名的组件（RGB 控制器）不进入按代号索引的表，但仍出现在列表中
            if (!comp.code.isEmpty()) {
                comps.put(comp.code.toLowerCase(Locale.ROOT), info);
            }
            compList.add(info);
        }
        this.ccBridge.publish(new ConsoleCcBridge.Snapshot(this.programPeriod,
                Map.copyOf(vars), Map.copyOf(comps), List.copyOf(varList), List.copyOf(compList)));
    }

    /** 生成写入浮点值后的组件副本；弧形仪表同时更新指针状态。 */
    private static CompEntry withFloatValue(CompEntry comp, float value) {
        float safe = Float.isFinite(value) ? value : 0.0F;
        if (comp.type.isThreeState()) {
            // 三状态开关：浮点值 0 / 1 / 2 对应三个档位，四舍五入后夹紧到 0~2
            long display = Math.round(Math.max(0.0F, Math.min(2.0F, safe)));
            if (comp.value == (float) display && comp.state == display) {
                return comp;
            }
            return CompEntry.copyWith(comp, display, (float) display);
        }
        if (comp.type == ConsoleCompType.GAUGE_ARC) {
            // 指针状态限制在略超量程的显示范围内，原始值仍保存在 value 中
            long display = Math.round(Math.max(GAUGE_ARC_DISPLAY_MIN,
                    Math.min(GAUGE_ARC_DISPLAY_MAX, safe)));
            if (comp.value == safe && comp.state == display) {
                return comp;
            }
            return CompEntry.copyWith(comp, display, safe);
        }
        if (comp.type.supportsFloatVariable()) {
            if (comp.value == safe) {
                return comp;
            }
            return CompEntry.copyWith(comp, comp.state, safe);
        }
        return comp;
    }

    /** 生成写入布尔值后的组件副本（开关 / 指示灯用 state 的 0/1 表示）。 */
    private static CompEntry withBoolValue(CompEntry comp, boolean value) {
        if (!comp.type.supportsBoolVariable()) {
            return comp;
        }
        long newState = value ? 1L : 0L;
        if (comp.state == newState) {
            return comp;
        }
        return CompEntry.copyWith(comp, newState, comp.value);
    }

    /**
     * 按绑定的组件 id 查找变量槽位（找不到返回 -1）。
     *
     * 同一个圆盘记录仪可能同时绑定「数值变量」与「推送触发变量」，
     * 因此需要按角色过滤：trigger = true 找触发变量，false 找数值变量。
     */
    private int findVarSlotByComp(int compId, boolean trigger) {
        if (compId == 0) {
            return -1;
        }
        CompEntry owner = findComp(compId);
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            // 同步指示器的 <代号>_s（转速输入）与 <代号>_b（灯状态只读输出）不代表
            // 组件的「当前值」变量，查找时跳过，避免抢占主变量槽位
            if (def != null && def.compId == compId && def.trigger == trigger
                    && !isSynchroSpeedName(def.name, owner)
                    && !isSynchroLampName(def.name, owner)
                    && !isAlarmMuteName(def.name, owner)) {
                return i;
            }
        }
        return -1;
    }

    /** 按绑定的组件 id 查找「数值变量」槽位（找不到返回 -1）。 */
    private int findVarSlotByComp(int compId) {
        return findVarSlotByComp(compId, false);
    }

    /**
     * 把组件的当前状态写回其绑定变量（组件 → 变量）。
     *
     * 只处理玩家能手动改变状态的组件：开关类写布尔，仪表类写浮点；
     * 按钮面板的 state 是弹起时刻，发信终端 / 圆盘记录仪不参与变量系统。
     *
     * @return 变量值是否发生变化（需要同步）
     */
    private boolean writeBackToVar(CompEntry comp) {
        int slot = findVarSlotByComp(comp.id);
        if (slot < 0) {
            return false;
        }
        VarDef def = this.varDefs[slot];
        if (def == null) {
            return false;
        }
        switch (comp.type) {
            case KNOB_SWITCH, LEVER_SWITCH, LEVER_F, INDICATOR, LAMP_D, SWITCH_K, ALARM -> {
                if (def.type != VarType.BOOL) {
                    return false;
                }
                boolean value = comp.state != 0L;
                if (this.varBools[slot] == value) {
                    return false;
                }
                this.varBools[slot] = value;
                return true;
            }
            case T_SWITCH, T_LEVER -> {
                // 三状态开关：浮点变量取 state 的 0 / 1 / 2
                if (def.type != VarType.FLOAT) {
                    return false;
                }
                float value = (float) Math.floorMod(comp.state, 3L);
                if (this.varFloats[slot] == value) {
                    return false;
                }
                this.varFloats[slot] = value;
                return true;
            }
            case KEYPAD, BUTTON_X, BUTTON_S -> {
                // 按钮 / 旋转按钮 / 安全按钮为按压型：state 存弹起时刻或「按住直到松手」标记
                if (def.type != VarType.BOOL) {
                    return false;
                }
                boolean value = this.level != null && comp.isKeypadPressed(this.level.getGameTime());
                if (this.varBools[slot] == value) {
                    return false;
                }
                this.varBools[slot] = value;
                return true;
            }
            case GAUGE, NUMITRON, GAUGE_V, GAUGE_ARC, GAUGE_F, SYNCHROSCOPE -> {
                if (def.type != VarType.FLOAT) {
                    return false;
                }
                float value = Float.isFinite(comp.value) ? comp.value : 0.0F;
                if (this.varFloats[slot] == value) {
                    return false;
                }
                this.varFloats[slot] = value;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * 打开程序界面前刷新绑定变量（服务端调用）。
     *
     * 界面关闭期间玩家可能手动操作过开关 / 调节器仪表，组件状态才是最新值；
     * 这里统一回写一次，保证打开 aux 时看到的就是设备真实状态。
     */
    public int refreshVarsFromComponents() {
        if (this.level == null || this.level.isClientSide) {
            return VAR_INVALID;
        }
        boolean changed = false;
        for (CompEntry comp : this.comps) {
            changed |= writeBackToVar(comp);
        }
        if (changed) {
            sync();
        }
        return VAR_OK;
    }

    /**
     * 命中检测：返回该表面上覆盖点击点 (u, v) 的组件（没有返回 null）。
     * 组件矩形以保存的锚点为中心，尺寸取自 {@link ConsoleCompType} 的模型包围盒。
     */
    public CompEntry findCompAt(Direction face, float u, float v) {
        for (CompEntry entry : this.comps) {
            if (entry.face != face) {
                continue;
            }
            // 留 0.5 像素容差，方便选中（矩形尺寸含组件缩放，交互区域随缩放一起变化）
            if (Math.abs(u - entry.u) <= entry.halfWidthPx() + 0.5F
                    && Math.abs(v - entry.v) <= entry.halfHeightPx() + 0.5F) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 在结构表面新增一个组件（服务端调用）。
     * 默认代号按「类型前缀_序号」生成，例如 GAUGE_1。
     *
     * @return {@link #COMP_OK} 或对应失败状态
     */
    public int applyCompAdd(ConsoleCompType type, Direction face, float u, float v) {
        if (type == null) {
            return COMP_BAD_CODE;
        }
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return COMP_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return COMP_NOT_FORMED;
        }
        if (this.comps.size() >= MAX_COMPS) {
            return COMP_LIMIT;
        }

        // 位置夹在表面范围内
        Direction busFacing = getBusFacing();
        double halfW = ConsoleSurface.halfWidthPx(face, busFacing, this.sizeX, this.sizeY, this.sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(face, busFacing, this.sizeX, this.sizeY, this.sizeZ);
        float cu = (float) Math.max(-halfW, Math.min(halfW, u));
        float cv = (float) Math.max(-halfH, Math.min(halfH, v));

        if (overlapsOtherComp(-1, face, cu, cv, type, CompEntry.DEFAULT_SCALE)) {
            return COMP_DUP_POS;
        }

        // 放置时自动生成默认代号（RGB 颜色控制器同样需要默认代号：
        // 它既是组件注册名，也是默认布尔「启用」变量的名称）
        String code = nextFreeCode(type);
        // 圆盘记录仪自动生成第二名称（<代号>_T），其余类型没有第二名称
        String code2 = type.supportsHistory() ? nextFreeCode2(code) : "";
        CompEntry added = new CompEntry(this.nextCompId++, type, face, cu, cv,
                code, CompEntry.NO_COLOR, CompText.NONE,
                CompEntry.defaultState(type), CompEntry.defaultValue(type),
                type.supportsHistory() ? new float[CompEntry.HISTORY_LEN] : null,
                CompEntry.DEFAULT_DECIMALS, "", CompEntry.DEFAULT_HOLD_TICKS,
                CompEntry.DEFAULT_SCALE, code2,
                "", "", "", "", false);
        this.comps.add(added);
        // 名称回收：自动代号若与某个已解除绑定的变量同名且同类型，重新连接（不影响创建结果）
        if (!added.code.isEmpty()) {
            relinkVarToComp(added.code, added, false);
        }
        if (added.type.supportsHistory() && !added.code2.isEmpty()) {
            relinkVarToComp(added.code2, added, true);
        }
        // 同步指示器：放置时只重连已存在的同名转速变量，不再自动创建，
        // 避免刚放下组件就凭空多出一个变量（创建发生在设置界面点「完成」时）
        if (added.type.hasSpeedVariable() && !added.code.isEmpty()) {
            if (ensureSynchroSpeedVar(added, false)) {
                this.programDirty = true;
            }
            // 灯亮状态布尔变量同样只重连，不自动创建
            if (ensureSynchroLampVar(added, false)) {
                this.programDirty = true;
            }
        }
        // 警报器：放置时只重连已存在的同名静音变量，不自动创建，
        // 避免刚放下组件就凭空多出一个变量（创建发生在设置界面点「完成」时）
        if (added.type.hasMuteVariable() && !added.code.isEmpty()) {
            if (ensureAlarmMuteVar(added, false)) {
                this.programDirty = true;
            }
        }
        sync();
        return COMP_OK;
    }

    /**
     * 修改已有组件的代号、位置与颜色（服务端调用）。
     *
     * @param id    组件 id
     * @param code  新代号（英文字母 / 数字 / 下划线，1~32 位）
     * @param code2 第二名称（仅圆盘记录仪使用；空 = 不设触发名称）
     * @param color 新颜色（0xRRGGBB；{@link CompEntry#NO_COLOR} 表示恢复原样）
     * @param decimals 数字显示屏保留小数位（其它类型忽略，沿用原值）
     * @param unit     数字显示屏单位（其它类型忽略，沿用原值）
     * @param holdTicks 按钮弹起时间（tick，其它类型忽略，沿用原值；0 = 松手立刻弹起）
     * @param scale     组件缩放（1.0 = 模型默认大小，实际倍率还会乘以类型默认缩放）
     * @param links     RGB 颜色控制器：连接的组件代号（用「;」分隔；其它类型忽略）
     * @param rgbR      RGB 颜色控制器：R 通道变量名（空 = 不启用；其它类型忽略）
     * @param rgbG      RGB 颜色控制器：G 通道变量名
     * @param rgbB      RGB 颜色控制器：B 通道变量名
     * @param showOriginal RGB 颜色控制器：是否显示原色（= 停用；其它类型忽略）
     * @param speedVar  同步指示器：转速变量名（空 = 自动使用 代号 + "_s"；其它类型忽略）
     * @param lampVar   同步指示器：灯亮状态变量名（空 = 自动使用 代号 + "_b"；其它类型忽略）
     * @param muteVar   警报器：静音状态变量名（空 = 自动使用 代号 + "_Q"；其它类型忽略）
     * @param coverTicks 安全按钮：盖子自动关闭时间（tick，0 = 不自动关闭；其它类型忽略）
     * @param keyMode   钥匙开关：钥匙模式（0 = 开闭，1 = 常开，2 = 常闭；其它类型忽略）
     * @return {@link #COMP_OK} 或对应失败状态
     */
    public int applyCompEdit(int id, String code, String code2, float u, float v, int color,
                             int decimals, String unit, int holdTicks, float scale,
                             String links, String rgbR, String rgbG, String rgbB,
                             boolean showOriginal, String speedVar, String lampVar,
                             String muteVar, int coverTicks, int keyMode) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return COMP_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return COMP_NOT_FORMED;
        }

        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }

        String normalized = code == null ? "" : code.trim();
        // RGB 颜色控制器必须有代号（组件注册名 = 默认布尔「启用」变量的名称）；
        // 旧存档 / 旧组件里代号为空时在这里补一个默认代号（RGB_1、RGB_2…）
        if (normalized.isEmpty() && existing.type == ConsoleCompType.RGB_COLOR) {
            normalized = nextFreeCode(existing.type);
        }
        // 代号格式校验（RGB 上面已保证非空，其它类型本来就必填）
        if (!COMP_CODE_PATTERN.matcher(normalized).matches()) {
            return COMP_BAD_CODE;
        }
        if (codeUsed(normalized, id)
                || nameBlocked(normalized, id, existing.type.variableType(), false)) {
            return COMP_DUP_CODE;
        }

        // 第二名称只对圆盘记录仪生效；其它类型一律清空，避免留下无意义字段
        String normalized2 = "";
        if (existing.type.supportsHistory()) {
            normalized2 = code2 == null ? "" : code2.trim();
            if (!normalized2.isEmpty()) {
                if (!COMP_CODE_PATTERN.matcher(normalized2).matches()
                        || normalized2.equalsIgnoreCase(normalized)) {
                    return COMP_BAD_CODE;
                }
                if (codeUsed(normalized2, id)
                        || nameBlocked(normalized2, id, VarType.BOOL, true)) {
                    return COMP_DUP_CODE;
                }
            }
        }

        Direction busFacing = getBusFacing();
        double halfW = ConsoleSurface.halfWidthPx(existing.face, busFacing,
                this.sizeX, this.sizeY, this.sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(existing.face, busFacing,
                this.sizeX, this.sizeY, this.sizeZ);
        float cu = (float) Math.max(-halfW, Math.min(halfW, u));
        float cv = (float) Math.max(-halfH, Math.min(halfH, v));

        // 组件缩放：夹在允许范围内，重叠判定也按缩放后的矩形计算
        float safeScale = Math.max(CompEntry.MIN_SCALE,
                Math.min(CompEntry.MAX_SCALE, Float.isFinite(scale) ? scale : CompEntry.DEFAULT_SCALE));

        if (overlapsOtherComp(id, existing.face, cu, cv, existing.type, safeScale)) {
            return COMP_DUP_POS;
        }

        int index = this.comps.indexOf(existing);
        // 颜色只接受 0xRRGGBB 或「未设置」（-1），其它值一律按未设置处理
        int safeColor = (color >= 0 && color <= 0xFFFFFF) ? color : CompEntry.NO_COLOR;
        // 显示设置只对数字显示屏生效，其它类型沿用原值（避免切换类型时留下无意义字段）
        int safeDecimals = existing.decimals;
        String safeUnit = existing.unit;
        if (existing.type == ConsoleCompType.NUMITRON) {
            safeDecimals = Math.max(0, Math.min(CompEntry.MAX_DECIMALS, decimals));
            safeUnit = unit == null ? "" : unit.trim();
            if (safeUnit.length() > CompEntry.MAX_UNIT_LEN) {
                safeUnit = safeUnit.substring(0, CompEntry.MAX_UNIT_LEN);
            }
        }
        // 弹起时间只对按钮 / 旋转按钮 / 安全按钮生效，其它类型沿用原值
        int safeHold = existing.holdTicks;
        if (existing.type == ConsoleCompType.KEYPAD
                || existing.type == ConsoleCompType.BUTTON_X
                || existing.type == ConsoleCompType.BUTTON_S) {
            safeHold = Math.max(0, Math.min(CompEntry.MAX_HOLD_TICKS, holdTicks));
        }
        // 盖子自动关闭时间只对安全按钮生效，其它类型沿用原值
        int safeCover = existing.coverTicks;
        if (existing.type == ConsoleCompType.BUTTON_S) {
            safeCover = Math.max(0, Math.min(CompEntry.MAX_COVER_TICKS, coverTicks));
        }
        // RGB 颜色控制器：连接组件与三个通道变量名（其它类型一律沿用空值）
        String safeLinks = "";
        String safeR = "";
        String safeG = "";
        String safeB = "";
        boolean safeShowOriginal = false;
        if (existing.type == ConsoleCompType.RGB_COLOR) {
            safeLinks = normalizeLinks(links, existing.id);
            safeR = normalizeChannel(rgbR);
            safeG = normalizeChannel(rgbG);
            safeB = normalizeChannel(rgbB);
            // 通道名非法时只丢弃该通道，不拒绝整次编辑——
            // 否则玩家填错一个名字会导致连颜色设置一起保存失败
            if (safeR == null) {
                safeR = "";
            }
            if (safeG == null) {
                safeG = "";
            }
            if (safeB == null) {
                safeB = "";
            }
            // 与主代号相同、或与前面的通道重名时同样丢弃该通道（保证启用的布尔变量不被占用）
            if (!safeR.isEmpty() && safeR.equalsIgnoreCase(normalized)) {
                safeR = "";
            }
            if (!safeG.isEmpty() && (safeG.equalsIgnoreCase(normalized)
                    || safeG.equalsIgnoreCase(safeR))) {
                safeG = "";
            }
            if (!safeB.isEmpty() && (safeB.equalsIgnoreCase(normalized)
                    || safeB.equalsIgnoreCase(safeR) || safeB.equalsIgnoreCase(safeG))) {
                safeB = "";
            }
            safeShowOriginal = showOriginal;
        }
        // 同步指示器：转速变量名（空 = 自动使用 代号 + "_s"；其它类型一律为空）
        String safeSpeedVar = "";
        if (existing.type.hasSpeedVariable()) {
            String candidate = speedVar == null ? "" : speedVar.trim();
            if (!candidate.isEmpty()) {
                if (!COMP_CODE_PATTERN.matcher(candidate).matches()
                        || candidate.equalsIgnoreCase(normalized)) {
                    return COMP_BAD_CODE;
                }
                if (codeUsed(candidate, id)
                        || nameBlocked(candidate, id, VarType.FLOAT, false)) {
                    return COMP_DUP_CODE;
                }
                // 与自动名称相同则仍按自动保存，之后改代号时转速变量名会跟着变
                if (!candidate.equalsIgnoreCase(normalized + "_s")) {
                    safeSpeedVar = candidate;
                }
            }
        }
        // 同步指示器：灯亮状态变量名（空 = 自动使用 代号 + "_b"；其它类型一律为空）
        String safeLampVar = "";
        if (existing.type.hasLampVariable()) {
            String candidate = lampVar == null ? "" : lampVar.trim();
            if (!candidate.isEmpty()) {
                if (!COMP_CODE_PATTERN.matcher(candidate).matches()
                        || candidate.equalsIgnoreCase(normalized)) {
                    return COMP_BAD_CODE;
                }
                // 不能与转速变量重名（两者都是同一组件的附加变量）
                String speedName = safeSpeedVar.isEmpty() ? normalized + "_s" : safeSpeedVar;
                if (candidate.equalsIgnoreCase(speedName)) {
                    return COMP_DUP_CODE;
                }
                if (codeUsed(candidate, id)
                        || nameBlocked(candidate, id, VarType.BOOL, false)) {
                    return COMP_DUP_CODE;
                }
                // 与自动名称相同则仍按自动保存，之后改代号时灯状态变量名会跟着变
                if (!candidate.equalsIgnoreCase(normalized + "_b")) {
                    safeLampVar = candidate;
                }
            }
        }
        // 警报器：静音状态变量名（空 = 自动使用 代号 + "_Q"；其它类型一律为空）
        String safeMuteVar = "";
        if (existing.type.hasMuteVariable()) {
            String candidate = muteVar == null ? "" : muteVar.trim();
            if (!candidate.isEmpty()) {
                if (!COMP_CODE_PATTERN.matcher(candidate).matches()
                        || candidate.equalsIgnoreCase(normalized)) {
                    return COMP_BAD_CODE;
                }
                if (codeUsed(candidate, id)
                        || nameBlocked(candidate, id, VarType.BOOL, false)) {
                    return COMP_DUP_CODE;
                }
                // 与自动名称相同则仍按自动保存，之后改代号时静音变量名会跟着变
                if (!candidate.equalsIgnoreCase(normalized + "_Q")) {
                    safeMuteVar = candidate;
                }
            }
        }
        // 钥匙模式只对钥匙开关生效，其它类型沿用原值
        int safeKeyMode = existing.keyMode;
        if (existing.type.hasKeyMode()) {
            safeKeyMode = Math.max(CompEntry.KEY_MODE_TOGGLE,
                    Math.min(CompEntry.MAX_KEY_MODE, keyMode));
        }
        CompEntry updated = new CompEntry(existing.id, existing.type, existing.face,
                cu, cv, normalized, safeColor, existing.text, existing.state, existing.value,
                existing.history, safeDecimals, safeUnit, safeHold, safeScale, normalized2,
                safeLinks, safeR, safeG, safeB, safeShowOriginal, safeSpeedVar, safeLampVar,
                safeCover, existing.hasKey, safeKeyMode,
                existing.muted, existing.pressButton, existing.pressUntil, safeMuteVar);
        this.comps.set(index, updated);
        // 名称回收：组件被删除后其变量会解除绑定，这里把同名且同类型的变量重新连上
        if (!updated.code.isEmpty()) {
            relinkVarToComp(updated.code, updated, false);
        }
        if (updated.type.supportsHistory() && !updated.code2.isEmpty()) {
            relinkVarToComp(updated.code2, updated, true);
        }
        // RGB 颜色控制器：按通道名自动创建 / 绑定浮点变量，并解除不再使用的通道绑定
        if (updated.type == ConsoleCompType.RGB_COLOR) {
            syncRgbVars(updated);
            // 部分成型时在设置界面手动改的灯色 / 显示原色同步写回变量，
            // 保证计算机读到的就是界面上设置的颜色
            writeRgbCompToVars(updated);
        }
        // 同步指示器：按（可能已修改的）代号重建转速附加变量绑定
        if (updated.type.hasSpeedVariable()) {
            syncSynchroVars(updated);
        }
        // 警报器：按（可能已修改的）代号重建静音附加变量绑定
        if (updated.type.hasMuteVariable()) {
            syncAlarmVars(updated);
        }
        sync();
        return COMP_OK;
    }

    /**
     * 修改组件的附带文字（服务端调用）。
     *
     * 附带文字的位置由组件类型固定决定、大小固定，因此只保存内容与样式。
     *
     * @param id    组件 id
     * @param text  文字内容（空 = 清除附带文字）
     * @param color 文字颜色（0xRRGGBB；-1 = 默认白）
     * @return {@link #COMP_OK} 或对应失败状态
     */
    public int applyCompText(int id, String text, int color,
                             boolean bold, boolean italic, boolean underline, boolean strike) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return COMP_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return COMP_NOT_FORMED;
        }
        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }
        String normalized = text == null ? ""
                : text.replace("\n", "").replace("\r", "").trim();
        CompText compText = normalized.isEmpty()
                ? CompText.NONE
                : new CompText(normalized, color, bold, italic, underline, strike);
        int index = this.comps.indexOf(existing);
        this.comps.set(index, new CompEntry(existing.id, existing.type, existing.face,
                existing.u, existing.v, existing.code, existing.color, compText,
                existing.state, existing.value,
                existing.history, existing.decimals, existing.unit, existing.holdTicks,
                existing.scale, existing.code2,
                existing.links, existing.rgbR, existing.rgbG, existing.rgbB,
                existing.showOriginal, existing.speedVar, existing.lampVar,
                existing.coverTicks, existing.hasKey, existing.keyMode,
                existing.muted, existing.pressButton, existing.pressUntil,
                existing.muteVar));
        sync();
        return COMP_OK;
    }

    /**
     * 操作一个带功能的组件（服务端调用）。
     *
     * 组件功能按类型区分：
     *  - 旋钮开关 / 拉杆开关：开（1）与关（0）两个状态来回切换；
     *  - 三状态开关（t_switch / t_lever）：按 0 → 1 → 2 → 0 循环切换三个档位；
     *  - 按钮面板：进入「按下」状态，并记录弹起时刻（当前游戏时间 + 配置时长）；
     *  - 调节器仪表：左下（红）按钮 -步进、右下（绿）按钮 +步进（按住 Shift 时步进为 10%），数值范围 0~100；
     *  - 其余组件（立式仪表 / 数码管 / 指示灯 / 发信终端等）暂不处理。
     *
     * 部分成型与完全成型状态都可以操作（完全成型只是锁定编辑，不影响使用）。
     *
     * @param id     组件 id
     * @param action 调节器仪表的按钮：0 = 左（减），1 = 右（加）；其它组件忽略
     * @param bigStep 是否使用大步进（Shift 右键，10%）
     * @param player 操作玩家（钥匙开关取出 / 拔出钥匙时需要；其它组件忽略）
     * @return {@link #COMP_OK} 或对应失败状态
     */
    public int applyCompUse(int id, int action, boolean bigStep,
                            net.minecraft.world.entity.player.Player player) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.UNFORMED || this.origin == null) {
            return COMP_NOT_FORMED;
        }
        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }

        long newState = existing.state;
        switch (existing.type) {
            case KNOB_SWITCH, LEVER_SWITCH, LEVER_F ->
                    newState = existing.state == 0L ? 1L : 0L;
            case T_SWITCH, T_LEVER ->
                    // 三状态开关：0 → 1 → 2 → 0 循环（越界值先归一化）
                    newState = Math.floorMod(existing.state + 1L, 3L);
            case KEYPAD, BUTTON_X -> {
                // 弹起时间为 0：进入「按住直到松手」状态，由客户端松手时发释放包清零；
                // 否则记录弹起时刻（组件单独设置优先，未设置时跟随配置文件）
                int hold = existing.effectiveHoldTicks();
                newState = hold <= 0
                        ? CompEntry.HOLD_UNTIL_RELEASE
                        : this.level.getGameTime() + hold;
            }
            case BUTTON_S -> {
                // 安全按钮：Shift 右键开关玻璃罩；开盖时右键按下按钮
                if (bigStep) {
                    if (existing.state == 0L) {
                        // 合盖 → 开盖（并开始计时自动关闭）
                        newState = CompEntry.OPEN_COVER;
                        openCover(existing);
                    } else if (existing.state == CompEntry.OPEN_COVER) {
                        // 开盖 → 合盖（按钮已弹起，可直接合盖）
                        newState = 0L;
                        closeCover(existing);
                    } else {
                        // 按钮按下期间不允许开关盖子（与独立安全按钮一致）
                        return COMP_OK;
                    }
                } else {
                    if (existing.state != CompEntry.OPEN_COVER) {
                        // 未开盖 / 已按下时不响应普通右键
                        return COMP_OK;
                    }
                    int hold = existing.effectiveHoldTicks();
                    newState = hold <= 0
                            ? CompEntry.HOLD_UNTIL_RELEASE
                            : this.level.getGameTime() + hold;
                    // 按下期间暂停自动关盖计时，弹起后重新计时
                    this.coverDeadlines.remove(existing.id);
                    this.level.playSound(null, this.worldPosition,
                            net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.6F);
                }
            }
            case SWITCH_K -> {
                // 钥匙开关：未拔 / 已拔钥匙由 hasKey 区分，变量只看 state
                if (!existing.hasKey) {
                    // 成型前（部分成型）Shift+右键取出一把匹配的钥匙；成型后无钥匙不能操作
                    if (bigStep && this.formState == State.PARTIAL && player != null) {
                        giveKeyItem(player, existing);
                    }
                    return COMP_OK;
                }
                if (bigStep) {
                    // Shift+右键拔钥匙：常开只有开时能拔，常闭只有关时能拔，开闭都可以
                    boolean canRemove = switch (existing.keyMode) {
                        case CompEntry.KEY_MODE_ALWAYS_OPEN -> existing.state != 0L;
                        case CompEntry.KEY_MODE_ALWAYS_CLOSED -> existing.state == 0L;
                        default -> true;
                    };
                    if (!canRemove) {
                        return COMP_OK;
                    }
                    if (player != null) {
                        giveKeyItem(player, existing);
                    }
                    int keyIndex = this.comps.indexOf(existing);
                    this.comps.set(keyIndex, CompEntry.copyWithKey(existing, false));
                    this.level.playSound(null, this.worldPosition,
                            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.2F);
                    sync();
                    return COMP_OK;
                }
                // 有钥匙时普通右键切换开 / 关
                newState = existing.state == 0L ? 1L : 0L;
            }
            case GAUGE_ARC -> {
                long step = bigStep ? 10L : 1L;
                // action 0 = 左按钮（红，减），1 = 右按钮（绿，加）
                long delta = action == 0 ? -step : action == 1 ? step : 0L;
                newState = Math.max(0L, Math.min(100L, existing.state + delta));
            }
            case ALARM -> {
                // action 2 = 静音（中），3 = 重置（右）；左侧方形灯只显示，不在这里处理
                long pressUntil = this.level.getGameTime() + ALARM_PRESS_TICKS;
                if (action == 2) {
                    // 静音：<代号>_Q 置 true（下一 tick 由 applyAlarms 镜像到变量），灯常亮、停止发声
                    CompEntry updated = CompEntry.copyWithMuted(existing, true);
                    updated = CompEntry.copyWithPress(updated, 1, pressUntil);
                    this.comps.set(this.comps.indexOf(existing), updated);
                    sync();
                    return COMP_OK;
                }
                if (action != 3) {
                    // 未识别的按钮区域：不做任何改变
                    return COMP_OK;
                }
                // 重置：把自身变量写回 false、灯熄灭（静音由下一 tick 的 applyAlarms 一并解除）
                CompEntry updated = CompEntry.copyWithMuted(CompEntry.copyWith(existing, 0L,
                        existing.value), false);
                updated = CompEntry.copyWithPress(updated, 2, pressUntil);
                this.comps.set(this.comps.indexOf(existing), updated);
                writeBackToVar(updated);
                sync();
                return COMP_OK;
            }
            default -> {
                // 其它组件暂无功能：直接返回成功，不产生无意义的同步
                return COMP_OK;
            }
        }
        if (newState == existing.state) {
            return COMP_OK;
        }

        int index = this.comps.indexOf(existing);
        // 弧形仪表手动调节后，模拟量同步为新的百分比；三状态开关同步为档位值；其它组件保持原值
        float newValue = existing.type == ConsoleCompType.GAUGE_ARC
                || existing.type.isThreeState() ? (float) newState : existing.value;
        this.comps.set(index, CompEntry.copyWith(existing, newState, newValue));
        // 玩家手动操作组件后回写绑定变量，保证下次打开程序界面看到的是最新值
        writeBackToVar(this.comps.get(index));
        sync();
        return COMP_OK;
    }

    /**
     * 给玩家一把与组件匹配的控制柜钥匙（取出 / 拔下钥匙时调用）。
     *
     * 钥匙名字取「控制柜钥匙：<组件附带文字>」，附带文字为空时用组件代号；
     * NBT 记录组件代号，插入时只按代号匹配（名字不参与匹配）。
     */
    private void giveKeyItem(net.minecraft.world.entity.player.Player player, CompEntry comp) {
        String label = comp.text.isEmpty() ? comp.code : comp.text.text;
        net.minecraft.world.item.ItemStack key = com.cc_rc.item.ConsoleKeyItem.createKey(
                comp.code, label);
        if (!player.getInventory().add(key)) {
            player.drop(key, false);
        }
        this.level.playSound(null, this.worldPosition,
                net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.2F);
    }

    /**
     * 把「控制柜钥匙」插入钥匙开关（服务端调用，由钥匙物品右键触发）。
     *
     * 校验：组件为钥匙开关、当前无钥匙、钥匙 NBT 里的组件代号与本组件一致。
     * 通过后消耗一把钥匙（创造模式不消耗）并把组件标记为有钥匙。
     *
     * @param id    组件 id
     * @param stack 玩家手里的钥匙物品
     * @param player 操作玩家
     * @return {@link #COMP_OK} 或对应失败状态
     */
    public int applyKeyInsert(int id, net.minecraft.world.item.ItemStack stack,
                              net.minecraft.world.entity.player.Player player) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.UNFORMED || this.origin == null) {
            return COMP_NOT_FORMED;
        }
        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }
        if (existing.type != ConsoleCompType.SWITCH_K || existing.hasKey) {
            return COMP_INVALID;
        }
        // 只按 NBT 里的组件代号匹配（名字不管）
        String keyCode = com.cc_rc.item.ConsoleKeyItem.keyCode(stack);
        if (keyCode.isEmpty() || existing.code.isEmpty()
                || !existing.code.equalsIgnoreCase(keyCode)) {
            return COMP_INVALID;
        }
        // 插入后消耗钥匙（创造模式不消耗，方便反复调试）
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        int index = this.comps.indexOf(existing);
        this.comps.set(index, CompEntry.copyWithKey(existing, true));
        this.level.playSound(null, this.worldPosition,
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.4F);
        sync();
        return COMP_OK;
    }

    /** 删除一个组件（服务端调用）。 */
    public int applyCompDelete(int id) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return COMP_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return COMP_NOT_FORMED;
        }
        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }
        this.comps.remove(existing);
        // 删除安全按钮时一并清理它的自动关盖时刻
        this.coverDeadlines.remove(existing.id);
        // 组件删除后变量解除绑定（变量本身保留），同名同类型设备重新注册时会自动重连
        unbindVarsFromComp(existing.id);
        sync();
        return COMP_OK;
    }

    /** 某个名称是否已被其它组件占用（主代号与第二名称都参与判重；exceptId 为自己）。 */
    private boolean codeUsed(String code, int exceptId) {
        // 空名称不参与判重（未命名的 RGB 控制器可以存在多个）
        if (code == null || code.isEmpty()) {
            return false;
        }
        for (CompEntry entry : this.comps) {
            if (entry.id != exceptId
                    && (entry.code.equalsIgnoreCase(code) || entry.code2.equalsIgnoreCase(code))) {
                return true;
            }
        }
        return false;
    }

    /** 生成一个未被占用的默认代号：类型前缀_递增序号。 */
    private String nextFreeCode(ConsoleCompType type) {
        for (int i = 1; i < 10000; i++) {
            String candidate = type.codePrefix() + "_" + i;
            if (!codeUsed(candidate, -1)) {
                return candidate;
            }
        }
        return type.codePrefix() + "_" + System.currentTimeMillis();
    }

    /**
     * 生成圆盘记录仪的第二名称（推送触发名称）：<主代号>_T，被占用时追加序号。
     * 第二名称只用于绑定一个布尔触发变量，不参与组件查找。
     */
    private String nextFreeCode2(String base) {
        String candidate = base + "_T";
        if (!codeUsed(candidate, -1)) {
            return candidate;
        }
        for (int i = 2; i < 10000; i++) {
            candidate = base + "_T" + i;
            if (!codeUsed(candidate, -1)) {
                return candidate;
            }
        }
        return base + "_T" + System.currentTimeMillis();
    }

    /**
     * 该名称能否用作指定角色的设备名称（不能则返回 true）。
     *
     * 规则：
     *  - 变量仍绑定在其它「现存」组件上 → 占用；
     *  - 变量已解除绑定（组件被删除）但类型与当前角色不符 → 占用；
     *  - 已解除绑定且类型一致 → 允许，随后由 {@link #relinkVarToComp} 重新连接。
     */
    private boolean nameBlocked(String name, int compId, VarType expected, boolean trigger) {
        int slot = findVarSlot(name);
        if (slot <= 0 || this.varDefs[slot] == null) {
            return false;
        }
        VarDef def = this.varDefs[slot];
        // 仍绑定在其它现存组件：名称不能共用
        if (def.compId != 0 && def.compId != compId && findComp(def.compId) != null) {
            return true;
        }
        // 需要回收的同名变量必须与设备角色类型一致，否则阻止创建，避免类型冲突
        return expected == null || def.type != expected;
    }

    /**
     * 把已存在的同名「未绑定」变量重新连接到组件（删除组件后再次注册同名设备时使用）。
     *
     * @param trigger 是否按「推送触发」角色连接（圆盘记录仪第二名称）
     * @return 是否发生了绑定变化
     */
    private boolean relinkVarToComp(String name, CompEntry comp, boolean trigger) {
        int slot = findVarSlot(name);
        if (slot <= 0 || this.varDefs[slot] == null) {
            return false;
        }
        VarDef def = this.varDefs[slot];
        VarType expected = trigger ? VarType.BOOL : comp.type.variableType();
        if (expected == null || def.type != expected) {
            return false;
        }
        // 仍绑定在其它现存组件时不抢夺
        if (def.compId != 0 && def.compId != comp.id && findComp(def.compId) != null) {
            return false;
        }
        if (def.compId == comp.id && def.trigger == trigger) {
            return false;
        }
        this.varDefs[slot] = new VarDef(slot, def.name, def.type, comp.id, trigger);
        // 绑定关系变化会影响程序读取来源，作废已编译字节码
        this.programDirty = true;
        return true;
    }

    /**
     * 组件被删除时解除其变量的绑定（变量本身保留，名称与类型不变）。
     * 之后再次注册同名且同类型的设备，会通过 {@link #relinkVarToComp} 自动重连。
     */
    private void unbindVarsFromComp(int compId) {
        int slots = varSlots();
        boolean changed = false;
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def == null || def.compId != compId) {
                continue;
            }
            // 解除绑定后按普通变量处理，触发角色一并清除
            this.varDefs[i] = new VarDef(i, def.name, def.type, 0, false);
            changed = true;
        }
        if (changed) {
            this.programDirty = true;
        }
    }

    // ===================== RGB 颜色控制器的名称 / 变量处理 =====================

    /**
     * 规范化「连接的组件」列表：支持中英文分号分隔，去掉空项、自身与重复项。
     * 非法代号直接忽略（不会让整次编辑失败）。
     */
    private String normalizeLinks(String raw, int selfId) {
        if (raw == null) {
            return "";
        }
        CompEntry self = findComp(selfId);
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String part : raw.split("[;；]")) {
            String token = part.trim();
            if (token.isEmpty() || !COMP_CODE_PATTERN.matcher(token).matches()) {
                continue;
            }
            if (self != null && token.equalsIgnoreCase(self.code)) {
                continue;
            }
            boolean exists = seen.stream().anyMatch(s -> s.equalsIgnoreCase(token));
            if (!exists) {
                seen.add(token);
            }
            // 连接数量上限：避免配置文件里写入超长列表
            if (seen.size() >= 32) {
                break;
            }
        }
        return String.join(";", seen);
    }

    /** 规范化一个通道变量名：空返回空字符串，格式非法返回 null。 */
    private static String normalizeChannel(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return "";
        }
        return VAR_NAME_PATTERN.matcher(name).matches() ? name : null;
    }

    /** 该变量名是否是某个 RGB 控制器当前使用的通道名。 */
    private static boolean matchesChannel(String name, CompEntry comp) {
        return name.equalsIgnoreCase(comp.rgbR) || name.equalsIgnoreCase(comp.rgbG)
                || name.equalsIgnoreCase(comp.rgbB);
    }

    /**
     * 同步 RGB 控制器的变量绑定：
     *  - 解除不再使用的通道变量绑定（变量本身保留）；
     *  - 为三个通道名自动创建浮点变量（已存在同名浮点变量时直接绑定）。
     */
    private void syncRgbVars(CompEntry comp) {
        boolean changed = false;
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def == null || def.compId != comp.id) {
                continue;
            }
            boolean keep = def.type == VarType.BOOL
                    ? def.name.equalsIgnoreCase(comp.code)
                    : matchesChannel(def.name, comp);
            if (!keep) {
                this.varDefs[i] = new VarDef(i, def.name, def.type, 0, false);
                changed = true;
            }
        }
        for (String name : new String[]{comp.rgbR, comp.rgbG, comp.rgbB}) {
            if (!name.isEmpty()) {
                changed |= ensureChannelVar(name, comp);
            }
        }
        if (changed) {
            this.programDirty = true;
        }
    }

    /**
     * 把组件的灯色 / 显示原色写回已注册的变量（部分成型手动保存时调用）。
     *
     * RGB 的颜色与三个通道变量是同一份数据：手动设色后变量应立刻反映该颜色，
     * 启用布尔变量取「显示原色」的反值。未着色（原色）时不动通道变量。
     */
    private void writeRgbCompToVars(CompEntry comp) {
        if (comp.color != CompEntry.NO_COLOR) {
            int slot = findRgbChannelSlot(comp, comp.rgbR);
            if (slot > 0) {
                this.varFloats[slot] = (comp.color >> 16) & 0xFF;
            }
            slot = findRgbChannelSlot(comp, comp.rgbG);
            if (slot > 0) {
                this.varFloats[slot] = (comp.color >> 8) & 0xFF;
            }
            slot = findRgbChannelSlot(comp, comp.rgbB);
            if (slot > 0) {
                this.varFloats[slot] = comp.color & 0xFF;
            }
        }
        int boolSlot = findRgbBoolSlot(comp.id);
        if (boolSlot > 0) {
            this.varBools[boolSlot] = !comp.showOriginal;
        }
    }

    /** 确保某个通道名对应一个绑定到该组件的浮点变量（必要时占用空槽位创建）。 */
    private boolean ensureChannelVar(String name, CompEntry comp) {
        int slot = findVarSlot(name);
        if (slot > 0) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                return false;
            }
            // 仍绑定在其它现存组件：不抢夺（本次编辑仍会保存颜色，只是该通道未登记）
            if (def.compId != 0 && def.compId != comp.id && findComp(def.compId) != null) {
                return false;
            }
            // 已绑定在本组件且类型正确：无需处理
            if (def.compId == comp.id && !def.trigger && def.type == VarType.FLOAT) {
                return false;
            }
            // 类型不一致（未绑定或本组件名下的旧变量）：按通道重新登记为浮点变量，
            // 避免用户先建了布尔/其它类型变量后无法再作为颜色通道
            this.varDefs[slot] = new VarDef(slot, def.name, VarType.FLOAT, comp.id, false);
            this.varFloats[slot] = 0.0F;
            this.varBools[slot] = false;
            return true;
        }
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            if (this.varDefs[i] != null) {
                continue;
            }
            this.varDefs[i] = new VarDef(i, name, VarType.FLOAT, comp.id, false);
            this.varFloats[i] = 0.0F;
            this.varBools[i] = false;
            return true;
        }
        return false;
    }

    /** 该 RGB 控制器主名称对应的布尔变量槽位（没有返回 -1）。 */
    private int findRgbBoolSlot(int compId) {
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def != null && def.compId == compId && !def.trigger
                    && def.type == VarType.BOOL) {
                return i;
            }
        }
        return -1;
    }

    /** 某个通道名绑定到本组件的浮点变量槽位（没有返回 -1）。 */
    private int findRgbChannelSlot(CompEntry comp, String channel) {
        if (channel.isEmpty()) {
            return -1;
        }
        int slot = findVarSlot(channel);
        if (slot <= 0) {
            return -1;
        }
        VarDef def = this.varDefs[slot];
        return def != null && def.type == VarType.FLOAT && def.compId == comp.id
                && !def.trigger ? slot : -1;
    }

    // ===================== 同步指示器的附加变量（转速 / 灯状态） =====================

    /** 该变量名是否是某同步指示器的转速附加变量（自定义名，未自定义时为 代号 + "_s"）。 */
    private static boolean isSynchroSpeedName(String name, CompEntry comp) {
        return comp != null && comp.type.hasSpeedVariable() && !comp.code.isEmpty()
                && name != null && name.equalsIgnoreCase(comp.speedVarName());
    }

    /** 该变量名是否是某同步指示器的灯亮状态附加变量（代号 + "_b"，只读输出）。 */
    private static boolean isSynchroLampName(String name, CompEntry comp) {
        return comp != null && comp.type.hasLampVariable() && !comp.code.isEmpty()
                && name != null && name.equalsIgnoreCase(comp.lampVarName());
    }

    /** 该变量名是否是某警报器的静音状态附加变量（代号 + "_Q"，只读输出）。 */
    private static boolean isAlarmMuteName(String name, CompEntry comp) {
        return comp != null && comp.type.hasMuteVariable() && !comp.code.isEmpty()
                && name != null && name.equalsIgnoreCase(comp.muteVarName());
    }

    /**
     * 同步指示器的附加变量绑定：
     *  - 组件改名后解除旧附加变量的绑定（变量本身保留）；
     *  - 按新代号重新创建 / 重连 「转速（代号_s，浮点）」与「灯亮状态（代号_b，布尔）」。
     */
    private void syncSynchroVars(CompEntry comp) {
        boolean changed = false;
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def == null || def.compId != comp.id || def.trigger) {
                continue;
            }
            boolean keep = def.name.equalsIgnoreCase(comp.code)
                    || isSynchroSpeedName(def.name, comp)
                    || isSynchroLampName(def.name, comp);
            if (!keep) {
                this.varDefs[i] = new VarDef(i, def.name, def.type, 0, false);
                changed = true;
            }
        }
        // 组件编辑（含移动 / 改名）不允许自动创建变量：变量只能由玩家在变量表里显式新增，
        // 这里只把已存在的附加变量重连到组件
        if (!comp.code.isEmpty()) {
            changed |= ensureSynchroSpeedVar(comp, false);
            changed |= ensureSynchroLampVar(comp, false);
        }
        if (changed) {
            this.programDirty = true;
        }
    }

    /**
     * 确保转速变量绑定到该组件。
     *
     * @param create true = 同名浮点变量不存在时占用空槽位创建（设置界面点「完成」时）；
     *               false = 只重连已存在的同名变量，不创建（放置组件时）
     */
    private boolean ensureSynchroSpeedVar(CompEntry comp, boolean create) {
        String name = comp.speedVarName();
        int slot = findVarSlot(name);
        if (slot > 0) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                return false;
            }
            // 仍绑定在其它现存组件：不抢夺
            if (def.compId != 0 && def.compId != comp.id && findComp(def.compId) != null) {
                return false;
            }
            if (def.compId == comp.id && !def.trigger && def.type == VarType.FLOAT) {
                return false;
            }
            this.varDefs[slot] = new VarDef(slot, def.name, VarType.FLOAT, comp.id, false);
            this.varFloats[slot] = 0.0F;
            this.varBools[slot] = false;
            return true;
        }
        // 放置组件时不允许自动创建变量
        if (!create) {
            return false;
        }
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            if (this.varDefs[i] != null) {
                continue;
            }
            this.varDefs[i] = new VarDef(i, name, VarType.FLOAT, comp.id, false);
            this.varFloats[i] = 0.0F;
            this.varBools[i] = false;
            return true;
        }
        return false;
    }

    /**
     * 确保灯亮状态布尔变量绑定到该组件（只读输出，tick 中按实际判断值覆盖）。
     *
     * @param create true = 同名布尔变量不存在时占用空槽位创建（设置界面点「完成」时）；
     *               false = 只重连已存在的同名变量，不创建（放置组件时）
     */
    private boolean ensureSynchroLampVar(CompEntry comp, boolean create) {
        String name = comp.lampVarName();
        int slot = findVarSlot(name);
        if (slot > 0) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                return false;
            }
            // 仍绑定在其它现存组件：不抢夺
            if (def.compId != 0 && def.compId != comp.id && findComp(def.compId) != null) {
                return false;
            }
            if (def.compId == comp.id && !def.trigger && def.type == VarType.BOOL) {
                return false;
            }
            this.varDefs[slot] = new VarDef(slot, def.name, VarType.BOOL, comp.id, false);
            this.varFloats[slot] = 0.0F;
            this.varBools[slot] = comp.state != 0L;
            return true;
        }
        // 放置组件时不允许自动创建变量
        if (!create) {
            return false;
        }
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            if (this.varDefs[i] != null) {
                continue;
            }
            this.varDefs[i] = new VarDef(i, name, VarType.BOOL, comp.id, false);
            this.varFloats[i] = 0.0F;
            this.varBools[i] = comp.state != 0L;
            return true;
        }
        return false;
    }

    // ===================== 警报器的附加变量（静音状态 _Q） =====================

    /**
     * 警报器的附加变量绑定：
     *  - 组件改名后解除旧静音变量的绑定（变量本身保留）；
     *  - 只把已存在的 &lt;代号&gt;_Q（布尔，只读输出）重连到该组件，不创建新变量。
     */
    private void syncAlarmVars(CompEntry comp) {
        boolean changed = false;
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            VarDef def = this.varDefs[i];
            if (def == null || def.compId != comp.id || def.trigger) {
                continue;
            }
            boolean keep = def.name.equalsIgnoreCase(comp.code)
                    || isAlarmMuteName(def.name, comp);
            if (!keep) {
                this.varDefs[i] = new VarDef(i, def.name, def.type, 0, false);
                changed = true;
            }
        }
        // 组件编辑（含移动 / 改名）不允许自动创建变量：变量只能由玩家在变量表里显式新增
        if (!comp.code.isEmpty()) {
            changed |= ensureAlarmMuteVar(comp, false);
        }
        if (changed) {
            this.programDirty = true;
        }
    }

    /**
     * 确保静音状态布尔变量（&lt;代号&gt;_Q，只读输出）绑定到该警报器。
     *
     * @param create true = 同名布尔变量不存在时占用空槽位创建（放置 / 设置界面点「完成」时）；
     *               false = 只重连已存在的同名变量，不创建
     */
    private boolean ensureAlarmMuteVar(CompEntry comp, boolean create) {
        String name = comp.muteVarName();
        int slot = findVarSlot(name);
        if (slot > 0) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                return false;
            }
            // 仍绑定在其它现存组件：不抢夺
            if (def.compId != 0 && def.compId != comp.id && findComp(def.compId) != null) {
                return false;
            }
            if (def.compId == comp.id && !def.trigger && def.type == VarType.BOOL) {
                return false;
            }
            this.varDefs[slot] = new VarDef(slot, def.name, VarType.BOOL, comp.id, false);
            this.varFloats[slot] = 0.0F;
            this.varBools[slot] = comp.muted;
            return true;
        }
        // 放置组件时允许自动创建：_Q 是警报器的固有附加变量
        if (!create) {
            return false;
        }
        int slots = varSlots();
        for (int i = 1; i < slots; i++) {
            if (this.varDefs[i] != null) {
                continue;
            }
            this.varDefs[i] = new VarDef(i, name, VarType.BOOL, comp.id, false);
            this.varFloats[i] = 0.0F;
            this.varBools[i] = comp.muted;
            return true;
        }
        return false;
    }

    /**
     * RGB 颜色控制器结算（服务端 tick）。
     *
     * 启用条件：注册了主名称布尔变量时以变量值为准，否则由「显示原色」开关决定；
     * 启用且至少有一个通道变量时，按 R/G/B 计算颜色，并把同色写入「连接的组件」；
     * 停用（或没有通道变量）时自身恢复原色，连接组件保持当前颜色不动。
     */
    private void applyRgbControllers() {
        if (this.comps.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < this.comps.size(); i++) {
            CompEntry comp = this.comps.get(i);
            if (comp.type != ConsoleCompType.RGB_COLOR) {
                continue;
            }
            int boolSlot = findRgbBoolSlot(comp.id);
            // 自愈：通道名已填写但对应浮点变量缺失（旧存档 / 手工改档）时补建
            if ((!comp.rgbR.isEmpty() && findRgbChannelSlot(comp, comp.rgbR) <= 0)
                    || (!comp.rgbG.isEmpty() && findRgbChannelSlot(comp, comp.rgbG) <= 0)
                    || (!comp.rgbB.isEmpty() && findRgbChannelSlot(comp, comp.rgbB) <= 0)) {
                syncRgbVars(comp);
                changed = true;
            }
            boolean enabled = boolSlot > 0 ? this.varBools[boolSlot] : !comp.showOriginal;
            int rSlot = findRgbChannelSlot(comp, comp.rgbR);
            int gSlot = findRgbChannelSlot(comp, comp.rgbG);
            int bSlot = findRgbChannelSlot(comp, comp.rgbB);
            boolean hasChannel = rSlot > 0 || gSlot > 0 || bSlot > 0;
            // 目标颜色：启用且有通道变量时取通道值；停用时强制原色（NO_COLOR）；
            // 启用但没有任何通道变量时保留当前颜色（允许在组件设置里手动设置灯色）
            int target = enabled ? comp.color : CompEntry.NO_COLOR;
            if (enabled && hasChannel) {
                int r = channelValue(rSlot);
                int g = channelValue(gSlot);
                int b = channelValue(bSlot);
                target = (r << 16) | (g << 8) | b;
            }
            if (comp.color != target) {
                comp = CompEntry.copyWithColor(comp, target);
                this.comps.set(i, comp);
                changed = true;
            }
            if (!enabled || !hasChannel) {
                // 停用：只恢复自身原色，连接组件保持最后颜色
                continue;
            }
            for (String link : comp.links.split(";")) {
                CompEntry linked = findCompByCode(link);
                if (linked == null || linked.id == comp.id || linked.color == target) {
                    continue;
                }
                this.comps.set(this.comps.indexOf(linked),
                        CompEntry.copyWithColor(linked, target));
                changed = true;
            }
        }
        if (changed) {
            sync();
        }
    }

    /** 通道变量的数值 → 0~255 颜色分量（非有限值按 0 处理）。 */
    private int channelValue(int slot) {
        if (slot <= 0) {
            return 0;
        }
        float value = this.varFloats[slot];
        if (!Float.isFinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    /**
     * 同步指示器结算（服务端 tick，仅完全成型时调用）。
     *
     * 指针角度保存在组件的 value 中（-180~180，正上方 = 0），每 tick 按
     * 转速附加变量（&lt;代号&gt;_s，度/tick）推进并在 ±180 处环绕；
     * 转速绝对值小于配置阈值且角度在 ±15° 内时切换为「灯亮」模型（state 1），
     * 否则为「灯灭」模型（state 0）。角度同时回写到主变量，供程序 / 计算机读取。
     */
    private void applySynchroscopes() {
        if (this.comps.isEmpty()) {
            return;
        }
        float speedLimit = com.cc_rc.Config.getSynchroscopeLampSpeed();
        boolean changed = false;
        for (int i = 0; i < this.comps.size(); i++) {
            CompEntry comp = this.comps.get(i);
            if (comp.type != ConsoleCompType.SYNCHROSCOPE) {
                continue;
            }
            // 转速取自 <代号>_s 浮点变量；没有该变量时指针保持静止
            float speed = 0.0F;
            int speedSlot = comp.code.isEmpty() ? -1 : findVarSlot(comp.speedVarName());
            if (speedSlot > 0) {
                VarDef def = this.varDefs[speedSlot];
                if (def != null && def.type == VarType.FLOAT && def.compId == comp.id) {
                    float raw = this.varFloats[speedSlot];
                    speed = Float.isFinite(raw) ? raw : 0.0F;
                }
            }
            // 指针角度：先把存档值归一到 ±180，再按转速推进
            float angle = wrapAngle180(comp.value);
            if (speed != 0.0F) {
                angle = wrapAngle180(angle + speed);
            }
            boolean lamp = Math.abs(speed) < speedLimit
                    && Math.abs(angle) <= SYNCHRO_LAMP_ANGLE_DEG;
            long state = lamp ? 1L : 0L;
            // 灯亮状态附加布尔变量（<代号>_b）：只读输出，每 tick 用实际判断值覆盖，
            // 因此 CC / 程序手动写入的值会在下一 tick 被真实状态顶掉
            if (!comp.code.isEmpty()) {
                int lampSlot = findVarSlot(comp.lampVarName());
                if (lampSlot > 0) {
                    VarDef lampDef = this.varDefs[lampSlot];
                    if (lampDef != null && lampDef.type == VarType.BOOL
                            && lampDef.compId == comp.id && this.varBools[lampSlot] != lamp) {
                        this.varBools[lampSlot] = lamp;
                    }
                }
            }
            if (comp.value == angle && comp.state == state) {
                continue;
            }
            CompEntry updated = CompEntry.copyWith(comp, state, angle);
            this.comps.set(i, updated);
            // 指针角度回写主变量（findVarSlotByComp 会跳过转速附加变量）
            writeBackToVar(updated);
            changed = true;
        }
        if (changed) {
            sync();
        }
    }

    /**
     * 警报器结算（服务端 tick，仅完全成型时调用）。
     *
     * 主布尔变量驱动组件的 state：true = 报警中，false = 解除（由变量写入 / 重置按钮更新）。
     *  - 报警解除时自动解除静音，保证下一次「false → true」上升沿一定是未静音状态；
     *  - 报警中且未静音时每 {@link #ALARM_SOUND_INTERVAL} tick 从组件位置播放一次占位音效
     *    （之后替换为专用音频）；灯的闪烁由客户端按游戏时间计算，不需要逐 tick 同步；
     *  - 静音状态每 tick 镜像到只读布尔变量 &lt;代号&gt;_Q。
     */
    private void applyAlarms() {
        if (this.comps.isEmpty()) {
            return;
        }
        boolean changed = false;
        long now = this.level.getGameTime();
        for (int i = 0; i < this.comps.size(); i++) {
            CompEntry comp = this.comps.get(i);
            if (comp.type != ConsoleCompType.ALARM) {
                continue;
            }
            // 按钮按下反馈到期后清除（只影响短暂的「按下」模型）
            if (comp.pressUntil != 0L && now >= comp.pressUntil) {
                comp = CompEntry.copyWithPress(comp, 0, 0L);
                this.comps.set(i, comp);
                changed = true;
            }
            boolean active = comp.state != 0L;
            // 报警解除（重置按钮 / 变量被写回 false）时解除静音
            if (!active && comp.muted) {
                comp = CompEntry.copyWithMuted(comp, false);
                this.comps.set(i, comp);
                changed = true;
            }
            // 静音状态镜像到只读变量 <代号>_Q
            if (!comp.code.isEmpty()) {
                int slot = findVarSlot(comp.muteVarName());
                if (slot > 0) {
                    VarDef def = this.varDefs[slot];
                    if (def != null && def.type == VarType.BOOL && def.compId == comp.id
                            && this.varBools[slot] != comp.muted) {
                        this.varBools[slot] = comp.muted;
                        changed = true;
                    }
                }
            }
            // 报警中且未静音：从组件所在位置周期性播放占位音效
            if (active && !comp.muted && (now + comp.id) % ALARM_SOUND_INTERVAL == 0L) {
                net.minecraft.world.phys.Vec3 pos = ConsoleSurface.toWorld(
                        this.origin, this.sizeX, this.sizeY, this.sizeZ,
                        comp.face, getBusFacing(), comp.u, comp.v, 0.1D);
                this.level.playSound(null, pos.x, pos.y, pos.z,
                        net.minecraft.sounds.SoundEvents.BELL_BLOCK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }
        if (changed) {
            sync();
        }
    }

    /** 把角度（度）环绕到 -180~180（185 → -175，-185 → 175）。 */
    private static float wrapAngle180(float degrees) {
        if (!Float.isFinite(degrees)) {
            return 0.0F;
        }
        float value = degrees % 360.0F;
        if (value > 180.0F) {
            value -= 360.0F;
        } else if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    /** 该位置的组件矩形是否与其它组件重叠（exceptId 为自己；尺寸含组件缩放）。 */
    private boolean overlapsOtherComp(int exceptId, Direction face, float u, float v,
                                      ConsoleCompType type, float scale) {
        float halfW = type.widthPx * type.renderScale * scale / 2.0F;
        float halfH = type.heightPx * type.renderScale * scale / 2.0F;
        for (CompEntry entry : this.comps) {
            if (entry.id == exceptId || entry.face != face) {
                continue;
            }
            // 轴对齐矩形相交判定（两个矩形都以外接矩形近似）
            boolean overlapU = Math.abs(u - entry.u)
                    < halfW + entry.halfWidthPx();
            boolean overlapV = Math.abs(v - entry.v)
                    < halfH + entry.halfHeightPx();
            if (overlapU && overlapV) {
                return true;
            }
        }
        return false;
    }

    /** 当前总线水平朝向（顶/底面文字朝向依赖它）。 */
    public Direction getBusFacing() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(ConsoleBusBlock.FACING)) {
            return state.getValue(ConsoleBusBlock.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * 新增或修改一条表面文字（服务端调用）。
     *
     * @param id      已有文字 id；-1 表示新建（位置取 face/u/v）
     * @param face    文字所在的结构外表面（外法线方向）
     * @param u       锚点相对该表面中心的横向像素偏移
     * @param v       锚点相对该表面中心的纵向像素偏移
     * @param text    文字内容；已有文字被改为空 = 删除该条文字
     * @param anchorH 水平锚点：0 左 / 1 中 / 2 右
     * @param anchorV 垂直锚点：0 上 / 1 中 / 2 下
     * @param scale   文字缩放倍率（1.0 = 1 字体像素等于 1 结构像素，默认 0.25）
     * @return {@link #TEXT_OK} 或对应失败状态
     */
    public int applyTextEdit(int id, Direction face, float u, float v, String text,
                             int color, boolean bold, boolean italic,
                             boolean underline, boolean strikethrough,
                             int anchorH, int anchorV, float scale) {
        if (this.level == null || this.level.isClientSide) {
            return TEXT_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return TEXT_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return TEXT_NOT_FORMED;
        }

        // 统一去掉换行（表面文字为单行）
        String normalized = text == null ? "" : text.replace("\n", "").replace("\r", "");
        int h = Math.max(0, Math.min(2, anchorH));
        int w2 = Math.max(0, Math.min(2, anchorV));
        float s = Math.max(TextEntry.MIN_SCALE, Math.min(TextEntry.MAX_SCALE, scale));

        // 锚点始终夹在表面矩形内，避免文字整体跑到结构外面
        Direction busFacing = getBusFacing();
        double halfW = ConsoleSurface.halfWidthPx(face, busFacing, this.sizeX, this.sizeY, this.sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(face, busFacing, this.sizeX, this.sizeY, this.sizeZ);
        float cu = (float) Math.max(-halfW, Math.min(halfW, u));
        float cv = (float) Math.max(-halfH, Math.min(halfH, v));

        TextEntry existing = findText(id);
        if (existing != null) {
            if (normalized.trim().isEmpty()) {
                // 已有文字被清空 → 删除
                this.texts.remove(existing);
            } else {
                // 修改内容/样式/位置/锚点/缩放
                int index = this.texts.indexOf(existing);
                this.texts.set(index, new TextEntry(existing.id, existing.face, cu, cv,
                        normalized, color, bold, italic, underline, strikethrough, h, w2, s));
            }
            sync();
            return TEXT_OK;
        }

        // 新建：空文本直接忽略（不创建不可见的文字）
        if (normalized.trim().isEmpty()) {
            return TEXT_OK;
        }
        if (this.texts.size() >= MAX_TEXTS) {
            return TEXT_LIMIT;
        }
        this.texts.add(new TextEntry(this.nextTextId++, face, cu, cv, normalized,
                color, bold, italic, underline, strikethrough, h, w2, s));
        sync();
        return TEXT_OK;
    }

    /**
     * 拖动表面文字：只改锚点位置，内容 / 样式 / 锚点方向 / 缩放保持不变（服务端调用）。
     *
     * 用于多功能工具拖动，校验规则与 {@link #applyTextEdit} 一致。
     *
     * @return {@link #TEXT_OK}；找不到文字返回 {@link #TEXT_OUTSIDE}
     */
    public int applyTextMove(int id, float u, float v) {
        if (this.level == null || this.level.isClientSide) {
            return TEXT_NOT_FORMED;
        }
        if (this.formState == State.FULL) {
            return TEXT_LOCKED;
        }
        if (this.formState != State.PARTIAL || this.origin == null) {
            return TEXT_NOT_FORMED;
        }
        TextEntry existing = findText(id);
        if (existing == null) {
            return TEXT_OUTSIDE;
        }

        // 锚点夹在表面矩形内，避免拖出结构
        Direction busFacing = getBusFacing();
        double halfW = ConsoleSurface.halfWidthPx(existing.face, busFacing,
                this.sizeX, this.sizeY, this.sizeZ);
        double halfH = ConsoleSurface.halfHeightPx(existing.face, busFacing,
                this.sizeX, this.sizeY, this.sizeZ);
        float cu = (float) Math.max(-halfW, Math.min(halfW, u));
        float cv = (float) Math.max(-halfH, Math.min(halfH, v));

        int index = this.texts.indexOf(existing);
        this.texts.set(index, new TextEntry(existing.id, existing.face, cu, cv,
                existing.text, existing.color, existing.bold, existing.italic,
                existing.underline, existing.strikethrough,
                existing.anchorH, existing.anchorV, existing.scale));
        sync();
        return TEXT_OK;
    }

    /**
     * 结构表面的一条文字。
     *
     * 位置用「相对该表面中心的像素偏移」表示：结构尺寸变化或结构整体搬动时，
     * 文字会自动跟随表面，不需要额外换算。
     */
    public static final class TextEntry {
        /** 缩放倍率的允许区间（默认值来自配置项 console_text.default_scale）。 */
        public static final float MIN_SCALE = 0.05F;
        public static final float MAX_SCALE = 4.0F;

        public final int id;
        public final Direction face;
        /** 锚点像素坐标（相对该表面中心） */
        public final float u;
        public final float v;
        public final String text;
        /** RGB 颜色；-1 表示不设置（默认白色）。 */
        public final int color;
        public final boolean bold;
        public final boolean italic;
        public final boolean underline;
        public final boolean strikethrough;
        /** 水平锚点：0 左 / 1 中 / 2 右（u 对齐文本框的哪一侧）。 */
        public final int anchorH;
        /** 垂直锚点：0 上 / 1 中 / 2 下（v 对齐文本框的哪一侧）。 */
        public final int anchorV;
        /** 缩放倍率：1 字体像素 = scale 结构像素（1 结构像素 = 1/16 格）。 */
        public final float scale;

        public TextEntry(int id, Direction face, float u, float v, String text, int color,
                         boolean bold, boolean italic, boolean underline, boolean strikethrough,
                         int anchorH, int anchorV, float scale) {
            this.id = id;
            this.face = face;
            this.u = u;
            this.v = v;
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.anchorH = anchorH;
            this.anchorV = anchorV;
            this.scale = scale;
        }

        /** 构造带完整样式的文本组件。 */
        public Component toComponent() {
            if (this.text.isEmpty()) {
                return Component.empty();
            }
            Style style = Style.EMPTY.withBold(this.bold).withItalic(this.italic)
                    .withUnderlined(this.underline).withStrikethrough(this.strikethrough);
            if (this.color >= 0) {
                style = style.withColor(TextColor.fromRgb(this.color));
            }
            return Component.literal(this.text).withStyle(style);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", this.id);
            tag.putInt("Face", this.face.get3DDataValue());
            tag.putFloat("U", this.u);
            tag.putFloat("V", this.v);
            tag.putString("Text", this.text);
            tag.putInt("Color", this.color);
            tag.putBoolean("Bold", this.bold);
            tag.putBoolean("Italic", this.italic);
            tag.putBoolean("Underline", this.underline);
            tag.putBoolean("Strike", this.strikethrough);
            tag.putInt("AnchorH", this.anchorH);
            tag.putInt("AnchorV", this.anchorV);
            tag.putFloat("Scale", this.scale);
            return tag;
        }

        private static TextEntry load(CompoundTag tag) {
            Direction[] values = Direction.values();
            int faceOrd = tag.getInt("Face");
            Direction face = values[Math.max(0, Math.min(values.length - 1, faceOrd))];
            // 旧存档没有锚点/缩放字段：锚点居中、缩放取配置的默认值
            int anchorH = tag.contains("AnchorH") ? tag.getInt("AnchorH") : 1;
            int anchorV = tag.contains("AnchorV") ? tag.getInt("AnchorV") : 1;
            float scale = tag.contains("Scale") ? tag.getFloat("Scale")
                    : com.cc_rc.Config.getConsoleTextDefaultScale();
            return new TextEntry(tag.getInt("Id"), face, tag.getFloat("U"), tag.getFloat("V"),
                    tag.getString("Text"), tag.getInt("Color"), tag.getBoolean("Bold"),
                    tag.getBoolean("Italic"), tag.getBoolean("Underline"), tag.getBoolean("Strike"),
                    anchorH, anchorV, scale);
        }
    }

    /**
     * 组件的附带文字。
     *
     * 位置由组件类型固定决定（渲染时按组件矩形计算），大小固定为
     * 配置项 console_text.default_scale，因此不需要坐标 / 锚点 / 缩放字段。
     * 没有附带文字时用 {@link #NONE}（空文本），不写 NBT。
     */
    public static final class CompText {
        /** 无附带文字。 */
        public static final CompText NONE = new CompText("", -1, false, false, false, false);

        /** 文字内容（空 = 无附带文字）。 */
        public final String text;
        /** 文字颜色（0xRRGGBB）；-1 = 默认白。 */
        public final int color;
        public final boolean bold;
        public final boolean italic;
        public final boolean underline;
        public final boolean strikethrough;

        public CompText(String text, int color, boolean bold, boolean italic,
                        boolean underline, boolean strikethrough) {
            this.text = text == null ? "" : text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
        }

        /** 是否没有文字。 */
        public boolean isEmpty() {
            return this.text.isEmpty();
        }

        /** 构造带完整样式的文本组件（渲染 / 界面显示用）。 */
        public Component toComponent() {
            if (this.text.isEmpty()) {
                return Component.empty();
            }
            Style style = Style.EMPTY.withBold(this.bold).withItalic(this.italic)
                    .withUnderlined(this.underline).withStrikethrough(this.strikethrough);
            if (this.color >= 0) {
                style = style.withColor(TextColor.fromRgb(this.color));
            }
            return Component.literal(this.text).withStyle(style);
        }

        private void save(CompoundTag tag) {
            tag.putString("CompText", this.text);
            tag.putInt("CompTextColor", this.color);
            tag.putBoolean("CompTextBold", this.bold);
            tag.putBoolean("CompTextItalic", this.italic);
            tag.putBoolean("CompTextUnderline", this.underline);
            tag.putBoolean("CompTextStrike", this.strikethrough);
        }

        private static CompText load(CompoundTag tag) {
            // 旧存档没有附带文字字段 → 视为无附带文字
            if (!tag.contains("CompText")) {
                return NONE;
            }
            return new CompText(tag.getString("CompText"),
                    tag.contains("CompTextColor") ? tag.getInt("CompTextColor") : -1,
                    tag.getBoolean("CompTextBold"), tag.getBoolean("CompTextItalic"),
                    tag.getBoolean("CompTextUnderline"), tag.getBoolean("CompTextStrike"));
        }
    }

    /**
     * 变量表中的一个变量定义。
     *
     * 地址（slot）在创建后永久固定；绑定组件只是一个「取值来源」，
     * 组件被删除后变量仍然保留，只是不再从组件读取值。
     */
    public static final class VarDef {
        /** 变量地址（等于变量表槽位下标，1 ~ 63）。 */
        public final int slot;
        /** 变量名（不区分大小写唯一）。 */
        public final String name;
        /** 变量类型。 */
        public final VarType type;
        /** 绑定的组件 id；0 表示自定义变量。 */
        public final int compId;
        /**
         * 是否为「推送触发」变量（布尔）。
         * 只有圆盘记录仪的第二名称会生成该角色：tick 开始时清零，
         * 程序把它置 true（或 CC 显式 push）时，本 tick 结算阶段才给记录仪记一笔采样。
         */
        public final boolean trigger;

        public VarDef(int slot, String name, VarType type, int compId) {
            this(slot, name, type, compId, false);
        }

        public VarDef(int slot, String name, VarType type, int compId, boolean trigger) {
            this.slot = slot;
            this.name = name;
            this.type = type;
            this.compId = compId;
            this.trigger = trigger;
        }

        /** 是否绑定了组件。 */
        public boolean isBound() {
            return this.compId != 0;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Slot", this.slot);
            tag.putString("Name", this.name);
            tag.putString("Type", this.type.name());
            if (this.compId != 0) {
                tag.putInt("CompId", this.compId);
            }
            // 触发行角色：旧存档没有该字段，默认普通变量
            if (this.trigger) {
                tag.putBoolean("Trigger", true);
            }
            return tag;
        }

        private static VarDef load(CompoundTag tag) {
            int slot = tag.getInt("Slot");
            if (slot <= VAR_SLOT_PERIOD || slot >= VAR_SLOTS_MAX) {
                return null;
            }
            String name = tag.getString("Name");
            if (!VAR_NAME_PATTERN.matcher(name).matches()) {
                return null;
            }
            VarType type;
            try {
                type = VarType.valueOf(tag.getString("Type"));
            } catch (IllegalArgumentException ex) {
                // 未知类型（损坏数据或版本变化）直接丢弃该定义
                return null;
            }
            int compId = tag.contains("CompId") ? tag.getInt("CompId") : 0;
            // 触发变量必须是布尔；数据损坏时退回普通变量
            boolean trigger = tag.getBoolean("Trigger") && type == VarType.BOOL;
            return new VarDef(slot, name, type, compId, trigger);
        }
    }

    /**
     * 结构表面上的一个组件。
     *
     * 位置 (u, v) 与文字相同：相对该表面中心的像素偏移；
     * 组件模型以该点为中心、贴在结构外表面上渲染。
     * 代号（code）供后续逻辑引用（当前仅作标识）。
     *
     * state 保存组件的运行状态（含义随类型不同）：
     *  - 旋钮 / 拉杆开关：0 = 关，1 = 开；
     *  - 按钮面板：按下后应弹起的游戏时间（tick），0 = 未按下；
     *  - 弧形仪表：当前数值 0~100（默认 50）；
     *  - 其它类型暂未使用（0）。
     */
    public static final class CompEntry {
        /** 未设置颜色：渲染时使用原始贴图（-1 不能与任何合法 RGB 冲突）。 */
        public static final int NO_COLOR = -1;
        /**
         * 圆盘记录仪历史采样点数（索引 0 = 最新）。
         * 与单独实现的数字圆盘记录仪保持一致取 50 点，一点对应屏幕上一列，像素更细。
         */
        public static final int HISTORY_LEN = 50;
        /** 数字显示屏默认保留小数位。 */
        public static final int DEFAULT_DECIMALS = 2;
        /** 数字显示屏小数位允许范围。 */
        public static final int MAX_DECIMALS = 6;
        /** 显示单位最大长度。 */
        public static final int MAX_UNIT_LEN = 8;
        /** 按钮弹起时间的默认值：跟随配置文件 console_button.hold_ticks。 */
        public static final int DEFAULT_HOLD_TICKS = -1;
        /** 按钮弹起时间允许的最大值（tick），与配置文件上限一致。 */
        public static final int MAX_HOLD_TICKS = 200;
        /**
         * 按钮「按住直到松手」状态：弹起时间为 0 时 state 取该值，
         * 玩家按住期间一直按下，客户端松手时再发释放包清零（-1 不会与正常弹起时刻冲突）。
         */
        public static final long HOLD_UNTIL_RELEASE = -1L;
        /**
         * 安全按钮「盖子打开、按钮弹起」状态：state 取该值（-2 不会与弹起时刻、
         * {@link #HOLD_UNTIL_RELEASE} 冲突）。
         */
        public static final long OPEN_COVER = -2L;
        /** 安全按钮盖子自动关闭时间的默认值（tick；0 = 不自动关）。 */
        public static final int DEFAULT_COVER_TICKS = 100;
        /** 安全按钮盖子自动关闭时间允许的最大值（tick，约 1 小时）。 */
        public static final int MAX_COVER_TICKS = 72000;
        /** 玩家可编辑的组件缩放默认值（1.0 = 模型默认大小）。 */
        public static final float DEFAULT_SCALE = 1.0F;
        /** 组件缩放允许范围。 */
        public static final float MIN_SCALE = 0.25F;
        public static final float MAX_SCALE = 4.0F;
        /** 钥匙开关的钥匙模式：开闭（开 / 关两态都能拔钥匙）。 */
        public static final int KEY_MODE_TOGGLE = 0;
        /** 钥匙开关的钥匙模式：常开（只有开状态才允许拔钥匙）。 */
        public static final int KEY_MODE_ALWAYS_OPEN = 1;
        /** 钥匙开关的钥匙模式：常闭（只有关状态能拔钥匙）。 */
        public static final int KEY_MODE_ALWAYS_CLOSED = 2;
        /** 钥匙模式允许的最大值。 */
        public static final int MAX_KEY_MODE = 2;

        public final int id;
        public final ConsoleCompType type;
        public final Direction face;
        public final float u;
        public final float v;
        public final String code;
        /**
         * 第二名称（仅圆盘记录仪使用，其余类型为空字符串）。
         * 玩家用该名称注册布尔变量后，变量的值就作为「本 tick 是否推送一笔采样」的触发信号。
         */
        public final String code2;
        /** 组件颜色（0xRRGGBB）；{@link #NO_COLOR} 表示保持贴图原样。 */
        public final int color;
        /** 附带文字（位置随组件固定，大小固定；{@link CompText#NONE} = 无）。 */
        public final CompText text;
        /** 运行状态（含义随组件类型不同，见类注释）。 */
        public final long state;
        /**
         * 模拟量数值（变量系统写入 / 读取的浮点值）。
         * 开关量与按钮不使用该字段；弧形仪表的指针状态仍由 {@link #state} 驱动。
         */
        public final float value;
        /**
         * 圆盘记录仪的历史采样（索引 0 = 最新，写入新值时整体后移一位）。
         * 其它类型为 null；null 视为全 0，避免旧存档额外写 NBT。
         */
        public final float[] history;
        /** 数字显示屏：保留小数位（0~{@link #MAX_DECIMALS}）。 */
        public final int decimals;
        /** 数字显示屏：显示单位（空字符串 = 不显示）。 */
        public final String unit;
        /**
         * 按钮：按下后保持的 tick 数（0 = 松手立刻弹起）。
         * {@link #DEFAULT_HOLD_TICKS} 表示跟随配置文件，其它类型无意义。
         */
        public final int holdTicks;
        /**
         * 安全按钮：盖子打开后自动关闭的 tick 数（0 = 不自动关闭，默认
         * {@link #DEFAULT_COVER_TICKS}）；其它类型无意义。
         */
        public final int coverTicks;
        /**
         * 玩家可编辑的组件缩放（{@link #DEFAULT_SCALE} = 模型默认大小）。
         * 组件实际显示倍率 = 该值 × {@link ConsoleCompType#renderScale}（模型默认缩放）。
         */
        public final float scale;
        /**
         * RGB 颜色控制器的「连接的组件」：组件代号列表，用「;」分隔（其余类型为空字符串）。
         * 控制器启用时把自己的颜色同步给这些组件；停用后它们保持最后的颜色。
         */
        public final String links;
        /** RGB 颜色控制器的 R / G / B 三个浮点通道变量名（空 = 该通道不启用）。 */
        public final String rgbR;
        public final String rgbG;
        public final String rgbB;
        /**
         * RGB 颜色控制器「显示原色」（= 停用）：
         * 没有注册主名称布尔变量时，由该开关决定是否启用；
         * 有布尔变量时以变量值为准，该开关仅作界面上的手动控制。
         */
        public final boolean showOriginal;
        /**
         * 同步指示器的「转速变量名」（空 = 自动使用 代号 + "_s"）。
         * 仅 SYNCHROSCOPE 使用，其它类型为空字符串；玩家可在组件设置界面修改。
         */
        public final String speedVar;
        /**
         * 同步指示器的「灯亮状态变量名」（空 = 自动使用 代号 + "_b"）。
         * 该变量是只读输出（每 tick 由实际判断值覆盖），玩家可在组件设置界面改名。
         */
        public final String lampVar;
        /**
         * 警报器的「静音状态变量名」（空 = 自动使用 代号 + "_Q"）。
         * 该变量是只读输出（每 tick 由 {@link #muted} 覆盖），玩家可在组件设置界面改名。
         */
        public final String muteVar;
        /**
         * 钥匙开关：当前是否插着钥匙。
         * 成型后默认无钥匙（不能开关）；用代号匹配的「控制柜钥匙」右键插入后为 true。
         * 仅 SWITCH_K 使用，其它类型恒为 false。
         */
        public final boolean hasKey;
        /**
         * 钥匙开关的钥匙模式（{@link #KEY_MODE_TOGGLE} / {@link #KEY_MODE_ALWAYS_OPEN} /
         * {@link #KEY_MODE_ALWAYS_CLOSED}），决定拔钥匙需要开关处于哪个状态。
         * 仅 SWITCH_K 使用，其它类型恒为 {@link #KEY_MODE_TOGGLE}。
         */
        public final int keyMode;
        /**
         * 警报器：是否已静音（按下静音按钮后为 true，报警由 false 变 true 时自动清回 false）。
         * 仅 ALARM 使用，其它类型恒为 false。
         */
        public final boolean muted;
        /**
         * 警报器的「按下反馈」按钮：0 = 无，1 = 静音（中），2 = 重置（右）。
         * 只用于短暂显示按下状态的模型，配合 {@link #pressUntil} 使用；其它类型恒为 0。
         */
        public final int pressButton;
        /**
         * 警报器「按下反馈」的结束游戏时间（0 = 无反馈）。
         * 与按钮面板一致按绝对游戏时间判断，过期后自动回到未按下状态。
         */
        public final long pressUntil;

        /** 使用类型默认状态的构造（新增组件 / 旧调用点）。 */
        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text) {
            this(id, type, face, u, v, code, color, text, defaultState(type), defaultValue(type));
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state) {
            this(id, type, face, u, v, code, color, text, state, defaultValue(type));
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value) {
            this(id, type, face, u, v, code, color, text, state, value,
                    null, DEFAULT_DECIMALS, "");
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, DEFAULT_HOLD_TICKS);
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, holdTicks, DEFAULT_SCALE);
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, holdTicks, scale, "");
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, holdTicks, scale, code2, "", "", "", "", false);
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, holdTicks, scale, code2, links,
                    rgbR, rgbG, rgbB, showOriginal, "");
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal,
                         String speedVar) {
            this(id, type, face, u, v, code, color, text, state, value,
                    history, decimals, unit, holdTicks, scale, code2, links,
                    rgbR, rgbG, rgbB, showOriginal, speedVar, "");
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal,
                         String speedVar, String lampVar) {
            this(id, type, face, u, v, code, color, text, state, value, history, decimals,
                    unit, holdTicks, scale, code2, links, rgbR, rgbG, rgbB, showOriginal,
                    speedVar, lampVar, DEFAULT_COVER_TICKS);
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal,
                         String speedVar, String lampVar, int coverTicks) {
            // 旧调用点没有钥匙字段：默认「无钥匙 + 开闭模式」
            this(id, type, face, u, v, code, color, text, state, value, history, decimals,
                    unit, holdTicks, scale, code2, links, rgbR, rgbG, rgbB, showOriginal,
                    speedVar, lampVar, coverTicks, false, KEY_MODE_TOGGLE);
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal,
                         String speedVar, String lampVar, int coverTicks,
                         boolean hasKey, int keyMode) {
            // 旧调用点没有警报器字段：默认「未静音 + 无按下反馈 + 自动静音变量名」
            this(id, type, face, u, v, code, color, text, state, value, history, decimals,
                    unit, holdTicks, scale, code2, links, rgbR, rgbG, rgbB, showOriginal,
                    speedVar, lampVar, coverTicks, hasKey, keyMode, false, 0, 0L, "");
        }

        public CompEntry(int id, ConsoleCompType type, Direction face, float u, float v,
                         String code, int color, CompText text, long state, float value,
                         float[] history, int decimals, String unit, int holdTicks,
                         float scale, String code2, String links,
                         String rgbR, String rgbG, String rgbB, boolean showOriginal,
                         String speedVar, String lampVar, int coverTicks,
                         boolean hasKey, int keyMode, boolean muted,
                         int pressButton, long pressUntil, String muteVar) {
            this.id = id;
            this.type = type;
            this.face = face;
            this.u = u;
            this.v = v;
            this.code = code;
            this.code2 = code2 == null ? "" : code2;
            this.color = color;
            this.text = text == null ? CompText.NONE : text;
            this.state = state;
            this.value = value;
            this.history = history;
            this.decimals = decimals;
            this.unit = unit == null ? "" : unit;
            this.holdTicks = holdTicks;
            // 安全按钮盖子自动关闭时间：裁剪到合法范围（0 = 不自动关闭）
            this.coverTicks = Math.max(0, Math.min(MAX_COVER_TICKS, coverTicks));
            this.scale = scale;
            this.links = links == null ? "" : links;
            this.rgbR = rgbR == null ? "" : rgbR;
            this.rgbG = rgbG == null ? "" : rgbG;
            this.rgbB = rgbB == null ? "" : rgbB;
            this.showOriginal = showOriginal;
            this.speedVar = speedVar == null ? "" : speedVar;
            this.lampVar = lampVar == null ? "" : lampVar;
            // 警报器静音变量名：仅警报器保留，其它类型一律为空（自动名称无效）
            this.muteVar = type == ConsoleCompType.ALARM && muteVar != null ? muteVar : "";
            // 钥匙开关字段：非钥匙开关组件一律归零，避免旧数据带来无意义状态
            this.hasKey = type == ConsoleCompType.SWITCH_K && hasKey;
            this.keyMode = type == ConsoleCompType.SWITCH_K
                    ? Math.max(KEY_MODE_TOGGLE, Math.min(MAX_KEY_MODE, keyMode))
                    : KEY_MODE_TOGGLE;
            // 警报器字段：其它类型一律归零，避免旧数据带来无意义状态
            this.muted = type == ConsoleCompType.ALARM && muted;
            this.pressButton = type == ConsoleCompType.ALARM
                    ? Math.max(0, Math.min(2, pressButton)) : 0;
            this.pressUntil = type == ConsoleCompType.ALARM ? pressUntil : 0L;
        }

        /** 同步指示器转速变量的实际名称：玩家未自定义时自动取「代号 + _s」。 */
        public String speedVarName() {
            if (this.speedVar != null && !this.speedVar.isEmpty()) {
                return this.speedVar;
            }
            return this.code + "_s";
        }

        /**
         * 同步指示器灯亮状态附加布尔变量的实际名称：玩家未自定义时自动取「代号 + _b」。
         * 该变量是只读输出（每 tick 由实际判断值覆盖）。
         */
        public String lampVarName() {
            if (this.lampVar != null && !this.lampVar.isEmpty()) {
                return this.lampVar;
            }
            return this.code + "_b";
        }

        /**
         * 警报器静音状态附加布尔变量的实际名称：玩家未自定义时自动取「代号 + _Q」。
         * 该变量是只读输出（每 tick 由 {@link #muted} 覆盖）。
         */
        public String muteVarName() {
            if (this.muteVar != null && !this.muteVar.isEmpty()) {
                return this.muteVar;
            }
            return this.code + "_Q";
        }

        /** 复制组件并替换状态 / 数值，其余字段（含历史、显示设置）原样保留。 */
        private static CompEntry copyWith(CompEntry base, long state, float value) {
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, base.color, base.text, state, value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    base.muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 复制组件并替换位置，其余字段原样保留。 */
        private static CompEntry copyWithPos(CompEntry base, float u, float v) {
            return new CompEntry(base.id, base.type, base.face, u, v,
                    base.code, base.color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    base.muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 复制组件并替换颜色（RGB 控制器结算 / 联动时使用），其余字段原样保留。 */
        private static CompEntry copyWithColor(CompEntry base, int color) {
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    base.muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 生成只修改「显示原色」开关的组件副本（RGB 颜色控制器用）。 */
        private static CompEntry copyWithShowOriginal(CompEntry base, boolean showOriginal) {
            if (base.showOriginal == showOriginal) {
                return base;
            }
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, base.color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    base.muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 生成只修改「是否有钥匙」的组件副本（钥匙开关插入 / 拔出钥匙用）。 */
        private static CompEntry copyWithKey(CompEntry base, boolean hasKey) {
            if (base.hasKey == hasKey) {
                return base;
            }
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, base.color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, hasKey, base.keyMode,
                    base.muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 生成只修改「是否静音」的组件副本（按下静音 / 报警解除时使用）。 */
        private static CompEntry copyWithMuted(CompEntry base, boolean muted) {
            if (base.muted == muted) {
                return base;
            }
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, base.color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    muted, base.pressButton, base.pressUntil, base.muteVar);
        }

        /** 生成只修改「按钮按下反馈」的组件副本（pressButton = 0 表示清除反馈）。 */
        private static CompEntry copyWithPress(CompEntry base, int pressButton, long pressUntil) {
            return new CompEntry(base.id, base.type, base.face, base.u, base.v,
                    base.code, base.color, base.text, base.state, base.value,
                    base.history, base.decimals, base.unit, base.holdTicks, base.scale,
                    base.code2, base.links, base.rgbR, base.rgbG, base.rgbB, base.showOriginal,
                    base.speedVar, base.lampVar, base.coverTicks, base.hasKey, base.keyMode,
                    base.muted, pressButton, pressUntil, base.muteVar);
        }

        /** 组件实际显示倍率 = 模型默认缩放 × 玩家设置的组件缩放。 */
        public float effectiveScale() {
            return this.type.renderScale * this.scale;
        }

        /** 组件矩形半宽 / 半高（像素，含模型默认缩放与玩家缩放），用于命中检测与重叠判定。 */
        public float halfWidthPx() {
            return this.type.widthPx * effectiveScale() / 2.0F;
        }

        public float halfHeightPx() {
            return this.type.heightPx * effectiveScale() / 2.0F;
        }

        /** 各类型的初始状态：弧形仪表默认 50%，其余默认 0（关 / 未按下）。 */
        public static long defaultState(ConsoleCompType type) {
            return type == ConsoleCompType.GAUGE_ARC ? 50L : 0L;
        }

        /** 按钮的实际弹起时间：未单独设置时跟随配置文件；0 表示按住直到松手。 */
        public int effectiveHoldTicks() {
            return this.holdTicks == DEFAULT_HOLD_TICKS
                    ? com.cc_rc.Config.getConsoleButtonHoldTicks() : this.holdTicks;
        }

        /** 安全按钮盖子的实际自动关闭时间（tick）；0 表示不自动关闭。 */
        public int effectiveCoverTicks() {
            return this.coverTicks;
        }

        /**
         * 按钮当前是否处于按下状态。
         * state = 0 未按下；{@link #HOLD_UNTIL_RELEASE} 表示按住直到松手；其它为弹起时刻。
         */
        public boolean isKeypadPressed(long gameTime) {
            if ((this.type != ConsoleCompType.KEYPAD && this.type != ConsoleCompType.BUTTON_X
                    && this.type != ConsoleCompType.BUTTON_S)
                    || this.state == 0L) {
                return false;
            }
            // OPEN_COVER(-2) / 正常合盖时刻均为负值，会落到最后的比较中返回 false
            return this.state == HOLD_UNTIL_RELEASE || gameTime < this.state;
        }

        /** 各类型的初始模拟量：弧形仪表默认 50，其余默认 0。 */
        public static float defaultValue(ConsoleCompType type) {
            return type == ConsoleCompType.GAUGE_ARC ? 50.0F : 0.0F;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", this.id);
            tag.putString("Type", this.type.id);
            tag.putInt("Face", this.face.get3DDataValue());
            tag.putFloat("U", this.u);
            tag.putFloat("V", this.v);
            tag.putString("Code", this.code);
            // 第二名称只有圆盘记录仪且非空时才写，旧存档 / 其它类型不产生额外字段
            if (!this.code2.isEmpty()) {
                tag.putString("Code2", this.code2);
            }
            // 只有真正设置过颜色才写 NBT，旧存档加载后即为「原样」
            if (this.color != NO_COLOR) {
                tag.putInt("Color", this.color);
            }
            // 没有附带文字时不写字段，保持旧存档格式
            if (!this.text.isEmpty()) {
                this.text.save(tag);
            }
            // 状态与类型默认值不同时才写，兼容旧存档
            if (this.state != defaultState(this.type)) {
                tag.putLong("State", this.state);
            }
            // 模拟量与默认值不同时才写；旧存档没有该字段时按默认值处理
            if (this.value != defaultValue(this.type)) {
                tag.putFloat("Value", this.value);
            }
            // 圆盘记录仪历史采样：只在存在采样时写，旧存档 / 其它类型不产生额外字段
            // （1.20.1 的 CompoundTag 没有 FloatArrayTag 辅助方法，用 Float 列表保存）
            if (this.history != null) {
                ListTag hist = new ListTag();
                for (float sample : this.history) {
                    hist.add(FloatTag.valueOf(sample));
                }
                tag.put("History", hist);
            }
            // 数字显示屏显示设置：默认值不写，兼容旧存档
            if (this.decimals != DEFAULT_DECIMALS) {
                tag.putInt("Decimals", this.decimals);
            }
            if (!this.unit.isEmpty()) {
                tag.putString("Unit", this.unit);
            }
            // 按钮弹起时间：默认值（跟随配置文件）不写，兼容旧存档
            if (this.holdTicks != DEFAULT_HOLD_TICKS) {
                tag.putInt("HoldTicks", this.holdTicks);
            }
            // 安全按钮盖子自动关闭时间：默认值不写，兼容旧存档
            if (this.coverTicks != DEFAULT_COVER_TICKS) {
                tag.putInt("CoverTicks", this.coverTicks);
            }
            // 组件缩放：默认 1.0 不写，兼容旧存档
            if (this.scale != DEFAULT_SCALE) {
                tag.putFloat("Scale", this.scale);
            }
            // RGB 颜色控制器：连接组件与三个通道变量名，空值不写
            if (!this.links.isEmpty()) {
                tag.putString("Links", this.links);
            }
            if (!this.rgbR.isEmpty()) {
                tag.putString("RgbR", this.rgbR);
            }
            if (!this.rgbG.isEmpty()) {
                tag.putString("RgbG", this.rgbG);
            }
            if (!this.rgbB.isEmpty()) {
                tag.putString("RgbB", this.rgbB);
            }
            // 「显示原色」默认 false（启用），只在开启时写
            if (this.showOriginal) {
                tag.putBoolean("ShowOriginal", true);
            }
            // 同步指示器：自定义转速变量名，自动命名（空）时不写
            if (!this.speedVar.isEmpty()) {
                tag.putString("SpeedVar", this.speedVar);
            }
            // 同步指示器：自定义灯亮状态变量名，自动命名（空）时不写
            if (!this.lampVar.isEmpty()) {
                tag.putString("LampVar", this.lampVar);
            }
            // 警报器：自定义静音状态变量名，自动命名（空）时不写
            if (!this.muteVar.isEmpty()) {
                tag.putString("MuteVar", this.muteVar);
            }
            // 钥匙开关：插着钥匙时写标记，拔下后不写（保持旧存档格式）
            if (this.hasKey) {
                tag.putBoolean("HasKey", true);
            }
            // 钥匙模式：默认「开闭」不写，兼容旧存档
            if (this.keyMode != KEY_MODE_TOGGLE) {
                tag.putInt("KeyMode", this.keyMode);
            }
            // 警报器：静音状态 / 按钮按下反馈（默认值不写，兼容旧存档）
            if (this.muted) {
                tag.putBoolean("Muted", true);
            }
            if (this.pressButton != 0 || this.pressUntil != 0L) {
                tag.putInt("PressButton", this.pressButton);
                tag.putLong("PressUntil", this.pressUntil);
            }
            return tag;
        }

        private static CompEntry load(CompoundTag tag) {
            ConsoleCompType type = ConsoleCompType.byId(tag.getString("Type"));
            if (type == null) {
                return null;
            }
            Direction[] values = Direction.values();
            int faceOrd = tag.getInt("Face");
            Direction face = values[Math.max(0, Math.min(values.length - 1, faceOrd))];
            String code = tag.contains("Code") ? tag.getString("Code") : type.codePrefix();
            // 第二名称只对圆盘记录仪有意义；旧存档缺失时为空
            String code2 = type.supportsHistory() && tag.contains("Code2")
                    ? tag.getString("Code2") : "";
            int color = tag.contains("Color") ? tag.getInt("Color") : NO_COLOR;
            long state = tag.contains("State") ? tag.getLong("State") : defaultState(type);
            // 旧存档没有 Value 字段：弧形仪表沿用 state（默认 50），其余用类型默认值
            float value = tag.contains("Value")
                    ? tag.getFloat("Value")
                    : (type == ConsoleCompType.GAUGE_ARC ? state : defaultValue(type));
            // 历史采样：只有圆盘记录仪需要；长度不符时复制到固定长度，越界部分按 0 处理
            float[] history = null;
            if (type.supportsHistory()) {
                history = new float[HISTORY_LEN];
                ListTag hist = tag.getList("History", Tag.TAG_FLOAT);
                for (int i = 0; i < Math.min(hist.size(), HISTORY_LEN); i++) {
                    history[i] = hist.getFloat(i);
                }
            }
            int decimals = tag.contains("Decimals") ? tag.getInt("Decimals") : DEFAULT_DECIMALS;
            decimals = Math.max(0, Math.min(MAX_DECIMALS, decimals));
            String unit = tag.contains("Unit") ? tag.getString("Unit") : "";
            if (unit.length() > MAX_UNIT_LEN) {
                unit = unit.substring(0, MAX_UNIT_LEN);
            }
            // 按钮弹起时间：旧存档没有该字段时跟随配置文件（DEFAULT_HOLD_TICKS）
            int holdTicks = tag.contains("HoldTicks") ? tag.getInt("HoldTicks") : DEFAULT_HOLD_TICKS;
            if (holdTicks != DEFAULT_HOLD_TICKS) {
                holdTicks = Math.max(0, Math.min(MAX_HOLD_TICKS, holdTicks));
            }
            // 安全按钮盖子自动关闭时间：旧存档没有该字段时用默认值
            int coverTicks = tag.contains("CoverTicks") ? tag.getInt("CoverTicks") : DEFAULT_COVER_TICKS;
            coverTicks = Math.max(0, Math.min(MAX_COVER_TICKS, coverTicks));
            // 组件缩放：旧存档没有该字段时按 1.0（模型默认大小）
            float scale = tag.contains("Scale") ? tag.getFloat("Scale") : DEFAULT_SCALE;
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
            // RGB 颜色控制器字段：旧存档 / 其它类型缺失时为空
            String links = tag.contains("Links") ? tag.getString("Links") : "";
            String rgbR = tag.contains("RgbR") ? tag.getString("RgbR") : "";
            String rgbG = tag.contains("RgbG") ? tag.getString("RgbG") : "";
            String rgbB = tag.contains("RgbB") ? tag.getString("RgbB") : "";
            boolean showOriginal = tag.getBoolean("ShowOriginal");
            // 同步指示器的自定义转速变量名；旧存档 / 其它类型缺失时为空（自动命名）
            String speedVar = type.hasSpeedVariable() && tag.contains("SpeedVar")
                    ? tag.getString("SpeedVar") : "";
            // 同步指示器的自定义灯亮状态变量名；旧存档 / 其它类型缺失时为空（自动命名）
            String lampVar = type.hasLampVariable() && tag.contains("LampVar")
                    ? tag.getString("LampVar") : "";
            // 钥匙开关：是否有钥匙 + 钥匙模式；旧存档 / 其它类型按「无钥匙 + 开闭」处理
            boolean hasKey = type == ConsoleCompType.SWITCH_K && tag.getBoolean("HasKey");
            int keyMode = type == ConsoleCompType.SWITCH_K && tag.contains("KeyMode")
                    ? tag.getInt("KeyMode") : KEY_MODE_TOGGLE;
            keyMode = Math.max(KEY_MODE_TOGGLE, Math.min(MAX_KEY_MODE, keyMode));
            // 警报器：静音状态 + 按钮按下反馈；旧存档 / 其它类型按「未静音 + 无反馈」处理
            boolean muted = type.hasMuteVariable() && tag.getBoolean("Muted");
            int pressButton = type.hasMuteVariable() ? tag.getInt("PressButton") : 0;
            long pressUntil = type.hasMuteVariable() ? tag.getLong("PressUntil") : 0L;
            // 警报器的自定义静音变量名；旧存档 / 其它类型缺失时为空（自动命名）
            String muteVar = type.hasMuteVariable() && tag.contains("MuteVar")
                    ? tag.getString("MuteVar") : "";
            return new CompEntry(tag.getInt("Id"), type, face,
                    tag.getFloat("U"), tag.getFloat("V"), code, color, CompText.load(tag),
                    state, value, history, decimals, unit, holdTicks, scale, code2,
                    links, rgbR, rgbG, rgbB, showOriginal, speedVar, lampVar, coverTicks,
                    hasKey, keyMode, muted, pressButton, pressUntil, muteVar);
        }
    }

    /** 渲染包围盒：覆盖整个结构长方体，避免表面文字被视锥剔除。 */
    @Override
    public AABB getRenderBoundingBox() {
        if (this.origin != null && this.sizeX > 0 && this.sizeY > 0 && this.sizeZ > 0) {
            return new AABB(this.origin,
                    this.origin.offset(this.sizeX, this.sizeY, this.sizeZ)).inflate(1.0D);
        }
        return super.getRenderBoundingBox();
    }

    /** 校验并进入「部分成型」；失败时回到未成型并记录原因。 */
    public void formStructure() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        // 先清掉旧结构在成员上的标记（外接范围可能和新的不同）
        applyMembers(null, 0);

        ConsoleStructureHelper.Result result =
                ConsoleStructureHelper.validate(this.level, this.worldPosition);
        this.error = result.error;
        if (result.ok()) {
            this.formState = State.PARTIAL;
            // 结构范围变化时，先把已有组件/文字换算到新参考系，
            // 保证它们在世界中的位置不变（扩展/缩减结构后不会整体平移）
            if (this.lastOrigin != null) {
                rebaseSurfaceEntries(this.lastOrigin, this.lastSizeX, this.lastSizeY, this.lastSizeZ,
                        result.origin, result.sizeX, result.sizeY, result.sizeZ);
            }
            this.lastOrigin = result.origin;
            this.lastSizeX = result.sizeX;
            this.lastSizeY = result.sizeY;
            this.lastSizeZ = result.sizeZ;
            this.origin = result.origin;
            this.sizeX = result.sizeX;
            this.sizeY = result.sizeY;
            this.sizeZ = result.sizeZ;
            this.memberCount = result.count;
            this.structureColor = result.color;
            applyMembers(this.worldPosition, State.PARTIAL.ordinal());
        } else {
            this.formState = State.UNFORMED;
            this.origin = null;
            this.sizeX = 0;
            this.sizeY = 0;
            this.sizeZ = 0;
            this.memberCount = 0;
            this.structureColor = ConsoleColor.DEFAULT;
        }
        // 识别到的颜色写入方块状态：总线与紧邻的辅助计算机切换到对应贴图
        applyBusColor();
        sync();
    }

    /**
     * 把识别到的结构颜色写入总线方块状态（只影响贴图，不影响功能）。
     *
     * 使用 flag 3：既同步客户端外观，也触发邻居更新，让紧邻总线的
     * 辅助计算机在 neighborChanged 里跟随换色。
     */
    private void applyBusColor() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof ConsoleBusBlock)) {
            return;
        }
        if (state.getValue(ConsoleBusBlock.COLOR) == this.structureColor) {
            return;
        }
        this.level.setBlock(this.worldPosition,
                state.setValue(ConsoleBusBlock.COLOR, this.structureColor), 3);
    }

    /**
     * 结构外接长方体变化后，把表面坐标换算到新参考系。
     *
     * 表面坐标 (u, v) 以「该表面矩形中心」为原点；结构扩大/移动后，表面中心的
     * 世界位置会沿 U / V 方向平移。这里把新旧中心的偏移投影到该表面的 U、V 轴，
     * 从坐标里扣除，使组件与文字在表面上的世界位置保持不变。
     *
     * 法线方向（N）不补偿：组件/文字始终贴在新表面上，避免结构向外扩展后
     * 被留在旧表面位置、嵌进方块内部而看不见。
     */
    private void rebaseSurfaceEntries(BlockPos oldOrigin, int oldSizeX, int oldSizeY, int oldSizeZ,
                                      BlockPos newOrigin, int newSizeX, int newSizeY, int newSizeZ) {
        Direction busFacing = getBusFacing();
        // 每个面只计算一次中心偏移（单位：结构像素）
        Map<Direction, float[]> shifts = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            Vec3 oldCenter = ConsoleSurface.faceCenter(oldOrigin, oldSizeX, oldSizeY, oldSizeZ, face);
            Vec3 newCenter = ConsoleSurface.faceCenter(newOrigin, newSizeX, newSizeY, newSizeZ, face);
            Vec3 delta = newCenter.subtract(oldCenter);
            float du = (float) (delta.dot(ConsoleSurface.u(face, busFacing))
                    * ConsoleSurface.PIXELS_PER_BLOCK);
            float dv = (float) (delta.dot(ConsoleSurface.v(face, busFacing))
                    * ConsoleSurface.PIXELS_PER_BLOCK);
            shifts.put(face, new float[]{du, dv});
        }

        // 组件：替换为换算后的新坐标（组件条目不可变）
        for (int i = 0; i < this.comps.size(); i++) {
            CompEntry entry = this.comps.get(i);
            float[] shift = shifts.get(entry.face);
            if (shift == null) {
                continue;
            }
            float u = clampU(entry.face, entry.u - shift[0], busFacing, newSizeX, newSizeY, newSizeZ);
            float v = clampV(entry.face, entry.v - shift[1], busFacing, newSizeX, newSizeY, newSizeZ);
            this.comps.set(i, CompEntry.copyWithPos(entry, u, v));
        }

        // 表面文字：同样只改锚点坐标，内容 / 样式 / 锚点方向 / 缩放保持不变
        for (int i = 0; i < this.texts.size(); i++) {
            TextEntry entry = this.texts.get(i);
            float[] shift = shifts.get(entry.face);
            if (shift == null) {
                continue;
            }
            float u = clampU(entry.face, entry.u - shift[0], busFacing, newSizeX, newSizeY, newSizeZ);
            float v = clampV(entry.face, entry.v - shift[1], busFacing, newSizeX, newSizeY, newSizeZ);
            this.texts.set(i, new TextEntry(entry.id, entry.face, u, v, entry.text, entry.color,
                    entry.bold, entry.italic, entry.underline, entry.strikethrough,
                    entry.anchorH, entry.anchorV, entry.scale));
        }

        // 绘图图形：整体平移，保证图形在表面上的世界位置不变
        for (Map.Entry<Direction, List<ConsoleShape>> entry : this.drawShapes.entrySet()) {
            float[] shift = shifts.get(entry.getKey());
            if (shift == null) {
                continue;
            }
            List<ConsoleShape> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                list.set(i, shiftDrawShape(list.get(i), -shift[0], -shift[1], busFacing,
                        newSizeX, newSizeY, newSizeZ));
            }
        }
    }

    /** 表面坐标 U 夹紧到新范围内。 */
    private static float clampU(Direction face, float u, Direction busFacing,
                                int sizeX, int sizeY, int sizeZ) {
        double half = ConsoleSurface.halfWidthPx(face, busFacing, sizeX, sizeY, sizeZ);
        return (float) Math.max(-half, Math.min(half, u));
    }

    /** 表面坐标 V 夹紧到新范围内。 */
    private static float clampV(Direction face, float v, Direction busFacing,
                                int sizeX, int sizeY, int sizeZ) {
        double half = ConsoleSurface.halfHeightPx(face, busFacing, sizeX, sizeY, sizeZ);
        return (float) Math.max(-half, Math.min(half, v));
    }

    /** 部分成型 → 完全成型（锁定）。 */
    public void lock() {
        if (this.formState != State.PARTIAL) {
            return;
        }
        this.formState = State.FULL;
        applyMembers(this.worldPosition, State.FULL.ordinal());
        sync();
    }

    /** 完全成型 → 部分成型（解锁，可继续编辑）。 */
    public void unlock() {
        if (this.formState != State.FULL) {
            return;
        }
        this.formState = State.PARTIAL;
        applyMembers(this.worldPosition, State.PARTIAL.ordinal());
        sync();
    }

    /** 结构变动导致失效：回到未成型并清空成员标记（保留失败原因为空）。 */
    public void invalidateStructure() {
        if (this.formState == State.UNFORMED) {
            return;
        }
        // 失效前保存结构范围：重新成型时用它换算组件/文字坐标，
        // 保证「拆掉方块再补回来」后它们的位置不变
        this.lastOrigin = this.origin;
        this.lastSizeX = this.sizeX;
        this.lastSizeY = this.sizeY;
        this.lastSizeZ = this.sizeZ;
        applyMembers(null, 0);
        this.formState = State.UNFORMED;
        this.origin = null;
        this.sizeX = 0;
        this.sizeY = 0;
        this.sizeZ = 0;
        this.memberCount = 0;
        // 结构失效后总线恢复默认外观（相邻辅助计算机随之跟随）
        this.structureColor = ConsoleColor.DEFAULT;
        applyBusColor();
        sync();
    }

    /** 主线程低频巡检：结构被外部改动（爆炸、指令等）时自动失效；同时驱动程序执行与 CC 结算。 */
    public static void tick(Level level, BlockPos pos, BlockState state, ConsoleBusBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        if (be.formState == State.UNFORMED) {
            // 结构未成型时停止计时，重新成型后从 0 开始累计；
            // CC 接口仍可读写已有变量 / 组件（程序不运行）
            be.programTimer = 0;
            // 触发变量每 tick 从 false 起算，避免上一 tick 的值残留导致误推送
            be.clearPlotTriggers();
            be.applyCcOps();
            be.flushPlotters();
            be.publishCcSnapshot();
            return;
        }
        // 每 5 秒检查一次，足够及时且开销可忽略
        if (level.getGameTime() % 100L == 0L
                && !ConsoleStructureHelper.stillValid(level, be.origin,
                        be.sizeX, be.sizeY, be.sizeZ, be.memberCount, be.structureColor)) {
            be.invalidateStructure();
            be.clearPlotTriggers();
            be.applyCcOps();
            be.flushPlotters();
            be.publishCcSnapshot();
            return;
        }
        // 按钮面板到点弹起：先恢复状态，再跑程序，保证本轮读到的就是「已弹起」
        be.releaseExpiredKeypads();
        // 安全按钮玻璃罩到点自动关闭（按钮按下期间不关）
        be.releaseExpiredCovers();
        // 推送触发变量在本 tick 开始时清零：之后只有程序赋值 true 或 CC 显式 push 才会记录
        be.clearPlotTriggers();
        // 只有完全成型才执行程序（部分成型仍允许编辑）
        be.tickProgram();
        // 顺序：辅助计算机 → CC 变量 / 组件出入 → 组件状态统一上传（最多各同步一次）
        be.applyCcOps();
        // 记录仪补采样：触发变量为 true 的圆盘记录仪按其数值变量记一笔
        be.flushPlotters();
        // RGB 颜色控制器结算：仅完全成型时由变量驱动灯色与连接组件；
        // 部分成型（可编辑状态）保持手动设置的颜色 / 显示原色开关，不在这里覆盖
        if (be.formState == State.FULL) {
            be.applyRgbControllers();
            // 同步指示器：按转速变量推进指针角度，并按条件切换指示灯模型
            be.applySynchroscopes();
            // 警报器：报警解除时清静音、报警中未静音时周期性发声，并镜像 <代号>_Q
            be.applyAlarms();
        }
        // 结算完成后发布新快照给 CC 的 Lua 线程
        be.publishCcSnapshot();
    }

    /**
     * 把全部「推送触发变量」清零（服务端 tick，按钮弹起之后、程序执行之前）。
     *
     * 触发变量表示「本 tick 是否给圆盘记录仪记一笔」，生命周期只有一个 tick：
     * 每 tick 开始时清零，因此程序本 tick 赋 true 才有效，不会跨 tick 残留。
     */
    private void clearPlotTriggers() {
        int slots = varSlots();
        boolean changed = false;
        for (int slot = 1; slot < slots; slot++) {
            VarDef def = this.varDefs[slot];
            if (def == null || !def.trigger || !this.varBools[slot]) {
                continue;
            }
            this.varBools[slot] = false;
            changed = true;
        }
        if (changed) {
            sync();
        }
    }

    /**
     * 组件状态更新阶段：为「触发变量为 true」的圆盘记录仪补记一笔采样。
     *
     * 采样值取自同一组件绑定的数值变量（没有数值变量时用组件当前值）。
     * 触发变量为 false 的记录仪完全不动，实现「程序赋值 true 或 CC 调用 push 才动」。
     */
    private void flushPlotters() {
        int slots = varSlots();
        boolean changed = false;
        for (int slot = 1; slot < slots; slot++) {
            VarDef triggerDef = this.varDefs[slot];
            if (triggerDef == null || !triggerDef.trigger || triggerDef.compId == 0
                    || !this.varBools[slot]) {
                continue;
            }
            CompEntry comp = findComp(triggerDef.compId);
            if (comp == null || !comp.type.supportsHistory()) {
                continue;
            }
            int valueSlot = findVarSlotByComp(triggerDef.compId, false);
            float value = valueSlot > 0 ? this.varFloats[valueSlot] : comp.value;
            changed |= replaceComp(comp, withHistorySample(comp, value));
        }
        if (changed) {
            sync();
        }
    }

    /**
     * 把已到弹起时刻的按钮面板恢复为弹起状态（服务端 tick）。
     *
     * 按钮的 state 存的是「弹起时刻」（按下时的 gameTime + 配置时长）或
     * {@link CompEntry#HOLD_UNTIL_RELEASE}（弹起时间为 0，按住直到松手，不计时）；
     * 定时到点后必须把 state 清回 0：程序与变量读取都按按下状态判定，
     * 不清零会表现为「按一次之后该变量永远是 true」。
     */
    private void releaseExpiredKeypads() {
        if (this.level == null || this.comps.isEmpty()) {
            return;
        }
        long now = this.level.getGameTime();
        boolean changed = false;
        for (int i = 0; i < this.comps.size(); i++) {
            CompEntry comp = this.comps.get(i);
            // 「按住直到松手」模式不按时间弹起，等待客户端释放包；
            // state 只有「正数弹起时刻」才是计时按下状态：0 = 未按下，
            // 负数（HOLD_UNTIL_RELEASE / OPEN_COVER）都不是计时状态。
            // 这里必须排除 OPEN_COVER(-2)：否则安全按钮的「开盖」状态每 tick 都会被
            // 当成「已到点」处理一次，反复重置自动关盖倒计时，盖子就永远不关。
            if ((comp.type != ConsoleCompType.KEYPAD && comp.type != ConsoleCompType.BUTTON_X
                    && comp.type != ConsoleCompType.BUTTON_S)
                    || comp.state <= 0L || now < comp.state) {
                continue;
            }
            // 安全按钮弹起后回到「开盖」状态，其它按钮直接回到未按下
            long target = comp.type == ConsoleCompType.BUTTON_S
                    ? CompEntry.OPEN_COVER : 0L;
            CompEntry released = CompEntry.copyWith(comp, target, comp.value);
            this.comps.set(i, released);
            changed = true;
            // 安全按钮弹起后重新开始自动关盖倒计时
            if (comp.type == ConsoleCompType.BUTTON_S) {
                refreshCoverDeadline(released);
            }
            // 同步把绑定变量的布尔值改回 false，界面/程序读数保持一致
            writeBackToVar(released);
        }
        if (changed) {
            sync();
        }
    }

    /**
     * 释放一个「按住直到松手」的按钮（服务端，客户端松手时调用）。
     * 定时弹起的按钮由 {@link #releaseExpiredKeypads()} 自动处理，这里不干预。
     */
    public int releaseKeypad(int id) {
        if (this.level == null || this.level.isClientSide) {
            return COMP_NOT_FORMED;
        }
        CompEntry existing = findComp(id);
        if (existing == null) {
            return COMP_NOT_FOUND;
        }
        if ((existing.type != ConsoleCompType.KEYPAD && existing.type != ConsoleCompType.BUTTON_X
                && existing.type != ConsoleCompType.BUTTON_S)
                || existing.state != CompEntry.HOLD_UNTIL_RELEASE) {
            return COMP_OK;
        }
        int index = this.comps.indexOf(existing);
        // 安全按钮松手后回到「开盖」状态，其它按钮直接回到未按下
        long target = existing.type == ConsoleCompType.BUTTON_S
                ? CompEntry.OPEN_COVER : 0L;
        CompEntry released = CompEntry.copyWith(existing, target, existing.value);
        this.comps.set(index, released);
        // 安全按钮松手后重新开始自动关盖倒计时
        if (existing.type == ConsoleCompType.BUTTON_S) {
            refreshCoverDeadline(released);
        }
        writeBackToVar(released);
        sync();
        return COMP_OK;
    }

    /** 打开安全按钮的玻璃罩：播放开盖音效，并按 coverTicks 记录自动关闭时刻。 */
    private void openCover(CompEntry comp) {
        this.level.playSound(null, this.worldPosition,
                net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.7F);
        refreshCoverDeadline(comp);
    }

    /** 合上安全按钮的玻璃罩：播放关盖音效并清除自动关闭时刻。 */
    private void closeCover(CompEntry comp) {
        this.coverDeadlines.remove(comp.id);
        this.level.playSound(null, this.worldPosition,
                net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.7F);
    }

    /**
     * 重新开始安全按钮盖子的自动关闭倒计时（coverTicks = 0 表示不自动关闭）。
     * 打开盖子、按钮弹起、松手弹起时都会重置，与独立安全按钮「回到开盖状态重新计时」一致。
     */
    private void refreshCoverDeadline(CompEntry comp) {
        int ticks = comp.effectiveCoverTicks();
        if (ticks <= 0) {
            this.coverDeadlines.remove(comp.id);
        } else {
            this.coverDeadlines.put(comp.id, this.level.getGameTime() + ticks);
        }
    }

    /**
     * 安全按钮盖子到点自动关闭（服务端 tick）。
     * 只有仍处于「开盖且按钮已弹起」的组件才会被合上；
     * 按钮按下期间不关盖（与独立安全按钮一致）。
     */
    private void releaseExpiredCovers() {
        if (this.level == null || this.coverDeadlines.isEmpty()) {
            return;
        }
        long now = this.level.getGameTime();
        boolean changed = false;
        for (Map.Entry<Integer, Long> entry : new ArrayList<>(this.coverDeadlines.entrySet())) {
            if (now < entry.getValue()) {
                continue;
            }
            this.coverDeadlines.remove(entry.getKey());
            CompEntry comp = findComp(entry.getKey());
            if (comp == null || comp.type != ConsoleCompType.BUTTON_S
                    || comp.state != CompEntry.OPEN_COVER) {
                continue;
            }
            int index = this.comps.indexOf(comp);
            this.comps.set(index, CompEntry.copyWith(comp, 0L, comp.value));
            changed = true;
            // 自动关盖音效（与原版活板门关闭一致）
            this.level.playSound(null, this.worldPosition,
                    net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.7F);
        }
        if (changed) {
            sync();
        }
    }

    /**
     * 程序调度：按运行间隔累计 tick，到点执行一次。
     *
     * 执行流程遵循「影子数组」三步：读取组件 → VM 执行 → 统一提交；
     * 只有完全成型且 period > 0 时才运行。
     */
    private void tickProgram() {
        if (this.formState != State.FULL) {
            this.programTimer = 0;
            return;
        }
        int period = (int) Math.round(this.programPeriod);
        if (period <= 0) {
            // 0 = 程序停止；保留上一次警告，便于玩家回头看原因
            this.programTimer = 0;
            return;
        }
        if (++this.programTimer < period) {
            return;
        }
        this.programTimer = 0;
        if (this.programDirty) {
            this.programCode = ProgramVM.compile(this.programLines, getProgramScope());
            this.programDirty = false;
        }
        if (this.programCode == null) {
            // 没有可执行行 / 指令数超限：不执行
            return;
        }
        runProgram();
    }

    /** 执行一轮程序：读影子数组 → VM → 批量提交（组件只在值变化时更新，最多同步一次）。 */
    private void runProgram() {
        int slots = varSlots();
        List<ProgramVM.Warning> warnings = new ArrayList<>();
        // 1. 读取：绑定组件的变量取组件当前状态，自定义变量沿用总线值，0 号槽位为运行间隔
        for (int slot = 1; slot < slots; slot++) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                this.shadowFloats[slot] = 0.0F;
                this.shadowBools[slot] = false;
                continue;
            }
            this.shadowFloats[slot] = this.varFloats[slot];
            this.shadowBools[slot] = this.varBools[slot];
            if (def.compId == 0) {
                continue;
            }
            CompEntry comp = findComp(def.compId);
            if (comp == null) {
                // 来源已失效：使用冻结值继续计算，记录一次警告
                addWarning(warnings, ProgramVM.Reason.SOURCE_INVALID);
                continue;
            }
            // 圆盘记录仪的数值变量是唯一数据源，不从组件回读（组件只镜像变量，历史另算）；
            // 同步指示器的转速附加变量同样只读变量本身，不从组件回读指针角度
            if (def.type == VarType.FLOAT && comp.type.supportsFloatVariable()
                    && !comp.type.supportsHistory() && !isSynchroSpeedName(def.name, comp)) {
                this.shadowFloats[slot] = Float.isFinite(comp.value) ? comp.value : 0.0F;
            } else if (def.type == VarType.BOOL && comp.type.supportsBoolVariable()
                    && !comp.type.supportsHistory()) {
                // 按钮类（含安全按钮）：state 存的是弹起时刻 / 开盖标记，不能直接当布尔值，
                // 必须用 isKeypadPressed 判断「此刻是否按下」，否则开盖（-2）会被读成 true
                if (comp.type == ConsoleCompType.KEYPAD || comp.type == ConsoleCompType.BUTTON_X
                        || comp.type == ConsoleCompType.BUTTON_S) {
                    this.shadowBools[slot] = this.level != null
                            && comp.isKeypadPressed(this.level.getGameTime());
                } else if (!isAlarmMuteName(def.name, comp)) {
                    // 警报器静音附加变量（_Q）只读变量自身的值，不从「是否报警」回读
                    this.shadowBools[slot] = comp.state != 0L;
                }
            }
        }
        this.shadowFloats[VAR_SLOT_PERIOD] = this.programPeriod;
        this.shadowBools[VAR_SLOT_PERIOD] = false;

        // 2. 执行：只读写影子数组，不碰组件、不同步
        for (ProgramVM.Warning warning
                : ProgramVM.execute(this.programCode, this.shadowFloats, this.shadowBools)) {
            addWarning(warnings, warning);
        }

        // 3. 提交：变量值写回总线；值变化的绑定组件才上传；最后统一同步一次
        boolean varChanged = false;
        boolean compChanged = false;
        for (int slot = 1; slot < slots; slot++) {
            VarDef def = this.varDefs[slot];
            if (def == null) {
                continue;
            }
            if (def.type == VarType.FLOAT) {
                boolean valueChanged = this.varFloats[slot] != this.shadowFloats[slot];
                if (valueChanged) {
                    this.varFloats[slot] = this.shadowFloats[slot];
                    varChanged = true;
                }
                // 圆盘记录仪的采样改由「推送触发变量为 true」或 CC 显式 push 决定，
                // 这里只上传普通设备的值变化
                if (valueChanged) {
                    compChanged |= uploadVarToComponent(def);
                }
            } else if (this.varBools[slot] != this.shadowBools[slot]) {
                this.varBools[slot] = this.shadowBools[slot];
                varChanged = true;
                compChanged |= uploadVarToComponent(def);
            }
        }
        boolean warningsChanged = !this.runWarnings.equals(warnings);
        if (warningsChanged) {
            this.runWarnings.clear();
            this.runWarnings.addAll(warnings);
        }
        if (varChanged || compChanged || warningsChanged) {
            sync();
        }
    }

    /** 向本轮警告列表追加一条（去重，最多 {@link ProgramVM#MAX_WARNINGS} 条）。 */
    private static void addWarning(List<ProgramVM.Warning> warnings, ProgramVM.Reason reason) {
        addWarning(warnings, new ProgramVM.Warning(-1, reason));
    }

    private static void addWarning(List<ProgramVM.Warning> warnings, ProgramVM.Warning warning) {
        if (warnings.size() < ProgramVM.MAX_WARNINGS && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    /** 最近一次执行的运行期警告（GUI 显示用；客户端通过方块实体同步获得）。 */
    public List<ProgramVM.Warning> getRunWarnings() {
        return this.runWarnings;
    }

    /**
     * 注意：这里刻意不重写 setRemoved()。
     *
     * setRemoved() 在「区块卸载 / 世界保存退出」时也会被调用；若此时遍历成员并调用
     * setChanged()/sendBlockUpdated()，会让正在保存的区块重新变脏并触发区块系统重入，
     * 导致保存世界永久卡死（社区已知问题：在 onChunkUnloaded/setRemoved 中同步方块实体）。
     * 成员标记的清理交给总线被拆时的 playerWillDestroy/onRemove（世界正常运行时执行，可安全同步）。
     */

    /**
     * 把结构信息写入外接长方体范围内的全部控制台方块实体。
     *
     * @param busPos  所属总线坐标；null 表示解除关联
     * @param state   0 未成型 / 1 部分成型 / 2 完全成型
     */
    private void applyMembers(BlockPos busPos, int state) {
        if (this.level == null || this.origin == null
                || this.sizeX <= 0 || this.sizeY <= 0 || this.sizeZ <= 0) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(this.origin,
                this.origin.offset(this.sizeX - 1, this.sizeY - 1, this.sizeZ - 1))) {
            if (this.level.getBlockEntity(pos) instanceof ConsoleBlockEntity member) {
                member.setLink(busPos, state);
            }
        }
    }

    /** 标记存档 + 同步客户端（总线自身与成员）。 */
    private void sync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    /**
     * 是否已经存在任何需要玩家确认覆盖的数据（组件 / 文字 / 变量 / 程序）。
     * 客户端在加载配置前用它决定是否弹出「覆盖确认」。
     */
    public boolean hasConfigData() {
        if (!this.comps.isEmpty() || !this.texts.isEmpty() || countDrawShapes() > 0) {
            return true;
        }
        for (int i = 1; i < VAR_SLOTS_MAX; i++) {
            if (this.varDefs[i] != null) {
                return true;
            }
        }
        for (ProgramIR.Line line : this.programLines) {
            if (line != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 导出当前总线配置为可读 JSON（客户端保存文件时调用）。
     *
     * 只导出可复现的结构信息：结构尺寸 / 朝向、组件、表面文字、变量表、程序；
     * 运行期状态（开关状态、仪表数值、记录仪历史）不写入，加载后按类型默认值初始化。
     */
    public String exportConfigJson() {
        ConsoleConfigJson.Root root = new ConsoleConfigJson.Root();
        // 显式写入格式标识与版本（Root 的 format 默认为 null，缺失即视为非法文件）
        root.format = ConsoleConfigJson.FORMAT;
        root.version = ConsoleConfigJson.VERSION;
        root.size = new ConsoleConfigJson.Size(this.sizeX, this.sizeY, this.sizeZ);
        root.facing = getBusFacing().getName();

        // 组件：按列表顺序导出（加载时按同样顺序重新分配 id）
        for (CompEntry comp : this.comps) {
            ConsoleConfigJson.Component dto = new ConsoleConfigJson.Component();
            dto.type = comp.type.id;
            dto.face = comp.face.getName();
            dto.u = comp.u;
            dto.v = comp.v;
            dto.code = comp.code;
            dto.code2 = comp.code2;
            dto.color = comp.color;
            dto.decimals = comp.decimals;
            dto.unit = comp.unit;
            dto.holdTicks = comp.holdTicks;
            dto.scale = comp.scale;
            // RGB 颜色控制器：连接组件与三个通道变量名（其它类型保持空）
            dto.links = comp.links;
            dto.rgbR = comp.rgbR;
            dto.rgbG = comp.rgbG;
            dto.rgbB = comp.rgbB;
            dto.showOriginal = comp.showOriginal;
            if (!comp.text.isEmpty()) {
                ConsoleConfigJson.TextContent text = new ConsoleConfigJson.TextContent();
                text.content = comp.text.text;
                text.color = comp.text.color;
                text.bold = comp.text.bold;
                text.italic = comp.text.italic;
                text.underline = comp.text.underline;
                text.strikethrough = comp.text.strikethrough;
                dto.text = text;
            }
            root.components.add(dto);
        }

        // 表面文字
        for (TextEntry entry : this.texts) {
            ConsoleConfigJson.Text dto = new ConsoleConfigJson.Text();
            dto.face = entry.face.getName();
            dto.u = entry.u;
            dto.v = entry.v;
            dto.content = entry.text;
            dto.color = entry.color;
            dto.bold = entry.bold;
            dto.italic = entry.italic;
            dto.underline = entry.underline;
            dto.strikethrough = entry.strikethrough;
            dto.anchorH = entry.anchorH;
            dto.anchorV = entry.anchorV;
            dto.scale = entry.scale;
            root.texts.add(dto);
        }

        // 变量表：绑定关系写组件代号（触发变量写圆盘记录仪第二名称），不写内部 id
        for (int i = 1; i < VAR_SLOTS_MAX; i++) {
            VarDef def = this.varDefs[i];
            if (def == null) {
                continue;
            }
            ConsoleConfigJson.Variable dto = new ConsoleConfigJson.Variable();
            dto.name = def.name;
            dto.type = def.type.name();
            dto.trigger = def.trigger;
            if (def.compId != 0) {
                CompEntry bound = findComp(def.compId);
                if (bound != null) {
                    dto.component = def.trigger ? bound.code2 : bound.code;
                }
            }
            if (def.type == VarType.FLOAT) {
                dto.value = this.varFloats[i];
            } else {
                dto.bool = this.varBools[i];
            }
            root.variables.add(dto);
        }

        // 程序：只导出非空行，行号单独记录
        root.program = new ConsoleConfigJson.Program();
        root.program.period = this.programPeriod;
        for (int i = 0; i < MAX_PROGRAM_LINES; i++) {
            ProgramIR.Line line = this.programLines[i];
            if (line == null) {
                continue;
            }
            ConsoleConfigJson.Line dto = new ConsoleConfigJson.Line();
            dto.row = i;
            dto.enabled = line.enabled;
            dto.cond = line.cond.name();
            if (line.cond != ProgramIR.Cond.ALWAYS) {
                dto.condition = varNameOf(line.condSlot);
            }
            // 新版条件表达式与原文（为空时省略，导入端回落到旧三态条件）
            dto.condition_expr = exportExpr(line.condExpr);
            dto.condition_source = line.condSource;
            dto.target = varNameOf(line.targetSlot);
            dto.expr = exportExpr(line.expr);
            dto.comment = line.comment;
            dto.source = line.exprSource;
            root.program.lines.add(dto);
        }
        return ConsoleConfigJson.GSON.toJson(root);
    }

    /** 槽位 → 变量名（未定义时返回 null）。 */
    private String varNameOf(int slot) {
        VarDef def = getVarDef(slot);
        return def == null ? null : def.name;
    }

    /** 表达式原文：空串表示没有；超过上限则截断（配置导入用）。 */
    private static String clipSource(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return source.length() > ProgramIR.Line.MAX_SOURCE
                ? source.substring(0, ProgramIR.Line.MAX_SOURCE) : source;
    }

    /** 表达式树 → JSON（变量写名称）。 */
    private ConsoleConfigJson.Expr exportExpr(ProgramIR.Expr expr) {
        if (expr == null) {
            return null;
        }
        ConsoleConfigJson.Expr dto = new ConsoleConfigJson.Expr();
        dto.op = expr.op.name();
        switch (expr.op) {
            case FLOAT -> dto.number = expr.number;
            case BOOL -> dto.flag = expr.flag;
            case VAR -> dto.var = varNameOf(expr.slot);
            case NEG, NOT, CAST -> dto.left = exportExpr(expr.left);
            default -> {
                dto.left = exportExpr(expr.left);
                dto.right = exportExpr(expr.right);
            }
        }
        return dto;
    }

    /**
     * 应用配置文件（服务端调用）。
     *
     * 校验顺序：成型状态 → JSON 格式 → 结构尺寸 → 总线朝向 → 数量上限 → 内容合法性。
     * 全部校验通过后才整体替换总线数据，任何一步失败都不会改动现有内容。
     */
    public int applyConfigJson(String json) {
        if (this.level == null || this.level.isClientSide) {
            return CFG_INVALID;
        }
        // 只有部分成型可以编辑配置：完全成型已锁定，未成型没有有效表面
        if (this.formState != State.PARTIAL || this.origin == null) {
            return CFG_NOT_FORMED;
        }
        ConsoleConfigJson.Root root;
        try {
            root = ConsoleConfigJson.GSON.fromJson(json, ConsoleConfigJson.Root.class);
        } catch (RuntimeException ex) {
            return CFG_BAD_JSON;
        }
        // 格式标识与版本都必须匹配，避免把无关 JSON 或未来版本的文件套用进来
        if (root == null || !ConsoleConfigJson.FORMAT.equals(root.format)
                || root.version != ConsoleConfigJson.VERSION) {
            return CFG_BAD_JSON;
        }
        // 总线朝向与结构可以整体绕 Y 轴旋转：按保存朝向与当前朝向的差值
        // 把配置一起旋转（face 跟着转、X/Z 尺寸互换），局部 u/v 坐标随坐标系一起转，无需改动。
        Direction busFacing = getBusFacing();
        Direction savedFacing = parseDirection(root.facing);
        if (savedFacing == null || savedFacing.getAxis() == Direction.Axis.Y) {
            return CFG_FACING_MISMATCH;
        }
        int rotation = rotationSteps(savedFacing, busFacing);
        if (rotation < 0) {
            return CFG_FACING_MISMATCH;
        }
        if (root.size == null) {
            return CFG_SIZE_MISMATCH;
        }
        // 旋转 k 个 90° 后，X/Z 两个方向的大小互换；尺寸对不上时组件会整体错位
        int expectX = rotation % 2 == 0 ? root.size.x : root.size.z;
        int expectZ = rotation % 2 == 0 ? root.size.z : root.size.x;
        if (expectX != this.sizeX || root.size.y != this.sizeY || expectZ != this.sizeZ) {
            return CFG_SIZE_MISMATCH;
        }

        List<ConsoleConfigJson.Component> compDtos =
                root.components == null ? List.of() : root.components;
        List<ConsoleConfigJson.Text> textDtos = root.texts == null ? List.of() : root.texts;
        List<ConsoleConfigJson.Variable> varDtos =
                root.variables == null ? List.of() : root.variables;
        if (compDtos.size() > MAX_COMPS || textDtos.size() > MAX_TEXTS
                || varDtos.size() >= varSlots()
                || (root.program != null && root.program.lines != null
                && root.program.lines.size() > MAX_PROGRAM_LINES)) {
            return CFG_LIMIT;
        }

        // ---------- 组件 ----------
        List<CompEntry> newComps = new ArrayList<>();
        Map<String, Integer> codeToId = new HashMap<>();
        Map<String, Integer> code2ToId = new HashMap<>();
        for (ConsoleConfigJson.Component dto : compDtos) {
            if (dto == null) {
                return CFG_INVALID;
            }
            ConsoleCompType type = ConsoleCompType.byId(dto.type);
            // 面向随整体旋转一起转，表面局部坐标 (u, v) 不变
            Direction face = rotateFace(parseDirection(dto.face), rotation);
            if (type == null || face == null) {
                return CFG_INVALID;
            }
            String code = dto.code == null ? "" : dto.code.trim();
            // RGB 颜色控制器的名称为可选项（未命名时只显示模型）
            if (code.isEmpty()) {
                if (type != ConsoleCompType.RGB_COLOR) {
                    return CFG_INVALID;
                }
            } else if (!COMP_CODE_PATTERN.matcher(code).matches()) {
                return CFG_INVALID;
            }
            String key = code.toLowerCase(Locale.ROOT);
            // 主代号与第二名称共用命名空间，任一重复都拒绝（空代号不占用名称）
            if (!code.isEmpty()
                    && (codeToId.containsKey(key) || code2ToId.containsKey(key))) {
                return CFG_INVALID;
            }
            String code2 = "";
            if (type.supportsHistory() && dto.code2 != null && !dto.code2.isBlank()) {
                code2 = dto.code2.trim();
                if (!COMP_CODE_PATTERN.matcher(code2).matches() || code2.equalsIgnoreCase(code)) {
                    return CFG_INVALID;
                }
                String key2 = code2.toLowerCase(Locale.ROOT);
                if (codeToId.containsKey(key2) || code2ToId.containsKey(key2)) {
                    return CFG_INVALID;
                }
            }
            int color = dto.color;
            if (color != CompEntry.NO_COLOR && (color < 0 || color > 0xFFFFFF)) {
                return CFG_INVALID;
            }
            // 坐标夹在表面范围内（配置文件被手工修改时也不会越界）
            double halfW = ConsoleSurface.halfWidthPx(face, busFacing,
                    this.sizeX, this.sizeY, this.sizeZ);
            double halfH = ConsoleSurface.halfHeightPx(face, busFacing,
                    this.sizeX, this.sizeY, this.sizeZ);
            float cu = (float) Math.max(-halfW, Math.min(halfW, dto.u));
            float cv = (float) Math.max(-halfH, Math.min(halfH, dto.v));
            int decimals = Math.max(0, Math.min(CompEntry.MAX_DECIMALS, dto.decimals));
            String unit = dto.unit == null ? "" : dto.unit.trim();
            if (unit.length() > CompEntry.MAX_UNIT_LEN) {
                unit = unit.substring(0, CompEntry.MAX_UNIT_LEN);
            }
            int holdTicks = dto.holdTicks;
            if (holdTicks != CompEntry.DEFAULT_HOLD_TICKS) {
                holdTicks = Math.max(0, Math.min(CompEntry.MAX_HOLD_TICKS, holdTicks));
            }
            float scale = Float.isFinite(dto.scale) ? dto.scale : CompEntry.DEFAULT_SCALE;
            scale = Math.max(CompEntry.MIN_SCALE, Math.min(CompEntry.MAX_SCALE, scale));
            CompText text = CompText.NONE;
            if (dto.text != null && dto.text.content != null && !dto.text.content.isBlank()) {
                int textColor = dto.text.color;
                if (textColor < -1 || textColor > 0xFFFFFF) {
                    textColor = -1;
                }
                text = new CompText(dto.text.content.replace("\n", "").replace("\r", "").trim(),
                        textColor, dto.text.bold, dto.text.italic,
                        dto.text.underline, dto.text.strikethrough);
            }
            // RGB 颜色控制器：连接组件与三个通道变量名（其它类型保持空值）
            String links = "";
            String rgbR = "";
            String rgbG = "";
            String rgbB = "";
            boolean showOriginal = false;
            if (type == ConsoleCompType.RGB_COLOR) {
                // 连接组件：按「;」拆分，去掉空项 / 自身 / 重复项，非法代号直接忽略
                java.util.LinkedHashSet<String> seenLinks = new java.util.LinkedHashSet<>();
                String rawLinks = dto.links == null ? "" : dto.links;
                for (String part : rawLinks.split("[;；]")) {
                    String token = part.trim();
                    if (token.isEmpty() || !COMP_CODE_PATTERN.matcher(token).matches()) {
                        continue;
                    }
                    if (!code.isEmpty() && token.equalsIgnoreCase(code)) {
                        continue;
                    }
                    if (seenLinks.stream().noneMatch(s -> s.equalsIgnoreCase(token))) {
                        seenLinks.add(token);
                    }
                    if (seenLinks.size() >= 32) {
                        break;
                    }
                }
                links = String.join(";", seenLinks);
                rgbR = normalizeChannel(dto.rgbR);
                rgbG = normalizeChannel(dto.rgbG);
                rgbB = normalizeChannel(dto.rgbB);
                if (rgbR == null || rgbG == null || rgbB == null) {
                    return CFG_INVALID;
                }
                // 三个通道名必须互不相同，且不能与主代号相同
                if ((!rgbR.isEmpty() && (rgbR.equalsIgnoreCase(rgbG) || rgbR.equalsIgnoreCase(rgbB)))
                        || (!rgbG.isEmpty() && rgbG.equalsIgnoreCase(rgbB))) {
                    return CFG_INVALID;
                }
                for (String channel : new String[]{rgbR, rgbG, rgbB}) {
                    if (!channel.isEmpty() && channel.equalsIgnoreCase(code)) {
                        return CFG_INVALID;
                    }
                }
                showOriginal = dto.showOriginal;
            }
            int id = newComps.size() + 1;
            CompEntry entry = new CompEntry(id, type, face, cu, cv, code,
                    color, text, CompEntry.defaultState(type), CompEntry.defaultValue(type),
                    null, decimals, unit, holdTicks, scale, code2,
                    links, rgbR, rgbG, rgbB, showOriginal);
            newComps.add(entry);
            if (!code.isEmpty()) {
                codeToId.put(key, id);
            }
            if (!code2.isEmpty()) {
                code2ToId.put(code2.toLowerCase(Locale.ROOT), id);
            }
        }
        // 组件矩形不能重叠（与逐块放置时的规则一致）
        for (int i = 0; i < newComps.size(); i++) {
            CompEntry a = newComps.get(i);
            for (int j = i + 1; j < newComps.size(); j++) {
                CompEntry b = newComps.get(j);
                if (a.face == b.face
                        && Math.abs(a.u - b.u) < a.halfWidthPx() + b.halfWidthPx()
                        && Math.abs(a.v - b.v) < a.halfHeightPx() + b.halfHeightPx()) {
                    return CFG_INVALID;
                }
            }
        }

        // ---------- 表面文字 ----------
        List<TextEntry> newTexts = new ArrayList<>();
        for (ConsoleConfigJson.Text dto : textDtos) {
            if (dto == null || dto.content == null || dto.content.isBlank()) {
                continue;
            }
            Direction face = rotateFace(parseDirection(dto.face), rotation);
            if (face == null) {
                return CFG_INVALID;
            }
            double halfW = ConsoleSurface.halfWidthPx(face, busFacing,
                    this.sizeX, this.sizeY, this.sizeZ);
            double halfH = ConsoleSurface.halfHeightPx(face, busFacing,
                    this.sizeX, this.sizeY, this.sizeZ);
            float u = (float) Math.max(-halfW, Math.min(halfW, dto.u));
            float v = (float) Math.max(-halfH, Math.min(halfH, dto.v));
            int color = (dto.color >= -1 && dto.color <= 0xFFFFFF) ? dto.color : -1;
            int anchorH = Math.max(0, Math.min(2, dto.anchorH));
            int anchorV = Math.max(0, Math.min(2, dto.anchorV));
            float scale = Float.isFinite(dto.scale) ? dto.scale : 0.25F;
            scale = Math.max(TextEntry.MIN_SCALE, Math.min(TextEntry.MAX_SCALE, scale));
            newTexts.add(new TextEntry(newTexts.size() + 1, face, u, v,
                    dto.content.replace("\n", "").replace("\r", ""),
                    color, dto.bold, dto.italic, dto.underline, dto.strikethrough,
                    anchorH, anchorV, scale));
        }

        // ---------- 变量表 ----------
        VarDef[] newDefs = new VarDef[VAR_SLOTS_MAX];
        float[] newFloats = new float[VAR_SLOTS_MAX];
        boolean[] newBools = new boolean[VAR_SLOTS_MAX];
        Map<String, Integer> nameToSlot = new HashMap<>();
        int slot = 1;
        for (ConsoleConfigJson.Variable dto : varDtos) {
            if (dto == null || dto.name == null) {
                return CFG_INVALID;
            }
            if (slot >= varSlots()) {
                return CFG_LIMIT;
            }
            String name = dto.name.trim();
            if (!VAR_NAME_PATTERN.matcher(name).matches()) {
                return CFG_INVALID;
            }
            char first = name.charAt(0);
            if ((first >= '0' && first <= '9')
                    || name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                return CFG_INVALID;
            }
            String key = name.toLowerCase(Locale.ROOT);
            if (nameToSlot.containsKey(key)) {
                return CFG_INVALID;
            }
            VarType type;
            try {
                type = VarType.valueOf(dto.type);
            } catch (RuntimeException ex) {
                return CFG_INVALID;
            }
            boolean trigger = dto.trigger;
            int compId = 0;
            if (dto.component != null && !dto.component.isBlank()) {
                String compKey = dto.component.trim().toLowerCase(Locale.ROOT);
                Integer boundId = trigger ? code2ToId.get(compKey) : codeToId.get(compKey);
                if (boundId == null) {
                    // 触发角色必须绑定到圆盘记录仪的第二名称，找不到说明文件被改坏
                    if (trigger) {
                        return CFG_INVALID;
                    }
                    // 普通变量：组件不存在时保留变量本身，按未绑定处理
                } else {
                    CompEntry bound = newComps.get(boundId - 1);
                    if (bound.type == ConsoleCompType.RGB_COLOR) {
                        // RGB 控制器：主名称对应启用布尔变量，三个通道名对应浮点变量
                        boolean isMain = !bound.code.isEmpty()
                                && name.equalsIgnoreCase(bound.code);
                        boolean isChannel = matchesChannel(name, bound);
                        if (!isMain && !isChannel) {
                            return CFG_INVALID;
                        }
                        type = isMain ? VarType.BOOL : VarType.FLOAT;
                    } else if (bound.type.hasSpeedVariable()
                            && isSynchroSpeedName(name, bound)) {
                        // 同步指示器：转速附加变量固定为浮点
                        type = VarType.FLOAT;
                    } else {
                        // 绑定变量的名称必须与设备代号一致（与 applyVarCreate 的按名绑定规则对齐），
                        // 否则后续解绑 / 重连会按名称找不到它
                        if (!name.equalsIgnoreCase(trigger ? bound.code2 : bound.code)) {
                            return CFG_INVALID;
                        }
                        if (trigger) {
                            if (!bound.type.supportsHistory()) {
                                return CFG_INVALID;
                            }
                            // 触发变量固定为布尔
                            type = VarType.BOOL;
                        } else {
                            VarType deviceType = bound.type.variableType();
                            if (deviceType == null) {
                                return CFG_INVALID;
                            }
                            type = deviceType;
                        }
                    }
                    compId = boundId;
                }
            } else if (trigger) {
                return CFG_INVALID;
            }
            // 未绑定变量不得占用设备名称（与创建变量的规则保持一致）
            if (compId == 0 && (codeToId.containsKey(key) || code2ToId.containsKey(key))) {
                return CFG_INVALID;
            }
            newDefs[slot] = new VarDef(slot, name, type, compId, trigger);
            if (type == VarType.FLOAT) {
                newFloats[slot] = dto.value != null && Float.isFinite(dto.value)
                        ? dto.value : 0.0F;
            } else {
                newBools[slot] = dto.bool != null && dto.bool;
            }
            nameToSlot.put(key, slot);
            slot++;
        }

        // RGB 控制器：主名称（布尔）与三个通道名（浮点）按名称补绑。
        // 控制器可以没有名称，因此通道变量未必能通过 component 字段引用，这里统一按名称接上。
        for (CompEntry comp : newComps) {
            if (comp.type != ConsoleCompType.RGB_COLOR) {
                continue;
            }
            if (!comp.code.isEmpty()) {
                Integer mainSlot = nameToSlot.get(comp.code.toLowerCase(Locale.ROOT));
                if (mainSlot != null) {
                    VarDef def = newDefs[mainSlot];
                    if (def.type != VarType.BOOL
                            || (def.compId != 0 && def.compId != comp.id)) {
                        return CFG_INVALID;
                    }
                    newDefs[mainSlot] = new VarDef(mainSlot, def.name, VarType.BOOL,
                            comp.id, false);
                }
            }
            for (String channel : new String[]{comp.rgbR, comp.rgbG, comp.rgbB}) {
                if (channel.isEmpty()) {
                    continue;
                }
                Integer channelSlot = nameToSlot.get(channel.toLowerCase(Locale.ROOT));
                if (channelSlot == null) {
                    continue;
                }
                VarDef def = newDefs[channelSlot];
                if (def.type != VarType.FLOAT
                        || (def.compId != 0 && def.compId != comp.id)) {
                    return CFG_INVALID;
                }
                newDefs[channelSlot] = new VarDef(channelSlot, def.name, VarType.FLOAT,
                        comp.id, false);
            }
        }

        // 同步指示器：转速附加变量（<代号>_s）按名称补绑为浮点变量
        for (CompEntry comp : newComps) {
            if (!comp.type.hasSpeedVariable() || comp.code.isEmpty()) {
                continue;
            }
            Integer speedSlot = nameToSlot.get(comp.speedVarName().toLowerCase(Locale.ROOT));
            if (speedSlot == null) {
                continue;
            }
            VarDef def = newDefs[speedSlot];
            if (def.type != VarType.FLOAT
                    || (def.compId != 0 && def.compId != comp.id)) {
                return CFG_INVALID;
            }
            newDefs[speedSlot] = new VarDef(speedSlot, def.name, VarType.FLOAT,
                    comp.id, false);
        }

        // 同步指示器：灯亮状态附加变量（<代号>_b）按名称补绑为布尔变量
        for (CompEntry comp : newComps) {
            if (!comp.type.hasLampVariable() || comp.code.isEmpty()) {
                continue;
            }
            Integer lampSlot = nameToSlot.get(comp.lampVarName().toLowerCase(Locale.ROOT));
            if (lampSlot == null) {
                continue;
            }
            VarDef def = newDefs[lampSlot];
            if (def.type != VarType.BOOL
                    || (def.compId != 0 && def.compId != comp.id)) {
                return CFG_INVALID;
            }
            newDefs[lampSlot] = new VarDef(lampSlot, def.name, VarType.BOOL,
                    comp.id, false);
        }

        // 警报器：静音状态附加变量（<代号>_Q）按名称补绑为布尔变量
        for (CompEntry comp : newComps) {
            if (!comp.type.hasMuteVariable() || comp.code.isEmpty()) {
                continue;
            }
            Integer muteSlot = nameToSlot.get(comp.muteVarName().toLowerCase(Locale.ROOT));
            if (muteSlot == null) {
                continue;
            }
            VarDef def = newDefs[muteSlot];
            if (def.type != VarType.BOOL
                    || (def.compId != 0 && def.compId != comp.id)) {
                return CFG_INVALID;
            }
            newDefs[muteSlot] = new VarDef(muteSlot, def.name, VarType.BOOL,
                    comp.id, false);
        }

        // ---------- 程序 ----------
        ProgramIR.Line[] newLines = new ProgramIR.Line[MAX_PROGRAM_LINES];
        float period = 0.0F;
        if (root.program != null) {
            float p = Float.isFinite(root.program.period) ? root.program.period : 0.0F;
            period = Math.max(0.0F, Math.min(MAX_PERIOD_TICKS, Math.round(p)));
            if (root.program.lines != null) {
                // 校验用视图：与最终提交的变量表一致
                ProgramIR.Scope scope = new ProgramIR.Scope() {
                    @Override
                    public VarType typeOf(int s) {
                        return s > 0 && s < VAR_SLOTS_MAX && newDefs[s] != null
                                ? newDefs[s].type : null;
                    }

                    @Override
                    public String nameOf(int s) {
                        return s > 0 && s < VAR_SLOTS_MAX && newDefs[s] != null
                                ? newDefs[s].name : "#" + s;
                    }
                };
                int slots = varSlots();
                for (ConsoleConfigJson.Line dto : root.program.lines) {
                    if (dto == null) {
                        return CFG_INVALID;
                    }
                    if (dto.row < 0 || dto.row >= MAX_PROGRAM_LINES || newLines[dto.row] != null) {
                        return CFG_INVALID;
                    }
                    Integer targetSlot = nameToSlot.get(
                            dto.target == null ? "" : dto.target.trim().toLowerCase(Locale.ROOT));
                    if (targetSlot == null) {
                        return CFG_INVALID;
                    }
                    ProgramIR.Cond cond = ProgramIR.Cond.ALWAYS;
                    int condSlot = 0;
                    ProgramIR.Expr condExpr = null;
                    String condSource = "";
                    if (dto.condition_expr != null) {
                        // 新版条件表达式优先：类型 / 深度由 Line.validate 统一检查
                        condExpr = buildConfigExpr(dto.condition_expr, nameToSlot, 1);
                        if (condExpr == null) {
                            return CFG_INVALID;
                        }
                        condSource = clipSource(dto.condition_source);
                    } else {
                        // 旧三态条件
                        try {
                            cond = ProgramIR.Cond.valueOf(dto.cond);
                        } catch (RuntimeException ex) {
                            return CFG_INVALID;
                        }
                        if (cond != ProgramIR.Cond.ALWAYS) {
                            Integer found = nameToSlot.get(dto.condition == null ? ""
                                    : dto.condition.trim().toLowerCase(Locale.ROOT));
                            if (found == null) {
                                return CFG_INVALID;
                            }
                            condSlot = found;
                        }
                    }
                    ProgramIR.Expr expr = buildConfigExpr(dto.expr, nameToSlot, 1);
                    if (expr == null) {
                        return CFG_INVALID;
                    }
                    String comment = dto.comment == null ? "" : dto.comment.trim();
                    if (comment.length() > ProgramIR.Line.MAX_COMMENT) {
                        comment = comment.substring(0, ProgramIR.Line.MAX_COMMENT);
                    }
                    ProgramIR.Line line = new ProgramIR.Line(dto.enabled, cond, condSlot,
                            targetSlot, expr, comment,
                            clipSource(dto.source), condExpr, condSource);
                    if (line.validate(scope, slots) != ProgramIR.Line.Error.OK) {
                        return CFG_INVALID;
                    }
                    // 按钮面板的变量状态只读，不能作为程序写入目标
                    VarDef targetDef = newDefs[targetSlot];
                    if (targetDef.compId != 0) {
                        CompEntry targetComp = newComps.get(targetDef.compId - 1);
                        if (!targetComp.type.supportsVariableWrite()) {
                            return CFG_INVALID;
                        }
                    }
                    newLines[dto.row] = line;
                }
            }
        }

        // ---------- 全部通过后整体替换 ----------
        this.comps.clear();
        this.comps.addAll(newComps);
        this.nextCompId = newComps.size() + 1;
        this.texts.clear();
        this.texts.addAll(newTexts);
        this.nextTextId = newTexts.size() + 1;
        Arrays.fill(this.varDefs, null);
        Arrays.fill(this.varFloats, 0.0F);
        Arrays.fill(this.varBools, false);
        System.arraycopy(newDefs, 0, this.varDefs, 0, VAR_SLOTS_MAX);
        System.arraycopy(newFloats, 0, this.varFloats, 0, VAR_SLOTS_MAX);
        System.arraycopy(newBools, 0, this.varBools, 0, VAR_SLOTS_MAX);
        Arrays.fill(this.programLines, null);
        System.arraycopy(newLines, 0, this.programLines, 0, MAX_PROGRAM_LINES);
        this.programPeriod = period;
        // 程序与变量都换了：作废字节码与上一轮警告，下次执行前重新编译
        this.programDirty = true;
        this.programTimer = 0;
        this.runWarnings.clear();
        sync();
        return CFG_OK;
    }

    /** 方向名 → Direction（未知返回 null）。 */
    private static Direction parseDirection(String name) {
        if (name == null) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            if (direction.getName().equalsIgnoreCase(name)) {
                return direction;
            }
        }
        return null;
    }

    /**
     * 保存朝向 → 当前朝向需要绕 Y 轴顺时针旋转多少个 90°（0~3）。
     * 任一朝向不是水平方向时返回 -1。
     */
    private static int rotationSteps(Direction from, Direction to) {
        if (from == null || to == null
                || from.getAxis() == Direction.Axis.Y || to.getAxis() == Direction.Axis.Y) {
            return -1;
        }
        Direction current = from;
        for (int i = 0; i < 4; i++) {
            if (current == to) {
                return i;
            }
            current = current.getClockWise();
        }
        return -1;
    }

    /** 让表面朝向跟着结构一起旋转；顶面 / 底面绕 Y 旋转时朝向不变。 */
    private static Direction rotateFace(Direction face, int steps) {
        if (face == null || face.getAxis() == Direction.Axis.Y) {
            return face;
        }
        Direction result = face;
        for (int i = 0; i < steps; i++) {
            result = result.getClockWise();
        }
        return result;
    }

    /**
     * JSON 表达式节点 → IR 表达式树（变量按名称查槽位）。
     * 结构非法、引用不存在的变量或嵌套过深时返回 null。
     */
    private static ProgramIR.Expr buildConfigExpr(ConsoleConfigJson.Expr dto,
                                                  Map<String, Integer> nameToSlot, int depth) {
        if (dto == null || dto.op == null || depth > ProgramIR.Line.MAX_DEPTH) {
            return null;
        }
        ProgramIR.Op op;
        try {
            op = ProgramIR.Op.valueOf(dto.op);
        } catch (RuntimeException ex) {
            return null;
        }
        switch (op) {
            case FLOAT: {
                float value = dto.number == null ? 0.0F : dto.number;
                return Float.isFinite(value) ? ProgramIR.Expr.number(value) : null;
            }
            case BOOL:
                return ProgramIR.Expr.bool(dto.flag != null && dto.flag);
            case VAR: {
                Integer found = nameToSlot.get(
                        dto.var == null ? "" : dto.var.trim().toLowerCase(Locale.ROOT));
                return found == null ? null : ProgramIR.Expr.var(found);
            }
            case NEG:
            case NOT:
            case CAST:
            case FLOOR:
            case FRAC:
            case ABS: {
                ProgramIR.Expr inner = buildConfigExpr(dto.left, nameToSlot, depth + 1);
                return inner == null ? null : ProgramIR.Expr.unary(op, inner);
            }
            default: {
                ProgramIR.Expr left = buildConfigExpr(dto.left, nameToSlot, depth + 1);
                ProgramIR.Expr right = buildConfigExpr(dto.right, nameToSlot, depth + 1);
                return left == null || right == null
                        ? null : ProgramIR.Expr.binary(op, left, right);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("FormState", this.formState.ordinal());
        tag.putInt("Error", this.error.ordinal());
        if (this.origin != null) {
            tag.putLong("Origin", this.origin.asLong());
        }
        tag.putInt("SizeX", this.sizeX);
        tag.putInt("SizeY", this.sizeY);
        tag.putInt("SizeZ", this.sizeZ);
        tag.putInt("Count", this.memberCount);
        // 识别到的结构颜色（决定总线 / aux 外观，也用于结构同色校验）
        tag.putInt("Color", this.structureColor.ordinal());
        // 上一次成型范围：用于重新成型时换算组件/文字坐标（失效后仍需保留）
        if (this.lastOrigin != null) {
            tag.putLong("LastOrigin", this.lastOrigin.asLong());
        }
        tag.putInt("LastSizeX", this.lastSizeX);
        tag.putInt("LastSizeY", this.lastSizeY);
        tag.putInt("LastSizeZ", this.lastSizeZ);
        // 表面文字：整表写入（数量有上限，开销可忽略）
        ListTag textList = new ListTag();
        for (TextEntry entry : this.texts) {
            textList.add(entry.save());
        }
        tag.put("Texts", textList);
        tag.putInt("NextTextId", this.nextTextId);
        // 绘图图形：所有表面合并且带 Face 字段，整表写入
        ListTag drawList = new ListTag();
        for (List<ConsoleShape> list : this.drawShapes.values()) {
            for (ConsoleShape shape : list) {
                drawList.add(shape.save());
            }
        }
        tag.put("Draws", drawList);
        tag.putInt("NextDrawId", this.nextDrawId);
        // 表面组件：整表写入
        ListTag compList = new ListTag();
        for (CompEntry entry : this.comps) {
            compList.add(entry.save());
        }
        tag.put("Comps", compList);
        tag.putInt("NextCompId", this.nextCompId);

        // 程序变量表：定义与值都存放在总线（变量表是唯一数据源，aux 只保存定义）
        tag.putFloat("ProgramPeriod", this.programPeriod);
        ListTag varList = new ListTag();
        // 只保存到「最后一个已定义槽位」，减少默认 64 槽时的 NBT 体积；
        // 配置被调小后，超过当前上限的旧定义仍会完整保留，避免丢数据。
        int saveCount = 1;
        for (int i = 1; i < VAR_SLOTS_MAX; i++) {
            if (this.varDefs[i] != null) {
                varList.add(this.varDefs[i].save());
                saveCount = i + 1;
            }
        }
        tag.put("Vars", varList);
        // 1.20.1 的 CompoundTag 没有 FloatArrayTag 辅助方法，用 Float 列表保存
        ListTag floatList = new ListTag();
        for (int i = 0; i < saveCount; i++) {
            floatList.add(FloatTag.valueOf(this.varFloats[i]));
        }
        tag.put("VarF", floatList);
        int[] boolInts = new int[saveCount];
        for (int i = 0; i < saveCount; i++) {
            boolInts[i] = this.varBools[i] ? 1 : 0;
        }
        tag.putIntArray("VarB", boolInts);

        // 程序行：只保存非空行，行号单独记录，读取时按行号还原
        ListTag progList = new ListTag();
        for (int i = 0; i < MAX_PROGRAM_LINES; i++) {
            ProgramIR.Line line = this.programLines[i];
            if (line == null) {
                continue;
            }
            CompoundTag lineTag = new CompoundTag();
            lineTag.putInt("Idx", i);
            line.save(lineTag);
            progList.add(lineTag);
        }
        tag.put("ProgLines", progList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.formState = State.values()[clamp(tag.getInt("FormState"), State.values().length)];
        this.error = ConsoleStructureHelper.Error.values()[
                clamp(tag.getInt("Error"), ConsoleStructureHelper.Error.values().length)];
        this.origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        this.sizeX = tag.getInt("SizeX");
        this.sizeY = tag.getInt("SizeY");
        this.sizeZ = tag.getInt("SizeZ");
        this.memberCount = tag.getInt("Count");
        // 旧存档没有 Color 字段：按默认色处理
        this.structureColor = ConsoleColor.values()[clamp(tag.getInt("Color"),
                ConsoleColor.values().length)];

        // 上一次成型范围：旧存档没有该字段时，用当前范围兜底（已成型的情况下）
        if (tag.contains("LastOrigin")) {
            this.lastOrigin = BlockPos.of(tag.getLong("LastOrigin"));
            this.lastSizeX = tag.getInt("LastSizeX");
            this.lastSizeY = tag.getInt("LastSizeY");
            this.lastSizeZ = tag.getInt("LastSizeZ");
        } else {
            this.lastOrigin = this.origin;
            this.lastSizeX = this.sizeX;
            this.lastSizeY = this.sizeY;
            this.lastSizeZ = this.sizeZ;
        }

        // 表面文字（旧存档没有该字段时保持空列表）
        this.texts.clear();
        ListTag textList = tag.getList("Texts", Tag.TAG_COMPOUND);
        for (int i = 0; i < textList.size(); i++) {
            this.texts.add(TextEntry.load(textList.getCompound(i)));
        }
        this.nextTextId = Math.max(1, tag.getInt("NextTextId"));

        // 绘图图形（旧存档没有该字段时保持空表）
        this.drawShapes.clear();
        ListTag drawList = tag.getList("Draws", Tag.TAG_COMPOUND);
        for (int i = 0; i < drawList.size(); i++) {
            ConsoleShape shape = ConsoleShape.load(drawList.getCompound(i));
            this.drawShapes.computeIfAbsent(shape.face, key -> new ArrayList<>()).add(shape);
        }
        this.nextDrawId = Math.max(1, tag.getInt("NextDrawId"));

        // 表面组件（旧存档没有该字段时保持空列表）
        this.comps.clear();
        this.coverDeadlines.clear();
        ListTag compList = tag.getList("Comps", Tag.TAG_COMPOUND);
        for (int i = 0; i < compList.size(); i++) {
            CompEntry entry = CompEntry.load(compList.getCompound(i));
            if (entry != null) {
                this.comps.add(entry);
                // 安全按钮读档时若盖子处于打开状态，从现在重新开始自动关闭倒计时
                // （关闭期间流逝的游戏时间不应计入，否则重进世界会立刻关盖）
                if (entry.type == ConsoleCompType.BUTTON_S
                        && entry.state == CompEntry.OPEN_COVER
                        && this.level != null
                        && entry.effectiveCoverTicks() > 0) {
                    this.coverDeadlines.put(entry.id,
                            this.level.getGameTime() + entry.effectiveCoverTicks());
                }
            }
        }
        this.nextCompId = Math.max(1, tag.getInt("NextCompId"));

        // 程序变量表（旧存档没有该字段时保持空表 / 0 值）
        this.programPeriod = tag.getFloat("ProgramPeriod");
        Arrays.fill(this.varDefs, null);
        Arrays.fill(this.varFloats, 0.0F);
        Arrays.fill(this.varBools, false);
        ListTag varList = tag.getList("Vars", Tag.TAG_COMPOUND);
        for (int i = 0; i < varList.size(); i++) {
            VarDef def = VarDef.load(varList.getCompound(i));
            // 槽位重复时以先读到的定义为准，避免损坏存档覆盖有效数据
            if (def != null && this.varDefs[def.slot] == null) {
                this.varDefs[def.slot] = def;
            }
        }
        ListTag floatList = tag.getList("VarF", Tag.TAG_FLOAT);
        int floatCount = Math.min(floatList.size(), VAR_SLOTS_MAX);
        for (int i = 0; i < floatCount; i++) {
            this.varFloats[i] = floatList.getFloat(i);
        }
        int[] boolInts = tag.getIntArray("VarB");
        int boolCount = Math.min(boolInts.length, VAR_SLOTS_MAX);
        for (int i = 0; i < boolCount; i++) {
            this.varBools[i] = boolInts[i] != 0;
        }

        // 程序行（旧存档没有该字段时保持空表）
        Arrays.fill(this.programLines, null);
        ListTag progList = tag.getList("ProgLines", Tag.TAG_COMPOUND);
        for (int i = 0; i < progList.size(); i++) {
            CompoundTag lineTag = progList.getCompound(i);
            int index = lineTag.getInt("Idx");
            if (index < 0 || index >= MAX_PROGRAM_LINES) {
                continue;
            }
            ProgramIR.Line line = ProgramIR.Line.load(lineTag);
            // 重复行号以先读到的为准，避免损坏存档覆盖有效数据
            if (line != null && this.programLines[index] == null) {
                this.programLines[index] = line;
            }
        }

        // 字节码不持久化：读取后重新编译；运行期警告只在同步标签中携带
        this.programCode = null;
        this.programDirty = true;
        this.programTimer = 0;
        this.runWarnings.clear();
        ListTag warnList = tag.getList("ProgWarn", Tag.TAG_COMPOUND);
        for (int i = 0; i < warnList.size(); i++) {
            CompoundTag warnTag = warnList.getCompound(i);
            try {
                ProgramVM.Reason reason = ProgramVM.Reason.valueOf(warnTag.getString("R"));
                this.runWarnings.add(new ProgramVM.Warning(warnTag.getInt("L"), reason));
            } catch (IllegalArgumentException ignored) {
                // 未知警告类型直接忽略，保证旧/新版本互不影响
            }
        }
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(length - 1, value));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = this.saveWithoutMetadata();
        // 运行期警告只存在内存中：随同步标签发给客户端显示，不写入存档
        ListTag warnList = new ListTag();
        for (ProgramVM.Warning warning : this.runWarnings) {
            CompoundTag warnTag = new CompoundTag();
            warnTag.putInt("L", warning.line);
            warnTag.putString("R", warning.reason.name());
            warnList.add(warnTag);
        }
        tag.put("ProgWarn", warnList);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
