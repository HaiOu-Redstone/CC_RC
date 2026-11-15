package com.cc_rc.item;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleCompType;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.block.console.ConsoleSurface;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 控制柜钥匙（钥匙开关专用）。
 *
 * 来源：控制柜处于「部分成型」时，空手 Shift+右键钥匙开关可取出一把钥匙
 * （见 {@link ConsoleBusBlockEntity#applyCompUse}）。
 *
 * 用法：手持钥匙右键钥匙开关 → 若钥匙 NBT 里的组件代号与开关代号一致，
 * 消耗钥匙并让开关进入「有钥匙」状态，之后空手右键即可开关。
 * 插入时只比较代号（名字不参与匹配）。
 */
public class ConsoleKeyItem extends Item {

    /** 记录所属组件代号的 NBT 键。 */
    public static final String TAG_CODE = "CompCode";

    public ConsoleKeyItem(Properties properties) {
        super(properties);
    }

    /**
     * 生成一把与指定组件匹配的钥匙。
     *
     * @param code  组件代号（写入 NBT，插入时按此匹配）
     * @param label 显示后缀（组件附带文字；为空时由调用方传代号）
     */
    public static ItemStack createKey(String code, String label) {
        ItemStack stack = new ItemStack(com.cc_rc.ModItems.CONSOLE_KEY.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_CODE, code == null ? "" : code);
        String suffix = label == null || label.isEmpty() ? (code == null ? "" : code) : label;
        stack.setHoverName(Component.translatable("item.cc_rc.console_key.bound", suffix));
        return stack;
    }

    /** 读取钥匙里记录的组件代号（没有返回空字符串）。 */
    public static String keyCode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_CODE);
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
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            insertKey(serverPlayer, level, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 服务端：定位点击位置上的钥匙开关并插入钥匙。 */
    private void insertKey(ServerPlayer player, Level level, UseOnContext context) {
        BlockPos busPos = ConsoleStructureHelper.findBus(level, context.getClickedPos());
        if (busPos == null
                || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            return;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED || bus.getOrigin() == null) {
            return;
        }
        // 点击点 → 表面像素坐标 → 命中组件
        Direction face = context.getClickedFace();
        Direction busFacing = bus.getBusFacing();
        double u = ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        double v = ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        if (!ConsoleSurface.contains(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(), u, v)) {
            return;
        }
        ConsoleBusBlockEntity.CompEntry hit = bus.findCompAt(face, (float) u, (float) v);
        if (hit == null || hit.type != ConsoleCompType.SWITCH_K) {
            player.displayClientMessage(
                    Component.translatable("message.cc_rc.console.key_wrong"), true);
            return;
        }
        int status = bus.applyKeyInsert(hit.id, player.getMainHandItem(), player);
        if (status == ConsoleBusBlockEntity.COMP_OK) {
            player.displayClientMessage(
                    Component.translatable("message.cc_rc.console.key_inserted"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.cc_rc.console.key_wrong"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        String code = keyCode(stack);
        if (!code.isEmpty()) {
            tooltipComponents.add(Component.translatable("item.cc_rc.console_key.code", code)
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("item.cc_rc.console_key.desc")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
