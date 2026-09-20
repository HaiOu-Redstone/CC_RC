package com.cc_rc.entity;

import com.cc_rc.ModSounds;
import com.cc_rc.network.EvilGajinLockPacket;
import com.cc_rc.network.EvilGajinRwrPacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * 邪恶盖金（evil_gajin）——敌对生物。
 *
 * 设计（按用户要求）：
 *  - **AI 复用原版僵尸（Zombie）**：继承僵尸全部寻路/目标选择/近战攻击行为
 *    （registerGoals 调 super 获得完整僵尸 AI，会主动锁定并攻击玩家）；
 *  - **不会燃烧**：覆盖 isSunBurnTick() 返回 false（僵尸白天暴露会着火，邪恶盖金不会）；
 *  - **不会捡起装备**：覆盖 canPickUpLoot() 返回 false；
 *  - **静音原版僵尸音效**：覆盖 getAmbientSound/getHurtSound/getDeathSound 返回 null
 *    （不播放僵尸呻吟/受伤/死亡音，全部用自定义语音替代）；
 *  - **属性**：生命 100、攻击伤害 20、基础速度 0.35（血量/伤害在 createAttributes 覆盖）：
 *    - 远处追击（目标 > 10 格）：速度 0.35（≈15.1 m/s，明显快于玩家疾跑 ≈5.6 m/s）；
 *    - 近身（目标 ≤ 10 格）：速度 0.28（≈12.1 m/s，中等速度——用户确认近身 0.12 过慢，
 *      改回此前三档方案的中等档）；
 *  - **语音音效**（详见 ModSounds）：
 *    - 锁定目标瞬间：播放「进攻D点」一次（evil_gajin_lock）——目标为玩家时通过 EvilGajinLockPacket
 *      客户端无距离衰减播放（服务端 playSound 受 16 格衰减限制，锁定时常相隔 16 格以上听不见）；
 *      其它目标保持服务端广播；
 *    - 追击 RWR 循环音（evil_gajin_rwr，约 3 分钟长音，客户端 SoundEngine 循环实例播放）：
 *      - 刚锁定玩家：先播「进攻D点」，等音频播完再等 1 秒（共 LOCK_GAP_TICKS=38 tick）后才启动 RWR；
 *      - 追击中每 RWR_INTERVAL_TICKS=200 tick（10 秒）周期性打断：停 RWR → 播「进攻D点」→ 等待 → 再启动 RWR；
 *      - 状态机保证 RWR 与「进攻D点」**绝不重叠**（先停再播、先播完再起）；
 *    - 攻击造成伤害：随机播放「干得好」/「命中」（evil_gajin_good / evil_gajin_hit，
 *      在 doHurtTarget 内播放，1.20.1 Zombie 无 getAttackSound hook）；
 *    - 击杀目标：目标因本次攻击死亡时播放「摧毁目标」（evil_gajin_destroy）；
 *    - 受到攻击：播放「局势不太妙」（evil_gajin_bad），带 10 秒冷却防止火焰等持续伤害刷屏。
 */
public class EvilGajin extends Zombie {

    /** 距离阈值（格）：超过此距离视为「远处」（加速追击）；其余视为「近身」（中等速度） */
    private static final double FAST_DIST = 10.0;

    /** 两档速度：远处加速追击 / 近身中等速度 */
    private static final double SPEED_FAST = 0.35;
    private static final double SPEED_CLOSE = 0.28;

    /** 受击语音冷却（tick）：10 秒内最多播放一次「局势不太妙」 */
    private static final long HURT_SOUND_COOLDOWN = 200;

    /**
     * 「进攻D点」（evil_gajin_lock）音频约 0.897 秒 ≈ 18 tick，再加上 1 秒（20 tick）
     * 静默间隔，共 38 tick 后才允许启动 RWR 循环音（保证两者不重叠）。
     */
    private static final long LOCK_GAP_TICKS = 18 + 20;

    /** 追击中周期性打断间隔（tick）：每 10 秒停一次 RWR 并插播「进攻D点」 */
    private static final long RWR_INTERVAL_TICKS = 200;

    /** RWR 状态机相位：0=空闲（无玩家目标）；1=锁定玩家等待（LOCK 播完+1s 后启动 RWR）；2=RWR 播放中 */
    private static final int PHASE_IDLE = 0;
    private static final int PHASE_LOCKING = 1;
    private static final int PHASE_RWR = 2;

    /** 上次播放受击语音的游戏时刻（哨兵 Long.MIN_VALUE 表示从未播放，允许首次立即播放） */
    private long lastBadSoundTick = Long.MIN_VALUE;

    /** RWR 状态机：当前相位与相位起始时刻、是否已向玩家发出 RWR 开始包 */
    private int rwrPhase = PHASE_IDLE;
    private long rwrPhaseStartTick = 0;
    private boolean rwrStarted = false;

    /** 当前 RWR 循环音所属的玩家（用于目标切换/失去时补发停止包） */
    private ServerPlayer rwrPlayer = null;

    /** 上一 tick 的锁定目标（用于检测"刚锁定"） */
    private LivingEntity lastLockedTarget = null;

    public EvilGajin(EntityType<? extends EvilGajin> type, Level level) {
        super(type, level);
    }

    /**
     * 属性：以僵尸属性为基础，覆盖生命 100 / 攻击 20 / 速度 0.35。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    /** AI 目标列表：直接继承僵尸完整 AI（追击玩家/近战攻击/寻路等）。 */
    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    /** 不会燃烧：僵尸白天暴露在阳光下会着火，邪恶盖金不受影响。 */
    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    /** 不会捡起装备。 */
    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    /** 无 ambient 呻吟声（僵尸平时哼哼的叫声不播放） */
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    /** 受伤时不播放原版音效（受击语音由 hurt() 播放「局势不太妙」） */
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    /** 死亡时不播放原版音效 */
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    /**
     * 攻击与击杀语音（1.20.1 Zombie 无 getAttackSound hook，改为在 doHurtTarget 内处理）：
     *  - 成功造成伤害：随机播放「干得好」/「命中」；
     *  - 造成伤害且目标死亡：额外播放「摧毁目标」。
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean attacked = super.doHurtTarget(target);
        if (attacked && !this.level().isClientSide) {
            // 攻击/击杀语音：音量 1.2（比默认稍大，避免被环境音盖过；rwr 等循环音不在此调整）
            this.level().playSound(null, this,
                    random.nextBoolean() ? ModSounds.EVIL_GAJIN_GOOD.get() : ModSounds.EVIL_GAJIN_HIT.get(),
                    SoundSource.HOSTILE, 1.2F, 1.0F);
            if (target instanceof LivingEntity living && living.isDeadOrDying()) {
                this.level().playSound(null, this, ModSounds.EVIL_GAJIN_DESTROY.get(),
                        SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
        return attacked;
    }

    /** 受击语音：受到实际伤害时播放「局势不太妙」，10 秒冷却防刷屏（火焰/中毒等持续伤害）。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide) {
            long now = this.level().getGameTime();
            if (lastBadSoundTick == Long.MIN_VALUE || now - lastBadSoundTick >= HURT_SOUND_COOLDOWN) {
                lastBadSoundTick = now;
                this.level().playSound(null, this, ModSounds.EVIL_GAJIN_BAD.get(),
                        SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
        return damaged;
    }

    /**
     * 服务端每 tick 自定义逻辑（仅在服务端执行）：
     *  - 目标从无到有时播放「进攻D点」一次；
     *  - RWR 追击循环音状态机（仅玩家目标）：锁定后延迟启动 + 周期打断插播「进攻D点」，保证不重叠；
     *  - 按目标距离切换移动速度：>10 格快速追击（快于玩家疾跑）、≤10 格中等速度。
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = this.level().getGameTime();
        LivingEntity target = this.getTarget();
        boolean hasTarget = target != null;

        // 1) 锁定瞬间（目标从无到有）触发「进攻D点」：
        //    - 目标为玩家：发 EvilGajinLockPacket，客户端无距离衰减播放（服务端 playSound 受 16 格衰减限制，
        //      锁定时常相隔 16 格以上听不见）；
        //    - 其它目标（村民/铁傀儡等）：保持服务端广播（周围玩家可听到）。
        if (hasTarget && target != lastLockedTarget) {
            if (target instanceof ServerPlayer sp) {
                sendLock(sp);
            } else {
                this.level().playSound(null, this, ModSounds.EVIL_GAJIN_LOCK.get(),
                        SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
        lastLockedTarget = target;

        // 2) RWR 追击循环音状态机（仅玩家目标，时序保证与「进攻D点」不重叠）
        ServerPlayer rwrTarget = (hasTarget && target instanceof ServerPlayer sp) ? sp : null;
        if (rwrTarget == null) {
            // 无玩家目标（含目标为村民等）：停止 RWR 并回到空闲
            if (rwrStarted) {
                sendRwr(false, rwrPlayer);
                rwrStarted = false;
            }
            rwrPlayer = null;
            rwrPhase = PHASE_IDLE;
        } else if (rwrTarget != rwrPlayer) {
            // 刚锁定玩家 / 目标玩家切换：先停旧 RWR（若在播），随后已播过「进攻D点」，
            // 进入等待期（LOCK 播完 + 1s）后再启动新 RWR
            if (rwrStarted) {
                sendRwr(false, rwrPlayer);
                rwrStarted = false;
            }
            rwrPlayer = rwrTarget;
            rwrPhase = PHASE_LOCKING;
            rwrPhaseStartTick = now;
        } else if (rwrPhase == PHASE_LOCKING) {
            // 等待期结束（LOCK 播完 + 1 秒）→ 启动 RWR
            if (now - rwrPhaseStartTick >= LOCK_GAP_TICKS) {
                sendRwr(true, rwrPlayer);
                rwrStarted = true;
                rwrPhase = PHASE_RWR;
                rwrPhaseStartTick = now;
            }
        } else if (rwrPhase == PHASE_RWR) {
            // 追击中周期性打断：先停 RWR，再播「进攻D点」（客户端无衰减），回到等待期（不重叠）
            if (now - rwrPhaseStartTick >= RWR_INTERVAL_TICKS) {
                sendRwr(false, rwrPlayer);
                rwrStarted = false;
                sendLock(rwrPlayer);
                rwrPhase = PHASE_LOCKING;
                rwrPhaseStartTick = now;
            }
        }

        // 3) 距离变速（仅两档）：>10 格快速追击（快于玩家疾跑），≤10 格中等速度
        if (hasTarget && this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = (this.distanceToSqr(target) > FAST_DIST * FAST_DIST)
                    ? SPEED_FAST : SPEED_CLOSE;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
    }

    /** 向指定玩家发送 RWR 循环音开始/停止包（玩家已移除/离线时忽略）。 */
    private void sendRwr(boolean start, ServerPlayer player) {
        if (player == null || player.isRemoved()) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new EvilGajinRwrPacket(this.getId(), start));
    }

    /** 向指定玩家发送「进攻D点」客户端播放包（无距离衰减，玩家已移除/离线时忽略）。 */
    private void sendLock(ServerPlayer player) {
        if (player == null || player.isRemoved()) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new EvilGajinLockPacket(this.getId()));
    }
}
