package com.cc_rc;

import com.cc_rc.entity.BaoZi;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体类型注册表（DeferredRegister&lt;EntityType&lt;?&gt;&gt;）。
 * 目前仅注册弹射物 bao_zi（包子），飞行逻辑复用原版雪球，命中触发爆炸。
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
}
