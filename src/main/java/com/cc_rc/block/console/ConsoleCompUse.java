package com.cc_rc.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 控制台组件「使用」交互（空手右键）。
 *
 * 组件本身没有方块级碰撞体，点击位置由 {@link BlockHitResult} 的精确命中点换算成
 * 表面像素坐标，再在组件矩形内做命中检测（与放置 / 编辑使用同一套坐标）。
 *
 * 操作方式：
 *  - 旋钮开关 / 拉杆开关：右键切换开 / 关；
 *  - 按钮面板：右键按下；弹起时间 > 0 时保持该时长后自动弹起，为 0 时按住一直按下、松开右键才弹起；
 *  - 弧形仪表（调节器仪表）：左下（红）按钮 -1%（Shift 时 -10%）、右下（绿）按钮 +1%（Shift 时 +10%）；
 *  - 数字键盘：打开数字输入界面（确认后写入数值与绑定变量）；
 *  - 其余组件暂无功能（立式仪表等预留）。
 *
 * 服务端与客户端都会调用本方法：客户端只负责判断「是否点中组件」以决定是否消费交互，
 * 真正的状态变更只在服务端执行（{@link ConsoleBusBlockEntity#applyCompUse}）。
 */
public final class ConsoleCompUse {

    private ConsoleCompUse() {
    }

    /**
     * 尝试把一次空手右键作用于命中的组件。
     *
     * @return true 表示点中了组件（调用方应消费该次交互）
     */
    public static boolean tryUse(Level level, Player player, BlockPos clickedPos,
                                 BlockHitResult hit) {
        BlockPos busPos;
        Direction face;
        double u;
        double v;
        // 斜面控制台：用「眼睛 → 命中点」视线与坡面求交，换算出宿主表面上的等效坐标
        ConsoleSlopeGeometry.Target slopeTarget =
                ConsoleSlopeGeometry.resolve(level, clickedPos,
                        player.getEyePosition(), hit.getLocation());
        if (slopeTarget != null) {
            busPos = slopeTarget.busPos();
            face = slopeTarget.face();
            u = slopeTarget.u();
            v = slopeTarget.v();
        } else {
            // 1. 找所属总线，未成型时组件不存在
            busPos = ConsoleStructureHelper.findBus(level, clickedPos);
            if (busPos == null
                    || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus0)) {
                return false;
            }
            if (bus0.getFormState() == ConsoleBusBlockEntity.State.UNFORMED
                    || bus0.getOrigin() == null) {
                return false;
            }

            // 2. 命中点 → 表面像素坐标
            face = hit.getDirection();
            Direction busFacing = bus0.getBusFacing();
            u = ConsoleSurface.pixelU(bus0.getOrigin(), bus0.getSizeX(), bus0.getSizeY(),
                    bus0.getSizeZ(), face, busFacing, hit.getLocation());
            v = ConsoleSurface.pixelV(bus0.getOrigin(), bus0.getSizeX(), bus0.getSizeY(),
                    bus0.getSizeZ(), face, busFacing, hit.getLocation());
            if (!ConsoleSurface.contains(face, busFacing,
                    bus0.getSizeX(), bus0.getSizeY(), bus0.getSizeZ(), u, v)) {
                return false;
            }
        }
        if (!(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            return false;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED
                || bus.getOrigin() == null) {
            return false;
        }

        // 3. 组件命中检测
        ConsoleBusBlockEntity.CompEntry comp = bus.findCompAt(face, (float) u, (float) v);
        if (comp == null) {
            return false;
        }

        // 数字键盘：空手右键打开数字输入界面（界面只在客户端打开，确认时发包提交数值）
        if (comp.type == ConsoleCompType.NUMPAD) {
            if (level.isClientSide) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.cc_rc.client.ConsoleNumpadScreen.open(
                                bus.getBlockPos().immutable(), comp.id));
            }
            return true;
        }

        // 4. 调节器仪表只在下半区的左右按钮上响应（上半是表盘）
        int action = -1;
        if (comp.type == ConsoleCompType.GAUGE_ARC) {
            if (v >= comp.v) {
                return true;
            }
            // U 轴向外看是「向右」：左侧按钮（观察者左，红色）为 -U，即 action 0 = 减
            action = u < comp.u ? 0 : 1;
        }
        // 警报器：从左到右为 方形灯 / 静音按钮 / 重置按钮，
        // 按模型实测范围（相对组件中心，含模型缩放）划分三段：
        // 灯在 -0.92~-0.36，中间按钮约 -0.31~0.31，右按钮约 0.33~0.93（均为半宽的比例）
        if (comp.type == ConsoleCompType.ALARM) {
            float half = comp.halfWidthPx();
            float rel = half <= 0.0F ? 0.0F : (float) ((u - comp.u) / half);
            if (rel < -0.33F) {
                // 最左侧方形灯只显示，不响应点击（仍消费交互，避免误开界面）
                return true;
            }
            // action 2 = 静音（中），3 = 重置（右）
            action = rel > 0.32F ? 3 : 2;
        }

        // 5. 客户端只需消费交互；状态变更由服务端执行并同步回客户端
        if (level.isClientSide) {
            // 弹起时间为 0 的按钮为「按住直到松手」：客户端记录当前按住的按钮，
            // 松开右键时补发释放包（按住期间原版重复触发 use 在服务端是幂等的）
            boolean holdButton = (comp.type == ConsoleCompType.KEYPAD
                    || comp.type == ConsoleCompType.BUTTON_X
                    || comp.type == ConsoleCompType.BUTTON_S)
                    && comp.effectiveHoldTicks() <= 0;
            // 安全按钮只有开盖状态才能按下（合盖时右键无响应）
            if (comp.type == ConsoleCompType.BUTTON_S
                    && comp.state != ConsoleBusBlockEntity.CompEntry.OPEN_COVER) {
                holdButton = false;
            }
            if (holdButton) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.cc_rc.client.ConsoleKeypadClient.press(
                                bus.getBlockPos().immutable(), comp.id));
            }
            return true;
        }
        bus.applyCompUse(comp.id, action, player.isShiftKeyDown(), player);
        return true;
    }
}
