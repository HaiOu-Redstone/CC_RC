package com.cc_rc.entity;

import com.cc_rc.ModEntities;
import com.cc_rc.ModItems;
import com.cc_rc.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 盖金蜗牛（gajin）——被动动物实体。
 *
 * 设计（按用户要求，AI 参考原版猪 Pig）：
 *  - 被动动物（Animal，MobCategory.CREATURE），可通过刷怪蛋召唤；
 *  - AI 复制原版猪的寻路与行为：恐慌逃跑（PanicGoal）、用「金鹰」繁殖
 *    （BreedGoal）、手持「金鹰」吸引跟随（TemptGoal）、跟随父母
 *    （FollowParentGoal）、绕水随机游走、注视玩家、随机环视；
 *  - **没有乘骑机制**：不实现原版猪的鞍座/骑乘（不覆写
 *    isSaddleable / equipSaddle / getSaddleSlot 等）；
 *  - **右键播放音效**：任何物品（含空手）右键蜗牛播放「蜗牛音效」
 *    gajin（平常无任何 ambient 叫声——不覆写 getAmbientSound，默认 null）；
 *    手持「金鹰」右键额外触发喂食/进入繁殖模式。
 *
 * 属性对齐原版猪：最大生命 10、移动速度 0.25。
 */
public class GajinSnail extends Animal {

    /** 「金鹰」是蜗牛的食物：用于 TemptGoal 吸引与 BreedGoal 繁殖。 */
    private static final Ingredient FOOD_INGREDIENT = Ingredient.of(ModItems.GOLDEN_EAGLE.get());

    /** 右键音效防抖：距上次播放不足 10 tick（0.5 秒）时不再重复播放（防连点叠加）。
     *  哨兵值 Long.MIN_VALUE 表示「从未播放过」，首次右键必定放音（见 mobInteract 的处理）。 */
    private long lastSoundGameTime = Long.MIN_VALUE;

    public GajinSnail(EntityType<? extends GajinSnail> type, Level level) {
        super(type, level);
    }

    /**
     * 属性（对齐原版猪 Pig#createAttributes）：
     *  - 最大生命 10（5 颗心）
     *  - 移动速度 0.25（与猪一致）
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** AI 目标列表（对照原版猪 Pig#registerGoals，去掉骑乘相关逻辑）。 */
    @Override
    protected void registerGoals() {
        // 1 层：游泳（防止溺水）
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 2 层：受到惊吓时恐慌逃跑（与猪一致的速度 2.0）
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        // 3 层：附近有两只成年蜗牛且各持/已食用金鹰时繁殖（slowedDown 1.0）
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        // 4 层：玩家手持「金鹰」时被吸引跟随（1.25 速度，不忽略视野内玩家）
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, FOOD_INGREDIENT, false));
        // 5 层：跟随附近成年亲代
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        // 6 层：绕水随机游走
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        // 7 层：注视玩家（半径 6 格）
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        // 8 层：随机环视
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /**
     * 右键交互：
     *  - 手持「金鹰」：先播放音效，再交给 Animal 默认逻辑（喂食并进入繁殖模式，
     *    消耗物品）；
     *  - 其他物品/空手：仅播放「蜗牛音效」（服务端播放，客户端放可听到范围的
     *    SoundSource.NEUTRAL），返回 SUCCESS。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 服务端播放右键音效（平常无 ambient 叫声，右键是唯一发声途径）；带 0.5 秒防抖
        // 注意：lastSoundGameTime 初始为 Long.MIN_VALUE，若直接算 now - lastSoundGameTime 会
        // 整数溢出成负数，导致首次右键永远不满足 >= 10，音效完全无声；故先显式排除哨兵值再比较。
        if (!this.level().isClientSide) {
            long now = this.level().getGameTime();
            if (lastSoundGameTime == Long.MIN_VALUE || now - lastSoundGameTime >= 10) {
                lastSoundGameTime = now;
                this.level().playSound(null, this, ModSounds.GAJIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
        // 手持金鹰时进入繁殖/喂食逻辑（Animal#mobInteract 检测 isFood）
        if (stack.is(ModItems.GOLDEN_EAGLE.get())) {
            return super.mobInteract(player, hand);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** 食物判定：「金鹰」才能喂食/繁殖。 */
    @Override
    public boolean isFood(ItemStack stack) {
        return FOOD_INGREDIENT.test(stack);
    }

    /** 生成后代：新的一只小盖金蜗牛（同类型）。 */
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.GAJIN.get().create(level);
    }

    /**
     * 受击能力：蜗牛被攻击并实际受到伤害时，给自己施加
     * 「抗性提升 II」与「生命回复 II」效果，持续 30 秒（600 tick）。
     * 效果仅在服务端施加（实体状态效果只在服务端有意义）。
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 1));
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30 * 20, 1));
        }
        return damaged;
    }
}