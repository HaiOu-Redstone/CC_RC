package com.cc_rc.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * 自定义工具材质（按 1.20.1 的 Tier 接口实现，共 6 个方法，无 1.21 新增的 getIncorrectBlocksForDrops）。
 * 从旧模组（1.21）移植：冰冻罗非鱼。攻击伤害通过 attackDamageBonus 承担（64），
 * SwordItem 构造时"基础伤害修正"传 0，总攻击伤害 = 0 + 64 = 64，与旧模组一致。
 */
public enum ModToolTiers implements Tier {

    // 参数：耐久、挖掘速度、攻击伤害加成、挖掘等级、附魔能力、修复材料
    // 冰冻罗非鱼：彩蛋武器（蓝冰修复）。攻击伤害加成 255 承担输出（SwordItem 修正 0），
    // 修改器显示 +255 攻击伤害；玩家实际总伤害 = 基础 1 + 255 = 256。
    FROZEN_TILAPIA(2, 12.0F, 255.0F, 0, 0, () -> Ingredient.of(Items.BLUE_ICE)),
    // 撬棍：铁质"物理学圣剑"（铁锭修复）。攻击伤害加成 19 承担输出（SwordItem 修正 0），
    // 修改器显示 +19 攻击伤害；玩家实际总伤害 = 基础 1 + 19 = 20；攻速慢由物品构造传入 -3.0
    // （SwordItem 无攻速字段）；耐久 1024。
    CROWBAR(1024, 6.0F, 19.0F, 2, 10, () -> Ingredient.of(Items.IRON_INGOT)),
    // 简易长矛：耐久 130、攻击伤害加成 129（SwordItem 修正 0，修改器显示 +129，实际总伤害
    // = 基础 1 + 129 = 130），铁锭修复；攻击范围 +13 由 SimpleSpearItem 覆写 getAttributeModifiers 追加。
    SIMPLE_SPEAR(130, 1.5F, 129.0F, 2, 10, () -> Ingredient.of(Items.IRON_INGOT));

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int level;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    ModToolTiers(int uses, float speed, float attackDamageBonus, int level, int enchantmentValue,
                 Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.level = level;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    // 耐久（旧模组只给了 2 点：冰冻罗非鱼几乎一次即坏，属彩蛋武器设定）
    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamageBonus;
    }

    // 挖掘等级：0 = 木/金，1 = 石，2 = 铁，3 = 钻石
    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}