package com.cc_rc;

import com.cc_rc.gui.EditTextMenu;
import com.cc_rc.gui.PosterMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册表（DeferredRegister&lt;MenuType&lt;?&gt;&gt;）。
 * 目前仅注册编辑工具的文字编辑菜单（edit_text）。
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CcRc.MODID);

    // 编辑工具文字编辑菜单：构造参数 = 方块坐标 + 当前文字（初始输入框内容）
    public static final RegistryObject<MenuType<EditTextMenu>> EDIT_TEXT =
            MENU_TYPES.register("edit_text",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new EditTextMenu(windowId, data.readBlockPos(), data.readComponent())));

    // 海报设置菜单：构造参数 = 方块坐标 + 当前图片索引 / 缩放倍率 / 垂直偏移 / 水平偏移
    public static final RegistryObject<MenuType<PosterMenu>> POSTER =
            MENU_TYPES.register("poster",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new PosterMenu(windowId, data.readBlockPos(), data.readVarInt(),
                                    data.readFloat(), data.readFloat(), data.readFloat())));
}