package com.cc_rc;

import com.cc_rc.recipe.SinkConversionRecipe;
import com.cc_rc.recipe.SinkConversionSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义配方类型与序列化器注册。
 *
 * 目前仅一个配方类型：cc_rc:sink_conversion（水槽四向转换：桶↔水桶、玻璃瓶↔水瓶），
 * 配方数据文件位于 data/cc_rc/recipes/，由 SinkBlock 右键时通过 RecipeManager 查询驱动。
 */
public class ModRecipes {
    // 配方序列化器注册表（解析 data/cc_rc/recipes/ 下 JSON 的 "type" 字段）
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CcRc.MODID);

    // 配方类型注册表（RecipeManager 按类型分组存放）
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, CcRc.MODID);

    // 水槽转换配方：序列化器 + 配方类型
    public static final RegistryObject<RecipeSerializer<SinkConversionRecipe>> SINK_CONVERSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("sink_conversion", SinkConversionSerializer::new);

    public static final RegistryObject<RecipeType<SinkConversionRecipe>> SINK_CONVERSION_TYPE =
            RECIPE_TYPES.register("sink_conversion",
                    () -> RecipeType.simple(new ResourceLocation(CcRc.MODID, "sink_conversion")));
}