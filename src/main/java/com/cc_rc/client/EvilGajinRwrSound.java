package com.cc_rc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * 邪恶盖金追击循环音（RWR）客户端实例：跟随某只邪恶盖金实体位置循环播放
 * （实现模式参考原版蜜蜂飞行音效 BeeFlightSoundInstance）。
 *
 * 实现要点：
 *  - 继承 AbstractTickableSoundInstance（可 tick 的音效实例，SoundEngine 每 tick 回调 tick()）；
 *  - looping = true：SoundEngine 对该实例持续循环播放（rwr 音效长达约 3 分钟，作为
 *    "追击警示音"循环发声），直到调用父类 stop()（final 方法，置 stopped 后 SoundEngine 移除）；
 *  - 衰减采用**手动控制**：attenuation = NONE（不交给 SoundEngine 的默认 16 格陡峭衰减），
 *    改为在 tick() 内按与玩家的实际距离计算 volume = 1 - 距离/32（32 格平缓衰减）——
 *    满足"衰减较小且基础音量也不大"：10 格约 0.69、16 格约 0.5、32 格衰减到 0；
 *    （SoundEngine 每 tick 都会重读实例的 volume 字段，tick() 内赋值即时生效）
 *  - tick() 内从客户端世界按实体 ID 取回实体并同步 x/y/z，实现音源跟随追击者移动；
 *  - 实体消失/死亡（含卸载）时 tick() 内主动 stop()，作为服务端停止包之外的兜底。
 */
public class EvilGajinRwrSound extends AbstractTickableSoundInstance {

    private final int entityId;

    public EvilGajinRwrSound(SoundEvent event, int entityId) {
        super(event, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.entityId = entityId;
        this.looping = true;                      // 循环播放（追击期间持续）
        this.volume = 1.0F;                       // 基础音量（tick() 内按距离实时覆盖）
        this.attenuation = SoundInstance.Attenuation.NONE;  // 衰减交给 tick() 手动控制
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            this.stop();
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        // 实体不存在/已死亡（含卸载）→ 停止循环音（父类 stop() 置 stopped，SoundEngine 随后移除）
        if (entity == null || !entity.isAlive()) {
            this.stop();
            return;
        }
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        // 手动平缓衰减（32 格，默认 16 格的一半斜率）：距离越远越小，但远处仍可闻、近处不爆响
        double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
        this.volume = (float) Math.max(0.0D, 1.0D - dist / 32.0D);
    }

    /** 公开停止入口：父类 stop() 为 protected，供管理器（EvilGajinRwrClientHandler）调用。 */
    public void requestStop() {
        this.stop();
    }
}
