package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.block.audio_player.AudioPlayerBlock;
import com.cc_rc.block.audio_player.AudioPlayerBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 音频播放器客户端调度器（仅 Dist.CLIENT 加载）。
 *
 * 工作方式（参考 FAAS 服务器循环音效）：
 *  - 每 20 tick 按区块扫描玩家周围已加载区块中的 AudioPlayerBlockEntity，登记为 Track；
 *  - 每 tick 对每个 Track 读取配置与 POWERED 方块状态，按模式决定是否播放：
 *      触发 = 有红石、反触发 = 无红石、脉冲 = 上升沿播一次、无红石 = 常开；
 *  - 播放间隔：下一次开始 = 本次开始 + 播放长度 + 间隔（负间隔即重叠）；
 *  - 每个方块最多同时 64 个实例，实例超出范围/到截至点自动停止。
 *
 * 音量不交给引擎衰减：attenuation=NONE，每 tick 按玩家距离手动计算
 * volume = clamp(1 - dist/range * 衰减百分比, 0, 1) × 音量百分比。
 * 原版 SoundEngine 会把单个实例的音量夹在 0~1，故超过 100% 的部分
 * 用「同位置多实例叠加」实现（每份音量 ≤1，混音时振幅相加）。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AudioPlayerSoundHandler {

    /** 扫描间隔（tick）。 */
    private static final int SCAN_INTERVAL = 20;
    /** 扫描区块半径：5 区块 = 80 格，覆盖最大范围 64 格。 */
    private static final int SCAN_CHUNK_RADIUS = 5;
    /** 单方块最大并发实例数（负间隔重叠 / 高音量多份叠加时的上限）。 */
    private static final int MAX_INSTANCES = 64;
    /** 音量超过 100% 时最多拆成几份实例（500% = 5 份）。 */
    private static final int MAX_VOLUME_COPIES = 5;
    /** 无法解析曲长时的兜底长度（tick，10 秒）。 */
    private static final int FALLBACK_TICKS = 200;
    /** 超出该距离（格）的方块直接停止并取消登记。 */
    private static final double DROP_DISTANCE = 80.0D;

    private static final Map<BlockPos, Track> TRACKS = new HashMap<>();

    private static int tickCounter;
    private static ClientLevel lastLevel;

    /** 单个方块的播放状态。 */
    private static final class Track {
        AudioPlayerBlockEntity.Settings settings;
        boolean prevPowered;
        boolean active;
        /** 下一次开始播放的游戏时间（Long.MAX_VALUE = 不排期）。 */
        long nextStartTick = Long.MAX_VALUE;
        boolean pulsePending;
        final List<AudioPlayerSoundInstance> instances = new ArrayList<>();

        Track(AudioPlayerBlockEntity.Settings settings, boolean powered) {
            this.settings = settings;
            this.prevPowered = powered;
        }

        /** 停止全部实例并清空列表。 */
        void stopAll() {
            for (AudioPlayerSoundInstance instance : this.instances) {
                instance.stopNow();
            }
            this.instances.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            stopEverything();
            return;
        }
        if (level != lastLevel) {
            stopEverything();
            lastLevel = level;
        }

        // 定期扫描：发现新方块（区块加载 / 玩家移动后进入范围）
        if (++tickCounter % SCAN_INTERVAL == 0) {
            scan(level, mc.player);
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Track>> iterator = TRACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Track> entry = iterator.next();
            if (!tickTrack(level, mc.player, entry.getKey(), entry.getValue(), now)) {
                iterator.remove();
            }
        }
    }

    /** 扫描玩家周围区块内的音频播放器方块，登记尚未跟踪的方块。 */
    private static void scan(ClientLevel level, Player player) {
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        for (int dx = -SCAN_CHUNK_RADIUS; dx <= SCAN_CHUNK_RADIUS; dx++) {
            for (int dz = -SCAN_CHUNK_RADIUS; dz <= SCAN_CHUNK_RADIUS; dz++) {
                ChunkAccess chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }
                for (BlockEntity be : levelChunk.getBlockEntities().values()) {
                    if (be instanceof AudioPlayerBlockEntity audio) {
                        BlockPos pos = be.getBlockPos();
                        TRACKS.computeIfAbsent(pos, p -> new Track(
                                audio.getSettings(), isPowered(audio)));
                    }
                }
            }
        }
    }

    /**
     * 更新单个方块：返回 false 表示该方块已失效（应取消登记）。
     */
    private static boolean tickTrack(ClientLevel level, Player player, BlockPos pos,
                                     Track track, long now) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AudioPlayerBlockEntity audio) || be.isRemoved()) {
            track.stopAll();
            return false;
        }
        // 玩家远离到扫描覆盖以外：停止并取消登记（回到范围后会重新扫描登记）
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                > DROP_DISTANCE * DROP_DISTANCE) {
            track.stopAll();
            return false;
        }

        // 配置变化（界面保存 / 方块更新）：停止旧实例，按新配置重新排期
        AudioPlayerBlockEntity.Settings settings = audio.getSettings();
        if (!settings.equals(track.settings)) {
            track.stopAll();
            track.settings = settings;
            track.active = false;
            track.pulsePending = false;
            track.nextStartTick = Long.MAX_VALUE;
        }

        boolean powered = isPowered(audio);
        int mode = settings.mode();
        int startTicks = toTicks(settings.start());

        if (mode == AudioPlayerBlockEntity.MODE_PULSE) {
            // 脉冲：只在红石由无变有时排期一次
            if (!track.prevPowered && powered) {
                track.pulsePending = true;
                track.nextStartTick = now + startTicks;
            }
            if (track.pulsePending && now >= track.nextStartTick) {
                spawn(level, pos, track, settings);
                track.pulsePending = false;
                track.nextStartTick = Long.MAX_VALUE;
            }
        } else {
            boolean shouldPlay = switch (mode) {
                case AudioPlayerBlockEntity.MODE_TRIGGER -> powered;
                case AudioPlayerBlockEntity.MODE_INVERSE -> !powered;
                default -> true;
            };
            if (shouldPlay && !track.active) {
                // 条件刚满足：从「现在 + 起始延迟」开始排期
                track.nextStartTick = now + startTicks;
            } else if (!shouldPlay && track.active) {
                // 条件失效：立即停止全部实例
                track.stopAll();
                track.nextStartTick = Long.MAX_VALUE;
            }
            track.active = shouldPlay;

            if (shouldPlay && now >= track.nextStartTick) {
                spawn(level, pos, track, settings);
                int length = playbackLength(settings);
                // 下一次开始 = 本次开始 + 播放长度 + 间隔（负间隔产生重叠）
                track.nextStartTick = now + Math.max(1, length + toTicks(settings.interval()));
            }
        }

        track.prevPowered = powered;
        // 清理已自行停止的实例，避免占满并发上限
        track.instances.removeIf(AudioPlayerSoundInstance::isDone);
        return true;
    }

    /** 播放长度（tick）：截至点>0 时取 min(截至点, 曲长)，否则整首。 */
    private static int playbackLength(AudioPlayerBlockEntity.Settings settings) {
        int duration = durationTicks(settings.soundId());
        int stopTicks = toTicks(settings.stop());
        if (stopTicks > 0) {
            return Math.min(stopTicks, duration);
        }
        return duration;
    }

    /** 曲长（tick）：解析失败按兜底长度。 */
    private static int durationTicks(String soundId) {
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        int ticks = id == null ? -1 : SoundDurations.getTicks(id);
        return ticks > 0 ? ticks : FALLBACK_TICKS;
    }

    /** 创建一个播放实例（受并发上限约束）。 */
    private static void spawn(ClientLevel level, BlockPos pos, Track track,
                              AudioPlayerBlockEntity.Settings settings) {
        ResourceLocation id = ResourceLocation.tryParse(settings.soundId());
        if (id == null) {
            return;
        }
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (event == null) {
            return;
        }
        // 音量倍数（0~5）；原版引擎单实例音量上限为 1，
        // 超过 1 的部分拆成多份同时播放（例如 250% = 1.0 + 1.0 + 0.5）
        float volume = settings.volume() / 100.0F;
        if (volume <= 0.0F) {
            return;
        }
        int copies = Math.min(MAX_VOLUME_COPIES, (int) Math.ceil(volume));
        int maxTicks = playbackLength(settings);
        for (int i = 0; i < copies; i++) {
            float part = Math.min(1.0F, volume - i);
            if (part <= 0.0F || track.instances.size() >= MAX_INSTANCES) {
                break;
            }
            AudioPlayerSoundInstance instance = new AudioPlayerSoundInstance(level, event, pos,
                    Math.max(1.0F, settings.range()), settings.attenuation(), maxTicks, part);
            track.instances.add(instance);
            Minecraft.getInstance().getSoundManager().play(instance);
        }
    }

    private static boolean isPowered(AudioPlayerBlockEntity be) {
        return be.getBlockState().getValue(AudioPlayerBlock.POWERED);
    }

    private static int toTicks(float seconds) {
        return Math.round(seconds * 20.0F);
    }

    private static void stopEverything() {
        for (Track track : TRACKS.values()) {
            track.stopAll();
        }
        TRACKS.clear();
    }

    /**
     * 单个播放实例：固定在方块中心，关闭引擎衰减，每 tick 手动算音量。
     *
     * looping=true 的目的：让 SoundEngine 持续回调 tick()，由我们自己按
     * 「截至点 / 曲长」停止（原版 SoundInstance 没有时长回调，也没有 isStopped 查询）。
     */
    private static final class AudioPlayerSoundInstance extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final BlockPos pos;
        private final float range;
        /** 衰减百分比（0~100）。命名避开父类的 attenuation 枚举字段。 */
        private final float attenuationPercent;
        /** 本份实例承担的音量倍数（0~1，多份叠加得到界面上的总音量）。 */
        private final float volumeMul;
        private final int maxTicks;
        private int elapsed;
        private boolean done;

        AudioPlayerSoundInstance(ClientLevel level, SoundEvent event, BlockPos pos,
                                 float range, int attenuationPercent, int maxTicks,
                                 float volumeMul) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.pos = pos;
            this.range = range;
            this.attenuationPercent = attenuationPercent;
            this.volumeMul = volumeMul;
            this.maxTicks = Math.max(1, maxTicks);
            this.looping = true;
            this.delay = 0;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.volume = computeVolume();
        }

        @Override
        public void tick() {
            if (this.done) {
                return;
            }
            this.elapsed++;
            if (!(this.level.getBlockEntity(this.pos) instanceof AudioPlayerBlockEntity)) {
                stopNow();
                return;
            }
            this.volume = computeVolume();
            if (this.elapsed >= this.maxTicks || distanceSq() > (double) this.range * this.range) {
                stopNow();
            }
        }

        /** 按距离与衰减百分比计算音量：0% 衰减 = 范围内等音量；再乘本份音量倍数。 */
        private float computeVolume() {
            double dist = Math.sqrt(distanceSq());
            float fade = 1.0F - (float) (dist / this.range) * (this.attenuationPercent / 100.0F);
            return Mth.clamp(fade, 0.0F, 1.0F) * this.volumeMul;
        }

        private double distanceSq() {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return 0.0D;
            }
            return player.distanceToSqr(this.x, this.y, this.z);
        }

        boolean isDone() {
            return this.done;
        }

        /** 外部/内部停止入口（stop() 为 protected，包一层公开方法）。 */
        void stopNow() {
            this.done = true;
            this.stop();
        }
    }
}
