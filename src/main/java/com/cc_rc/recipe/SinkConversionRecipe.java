package com.cc_rc.recipe;

import com.cc_rc.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 水槽转换配方（自定义配方类型 cc_rc:sink_conversion）
 *
 * 数据驱动：由 data/cc_rc/recipes/ 下 json 定义，1 输入 ←→ 1 输出。
 * 输入与输出均可携带 NBT：输入 NBT 做"部分标签匹配"（物品标签包含输入 NBT 全部键值即命中，
 * 供水瓶 Potion:"minecraft:water" 这类区分用途）；输出 NBT 直接写入产物（用于生成水瓶）。
 * 水槽右键通过 RecipeManager 查询匹配该类型的配方来执行转换（桶↔水桶、玻璃瓶↔水瓶）。
 */
public class SinkConversionRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Item inputItem;          // 输入物品
    private final CompoundTag inputNbt;    // 可选：输入需包含的 NBT（部分匹配）
    private final ItemStack output;        // 输出物品（含 NBT）

    public SinkConversionRecipe(ResourceLocation id, Item inputItem, CompoundTag inputNbt, ItemStack output) {
        this.id = id;
        this.inputItem = inputItem;
        this.inputNbt = inputNbt;
        this.output = output;
    }

    /** 判断单个物品是否满足本配方输入（物品类型 + 输入 NBT 部分匹配） */
    public boolean matchesItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(inputItem)) {
            return false;
        }
        if (inputNbt == null) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        // 部分匹配：物品标签需包含输入 NBT 的全部键值（额外的键不影响匹配）
        for (String key : inputNbt.getAllKeys()) {
            if (!tag.contains(key) || !tag.get(key).equals(inputNbt.get(key))) {
                return false;
            }
        }
        return true;
    }

    /** 获取输出产物副本（含 NBT） */
    public ItemStack getOutputCopy() {
        return output.copy();
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() != 1) {
            return false;
        }
        return matchesItem(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SINK_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SINK_CONVERSION_TYPE.get();
    }

    public ItemStack getOutput() {
        return output;
    }

    public Item getInputItem() {
        return inputItem;
    }

    public CompoundTag getInputNbt() {
        return inputNbt;
    }

    public Ingredient getInputIngredient() {
        return Ingredient.of(inputItem);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(getInputIngredient());
    }
}