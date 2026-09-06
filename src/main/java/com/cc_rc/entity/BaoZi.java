package com.cc_rc.entity;

import com.cc_rc.ModEntities;
import com.cc_rc.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 包子（bao_zi）弹射物实体。
 * 飞行逻辑完全沿用原版雪球（ThrowableProjectile 自带重力、旋转等行为）。
 * 命中任意实体或方块时在落点触发爆炸：
 * - 有实体爆炸伤害（原版爆炸伤害计算）；
 * - 不破坏方块（ExplosionInteraction.NONE，方块不破碎、无掉落）；
 * - 粒子效果与音效采用原版爆炸（Level.explode 会播放 GENERIC_EXPLODE 音效并生成爆炸粒子）。
 */
public class BaoZi extends Snowball {
    // 爆炸半径（格）
    private static final float EXPLOSION_RADIUS = 4.0F;

    // 实体工厂构造（注册 EntityType 时使用）：接收实体类型与所在世界
    public BaoZi(EntityType<? extends BaoZi> type, Level level) {
        super(type, level);
    }

    // 投掷构造：必须显式传入本模组实体类型。
    // 注意：原版 Snowball(Level, LivingEntity) 会硬编码 EntityType.SNOWBALL，
    // 且 Snowball 没有 (EntityType, LivingEntity, Level) 构造，故先按本模组实体类型
    // 构造，再手动复刻 ThrowableProjectile 便利构造器的逻辑（设置出生位置 + 所有者）。
    public BaoZi(Level level, LivingEntity shooter) {
        super(ModEntities.BAO_ZI.get(), level);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.setOwner(shooter);
    }

    // 渲染使用的默认物品：抛出后显示为 bao_zi 物品贴图（ThrownItemRenderer 依据 getDefaultItem 兜底）
    @Override
    protected Item getDefaultItem() {
        return ModItems.BAO_ZI.get();
    }

    // 命中任意目标（实体或方块）时触发爆炸；tick() 通过虚方法调用本方法
    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            // 原版爆炸：ExplosionInteraction.NONE 只造成实体伤害 + 原版爆炸粒子/音效，不破坏方块
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }
}
