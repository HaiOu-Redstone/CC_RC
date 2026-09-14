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
    // 奶龙玩偶声音（非唱片）
    public static final RegistryObject<SoundEvent> NAI_LONG = register("nai_long");
    // F.A.A.S服务器环境音效（非唱片，靠近时持续播放）
    public static final RegistryObject<SoundEvent> SERVER_NOISE = register("server_noise");
    // 破解器破解音效（破解密码输入器时循环播放，中断/成功时停止）
    public static final RegistryObject<SoundEvent> PASSWORD_CRACK = register("password_crack");
    // 盖金蜗牛音效（玩家右键蜗牛播放；平常无 ambient 叫声）
    public static final RegistryObject<SoundEvent> GAJIN = register("gajin");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CcRc.MODID, name)));
    }
}