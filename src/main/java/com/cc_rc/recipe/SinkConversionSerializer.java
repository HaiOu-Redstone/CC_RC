package com.cc_rc.recipe;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 水槽转换配方的 JSON / 网络序列化器（配方类型 cc_rc:sink_conversion）
 *
 * JSON 格式：
 * {
 *   "type": "cc_rc:sink_conversion",
 *   "input":  { "item": "minecraft:glass_bottle" },                 // 输入物品（必须）
 *   "input_nbt": "{Potion:\"minecraft:water\"}",                    // 可选：输入 NBT（SNBT 字符串，部分匹配）
 *   "output": { "item": "minecraft:potion", "count": 1 },           // 输出物品（必须）
 *   "output_nbt": "{Potion:\"minecraft:water\"}"                    // 可选：输出 NBT（SNBT 字符串，写入产物）
 * }
 */
public class SinkConversionSerializer implements RecipeSerializer<SinkConversionRecipe> {

    @Override
    public SinkConversionRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        // 输入物品
        JsonObject inputJson = GsonHelper.getAsJsonObject(json, "input");
        ResourceLocation inputId = new ResourceLocation(GsonHelper.getAsString(inputJson, "item"));
        Item inputItem = BuiltInRegistries.ITEM.get(inputId);
        // 可选：输入 NBT（部分匹配）
        CompoundTag inputNbt = json.has("input_nbt")
                ? parseNbt(GsonHelper.getAsString(json, "input_nbt"))
                : null;

        // 输出物品（含数量与可选 NBT）
        JsonObject outputJson = GsonHelper.getAsJsonObject(json, "output");
        ResourceLocation outputId = new ResourceLocation(GsonHelper.getAsString(outputJson, "item"));
        int count = GsonHelper.getAsInt(outputJson, "count", 1);
        ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(outputId), count);
        if (json.has("output_nbt")) {
            output.setTag(parseNbt(GsonHelper.getAsString(json, "output_nbt")));
        }

        return new SinkConversionRecipe(recipeId, inputItem, inputNbt, output);
    }

    /** 解析 SNBT 字符串为 CompoundTag（如 {Potion:"minecraft:water"}） */
    private static CompoundTag parseNbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("无法解析水槽转换配方的 NBT: " + snbt, e);
        }
    }

    @Override
    public SinkConversionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        Item inputItem = Item.byId(buffer.readVarInt());
        CompoundTag inputNbt = buffer.readNbt();
        ItemStack output = buffer.readItem();
        return new SinkConversionRecipe(recipeId, inputItem, inputNbt, output);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, SinkConversionRecipe recipe) {
        buffer.writeVarInt(Item.getId(recipe.getInputItem()));
        buffer.writeNbt(recipe.getInputNbt());
        buffer.writeItem(recipe.getOutput());
    }
}