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

    // ===== 麦卡锡音频素材（96 条）：命名 = 英文类别_英文名 =====
    public static final RegistryObject<SoundEvent> MECHANICAL_AZ5_TRIGGER = register("mechanical_az5_trigger");
    public static final RegistryObject<SoundEvent> MECHANICAL_DOSIMETER_USE = register("mechanical_dosimeter_use");
    public static final RegistryObject<SoundEvent> MECHANICAL_SWITCH = register("mechanical_switch");
    public static final RegistryObject<SoundEvent> MECHANICAL_VALVE_TURN = register("mechanical_valve_turn");
    public static final RegistryObject<SoundEvent> MECHANICAL_PIZZA_TAKE = register("mechanical_pizza_take");
    public static final RegistryObject<SoundEvent> MECHANICAL_CONTROL_ROOM_BATTERY = register("mechanical_control_room_battery");
    public static final RegistryObject<SoundEvent> MECHANICAL_DIESEL_NOISE = register("mechanical_diesel_noise");
    public static final RegistryObject<SoundEvent> MECHANICAL_WATER_HAMMER = register("mechanical_water_hammer");
    public static final RegistryObject<SoundEvent> MECHANICAL_STEAM_TURBINE = register("mechanical_steam_turbine");
    public static final RegistryObject<SoundEvent> MECHANICAL_PRESSURE_RELEASE = register("mechanical_pressure_release");
    public static final RegistryObject<SoundEvent> MECHANICAL_PUMP_START_2 = register("mechanical_pump_start_2");
    public static final RegistryObject<SoundEvent> MECHANICAL_GRID_ALARM = register("mechanical_grid_alarm");
    public static final RegistryObject<SoundEvent> MECHANICAL_PHONE_WAIT = register("mechanical_phone_wait");
    public static final RegistryObject<SoundEvent> MECHANICAL_DOOR_OPEN_SUSPECT = register("mechanical_door_open_suspect");
    public static final RegistryObject<SoundEvent> MECHANICAL_MACHINE_STOP_SUSPECT = register("mechanical_machine_stop_suspect");
    public static final RegistryObject<SoundEvent> MECHANICAL_POWER_BELL = register("mechanical_power_bell");
    public static final RegistryObject<SoundEvent> MECHANICAL_ACCESS_CONTROL = register("mechanical_access_control");
    public static final RegistryObject<SoundEvent> MECHANICAL_GATE_OPEN = register("mechanical_gate_open");
    public static final RegistryObject<SoundEvent> EXPLOSION_REACTOR = register("explosion_reactor");
    public static final RegistryObject<SoundEvent> EXPLOSION_DULL = register("explosion_dull");
    public static final RegistryObject<SoundEvent> EXPLOSION_BURNING = register("explosion_burning");
    public static final RegistryObject<SoundEvent> EXPLOSION_STEAM = register("explosion_steam");
    public static final RegistryObject<SoundEvent> EXPLOSION_SHAKING = register("explosion_shaking");
    public static final RegistryObject<SoundEvent> ALARM_PRIMARY_LOOP_DRY = register("alarm_primary_loop_dry");
    public static final RegistryObject<SoundEvent> ALARM_COOLING_DAMAGE = register("alarm_cooling_damage");
    public static final RegistryObject<SoundEvent> ALARM_REACTOR_OVERHEAT = register("alarm_reactor_overheat");
    public static final RegistryObject<SoundEvent> ALARM_GATE = register("alarm_gate");
    public static final RegistryObject<SoundEvent> ALARM_CONTROL_ROOM = register("alarm_control_room");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN = register("alarm_unknown");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_2 = register("alarm_unknown_2");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_3 = register("alarm_unknown_3");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_4 = register("alarm_unknown_4");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_5 = register("alarm_unknown_5");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_6 = register("alarm_unknown_6");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_7 = register("alarm_unknown_7");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_8 = register("alarm_unknown_8");
    public static final RegistryObject<SoundEvent> ALARM_UNKNOWN_9 = register("alarm_unknown_9");
    public static final RegistryObject<SoundEvent> ALARM_AIRLOCK = register("alarm_airlock");
    public static final RegistryObject<SoundEvent> ALARM_BEEP = register("alarm_beep");
    public static final RegistryObject<SoundEvent> ALARM_HOTWELL_DRY = register("alarm_hotwell_dry");
    public static final RegistryObject<SoundEvent> ALARM_NO_WATER = register("alarm_no_water");
    public static final RegistryObject<SoundEvent> ALARM_NO_WATER_ALERT = register("alarm_no_water_alert");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER = register("alarm_buzzer");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_2 = register("alarm_buzzer_2");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_3 = register("alarm_buzzer_3");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_4 = register("alarm_buzzer_4");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_5 = register("alarm_buzzer_5");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_6 = register("alarm_buzzer_6");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_7 = register("alarm_buzzer_7");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_8 = register("alarm_buzzer_8");
    public static final RegistryObject<SoundEvent> ALARM_BUZZER_9 = register("alarm_buzzer_9");
    public static final RegistryObject<SoundEvent> ALARM_THREE = register("alarm_three");
    public static final RegistryObject<SoundEvent> ALARM_FOUR = register("alarm_four");
    public static final RegistryObject<SoundEvent> ALARM_CRANE_WARNING = register("alarm_crane_warning");
    public static final RegistryObject<SoundEvent> ALARM_LONG_BUZZER = register("alarm_long_buzzer");
    public static final RegistryObject<SoundEvent> ALARM_HIGH_FREQ_BEEP = register("alarm_high_freq_beep");
    public static final RegistryObject<SoundEvent> ALARM_HIGH_FREQ = register("alarm_high_freq");
    public static final RegistryObject<SoundEvent> RUNNING_SHUTDOWN = register("running_shutdown");
    public static final RegistryObject<SoundEvent> RUNNING_CLOSE_1 = register("running_close_1");
    public static final RegistryObject<SoundEvent> RUNNING_CLOSE_2 = register("running_close_2");
    public static final RegistryObject<SoundEvent> RUNNING_START_1 = register("running_start_1");
    public static final RegistryObject<SoundEvent> RUNNING_START_2 = register("running_start_2");
    public static final RegistryObject<SoundEvent> RUNNING_START_3 = register("running_start_3");
    public static final RegistryObject<SoundEvent> RUNNING_START_4 = register("running_start_4");
    public static final RegistryObject<SoundEvent> RUNNING_JET = register("running_jet");
    public static final RegistryObject<SoundEvent> RUNNING_HUM = register("running_hum");
    public static final RegistryObject<SoundEvent> RUNNING_MACHINE_2 = register("running_machine_2");
    public static final RegistryObject<SoundEvent> RUNNING_MACHINE_3 = register("running_machine_3");
    public static final RegistryObject<SoundEvent> RUNNING_MACHINE_5 = register("running_machine_5");
    public static final RegistryObject<SoundEvent> RUNNING_MACHINE_6 = register("running_machine_6");
    public static final RegistryObject<SoundEvent> RUNNING_MACHINE_7 = register("running_machine_7");
    public static final RegistryObject<SoundEvent> RUNNING_OPEN = register("running_open");
    public static final RegistryObject<SoundEvent> RUNNING_OPEN_2 = register("running_open_2");
    public static final RegistryObject<SoundEvent> RUNNING_UNKNOWN_2 = register("running_unknown_2");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_11 = register("running_mech_11");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_12 = register("running_mech_12");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_13 = register("running_mech_13");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_14 = register("running_mech_14");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_8 = register("running_mech_8");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_9 = register("running_mech_9");
    public static final RegistryObject<SoundEvent> RUNNING_MECH_9_LOOP = register("running_mech_9_loop");
    public static final RegistryObject<SoundEvent> RUNNING_DIESEL_NOISE = register("running_diesel_noise");
    public static final RegistryObject<SoundEvent> RUNNING_TURBINE_NOISE = register("running_turbine_noise");
    public static final RegistryObject<SoundEvent> RUNNING_PUMP_CLOSE = register("running_pump_close");
    public static final RegistryObject<SoundEvent> RUNNING_PUMP_START = register("running_pump_start");
    public static final RegistryObject<SoundEvent> RUNNING_PUMP_LOOP = register("running_pump_loop");
    public static final RegistryObject<SoundEvent> RUNNING_RUN_2 = register("running_run_2");
    public static final RegistryObject<SoundEvent> RUNNING_HIGH_PRESSURE_RELEASE = register("running_high_pressure_release");
    public static final RegistryObject<SoundEvent> MUSIC_OPENING = register("music_opening");
    public static final RegistryObject<SoundEvent> MUSIC_ACHIEVEMENT = register("music_achievement");
    public static final RegistryObject<SoundEvent> MUSIC_UNKNOWN_DANCE = register("music_unknown_dance");
    public static final RegistryObject<SoundEvent> MUSIC_NUKE = register("music_nuke");
    public static final RegistryObject<SoundEvent> MUSIC_PAYDAY = register("music_payday");
    public static final RegistryObject<SoundEvent> MUSIC_SETTLEMENT = register("music_settlement");
    public static final RegistryObject<SoundEvent> MUSIC_1 = register("music_1");
    public static final RegistryObject<SoundEvent> MUSIC_2 = register("music_2");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CcRc.MODID, name)));
    }
}
