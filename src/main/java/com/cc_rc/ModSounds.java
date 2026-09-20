package com.cc_rc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CcRc.MODID);

    public static final RegistryObject<SoundEvent> MUSIC_LEVEL5 = register("music_level5");
    public static final RegistryObject<SoundEvent> MUSIC_RAILUGUN = register("music_railugun");
    public static final RegistryObject<SoundEvent> MUSIC_NEVER = register("music_never");
    public static final RegistryObject<SoundEvent> MUSIC_ASSUMPTIONS = register("music_assumptions");
    public static final RegistryObject<SoundEvent> MUSIC_CONRNFIELD_CHASE = register("music_conrnfield_chase");
    public static final RegistryObject<SoundEvent> MUSIC_MOVE = register("music_move");
    public static final RegistryObject<SoundEvent> MUSIC_NIGHT = register("music_night");
    public static final RegistryObject<SoundEvent> MUSIC_RAIN = register("music_rain");
    public static final RegistryObject<SoundEvent> MUSIC_END = register("music_end");
    public static final RegistryObject<SoundEvent> MUSIC_UNDERGROUND_RIVER = register("music_underground_river");
    public static final RegistryObject<SoundEvent> MUSIC_HANEZEVE_CARADHINA = register("music_hanezeve_caradhina");
    // 新增 9 张唱片音乐
    public static final RegistryObject<SoundEvent> MUSIC_CUTIE_MEW_MEW_MAGIC = register("music_cutie_mew_mew_magic");
    public static final RegistryObject<SoundEvent> MUSIC_DENISE = register("music_denise");
    public static final RegistryObject<SoundEvent> MUSIC_GWANGJU = register("music_gwangju");
    public static final RegistryObject<SoundEvent> MUSIC_HIGHER = register("music_higher");
    public static final RegistryObject<SoundEvent> MUSIC_KING = register("music_king");
    public static final RegistryObject<SoundEvent> MUSIC_MARISA = register("music_marisa");
    public static final RegistryObject<SoundEvent> MUSIC_MIXUE = register("music_mixue");
    public static final RegistryObject<SoundEvent> MUSIC_RAW_TELL = register("music_raw_tell");
    public static final RegistryObject<SoundEvent> MUSIC_REIMU = register("music_reimu");
    public static final RegistryObject<SoundEvent> MUSIC_YOU_WILL_BE_PERFECT = register("music_you_will_be_perfect");
    // 新增 3 张唱片音乐
    public static final RegistryObject<SoundEvent> MUSIC_BLOOM = register("music_bloom");
    public static final RegistryObject<SoundEvent> MUSIC_JIGOKU_SHOUJO = register("music_jigoku_shoujo");
    public static final RegistryObject<SoundEvent> MUSIC_THE_IMITATION_GAME = register("music_the_imitation_game");
    // 新增 6 张唱片音乐（GitHub issue #1）
    public static final RegistryObject<SoundEvent> MUSIC_BIT = register("music_bit");
    public static final RegistryObject<SoundEvent> MUSIC_BROKEN_BOY = register("music_broken_boy");
    public static final RegistryObject<SoundEvent> MUSIC_PANIC_TRACK = register("music_panic_track");
    public static final RegistryObject<SoundEvent> MUSIC_RESONANCE = register("music_resonance");
    public static final RegistryObject<SoundEvent> MUSIC_ROLLER_MOBSTER = register("music_roller_mobster");
    public static final RegistryObject<SoundEvent> MUSIC_SABOTAGE = register("music_sabotage");
    // 新增 2 张唱片音乐
    public static final RegistryObject<SoundEvent> MUSIC_FRIENDS_WINE = register("music_friends_wine");
    public static final RegistryObject<SoundEvent> MUSIC_AIR = register("music_air");
    // 新增唱片「LEVEL !」：感叹号不进入注册名，key=level
    public static final RegistryObject<SoundEvent> MUSIC_LEVEL = register("music_level");
    // 邪恶盖金（evil_gajin）语音音效：
    //  - evil_gajin_lock：锁定目标瞬间播放一次「进攻D点」
    //  - evil_gajin_rwr：追击时循环播放的警示音（音效长约 3 分钟，客户端 SoundEngine 循环实例）
    //  - evil_gajin_good / evil_gajin_hit：攻击时随机二选一（「干得好」/「命中」）
    //  - evil_gajin_destroy：击杀目标时播放「摧毁目标」
    //  - evil_gajin_bad：受到攻击时播放「局势不太妙」（带冷却防刷屏）
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_LOCK = register("evil_gajin_lock");
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_RWR = register("evil_gajin_rwr");
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_GOOD = register("evil_gajin_good");
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_HIT = register("evil_gajin_hit");
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_DESTROY = register("evil_gajin_destroy");
    public static final RegistryObject<SoundEvent> EVIL_GAJIN_BAD = register("evil_gajin_bad");
    // 奶龙玩偶声音（非唱片）
    public static final RegistryObject<SoundEvent> NAI_LONG = register("nai_long");
    // F.A.A.S服务器环境音效（非唱片，靠近时持续播放）
    public static final RegistryObject<SoundEvent> SERVER_NOISE = register("server_noise");
    // 破解器破解音效（破解密码输入器时循环播放，中断/成功时停止）
    public static final RegistryObject<SoundEvent> PASSWORD_CRACK = register("password_crack");
    // 盖金蜗牛音效（玩家右键蜗牛播放；平常无 ambient 叫声）
    public static final RegistryObject<SoundEvent> GAJIN = register("gajin");
    // 反应堆启动音乐（非唱片，仅指令播放）：
    //  - reactor_start：仅前 56s 且末尾 4s 淡出（反应堆启动场景，淡出算在 56s 内）
    //  - reactor_start_full：完整全曲（仅指令播放）
    public static final RegistryObject<SoundEvent> REACTOR_START = register("reactor_start");
    public static final RegistryObject<SoundEvent> REACTOR_START_FULL = register("reactor_start_full");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CcRc.MODID, name)));
    }
}