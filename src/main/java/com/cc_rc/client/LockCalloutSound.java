package com.cc_rc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * 「进攻D点」（evil_gajin_lock）客户端一次性音效实例。
 *
 * 作用：服务端 playSound 播放的音效受 SoundEngine 默认 16 格衰减限制（16 格外音量直接归零），
 * 而锁定/追击发生时玩家与邪恶盖金常相距 16 格以上，导致完全听不见；本实例改为
 * **无距离衰减（Attenuation.NONE）+ 音量 1.0**，由服务端定向发送 EvilGajinLockPacket 给被追击玩家播放，
 * 保证任意距离都能清晰听到。
 *
 * 实现要点：
 *  - 继承 AbstractTickableSoundInstance；looping=false 一次性播放（音频约 0.9s，播完自动结束）；
 *  - tick() 内跟随邪恶盖金实体位置（音源贴实）；实体消失时 stop() 兜底。
 */
public class LockCalloutSound extends AbstractTickableSoundInstance {

    private final int entityId;

    public LockCalloutSound(SoundEvent event, int entityId) {
        super(event, SoundSource.HOSTILE, RandomSource.create());
        this.entityId = entityId;
        this.volume = 1.2F;                       // 音量 1.2（比默认稍大，用户要求语音增大）
        this.attenuation = SoundInstance.Attenuation.NONE;  // 无距离衰减：远处也清晰可闻
        this.looping = false;                     // 一次性：播完自动结束
        // 初始音源位置取实体当前位置（随后 tick 会持续跟随）
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity e = mc.level.getEntity(entityId);
            if (e != null) {
                this.x = e.getX();
                this.y = e.getY();
                this.z = e.getZ();
            }
        }
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            this.stop();
            return;
        }
        Entity e = mc.level.getEntity(entityId);
        // 实体不存在（含卸载/死亡）→ 停止（音频很短，正常情况下在实体还在时就播完了）
        if (e == null || !e.isAlive()) {
            this.stop();
            return;
        }
        this.x = e.getX();
        this.y = e.getY();
        this.z = e.getZ();
    }
}