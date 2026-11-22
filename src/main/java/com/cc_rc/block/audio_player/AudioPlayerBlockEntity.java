package com.cc_rc.block.audio_player;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 音频播放器方块实体：保存「音效 id + 模式 + 范围/衰减/起始/截至/间隔」。
 *
 * 播放本身完全在客户端完成（由 AudioPlayerSoundHandler 驱动），服务端只负责：
 *  - 持久化配置（NBT）；
 *  - 把配置同步给客户端（getUpdateTag / getUpdatePacket）；
 *  - 校验界面提交的 ConfigPacket 并写回（见 AudioPlayerConfigPacket）。
 */
public class AudioPlayerBlockEntity extends BlockEntity {

    // 四种工作模式（与界面下拉/循环按钮一一对应）
    /** 触发：有红石信号时持续播放。 */
    public static final int MODE_TRIGGER = 0;
    /** 反触发：无红石信号时持续播放。 */
    public static final int MODE_INVERSE = 1;
    /** 脉冲：红石由无变有时播放一次。 */
    public static final int MODE_PULSE = 2;
    /** 无红石：忽略红石，始终持续播放。 */
    public static final int MODE_ALWAYS = 3;

    // 各参数的允许范围（界面与服务端共用，统一在此夹取）
    public static final float MIN_RANGE = 1.0F;
    public static final float MAX_RANGE = 64.0F;
    public static final int MIN_ATTENUATION = 0;
    public static final int MAX_ATTENUATION = 100;
    public static final float MIN_START = 0.0F;
    public static final float MAX_START = 60.0F;
    public static final float MIN_STOP = 0.0F;
    public static final float MAX_STOP = 3600.0F;
    public static final float MIN_INTERVAL = -60.0F;
    public static final float MAX_INTERVAL = 3600.0F;
    /** 音量百分比：0 = 静音，100 = 原始音量，可超过 100（上限 500，即 5 倍）。 */
    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 500;
    public static final int DEFAULT_VOLUME = 100;
    /** 音效 id 最大长度（网络包与 NBT 共用）。 */
    public static final int MAX_SOUND_ID_LENGTH = 128;

    /** 当前音效 id（完整命名空间形式，如 cc_rc:music_level5）。 */
    private String soundId = "";
    private int mode = MODE_TRIGGER;
    private float range = 16.0F;
    /** 距离衰减百分比：0 = 范围内等音量，100 = 到范围边缘线性降到 0。 */
    private int attenuation = MAX_ATTENUATION;
    /** 起始点：开始播放前的延迟（秒）。 */
    private float startSeconds;
    /** 截至点：开始播放后持续多少秒停止（0 = 整首）。 */
    private float stopSeconds;
    /** 播放间隔（秒，可为负）：下一次开始 = 本次开始 + 播放长度 + 间隔。 */
    private float intervalSeconds;
    /** 音量百分比（0~500，默认 100）；超过 100 由客户端用多实例叠加实现。 */
    private int volumePercent = DEFAULT_VOLUME;

    public AudioPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUDIO_PLAYER_BE.get(), pos, state);
    }

    /**
     * 配置快照：客户端调度器用 equals 判断「界面是否改过配置」，
     * 一旦变化就停止旧实例并按新配置重新排期。
     */
    public record Settings(String soundId, int mode, float range, int attenuation,
                           float start, float stop, float interval, int volume) {
    }

    public Settings getSettings() {
        return new Settings(this.soundId, this.mode, this.range, this.attenuation,
                this.startSeconds, this.stopSeconds, this.intervalSeconds, this.volumePercent);
    }

    /** 规范化音效 id：去空白；缺命名空间时补 cc_rc:；超长截断。空串保持空串。 */
    public static String normalizeSoundId(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (!value.contains(":")) {
            value = "cc_rc:" + value;
        }
        if (value.length() > MAX_SOUND_ID_LENGTH) {
            value = value.substring(0, MAX_SOUND_ID_LENGTH);
        }
        return value;
    }

    /** 写入全部配置（越界值统一夹取），并标记存档。 */
    public void setConfig(String soundId, int mode, float range, int attenuation,
                          float start, float stop, float interval, int volume) {
        this.soundId = normalizeSoundId(soundId);
        this.mode = Mth.clamp(mode, MODE_TRIGGER, MODE_ALWAYS);
        this.range = Mth.clamp(range, MIN_RANGE, MAX_RANGE);
        this.attenuation = Mth.clamp(attenuation, MIN_ATTENUATION, MAX_ATTENUATION);
        this.startSeconds = Mth.clamp(start, MIN_START, MAX_START);
        this.stopSeconds = Mth.clamp(stop, MIN_STOP, MAX_STOP);
        this.intervalSeconds = Mth.clamp(interval, MIN_INTERVAL, MAX_INTERVAL);
        this.volumePercent = Mth.clamp(volume, MIN_VOLUME, MAX_VOLUME);
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeConfig(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readConfig(tag);
    }

    /** 客户端同步用：随方块更新下发的完整配置。 */
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void writeConfig(CompoundTag tag) {
        tag.putString("Sound", this.soundId);
        tag.putInt("Mode", this.mode);
        tag.putFloat("Range", this.range);
        tag.putInt("Atten", this.attenuation);
        tag.putFloat("Start", this.startSeconds);
        tag.putFloat("Stop", this.stopSeconds);
        tag.putFloat("Interval", this.intervalSeconds);
        tag.putInt("Volume", this.volumePercent);
    }

    private void readConfig(CompoundTag tag) {
        this.soundId = normalizeSoundId(tag.getString("Sound"));
        this.mode = Mth.clamp(tag.getInt("Mode"), MODE_TRIGGER, MODE_ALWAYS);
        this.range = Mth.clamp(tag.contains("Range") ? tag.getFloat("Range") : 16.0F,
                MIN_RANGE, MAX_RANGE);
        this.attenuation = Mth.clamp(tag.contains("Atten") ? tag.getInt("Atten") : MAX_ATTENUATION,
                MIN_ATTENUATION, MAX_ATTENUATION);
        this.startSeconds = Mth.clamp(tag.getFloat("Start"), MIN_START, MAX_START);
        this.stopSeconds = Mth.clamp(tag.getFloat("Stop"), MIN_STOP, MAX_STOP);
        this.intervalSeconds = Mth.clamp(tag.getFloat("Interval"), MIN_INTERVAL, MAX_INTERVAL);
        // 旧存档没有 Volume 键：用默认 100%
        this.volumePercent = Mth.clamp(tag.contains("Volume") ? tag.getInt("Volume") : DEFAULT_VOLUME,
                MIN_VOLUME, MAX_VOLUME);
    }
}
