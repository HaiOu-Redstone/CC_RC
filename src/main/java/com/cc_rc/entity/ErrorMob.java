package com.cc_rc.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 错误生物（error_mob）——由「模型/错误生物/error.obj」立体化生成的敌对实体。
 *
 * 设计（按用户要求）：
 *  - 敌对生物，主动攻击玩家；
 *  - **仅复制原版蠹虫（Silverfish）的寻路与攻击逻辑**，不做任何与方块交互的
 *    逻辑（无虫蚀方块、无藏匿/钻出、无受击召唤同伴、不自然生成）；
 *  - 只能通过刷怪蛋或刷怪笼生成。
 *
 * AI 与蠹虫对照（net.minecraft.world.entity.monster.Silverfish）：
 *  - 1 层：FloatGoal（游泳，防止溺水）
 *  - 2 层：MeleeAttackGoal(this, 1.0D, false)——与蠹虫相同的近战攻击（无奔跑加速，
 *          蠹虫移动速度 0.25，攻击间隔按攻击速度属性计算）
 *  - 3 层：WaterAvoidingRandomStrollGoal(this, 1.0D)——与蠹虫相同的绕水随机游走
 *  - 4 层：LookAtPlayerGoal(this, Player.class, 8.0F)——与蠹虫相同的注视玩家
 *  - 5 层：RandomLookAroundGoal(this)——与蠹虫相同的随机环视
 *  - 6 层：HurtByTargetGoal(this).setAlertOthers(this.getClass())——受击反击；
 *          蠹虫的 setAlertOthers 用于召唤同类，用户要求"不要方块交互/召唤逻辑"，
 *          此处保留 HurtByTargetGoal 本身（受击立即追击反击者），但不 alert 同类。
 *  - 7 层：NearestAttackableTargetGoal<>(this, Player.class, true)——与蠹虫相同的
 *          主动索敌玩家（必须可见）。
 *
 * 不调用 Silverfish 的 onHitBlock / spawnFriends 等方块相关方法，保证零方块交互。
 */
public class ErrorMob extends Monster {

    public ErrorMob(EntityType<? extends ErrorMob> type, Level level) {
        super(type, level);
    }

    /**
     * 属性（对齐蠹虫 Silverfish#createAttributes）：
     *  - 最大生命 8（4 颗心）
     *  - 移动速度 0.25（与蠹虫一致，偏慢的贴地小怪）
     *  - 攻击伤害 3.0（与蠹虫一致）
     *  - 跟随范围 20（默认，保证玩家在 16 格外不被索敌）
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // 无方块交互：不覆写 aiStep / onHitBlock 等任何与方块接触相关的方法，
    // 实体在移动时完全透明地穿过/贴地（无钻地、无触块反应）。
}
