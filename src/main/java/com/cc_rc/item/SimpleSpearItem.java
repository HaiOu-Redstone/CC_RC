package com.cc_rc.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

/**
 * 简易长矛（simple_spear）武器。
 *
 * 继承 SwordItem（自带横扫攻击与攻击属性显示），属性如下：
 * - 文字颜色类似附魔金苹果：Rarity.EPIC（物品名淡紫色）+ isFoil() 返回 true（附魔微光）；
 * - 耐久 130、攻击伤害 +129（Tier 攻击加成 129 + SwordItem 修正 0，tooltip 显示 +129，
 *   实际总伤害 = 基础 1 + 129 = 130）；
 * - +13 攻击范围：向主手属性追加 ForgeMod.ENTITY_REACH +13.0（ADDITION），
 *   攻击距离 = 3.0 默认 + 13.0 = 16.0 格（Forge 1.20.1 攻击判定读取该属性）；
 * - 悬停描述：紫色文字"魔女们的秘密武器"。
 */
public class SimpleSpearItem extends SwordItem {
    // +13 攻击范围属性修改器 UUID（固定值，跨会话一致；注意与原版 BASE_ATTACK_* UUID 不冲突）
    private static final UUID SPEAR_RANGE_MODIFIER_UUID = UUID.fromString("9c3dfa65-6b2e-4f0a-9d1c-8b7e5a4f1c20");

    public SimpleSpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    // 附魔微光：物品外观（含图标渲染）呈现出附魔效果，与附魔金苹果一致
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // 主手追加 +13 攻击范围属性（ForgeMod.ENTITY_REACH，别名 forge:attack_range）
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(super.getAttributeModifiers(slot, stack));
        if (slot == EquipmentSlot.MAINHAND) {
            modifiers.put(ForgeMod.ENTITY_REACH.get(),
                    new AttributeModifier(SPEAR_RANGE_MODIFIER_UUID, "Spear range", 13.0D, AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    // 悬停描述：紫色"魔女们的秘密武器"
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_simple_spear")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}