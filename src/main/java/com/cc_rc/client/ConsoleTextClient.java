package com.cc_rc.client;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.network.ConsoleCompTextRequestPacket;
import com.cc_rc.network.ConsoleTextRequestPacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端：控制台结构表面文字的交互入口（仅客户端使用）。
 *
 * 用编辑工具右键任意控制台方块时：
 *  1. 找到该方块所属的总线，读取结构范围与成型状态；
 *  2. 把点击点换算成该表面的像素坐标 (u, v)；
 *  3. 用字体宽度做命中检测——点击点是否落在某条已有文字的中心矩形内，
 *     命中则请求编辑那条文字，否则请求在该位置新建；
 *  4. 发送 {@link ConsoleTextRequestPacket}，由服务端校验并打开编辑界面。
 *
 * 命中检测必须放在客户端：服务端没有 Font，无法计算文字宽度。
 */
public final class ConsoleTextClient {

    /** 命中容差（结构像素）：让「右键文字覆盖区域」更容易选中。 */
    private static final float HIT_MARGIN = 1.0F;

    private ConsoleTextClient() {
    }

    /** 编辑工具右键控制台方块时调用。 */
    public static void requestEdit(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();

        // 1. 找所属总线
        BlockPos busPos = ConsoleStructureHelper.findBus(level, clickedPos);
        if (busPos == null || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            hint("message.cc_rc.console.need_form");
            return;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED || bus.getOrigin() == null) {
            hint("message.cc_rc.console.need_form");
            return;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.FULL) {
            hint("message.cc_rc.console.text_locked");
            return;
        }

        // 2. 点击点 → 表面像素坐标
        BlockPos origin = bus.getOrigin();
        int sizeX = bus.getSizeX();
        int sizeY = bus.getSizeY();
        int sizeZ = bus.getSizeZ();
        Direction busFacing = bus.getBusFacing();
        Vec3 hit = context.getClickLocation();
        double u = ConsoleSurface.pixelU(origin, sizeX, sizeY, sizeZ, face, busFacing, hit);
        double v = ConsoleSurface.pixelV(origin, sizeX, sizeY, sizeZ, face, busFacing, hit);

        if (!ConsoleSurface.contains(face, busFacing, sizeX, sizeY, sizeZ, u, v)) {
            hint("message.cc_rc.console.text_outside");
            return;
        }

        // 3a. 命中已放置组件 → 编辑它的附带文字（组件优先于表面文字）
        ConsoleBusBlockEntity.CompEntry comp = bus.findCompAt(face, (float) u, (float) v);
        if (comp != null) {
            ModNetwork.CHANNEL.sendToServer(new ConsoleCompTextRequestPacket(busPos, comp.id));
            return;
        }

        // 3b. 命中检测：把每条文字按「锚点 + 缩放」还原成结构像素矩形，判断点击点是否落在其中
        ConsoleBusBlockEntity.TextEntry hitText = findTextAt(bus, face, (float) u, (float) v);
        int hitId = hitText == null ? -1 : hitText.id;

        // 4. 请求服务端打开编辑界面（textId = -1 表示在点击处新建）
        ModNetwork.CHANNEL.sendToServer(new ConsoleTextRequestPacket(
                busPos, hitId, face.get3DDataValue(), (float) u, (float) v));
    }

    /** 动作栏提示。 */
    private static void hint(String key) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(key), true);
        }
    }

    /**
     * 命中检测：返回该表面上覆盖点击点 (u, v) 的文字，没有返回 null。
     *
     * 把每条文字按「锚点 + 缩放」还原成结构像素矩形（字体宽度只有客户端能算）。
     * 公开给多功能工具的拖动逻辑复用，保证「右键编辑」与「拖动」选中同一目标。
     */
    public static ConsoleBusBlockEntity.TextEntry findTextAt(ConsoleBusBlockEntity bus,
                                                            Direction face, float u, float v) {
        Font font = Minecraft.getInstance().font;
        for (ConsoleBusBlockEntity.TextEntry entry : bus.getTexts()) {
            if (entry.face != face) {
                continue;
            }
            float w = font.width(entry.toComponent()) * entry.scale;
            float h = font.lineHeight * entry.scale;
            // 锚点在文字框的哪一侧 → 推出文字框左右边界
            float left = entry.u - (entry.anchorH == 0 ? 0.0F : entry.anchorH == 2 ? w : w / 2.0F);
            float right = left + w;
            // v 向上为正：上锚点 = 顶边，下锚点 = 底边
            float top = entry.v + (entry.anchorV == 0 ? 0.0F : entry.anchorV == 2 ? h : h / 2.0F);
            float bottom = top - h;
            if (u >= left - HIT_MARGIN && u <= right + HIT_MARGIN
                    && v >= bottom - HIT_MARGIN && v <= top + HIT_MARGIN) {
                return entry;
            }
        }
        return null;
    }
}
