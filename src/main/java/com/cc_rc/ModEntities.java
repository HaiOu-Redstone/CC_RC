package com.cc_rc;

import com.cc_rc.entity.BaoZi;
import com.cc_rc.entity.ErrorMob;
import com.cc_rc.entity.EvilGajin;
import com.cc_rc.entity.GajinSnail;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体类型注册表（DeferredRegister&lt;EntityType&lt;?&gt;&gt;）。
 * 弹射物 bao_zi（包子）与敌对生物 error_mob（错误生物）。
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CcRc.MODID);

    // 包子弹射物：尺寸同雪球（0.25×0.25），追踪范围 4，更新间隔 10 tick
    public static final RegistryObject<EntityType<BaoZi>> BAO_ZI = ENTITY_TYPES.register("bao_zi",
            () -> EntityType.Builder.<BaoZi>of(BaoZi::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("cc_rc:bao_zi"));

    // 错误生物（error_mob）：立体化 ERROR 字牌实体，敌对，仿蠹虫寻路/攻击。
    // 尺寸 0.6×0.4×1.5（宽×厚×高，字牌横宽），追踪范围 8（肉眼即时可见），
    // 更新间隔 3（怪物标准）。
    // 不注册 SpawnPlacements —— 不会在世界中自然生成，只能刷怪蛋/刷怪笼。
    public static final RegistryObject<EntityType<ErrorMob>> ERROR_MOB = ENTITY_TYPES.register("error_mob",
            () -> EntityType.Builder.<ErrorMob>of(ErrorMob::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.5F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cc_rc:error_mob"));

    // 错误生物变种（null）：立体化 NULL 字牌（模型「模型/错误生物/null」）。
    // 模型宽约 0.8 方块、高约 0.41 方块 → 碰撞箱 0.6×0.6。行为与主变种一致
    // （共用 ErrorMob 类），仅模型/贴图不同；不自然生成，只能刷怪蛋/刷怪笼。
    public static final RegistryObject<EntityType<ErrorMob>> ERROR_MOB_NULL = ENTITY_TYPES.register("error_mob_null",
            () -> EntityType.Builder.<ErrorMob>of(ErrorMob::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cc_rc:error_mob_null"));

    // 错误生物变种（warn）：立体化 WARN 字牌（模型「模型/错误生物/WARN」）。
    // 模型宽约 2.0 方块、高约 0.5 方块 → 碰撞箱 0.6×1.2。行为与主变种一致
    // （共用 ErrorMob 类），仅模型/贴图不同；不自然生成，只能刷怪蛋/刷怪笼。
    public static final RegistryObject<EntityType<ErrorMob>> ERROR_MOB_WARN = ENTITY_TYPES.register("error_mob_warn",
            () -> EntityType.Builder.<ErrorMob>of(ErrorMob::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.2F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cc_rc:error_mob_warn"));

    // 盖金蜗牛（gajin）：被动动物（AI 参考原版猪，无乘骑机制，右键播放音效）。
    // 尺寸 0.5×0.5（蜗牛趴地，矮宽），追踪范围 8，更新间隔 3（动物标准）。
    // 不注册 SpawnPlacements —— 不会在世界中自然生成，只能刷怪蛋召唤。
    public static final RegistryObject<EntityType<GajinSnail>> GAJIN = ENTITY_TYPES.register("gajin",
            () -> EntityType.Builder.<GajinSnail>of(GajinSnail::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cc_rc:gajin"));

    // 邪恶盖金（evil_gajin）：敌对生物（继承原版僵尸 AI，见 EvilGajin 类注释）。
    // 尺寸 0.6×0.6（暂时复用盖金蜗牛模型，矮宽造型），追踪范围 8，更新间隔 3（怪物标准）。
    // 不注册 SpawnPlacements —— 不会在世界中自然生成，只能刷怪蛋/刷怪笼召唤。
    public static final RegistryObject<EntityType<EvilGajin>> EVIL_GAJIN = ENTITY_TYPES.register("evil_gajin",
            () -> EntityType.Builder.<EvilGajin>of(EvilGajin::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cc_rc:evil_gajin"));
}
