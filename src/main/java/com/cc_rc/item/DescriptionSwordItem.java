package com.cc_rc.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 通用描述型 SwordItem（带描述文字的武器）
 * 构造时传入一个翻译 key，鼠标悬停时显示该 key 对应的文本；
 * 可选传入 ChatFormatting 颜色，为描述文字染色（默认灰色）。
 */
public class DescriptionSwordItem extends SwordItem {
    private final String descriptionKey;
    @Nullable
    private final ChatFormatting color;

    // 无颜色构造：描述文字使用默认灰色
    public DescriptionSwordItem(Tier tier, int attackDamageModifier, float attackSpeed,
                                Properties properties, String descriptionKey) {
        this(tier, attackDamageModifier, attackSpeed, properties, descriptionKey, null);
    }

    // 带颜色构造：为描述文字指定颜色
    public DescriptionSwordItem(Tier tier, int attackDamageModifier, float attackSpeed,
                                Properties properties, String descriptionKey, @Nullable ChatFormatting color) {
        super(tier, attackDamageModifier, attackSpeed, properties);
        this.descriptionKey = descriptionKey;
        this.color = color;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        MutableComponent text = Component.translatable(descriptionKey);
        if (color != null) {
            text = text.withStyle(color);
        }
        tooltipComponents.add(text);
    }
}
