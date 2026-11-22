package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.ModBlocks;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSlopeBlock;
import com.cc_rc.block.console.ConsoleSlopeGeometry;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.network.ConsoleCompPacket;
import com.cc_rc.network.ConsoleDrawPacket;
import com.cc_rc.network.ConsoleTextMovePacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端：多功能工具拖动控制台表面组件 / 文字 / 绘图图案。
 *
 * 交互方式：按住右键拖动。
 *  - 右键按下时（{@link #begin}）先做一次命中检测，抓住点击点下的组件（优先）、
 *    文字或图案，并记录「目标锚点 - 点击点」的偏移；
 *  - 之后每个客户端 tick 用准星命中的方块表面点重新换算 (u, v)，加上偏移得到新锚点，
 *    有变化时发包给服务端（组件走 {@link ConsoleCompPacket}，文字走
 *    {@link ConsoleTextMovePacket}，图案走 {@link ConsoleDrawPacket}）；
 *  - 松开右键、打开界面、准星离开该表面或结构变化时结束拖动。
 *
 * 只做「抓取 / 跟随 / 发包」，位置合法性（成型状态、范围夹紧、重叠）仍由服务端校验。
 * 因为按住右键会周期性重复触发 useOn，{@link #begin} 在拖动期间重复进入时直接忽略。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConsoleDragClient {

    /** 未在拖动。 */
    private static final int KIND_NONE = 0;
    /** 正在拖动组件。 */
    private static final int KIND_COMP = 1;
    /** 正在拖动表面文字。 */
    private static final int KIND_TEXT = 2;
    /** 正在拖动绘图图案。 */
    private static final int KIND_SHAPE = 3;

    /** 位置变化超过该像素值才发包，避免每 tick 都发。 */
    private static final float SEND_THRESHOLD = 0.25F;

    private static boolean active = false;
    /** 被拖动目标所属的总线坐标。 */
    private static BlockPos busPos;
    /** 拖动起始的斜面控制台坐标（普通表面为 null）。 */
    private static BlockPos slopePos;
    /** 被抓取时点击的表面（拖动过程中必须保持不变）。 */
    private static Direction face;
    private static int kind = KIND_NONE;
    /** 被拖动目标的 id（组件 id 或文字 id）。 */
    private static int targetId = -1;
    /** 抓取偏移：目标锚点坐标 - 抓取时点击点坐标（像素）。 */
    private static float grabOffsetU;
    private static float grabOffsetV;
    /** 上一次发送给服务端的位置，用于去抖。 */
    private static float sentU;
    private static float sentV;

    private ConsoleDragClient() {
    }

    /**
     * 多功能工具右键控制台方块时调用：尝试抓起点击点下的组件或文字。
     *
     * @return 抓住目标返回 {@link InteractionResult#SUCCESS}；
     *         没抓住 / 方块不属于成型结构时返回 {@link InteractionResult#PASS}（不拦截其它交互）
     */
    public static InteractionResult begin(UseOnContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return InteractionResult.PASS;
        }
        // 按住右键会重复进入：已经在拖动就保持原抓取状态
        if (active) {
            return InteractionResult.SUCCESS;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        // 斜面控制台：用「眼睛 → 命中点」视线与坡面求交，换算出宿主表面上的等效坐标
        ConsoleSlopeGeometry.Target slopeTarget =
                ConsoleSlopeGeometry.resolve(level, clickedPos,
                        mc.player.getEyePosition(), context.getClickLocation());
        double u;
        double v;
        BlockPos foundBus;
        if (slopeTarget != null) {
            foundBus = slopeTarget.busPos();
            clickedFace = slopeTarget.face();
            u = slopeTarget.u();
            v = slopeTarget.v();
        } else {
            // 斜面未登记（未成型 / 朝向不符）时不能拖动
            if (level.getBlockState(clickedPos).getBlock() instanceof ConsoleSlopeBlock) {
                hint("message.cc_rc.console.need_form");
                return InteractionResult.SUCCESS;
            }
            u = 0.0D;
            v = 0.0D;
            foundBus = ConsoleStructureHelper.findBus(level, clickedPos);
        }

        // 1. 找所属总线并校验成型状态
        if (foundBus == null || !(level.getBlockEntity(foundBus) instanceof ConsoleBusBlockEntity bus)) {
            return InteractionResult.PASS;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED || bus.getOrigin() == null) {
            hint("message.cc_rc.console.need_form");
            return InteractionResult.SUCCESS;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.FULL) {
            hint("message.cc_rc.console.comp_locked");
            return InteractionResult.SUCCESS;
        }

        // 2. 普通表面：点击点 → 表面像素坐标（斜面的坐标已由 resolve 给出）
        Vec3 hit = context.getClickLocation();
        if (slopeTarget == null) {
            u = ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), clickedFace, bus.getBusFacing(), hit);
            v = ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), clickedFace, bus.getBusFacing(), hit);
            if (!ConsoleSurface.contains(clickedFace, bus.getBusFacing(), bus.getSizeX(),
                    bus.getSizeY(), bus.getSizeZ(), u, v)) {
                return InteractionResult.PASS;
            }
        }

        // 3. 先抓组件（与编辑工具一致：组件优先于表面文字）
        ConsoleBusBlockEntity.CompEntry comp = bus.findCompAt(clickedFace, (float) u, (float) v);
        if (comp != null) {
            kind = KIND_COMP;
            targetId = comp.id;
            grabOffsetU = comp.u - (float) u;
            grabOffsetV = comp.v - (float) v;
        } else {
            ConsoleBusBlockEntity.TextEntry text =
                    ConsoleTextClient.findTextAt(bus, clickedFace, (float) u, (float) v);
            if (text != null) {
                kind = KIND_TEXT;
                targetId = text.id;
                grabOffsetU = text.u - (float) u;
                grabOffsetV = text.v - (float) v;
            } else {
                // 最后尝试图案：以图形第一点（左下角 / 起点）作为拖动锚点
                ConsoleShape shape = bus.findDrawShapeAt(clickedFace, (float) u, (float) v);
                if (shape == null) {
                    // 没抓到东西：不消费交互
                    return InteractionResult.PASS;
                }
                kind = KIND_SHAPE;
                targetId = shape.id;
                grabOffsetU = shape.u1 - (float) u;
                grabOffsetV = shape.v1 - (float) v;
            }
        }

        active = true;
        busPos = foundBus;
        slopePos = slopeTarget == null ? null : clickedPos.immutable();
        face = clickedFace;
        // 记录目标当前锚点作为「上一次已发送位置」：吸附取整后位置有变化才会发包
        if (kind == KIND_COMP) {
            ConsoleBusBlockEntity.CompEntry grabbed = bus.findComp(targetId);
            sentU = grabbed != null ? grabbed.u : (float) u;
            sentV = grabbed != null ? grabbed.v : (float) v;
        } else if (kind == KIND_TEXT) {
            ConsoleBusBlockEntity.TextEntry grabbed = bus.findText(targetId);
            sentU = grabbed != null ? grabbed.u : (float) u;
            sentV = grabbed != null ? grabbed.v : (float) v;
        } else {
            ConsoleShape grabbed = bus.findDrawShape(targetId);
            sentU = grabbed != null ? grabbed.u1 : (float) u;
            sentV = grabbed != null ? grabbed.v1 : (float) v;
        }
        return InteractionResult.SUCCESS;
    }

    /** 每 tick 跟随准星位置并发送更新（松手 / 离开表面时结束拖动）。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // 松开右键 / 打开界面 / 离开世界 → 结束
        if (mc.player == null || mc.level == null || mc.screen != null
                || !mc.options.keyUse.isDown()) {
            stop();
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            stop();
            return;
        }
        double u;
        double v;
        ConsoleBusBlockEntity bus;
        if (slopePos != null) {
            // 斜面拖动：准星必须仍停在同一块斜面上，并沿坡面投影得到宿主坐标
            if (!blockHit.getBlockPos().equals(slopePos)) {
                stop();
                return;
            }
            ConsoleSlopeGeometry.Target target = ConsoleSlopeGeometry.resolve(
                    mc.level, slopePos, mc.player.getEyePosition(), blockHit.getLocation());
            if (target == null
                    || !(mc.level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity slopeBus)
                    || slopeBus.getOrigin() == null) {
                stop();
                return;
            }
            bus = slopeBus;
            u = target.u();
            v = target.v();
        } else {
            // 准星必须仍停留在同一表面：同一朝向、同一结构（可跨方块，方便大结构上拖动）
            if (blockHit.getDirection() != face
                    || !ConsoleStructureHelper.isConsole(
                            mc.level.getBlockState(blockHit.getBlockPos()))
                    || !busPos.equals(ConsoleStructureHelper.findBus(mc.level, blockHit.getBlockPos()))
                    || !(mc.level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity normalBus)
                    || normalBus.getOrigin() == null) {
                stop();
                return;
            }
            bus = normalBus;
            // 当前准星点 → 表面像素坐标，加上抓取偏移得到目标新锚点
            Vec3 hit = blockHit.getLocation();
            u = ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), face, bus.getBusFacing(), hit);
            v = ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), face, bus.getBusFacing(), hit);
        }
        float nu = (float) u + grabOffsetU;
        float nv = (float) v + grabOffsetV;

        // 组件拖动吸附到完整像素：只会落在整数坐标上，精细调节改在组件设置界面改坐标
        if (kind == KIND_COMP || kind == KIND_SHAPE) {
            nu = Math.round(nu);
            nv = Math.round(nv);
        }

        // 夹在表面范围内，避免拖出结构
        double halfW = ConsoleSurface.halfWidthPx(face, bus.getBusFacing(),
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        double halfH = ConsoleSurface.halfHeightPx(face, bus.getBusFacing(),
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        if (kind == KIND_COMP || kind == KIND_SHAPE) {
            // 组件已取整，边界也按整像素收缩，避免夹取后又变回小数
            nu = (float) Math.max(Math.ceil(-halfW), Math.min(Math.floor(halfW), nu));
            nv = (float) Math.max(Math.ceil(-halfH), Math.min(Math.floor(halfH), nv));
        } else {
            nu = (float) Math.max(-halfW, Math.min(halfW, nu));
            nv = (float) Math.max(-halfH, Math.min(halfH, nv));
        }

        // 位置几乎没变就不发包
        if (Math.abs(nu - sentU) < SEND_THRESHOLD && Math.abs(nv - sentV) < SEND_THRESHOLD) {
            return;
        }

        if (kind == KIND_COMP) {
            ConsoleBusBlockEntity.CompEntry comp = bus.findComp(targetId);
            if (comp == null) {
                // 组件已被删除
                stop();
                return;
            }
            // 客户端预判重叠：重叠时不发包，避免服务端每次都回「位置重叠」提示刷屏
            if (overlapsOtherComp(bus, comp, nu, nv)) {
                return;
            }
            ModNetwork.CHANNEL.sendToServer(new ConsoleCompPacket(
                    busPos, comp.id, comp.code, comp.code2, nu, nv, comp.color,
                    comp.decimals, comp.unit, comp.holdTicks, comp.scale,
                    comp.links, comp.rgbR, comp.rgbG, comp.rgbB, comp.showOriginal,
                    false));
        } else if (kind == KIND_TEXT) {
            if (bus.findText(targetId) == null) {
                stop();
                return;
            }
            ModNetwork.CHANNEL.sendToServer(new ConsoleTextMovePacket(busPos, targetId, nu, nv));
        } else if (kind == KIND_SHAPE) {
            if (bus.findDrawShape(targetId) == null) {
                stop();
                return;
            }
            ModNetwork.CHANNEL.sendToServer(
                    ConsoleDrawPacket.move(busPos, face, targetId, nu, nv));
        } else {
            stop();
            return;
        }
        sentU = nu;
        sentV = nv;
    }

    /** 目标组件新位置是否与同表面其它组件重叠（与服务端判定规则一致）。 */
    private static boolean overlapsOtherComp(ConsoleBusBlockEntity bus,
                                             ConsoleBusBlockEntity.CompEntry moving,
                                             float u, float v) {
        for (ConsoleBusBlockEntity.CompEntry entry : bus.getComps()) {
            if (entry.id == moving.id || entry.face != moving.face) {
                continue;
            }
            boolean overlapU = Math.abs(u - entry.u) < moving.halfWidthPx() + entry.halfWidthPx();
            boolean overlapV = Math.abs(v - entry.v) < moving.halfHeightPx() + entry.halfHeightPx();
            if (overlapU && overlapV) {
                return true;
            }
        }
        return false;
    }

    /** 结束当前拖动并清理状态。 */
    private static void stop() {
        active = false;
        busPos = null;
        slopePos = null;
        face = null;
        kind = KIND_NONE;
        targetId = -1;
    }

    /** 动作栏提示。 */
    private static void hint(String key) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(key), true);
        }
    }
}
