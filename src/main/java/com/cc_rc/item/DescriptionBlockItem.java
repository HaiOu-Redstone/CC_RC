package com.cc_rc.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * 通用描述型 BlockItem
 * 构造时传入一个或多个翻译 key / 已构建的样式化 Component，鼠标悬停时按顺序逐行显示
 */
public class DescriptionBlockItem extends BlockItem {
    private final Component[] descriptionLines;

    // 单个翻译 key 构造（默认灰色文字），兼容既有用法
    public DescriptionBlockItem(Block block, Properties properties, String descriptionKey) {
        this(block, properties, Component.translatable(descriptionKey));
    }

    // 多行样式化描述构造：调用方自行构建带颜色/字体样式的 Component，逐行显示
    public DescriptionBlockItem(Block block, Properties properties, Component... descriptionLines) {
        super(block, properties);
        this.descriptionLines = descriptionLines;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        for (Component line : descriptionLines) {
            tooltipComponents.add(line);
        }
    }
}
