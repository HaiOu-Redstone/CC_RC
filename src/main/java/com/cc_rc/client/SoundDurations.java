package com.cc_rc.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 客户端音效时长解析（仅 Dist.CLIENT 加载）。
 *
 * 原版 SoundInstance 不暴露时长，这里直接解析 OGG Vorbis 文件：
 *  - 扫描 vorbis 标识头（01 'vorbis'）取采样率（+12，4 字节小端）；
 *  - 从文件尾部向前找最后一个 OggS 页，取其 granule position（+6，8 字节小端）；
 *  - 时长（秒）= granule / 采样率，再换算成 tick。
 *
 * 结果按「音效事件 id」缓存（含解析失败 -1），避免重复读文件。
 */
public final class SoundDurations {

    private static final Map<ResourceLocation, Integer> CACHE = new HashMap<>();

    private SoundDurations() {
    }

    /**
     * 返回音效时长（tick）；无法解析（未注册 / 非 OGG / 读取失败）返回 -1。
     */
    public static int getTicks(ResourceLocation soundEventId) {
        if (soundEventId == null) {
            return -1;
        }
        return CACHE.computeIfAbsent(soundEventId, SoundDurations::readTicks);
    }

    private static int readTicks(ResourceLocation soundEventId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null || mc.getResourceManager() == null) {
            return -1;
        }
        // 先按音效事件解析到具体音频文件（sounds.json 可能带多个变体，取第一个）
        WeighedSoundEvents weighed = mc.getSoundManager().getSoundEvent(soundEventId);
        if (weighed == null) {
            return -1;
        }
        Sound sound = weighed.getSound(SoundInstance.createUnseededRandom());
        if (sound == null) {
            return -1;
        }
        ResourceLocation file = sound.getLocation();
        ResourceLocation ogg = new ResourceLocation(file.getNamespace(),
                "sounds/" + file.getPath() + ".ogg");
        Resource resource = mc.getResourceManager().getResource(ogg).orElse(null);
        if (resource == null) {
            return -1;
        }
        try (InputStream in = resource.open()) {
            byte[] data = in.readAllBytes();
            int rate = findSampleRate(data);
            long granule = findLastGranule(data);
            if (rate <= 0 || granule <= 0L) {
                return -1;
            }
            return (int) Math.max(1L, Math.round(granule / (double) rate * 20.0D));
        } catch (IOException e) {
            return -1;
        }
    }

    /** 扫描 Vorbis 标识头，返回采样率；找不到返回 -1。 */
    private static int findSampleRate(byte[] data) {
        for (int i = 0; i + 16 < data.length; i++) {
            if (data[i] == 1
                    && data[i + 1] == 'v' && data[i + 2] == 'o' && data[i + 3] == 'r'
                    && data[i + 4] == 'b' && data[i + 5] == 'i' && data[i + 6] == 's') {
                return (data[i + 12] & 0xFF)
                        | ((data[i + 13] & 0xFF) << 8)
                        | ((data[i + 14] & 0xFF) << 16)
                        | ((data[i + 15] & 0xFF) << 24);
            }
        }
        return -1;
    }

    /** 从尾部向前找最后一个 OggS 页，读 granule position；找不到返回 -1。 */
    private static long findLastGranule(byte[] data) {
        for (int i = data.length - 14; i >= 0; i--) {
            if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') {
                long granule = 0L;
                for (int b = 7; b >= 0; b--) {
                    granule = (granule << 8) | (data[i + 6 + b] & 0xFFL);
                }
                return granule;
            }
        }
        return -1L;
    }
}
