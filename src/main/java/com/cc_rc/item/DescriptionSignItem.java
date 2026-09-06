package com.cc_rc.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * 描述型告示牌物品：继承原版 SignItem（点击地面放立式、点击墙面放壁挂式），
 * 悬停时按顺序逐行显示自定义带样式的描述文字（如黄色斜体"搬运自农夫乐事"）。
 */
public class DescriptionSignItem extends SignItem {
    private final Component[] descriptionLines;

    public DescriptionSignItem(Properties properties, Block standing, Block wall, Component... descriptionLines) {
        super(properties, standing, wall);
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
