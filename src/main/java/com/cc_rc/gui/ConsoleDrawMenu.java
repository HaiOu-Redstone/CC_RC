package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.item.DrawToolItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

/**
 * 控制柜表面绘图图形的属性菜单。
 *
 * 只携带单个图形的全部参数：总线坐标、图形 id、所在表面、类型、颜色、线宽、
 * 图层、两端点像素坐标、预设参数，以及表面半宽 / 半高（用于界面夹紧与显示）。
 *
 * 图形位置与参数的写入都走网络包（服务端校验），菜单本身只负责传初始值。
 */
public class ConsoleDrawMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 总线方块坐标。 */
    public final BlockPos pos;
    /** 图形 id（-1 表示尚未创建）。 */
    public int shapeId;
    /** 是否处于「尚未创建」状态：此时关闭界面不会在表面上留下任何图案。 */
    public boolean pending;
    /** 图形所在表面（Direction.get3DDataValue）。 */
    public final int faceOrd;
    /** 图形类型。 */
    public final ConsoleShapeType type;
    /** 颜色（0xRRGGBB）。 */
    public final int color;
    /** 线宽（0.1~4 像素，支持一位小数）。 */
    public final float width;
    /** 图层（0~7）。 */
    public final int layer;
    /** 第一点：矩形对角 / 线段起点 / 箭头尾部 / 预设中心。 */
    public final float u1;
    public final float v1;
    /** 第二点：矩形对角 / 线段终点 / 箭头尖端。 */
    public final float u2;
    public final float v2;
    /** 预设图案 id、旋转角度（度）与缩放倍数（0.1~3，支持一位小数）。 */
    public final String preset;
    public final float rotation;
    public final float scale;
    /** 表面半宽 / 半高（像素）。 */
    public final float halfW;
    public final float halfH;

    public ConsoleDrawMenu(int windowId, BlockPos pos, int shapeId, boolean pending,
                           int faceOrd, ConsoleShapeType type, int color, float width, int layer,
                           float u1, float v1, float u2, float v2,
                           String preset, float rotation, float scale,
                           float halfW, float halfH) {
        super(ModMenuTypes.CONSOLE_DRAW.get(), windowId);
        this.pos = pos;
        this.shapeId = shapeId;
        this.pending = pending;
        this.faceOrd = faceOrd;
        this.type = type == null ? ConsoleShapeType.RECT : type;
        this.color = color & 0xFFFFFF;
        this.width = width;
        this.layer = layer;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.preset = preset == null ? "" : preset;
        this.rotation = rotation;
        this.scale = scale;
        this.halfW = halfW;
        this.halfH = halfH;
    }

    /** 图形所在表面。 */
    public Direction face() {
        Direction[] faces = Direction.values();
        return faces[Math.max(0, Math.min(faces.length - 1, this.faceOrd))];
    }

    /** 写入打开界面的同步数据（服务端调用），顺序必须与 {@link #read} 一致。 */
    public static void write(FriendlyByteBuf buf, ConsoleDrawMenu menu) {
        buf.writeBlockPos(menu.pos);
        buf.writeVarInt(menu.shapeId);
        buf.writeBoolean(menu.pending);
        buf.writeVarInt(menu.faceOrd);
        buf.writeVarInt(menu.type.ordinal());
        buf.writeInt(menu.color);
        buf.writeFloat(menu.width);
        buf.writeVarInt(menu.layer);
        buf.writeFloat(menu.u1);
        buf.writeFloat(menu.v1);
        buf.writeFloat(menu.u2);
        buf.writeFloat(menu.v2);
        buf.writeUtf(menu.preset, 32);
        buf.writeFloat(menu.rotation);
        buf.writeFloat(menu.scale);
        buf.writeFloat(menu.halfW);
        buf.writeFloat(menu.halfH);
    }

    /** 读取同步数据并构造菜单（客户端调用）。 */
    public static ConsoleDrawMenu read(int windowId, FriendlyByteBuf buf) {
        return new ConsoleDrawMenu(windowId, buf.readBlockPos(), buf.readVarInt(),
                buf.readBoolean(), buf.readVarInt(),
                ConsoleShapeType.byOrdinal(buf.readVarInt()), buf.readInt(),
                buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readUtf(32), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    /** 服务端打开某个已有图形的属性界面。 */
    public static void openForShape(ServerPlayer player, ConsoleBusBlockEntity bus,
                                    ConsoleShape shape) {
        open(player, bus, shape.face, shape.id, false, shape.type, shape.color,
                shape.width, shape.layer, shape.u1, shape.v1, shape.u2, shape.v2,
                shape.preset, shape.rotation, shape.scale);
    }

    /**
     * 服务端打开「尚未创建」的属性界面。
     *
     * 玩家刚点开时没有任何选中图形，关闭界面不会创建图案；只有点击了类型按钮或
     * 右侧贴图样式后，界面才发送创建请求（见 ConsoleDrawPacket.OP_CREATE）。
     */
    public static void openForCreate(ServerPlayer player, ConsoleBusBlockEntity bus,
                                     Direction face, float anchorU, float anchorV,
                                     DrawToolItem.Settings settings) {
        // 颜色 / 线宽 / 缩放 / 图层 / 预设取自工具 NBT；旋转默认 0，
        // 选择线段或箭头时界面会改用「长度 8、角度 0」的默认几何
        open(player, bus, face, -1, true, ConsoleShapeType.RECT, settings.color(),
                settings.width(), settings.layer(), anchorU, anchorV,
                anchorU + 16.0F, anchorV + 8.0F,
                settings.preset(), 0.0F, settings.scale());
    }

    /** 打开属性界面（所有入口共用）。 */
    private static void open(ServerPlayer player, ConsoleBusBlockEntity bus, Direction face,
                             int shapeId, boolean pending, ConsoleShapeType type, int color,
                             float width, int layer, float u1, float v1, float u2, float v2,
                             String preset, float rotation, float scale) {
        BlockPos busPos = bus.getBlockPos().immutable();
        Direction busFacing = bus.getBusFacing();
        float halfW = (float) ConsoleSurface.halfWidthPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        float halfH = (float) ConsoleSurface.halfHeightPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        ConsoleDrawMenu template = new ConsoleDrawMenu(-1, busPos, shapeId, pending,
                face.get3DDataValue(), type, color, width, layer, u1, v1, u2, v2,
                preset, rotation, scale, halfW, halfH);
        try {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.cc_rc.draw.title");
                }

                @Override
                public ConsoleDrawMenu createMenu(int windowId, Inventory inventory, Player p) {
                    return new ConsoleDrawMenu(windowId, busPos, shapeId, pending,
                            face.get3DDataValue(), type, color, width, layer, u1, v1, u2, v2,
                            preset, rotation, scale, halfW, halfH);
                }
            }, buf -> write(buf, template));
        } catch (Throwable t) {
            LOGGER.error("图案设置界面打开失败：", t);
            player.sendSystemMessage(Component.literal(
                    "图案设置界面打开失败。若刚进行过模组重编译，请重启游戏后再试。"));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // 实际校验在网络包服务端处理（成型状态 + 距离）；这里只负责「走远自动关闭」
        return player.distanceToSqr(
                this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    // 无物品槽位
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
