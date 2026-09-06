package com.cc_rc.item;

import com.cc_rc.CcRc;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 撬棍（crowbar）武器。
 * 继承 SwordItem（自带横扫攻击），铁质 Tier CROWBAR（攻击加成 12、耐久 1024、铁锭修复）。
 * - 攻击伤害：Tier 加成 12 + SwordItem 修正 0 = 12；
 * - 攻击速度慢：构造传入攻速修正 -3.0（攻击速度 1.0）；
 * - 暴击更强：原版暴击倍率 1.5 → 2.0（CriticalHitEvent 覆写）；
 * - 打击音效：命中实体时播放铁砧落地/放置金属撞击声（LivingHurtEvent，服务端广播）；
 * - 悬停描述两行："物理学圣剑"（浅蓝色粗体）、"f(x)dx"（深蓝色）。
 */
public class CrowbarItem extends SwordItem {
    // 撬棍暴击倍率：原版 1.5，撬棍更高
    private static final float CROWBAR_CRIT_MULTIPLIER = 2.0F;
    // 铁砧打击音效：落地/放置共用同一金属撞击声（block.anvil.land / place）
    private static final float ANVIL_HIT_VOLUME = 1.0F;

    public CrowbarItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    // 悬停描述：物理学圣剑（浅蓝粗体）+ f(x)dx（深蓝，无划线）
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_crowbar")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_crowbar_fx")
                .withStyle(ChatFormatting.DARK_BLUE));
    }

    // 暴击增强 + 铁砧打击音效：主手持撬棍时生效
    @Mod.EventBusSubscriber(modid = CcRc.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CritHandler {
        // 暴击增强：主手持撬棍且为原版暴击（下落跳跃攻击）时，把 1.5 倍暴击提升到 2.0 倍
        @SubscribeEvent
        public static void onCriticalHit(CriticalHitEvent event) {
            Player player = event.getEntity();
            if (player == null) return;
            if (event.isVanillaCritical() && player.getMainHandItem().getItem() instanceof CrowbarItem) {
                event.setDamageModifier(CROWBAR_CRIT_MULTIPLIER);
            }
        }

        // 打击音效：攻击者为手持撬棍的玩家时，在被击实体位置播放铁砧落地/放置声（服务端广播）
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            DamageSource source = event.getSource();
            if (!(source.getEntity() instanceof Player player)) return;
            if (!(player.getMainHandItem().getItem() instanceof CrowbarItem)) return;
            if (player.level().isClientSide) return;
            player.level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, ANVIL_HIT_VOLUME, 1.0F);
        }
    }
}