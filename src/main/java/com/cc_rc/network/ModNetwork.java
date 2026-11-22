package com.cc_rc.network;

import com.cc_rc.CcRc;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 简单网络通道（C2S：左键投掷包子）。
 * 客户端在攻击键事件中发送 ThrowBaoZiPacket，服务端据此生成包子弹射物。
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CcRc.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, ThrowBaoZiPacket.class,
                ThrowBaoZiPacket::encode,
                ThrowBaoZiPacket::decode,
                ThrowBaoZiPacket::handle);
        // 破解音效开始/停止（S2C，服务端通知客户端播放/停止密码输入器破解音效）
        CHANNEL.registerMessage(1, CrackSoundPacket.class,
                CrackSoundPacket::encode,
                CrackSoundPacket::decode,
                CrackSoundPacket::handle);
        // 编辑工具提交方块显示文字（C2S，客户端把输入框文本写入目标 ITextDisplay 方块）
        CHANNEL.registerMessage(2, EditTextPacket.class,
                EditTextPacket::encode,
                EditTextPacket::decode,
                EditTextPacket::handle);
        // 邪恶盖金追击循环音开始/停止（S2C，服务端在锁定的玩家目标改变时通知客户端播放/停止循环音）
        CHANNEL.registerMessage(3, EvilGajinRwrPacket.class,
                EvilGajinRwrPacket::encode,
                EvilGajinRwrPacket::decode,
                EvilGajinRwrPacket::handle);
        // 「进攻D点」语音客户端播放（S2C，服务端在锁定玩家/周期打断时通知客户端无衰减播放一次）
        CHANNEL.registerMessage(4, EvilGajinLockPacket.class,
                EvilGajinLockPacket::encode,
                EvilGajinLockPacket::decode,
                EvilGajinLockPacket::handle);
        // 音频播放指令（S2C，/ccrc sound 定向发送给目标玩家，客户端无衰减播放指定声音）
        CHANNEL.registerMessage(5, PlaySoundPacket.class,
                PlaySoundPacket::encode,
                PlaySoundPacket::decode,
                PlaySoundPacket::handle);
        // 海报设置提交（C2S，客户端把图片索引/缩放/偏移写回海报方块实体）
        CHANNEL.registerMessage(6, PosterConfigPacket.class,
                PosterConfigPacket::encode,
                PosterConfigPacket::decode,
                PosterConfigPacket::handle);
        // 控制台总线操作（C2S，客户端请求成型 / 锁定 / 解锁）
        CHANNEL.registerMessage(7, ConsoleStructurePacket.class,
                ConsoleStructurePacket::encode,
                ConsoleStructurePacket::decode,
                ConsoleStructurePacket::handle);
        // 控制台表面文字：请求打开编辑界面（C2S，客户端先做命中检测再请服务端开界面）
        CHANNEL.registerMessage(8, ConsoleTextRequestPacket.class,
                ConsoleTextRequestPacket::encode,
                ConsoleTextRequestPacket::decode,
                ConsoleTextRequestPacket::handle);
        // 控制台表面文字：提交内容与样式（C2S，写入总线方块实体）
        CHANNEL.registerMessage(9, ConsoleTextPacket.class,
                ConsoleTextPacket::encode,
                ConsoleTextPacket::decode,
                ConsoleTextPacket::handle);
        // 控制台表面组件：请求打开设置界面（C2S）
        CHANNEL.registerMessage(10, ConsoleCompRequestPacket.class,
                ConsoleCompRequestPacket::encode,
                ConsoleCompRequestPacket::decode,
                ConsoleCompRequestPacket::handle);
        // 控制台表面组件：提交代号/位置或删除（C2S）
        CHANNEL.registerMessage(11, ConsoleCompPacket.class,
                ConsoleCompPacket::encode,
                ConsoleCompPacket::decode,
                ConsoleCompPacket::handle);
        // 控制台组件附带文字：请求打开普通文字编辑界面（C2S）
        CHANNEL.registerMessage(12, ConsoleCompTextRequestPacket.class,
                ConsoleCompTextRequestPacket::encode,
                ConsoleCompTextRequestPacket::decode,
                ConsoleCompTextRequestPacket::handle);
        // 控制台组件附带文字：提交内容与样式（C2S，写入总线方块实体）
        CHANNEL.registerMessage(13, ConsoleCompTextPacket.class,
                ConsoleCompTextPacket::encode,
                ConsoleCompTextPacket::decode,
                ConsoleCompTextPacket::handle);
        // 控制台表面文字：拖动（只改锚点位置，C2S）
        CHANNEL.registerMessage(14, ConsoleTextMovePacket.class,
                ConsoleTextMovePacket::encode,
                ConsoleTextMovePacket::decode,
                ConsoleTextMovePacket::handle);
        // 控制柜变量表：创建 / 删除 / 保存数值（C2S）
        CHANNEL.registerMessage(15, ConsoleVarPacket.class,
                ConsoleVarPacket::encode,
                ConsoleVarPacket::decode,
                ConsoleVarPacket::handle);
        // 控制柜程序表：保存整张程序（C2S）
        CHANNEL.registerMessage(16, ConsoleProgramPacket.class,
                ConsoleProgramPacket::encode,
                ConsoleProgramPacket::decode,
                ConsoleProgramPacket::handle);
        // 控制台按钮：弹起时间为 0 时松手释放（C2S）
        CHANNEL.registerMessage(17, ConsoleKeypadReleasePacket.class,
                ConsoleKeypadReleasePacket::encode,
                ConsoleKeypadReleasePacket::decode,
                ConsoleKeypadReleasePacket::handle);
        // 控制柜配置：客户端上传本地 JSON 加载到总线（C2S）
        CHANNEL.registerMessage(18, ConsoleConfigUploadPacket.class,
                ConsoleConfigUploadPacket::encode,
                ConsoleConfigUploadPacket::decode,
                ConsoleConfigUploadPacket::handle);
        // 控制柜配置：服务端回执加载结果（S2C）
        CHANNEL.registerMessage(19, ConsoleConfigResultPacket.class,
                ConsoleConfigResultPacket::encode,
                ConsoleConfigResultPacket::decode,
                ConsoleConfigResultPacket::handle);
        // 控制柜表面绘图：修改 / 移动 / 删除 / 新建图案（C2S）
        CHANNEL.registerMessage(21, ConsoleDrawPacket.class,
                ConsoleDrawPacket::encode,
                ConsoleDrawPacket::decode,
                ConsoleDrawPacket::handle);
        // 控制柜表面绘图：新建图案的回执（S2C，携带新图形 id 供属性界面继续编辑）
        CHANNEL.registerMessage(22, ConsoleDrawCreatedPacket.class,
                ConsoleDrawCreatedPacket::encode,
                ConsoleDrawCreatedPacket::decode,
                ConsoleDrawCreatedPacket::handle);
        // 画图工具：保存默认设置（颜色 / 线宽 / 缩放 / 图层 / 类型 / 快速绘画开关等，C2S）
        CHANNEL.registerMessage(23, DrawToolSettingsPacket.class,
                DrawToolSettingsPacket::encode,
                DrawToolSettingsPacket::decode,
                DrawToolSettingsPacket::handle);
        // 数字键盘：提交输入界面确认的数值（C2S）
        CHANNEL.registerMessage(24, ConsoleNumpadSetPacket.class,
                ConsoleNumpadSetPacket::encode,
                ConsoleNumpadSetPacket::decode,
                ConsoleNumpadSetPacket::handle);
        // 涂鸦文字设置提交（C2S，客户端把文字/颜色/大小/偏移/字体格式写回涂鸦方块实体）
        CHANNEL.registerMessage(25, GraffitiConfigPacket.class,
                GraffitiConfigPacket::encode,
                GraffitiConfigPacket::decode,
                GraffitiConfigPacket::handle);
        // 涂鸦形状设置提交（C2S，客户端把类型/预设/颜色/大小/线宽/旋转/偏移写回涂鸦形状方块实体）
        CHANNEL.registerMessage(26, GraffitiShapeConfigPacket.class,
                GraffitiShapeConfigPacket::encode,
                GraffitiShapeConfigPacket::decode,
                GraffitiShapeConfigPacket::handle);
        // 便签文字提交（C2S，客户端把四行文字 / 颜色 / 字体格式写回便签实体）
        CHANNEL.registerMessage(27, NoteConfigPacket.class,
                NoteConfigPacket::encode,
                NoteConfigPacket::decode,
                NoteConfigPacket::handle);
        // 音频播放器设置提交（C2S，客户端把音效/模式/范围/衰减/起始/截至/间隔写回方块实体）
        CHANNEL.registerMessage(28, AudioPlayerConfigPacket.class,
                AudioPlayerConfigPacket::encode,
                AudioPlayerConfigPacket::decode,
                AudioPlayerConfigPacket::handle);
    }
}
