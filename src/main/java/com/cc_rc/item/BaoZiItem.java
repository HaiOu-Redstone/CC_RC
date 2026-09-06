package com.cc_rc.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 包子（bao_zi）物品。
 * - 右键：作为食物食用（营养 4 / 饱和度 2，由注册时 FoodProperties 驱动）；
 * - 左键：投掷出 bao_zi 弹射物实体（命中爆炸），由客户端输入事件 + C2S 数据包触发；
 * - 悬停描述"包子雷？"（红色斜体）。
 */
public class BaoZiItem extends Item {
    // 投掷冷却时长（游戏刻）：0.5 秒 = 10 tick，防止连续投掷
    public static final int THROW_COOLDOWN_TICKS = 10;

    public BaoZiItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_bao_zi")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}
