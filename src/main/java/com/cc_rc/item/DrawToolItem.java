package com.cc_rc.item;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.client.DrawQuickScreen;
import com.cc_rc.gui.ConsoleDrawMenu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

/**
 * 画图工具。
 *
 * 普通模式：
 *  - 右键控制柜表面 → 打开属性界面（命中已有图案则编辑它，否则以点击点为左下角
 *    打开「未创建」界面，点选类型或贴图后才真正创建）；
 *  - 图案的拖动交给多功能工具。
 *
 * 快速绘画模式（在设置界面里切换）：
 *  - shift + 右键空气 → 打开设置界面（颜色 / 类型 / 贴图 / 缩放 / 线宽 / 图层等）；
 *  - shift + 右键面板 → 只编辑已命中的图案；
 *  - 右键面板 → 直接按当前设置生成图案：预设图案立即生成；矩形 / 线段 / 箭头
 *    需要两次右键（同一条总线同一面），第二点生成图形；
 *  - 「吸附整数」把点击坐标四舍五入为整像素；「平直线优先」会在 |dx| 或 |dy|
 *    超过另一方向 2 倍时把线段 / 箭头拉成水平或垂直。
 *
 * 颜色、线宽、缩放、图层、类型、贴图与三个开关都记录在物品 NBT 上，
 * 换手 / 换物品栏格子后设置依旧保留。
 */
public class DrawToolItem extends Item {

    /** 悬停描述颜色（青色粗体，与编辑工具一致）。 */
    private static final int DESC_COLOR = 0x55FFFF;

    /** 物品 NBT 键。 */
    private static final String TAG_COLOR = "DrawColor";
    private static final String TAG_WIDTH = "DrawWidth";
    private static final String TAG_SCALE = "DrawScale";
    private static final String TAG_LAYER = "DrawLayer";
    private static final String TAG_TYPE = "DrawType";
    private static final String TAG_PRESET = "DrawPreset";
    private static final String TAG_QUICK = "DrawQuick";
    private static final String TAG_SNAP = "DrawSnap";
    private static final String TAG_STRAIGHT = "DrawStraight";

    /** 快速绘画：每个玩家已记录的第一点（仅服务端使用）。 */
    private static final Map<UUID, PendingPoint> PENDING = new HashMap<>();

    public DrawToolItem(Properties properties) {
        super(properties);
    }

    /**
     * 工具设置（保存在物品 NBT 上）。
     *
     * @param type 快速绘画时直接生成的图形类型
     */
    public record Settings(int color, float width, float scale, int layer,
                           ConsoleShapeType type, String preset,
                           boolean quick, boolean snap, boolean straight) {

        /** 新工具默认值。 */
        public static final Settings DEFAULT = new Settings(0xFFFFFF, 1.0F, 1.0F, 0,
                ConsoleShapeType.PRESET, ConsoleDrawPresets.DEFAULT_ID, false, false, false);
    }

    /** 快速绘画记录的第一点。 */
    private record PendingPoint(BlockPos busPos, Direction face, float u, float v) {
    }

    // ---------------------------------------------------------------- NBT 设置

    /** 读取工具设置（字段缺失时使用默认值并夹到合法范围）。 */
    public static Settings readSettings(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        Settings def = Settings.DEFAULT;
        if (tag == null) {
            return def;
        }
        int color = tag.contains(TAG_COLOR) ? tag.getInt(TAG_COLOR) & 0xFFFFFF : def.color();
        float width = tag.contains(TAG_WIDTH) ? tag.getFloat(TAG_WIDTH) : def.width();
        float scale = tag.contains(TAG_SCALE) ? tag.getFloat(TAG_SCALE) : def.scale();
        int layer = tag.contains(TAG_LAYER) ? tag.getInt(TAG_LAYER) : def.layer();
        int type = tag.contains(TAG_TYPE) ? tag.getInt(TAG_TYPE) : def.type().ordinal();
        String preset = tag.contains(TAG_PRESET) ? tag.getString(TAG_PRESET) : def.preset();
        if (ConsoleDrawPresets.byId(preset) == null) {
            preset = def.preset();
        }
        return new Settings(color,
                ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                        Math.min(ConsoleShape.MAX_WIDTH, width))),
                ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                        Math.min(ConsoleShape.MAX_SCALE, scale))),
                Math.max(0, Math.min(ConsoleShape.LAYERS - 1, layer)),
                ConsoleShapeType.byOrdinal(type), preset,
                tag.getBoolean(TAG_QUICK), tag.getBoolean(TAG_SNAP), tag.getBoolean(TAG_STRAIGHT));
    }

    /** 写入工具设置（统一夹紧并量化到 0.1）。 */
    public static void writeSettings(ItemStack stack, Settings settings) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_COLOR, settings.color() & 0xFFFFFF);
        tag.putFloat(TAG_WIDTH, ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, settings.width()))));
        tag.putFloat(TAG_SCALE, ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                Math.min(ConsoleShape.MAX_SCALE, settings.scale()))));
        tag.putInt(TAG_LAYER, Math.max(0, Math.min(ConsoleShape.LAYERS - 1,
                settings.layer())));
        tag.putInt(TAG_TYPE, settings.type().ordinal());
        tag.putString(TAG_PRESET, settings.preset() == null
                ? ConsoleDrawPresets.DEFAULT_ID : settings.preset());
        tag.putBoolean(TAG_QUICK, settings.quick());
        tag.putBoolean(TAG_SNAP, settings.snap());
        tag.putBoolean(TAG_STRAIGHT, settings.straight());
    }

    /** 找出玩家手上（主手优先）的画图工具，没有则返回空栈。 */
    public static ItemStack findHeld(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DrawToolItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 属性界面保存 / 创建后，把颜色、线宽、缩放、图层记回工具 NBT。 */
    public static void remember(Player player, int color, float width, float scale, int layer) {
        ItemStack stack = findHeld(player);
        if (stack.isEmpty()) {
            return;
        }
        Settings old = readSettings(stack);
        writeSettings(stack, new Settings(color, width, scale, layer, old.type(), old.preset(),
                old.quick(), old.snap(), old.straight()));
    }

    // ---------------------------------------------------------------- 交互

    /** 右键空气：shift 打开设置界面；否则取消快速绘画已记录的第一点。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> DrawQuickScreen.open(stack));
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer
                && PENDING.remove(serverPlayer.getUUID()) != null) {
            hint(serverPlayer, "message.cc_rc.draw.cancel_point");
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clickedPos = context.getClickedPos();
        if (!ConsoleStructureHelper.isConsole(level.getBlockState(clickedPos))) {
            // 右键非面板方块：取消快速绘画已记录的第一点
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && PENDING.remove(serverPlayer.getUUID()) != null) {
                hint(serverPlayer, "message.cc_rc.draw.cancel_point");
            }
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            handleUse(serverPlayer, level, clickedPos, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 服务端：普通模式打开属性界面；快速模式按设置直接生成图案。 */
    private void handleUse(ServerPlayer player, Level level, BlockPos clickedPos,
                           UseOnContext context) {
        BlockPos busPos = ConsoleStructureHelper.findBus(level, clickedPos);
        if (busPos == null || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            hint(player, "message.cc_rc.console.need_form");
            return;
        }
        ConsoleBusBlockEntity.State state = bus.getFormState();
        if (state == ConsoleBusBlockEntity.State.UNFORMED) {
            hint(player, "message.cc_rc.console.need_form");
            return;
        }
        if (state == ConsoleBusBlockEntity.State.FULL) {
            hint(player, "message.cc_rc.draw.locked");
            return;
        }

        // 设置取自手上的工具（优先使用触发交互的那只手）
        Settings settings = readSettings(context.getItemInHand());
        Direction face = context.getClickedFace();
        Direction busFacing = bus.getBusFacing();
        float u = (float) ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        float v = (float) ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        // 吸附整数：把点击坐标四舍五入为整像素
        if (settings.snap()) {
            u = Math.round(u);
            v = Math.round(v);
        }
        if (!ConsoleSurface.contains(face, busFacing, bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), u, v)) {
            hint(player, "message.cc_rc.draw.outside");
            return;
        }

        ConsoleShape hit = bus.findDrawShapeAt(face, u, v);

        if (settings.quick()) {
            handleQuickUse(player, bus, busPos, face, u, v, hit, settings);
            return;
        }

        // 普通模式：命中则编辑，否则以点击点为左下角打开「未创建」界面
        if (hit != null) {
            ConsoleDrawMenu.openForShape(player, bus, hit);
            return;
        }
        double halfW = ConsoleSurface.halfWidthPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        double halfH = ConsoleSurface.halfHeightPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        float anchorU = (float) Math.max(-halfW, Math.min(halfW - 2.0D, u));
        float anchorV = (float) Math.max(-halfH, Math.min(halfH - 2.0D, v));
        ConsoleDrawMenu.openForCreate(player, bus, face, anchorU, anchorV, settings);
    }

    /** 快速绘画：预设图案单击生成；矩形 / 线段 / 箭头两次单击生成。 */
    private void handleQuickUse(ServerPlayer player, ConsoleBusBlockEntity bus, BlockPos busPos,
                                Direction face, float u, float v, ConsoleShape hit,
                                Settings settings) {
        // shift 只用于编辑已命中的图案，不会新建
        if (player.isShiftKeyDown()) {
            if (hit != null) {
                ConsoleDrawMenu.openForShape(player, bus, hit);
            } else {
                hint(player, "message.cc_rc.draw.shift_edit_only");
            }
            return;
        }

        if (settings.type() == ConsoleShapeType.PRESET) {
            createPreset(player, bus, face, u, v, settings);
            return;
        }

        PendingPoint pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.busPos().equals(busPos) || pending.face() != face) {
            // 记录第一点（同一玩家在别处点过则覆盖）
            PENDING.put(player.getUUID(), new PendingPoint(busPos, face, u, v));
            hint(player, "message.cc_rc.draw.first_point");
            return;
        }
        PENDING.remove(player.getUUID());

        float u2 = u;
        float v2 = v;
        // 平直线优先：某一方向超过另一方向 2 倍时拉成水平 / 垂直
        if (settings.straight()
                && (settings.type() == ConsoleShapeType.LINE
                || settings.type() == ConsoleShapeType.ARROW)) {
            float dx = u2 - pending.u();
            float dy = v2 - pending.v();
            if (Math.abs(dx) > Math.abs(dy) * 2.0F) {
                v2 = pending.v();
            } else if (Math.abs(dy) > Math.abs(dx) * 2.0F) {
                u2 = pending.u();
            }
        }
        ConsoleShape created = bus.applyDrawAdd(face, settings.layer(), settings.type(),
                settings.color(), settings.width(), pending.u(), pending.v(), u2, v2,
                settings.preset(), 0.0F, settings.scale());
        if (created == null) {
            hint(player, "message.cc_rc.draw.bad");
        } else {
            hint(player, "message.cc_rc.draw.created");
        }
    }

    /** 快速绘画：以点击点为中心生成预设图案（超出表面时夹到表面内）。 */
    private static void createPreset(ServerPlayer player, ConsoleBusBlockEntity bus,
                                     Direction face, float u, float v, Settings settings) {
        ConsoleDrawPresets.Preset def = ConsoleDrawPresets.byId(settings.preset());
        if (def == null) {
            hint(player, "message.cc_rc.draw.bad");
            return;
        }
        float half = def.size() * ConsoleDrawPresets.DISPLAY_SCALE
                * Math.max(ConsoleShape.MIN_SCALE, settings.scale()) / 2.0F;
        double halfW = ConsoleSurface.halfWidthPx(face, bus.getBusFacing(),
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        double halfH = ConsoleSurface.halfHeightPx(face, bus.getBusFacing(),
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        float limU = Math.max(0.0F, (float) halfW - half);
        float limV = Math.max(0.0F, (float) halfH - half);
        float cu = Math.max(-limU, Math.min(limU, u));
        float cv = Math.max(-limV, Math.min(limV, v));
        ConsoleShape created = bus.applyDrawAdd(face, settings.layer(),
                ConsoleShapeType.PRESET, settings.color(), settings.width(), cu, cv, cu, cv,
                def.id(), 0.0F, settings.scale());
        if (created == null) {
            hint(player, "message.cc_rc.draw.bad");
        } else {
            hint(player, "message.cc_rc.draw.created");
        }
    }

    /** 取消玩家已记录的第一点（切换维度等异常情况由调用方按需使用）。 */
    public static void clearPending(Player player) {
        PENDING.remove(player.getUUID());
    }

    private static void hint(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_draw_tool")
                .withStyle(Style.EMPTY.withBold(true).withColor(DESC_COLOR)));
        // 显示工具当前设置，便于快速绘画前确认
        Settings settings = readSettings(stack);
        tooltipComponents.add(Component.translatable("item.cc_rc.draw_tool.settings",
                String.format("#%06X", settings.color()),
                typeName(settings.type()),
                settings.width(), settings.scale(), settings.layer(),
                settings.quick() ? Component.translatable("gui.cc_rc.draw.on")
                        : Component.translatable("gui.cc_rc.draw.off")));
    }

    /** 图形类型的本地化名称。 */
    private static Component typeName(ConsoleShapeType type) {
        return Component.translatable("gui.cc_rc.draw.tool."
                + type.name().toLowerCase(java.util.Locale.ROOT));
    }
}
