package com.cc_rc.block.password_inputer;

import com.cc_rc.CcRc;
import com.cc_rc.Config;
import com.cc_rc.item.PasswordCrackerItem;
import com.cc_rc.network.CrackSoundPacket;
import com.cc_rc.network.ModNetwork;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 密码输入器破解流程管理器（仅服务端逻辑）。
 *
 * 流程：玩家手持破解器右键密码输入器 → start() 登记破解会话并通知客户端播放破解音效；
 * 每服务端 tick 推进计时（remainingTicks--），同时校验三个中断条件：
 *   1. 玩家离开方块 5 格以上；
 *   2. 玩家主手不再是破解器（切换快捷栏/换手）；
 *   3. 目标方块被破坏/替换。
 * 任一条件不满足 → 会话取消（计时重置，需重新右键开始），同时通知客户端停止破解音效。
 * 计时走完 → 破解成功：调用 PasswordInputerBlock.powerOn() 使方块变 on 并输出红石，
 * 1 秒后由方块自动变回 off。所有提示均显示在 action bar（屏幕中间）。
 * 显示节奏：破解开始后前 20 tick（约 1 秒）显示"开始破解…"，之后显示"破解中…剩余 N 秒"
 * （N 从 19 倒数到 1，总时长仍为配置的 20 秒）。
 *
 * 注意：推进计时用 Map.Entry.setValue 而非 SESSIONS.put —— 后者会在迭代期间修改 HashMap
 * 结构计数，导致迭代器 remove() 抛 ConcurrentModificationException（曾致破解完成时服务器崩溃）。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PasswordCrackManager {

    // 玩家与密码输入器的最大有效距离（格），超出即中断
    public static final double MAX_DISTANCE = 5.0;

    // 开始字样在 action bar 保持的时长（tick），期间不显示倒计时
    private static final int START_HINT_TICKS = 20;

    /** 一条正在进行的破解会话：所在维度、目标方块坐标、剩余 tick、初始总 tick */
    private record CrackSession(ServerLevel level, BlockPos pos, int remainingTicks, int initialTicks) {}

    // 破解者 UUID -> 会话（并发安全：start()/onBlockRemoved() 可能与 tick 迭代交错触发）
    private static final Map<UUID, CrackSession> SESSIONS = new ConcurrentHashMap<>();

    private static int actionBarCounter = 0;

    /** 玩家右键密码输入器时调用：登记/重置破解会话、action bar 提示、通知客户端播放破解音效。 */
    public static void start(ServerPlayer player, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int total = Config.getPasswordCrackTicks();
        SESSIONS.put(player.getUUID(), new CrackSession(serverLevel, pos.immutable(), total, total));
        player.displayClientMessage(Component.literal("开始破解密码输入器…"), true);
        sendSound(player, pos, true);
    }

    /** 方块被破坏/替换时调用：取消指向该方块的全部破解会话并停止其破解音效。 */
    public static void onBlockRemoved(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Iterator<Map.Entry<UUID, CrackSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CrackSession> entry = it.next();
            CrackSession s = entry.getValue();
            if (s.level() == serverLevel && s.pos().equals(pos)) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
                it.remove();
                if (player != null) {
                    sendSound(player, pos, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null || SESSIONS.isEmpty()) return;

        boolean updateBar = ++actionBarCounter % 10 == 0;

        Iterator<Map.Entry<UUID, CrackSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CrackSession> entry = it.next();
            UUID playerId = entry.getKey();
            CrackSession s = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            // 中断：玩家掉线 / 已切换维度
            if (player == null || player.level() != s.level()) {
                it.remove();
                continue;
            }

            // 中断条件 1：距离超过 5 格
            double distSq = player.distanceToSqr(s.pos().getX() + 0.5, s.pos().getY() + 0.5, s.pos().getZ() + 0.5);
            boolean tooFar = distSq > MAX_DISTANCE * MAX_DISTANCE;

            // 中断条件 2：主手不再是破解器（切快捷栏/换手）
            boolean holdingCracker = player.getMainHandItem().getItem() instanceof PasswordCrackerItem;

            // 中断条件 3：目标方块已不是密码输入器
            boolean blockGone = !(s.level().getBlockState(s.pos()).getBlock() instanceof PasswordInputerBlock);

            if (tooFar || !holdingCracker || blockGone) {
                player.displayClientMessage(Component.literal("破解中断"), true);
                sendSound(player, s.pos(), false);
                it.remove();
                continue;
            }

            // 正常推进计时（setValue 不改变结构，迭代期间安全）
            int remaining = s.remainingTicks() - 1;
            if (remaining <= 0) {
                // 破解成功：方块切 on（1 秒后自动关），输出红石，停止破解音效
                PasswordInputerBlock block = (PasswordInputerBlock) s.level().getBlockState(s.pos()).getBlock();
                block.powerOn(s.level(), s.pos());
                player.displayClientMessage(Component.literal("破解成功！"), true);
                sendSound(player, s.pos(), false);
                it.remove();
                continue;
            }
            entry.setValue(new CrackSession(s.level(), s.pos(), remaining, s.initialTicks()));

            // action bar 每 10 tick 刷新：前 1 秒显示"开始破解"，之后显示剩余秒数（从 19 倒数）
            if (updateBar) {
                int elapsed = s.initialTicks() - remaining;
                if (elapsed <= START_HINT_TICKS) {
                    player.displayClientMessage(Component.literal("开始破解密码输入器…"), true);
                } else {
                    int seconds = (remaining + 19) / 20;
                    player.displayClientMessage(Component.literal("破解中…剩余 " + seconds + " 秒"), true);
                }
            }
        }
    }

    /** 通知指定玩家客户端开始/停止播放破解音效。 */
    private static void sendSound(ServerPlayer player, BlockPos pos, boolean start) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CrackSoundPacket(pos.immutable(), start));
    }

    private PasswordCrackManager() {
    }
}