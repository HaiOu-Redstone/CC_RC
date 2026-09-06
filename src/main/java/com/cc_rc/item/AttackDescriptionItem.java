package com.cc_rc.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 带攻击伤害的描述型普通物品（非武器类）
 * 继承 DescriptionItem（悬停描述 + 可选颜色），额外通过属性修饰符在主手附加攻击伤害，
 * 但无武器耐久、无攻速惩罚、可正常叠加，用于"五金月饼"这类带攻击力的普通物品。
 */
public class AttackDescriptionItem extends DescriptionItem {

    // 主手时的属性修饰符集合（仅攻击伤害加成）
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    // 无颜色构造：附加指定攻击伤害
    public AttackDescriptionItem(Properties properties, String descriptionKey, double attackDamage) {
        this(properties, descriptionKey, null, attackDamage);
    }

    // 带颜色构造：附加指定攻击伤害，描述文字可指定颜色
    public AttackDescriptionItem(Properties properties, String descriptionKey,
                                 @Nullable ChatFormatting color, double attackDamage) {
        super(properties, descriptionKey, color);
        // 与 SwordItem 相同的攻击伤害修饰符（UUID 复用原版 BASE_ATTACK_DAMAGE_UUID）
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    // 仅主手附加攻击伤害，其它槽位无属性
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }
}