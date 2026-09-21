package com.cc_rc.client;

import com.cc_rc.ModSounds;
import com.cc_rc.network.EvilGajinRwrPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 邪恶盖金追击循环音（RWR）客户端管理器：
 * 接收 EvilGajinRwrPacket 后在对应实体上创建/停止 EvilGajinRwrSound 循环实例。
 * 实体 ID → 正在播放的循环音实例映射，同一实体不会重复创建实例。
 */
public class EvilGajinRwrClientHandler {

    /** 正在播放的追击循环音（按邪恶盖金实体 ID 索引） */
    private static final Map<Integer, EvilGajinRwrSound> ACTIVE = new HashMap<>();

    /** 开始：为指定邪恶盖金实体创建并播放循环音（已存在则忽略，防止重复叠加）。 */
    public static void startRwr(int entityId) {
        if (ACTIVE.containsKey(entityId)) {
            return;
        }
        EvilGajinRwrSound sound = new EvilGajinRwrSound(ModSounds.EVIL_GAJIN_RWR.get(), entityId);
        Minecraft.getInstance().getSoundManager().play(sound);
        ACTIVE.put(entityId, sound);
    }

    /** 停止：停止并移除指定实体的循环音（不存在则忽略）。 */
    public static void stopRwr(int entityId) {
        EvilGajinRwrSound sound = ACTIVE.remove(entityId);
        if (sound != null) {
            sound.requestStop();
        }
    }

    /** 从 EvilGajinRwrPacket 取包并执行（由客户端主线程通过方法引用调用，服务端不会加载本类）。 */
    public static void dispatchPending() {
        EvilGajinRwrPacket msg = EvilGajinRwrPacket.consumePending();
        if (msg == null) {
            return;
        }
        if (msg.isStart()) {
            startRwr(msg.getEntityId());
        } else {
            stopRwr(msg.getEntityId());
        }
    }
}