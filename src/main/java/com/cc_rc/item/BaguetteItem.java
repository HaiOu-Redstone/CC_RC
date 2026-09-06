package com.cc_rc.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 法棍（baguette）物品。
 * 普通食物（营养 2 / 饱和度 2，可堆叠 16），同时通过属性修饰符在主手附加攻击伤害与较强击退：
 * - 攻击伤害：修饰符 3.0 + 空手基础 1.0 = 实际攻击 4（复用原版 BASE_ATTACK_DAMAGE_UUID）；
 * - 击退：ATTACK_KNOCKBACK = 3.0（原版击退附魔每级 +1，等效击退 III）。
 * 悬停描述"坚如磐石"（粗体棕色）。无武器耐久、无攻速惩罚。
 */
public class BaguetteItem extends Item {
    // 击退属性修饰符使用的固定 UUID（原版无击退专用 BASE UUID，故自定）
    private static final UUID KNOCKBACK_UUID = UUID.fromString("b1a2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

    // 描述文字颜色：棕色（使用原版棕色染料色 #835432，非 ChatFormatting 内置色）
    private static final int DESC_COLOR = 0x835432;

    // 主手时的属性修饰符集合（攻击伤害 + 击退）
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    // attackDamage：主手攻击伤害修饰符值（实际伤害 = 修饰符 + 空手基础 1.0）；
    // knockback：主手攻击附加的击退等级（3.0 即较强击退）
    public BaguetteItem(Properties properties, double attackDamage, double knockback) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID,
                        "Baguette damage modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK,
                new AttributeModifier(KNOCKBACK_UUID,
                        "Baguette knockback modifier", knockback, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    // 仅主手附加攻击伤害与击退属性，其它槽位无属性
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    // 悬停描述"坚如磐石"（粗体棕色）
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_baguette")
                .withStyle(Style.EMPTY.withBold(true).withColor(DESC_COLOR)));
    }
}
