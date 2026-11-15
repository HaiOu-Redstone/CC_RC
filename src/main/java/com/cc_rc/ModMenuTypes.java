package com.cc_rc;

import com.cc_rc.gui.ConsoleMenu;
import com.cc_rc.gui.ConsoleProgramMenu;
import com.cc_rc.gui.ConsoleTextMenu;
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

    // 编辑工具文字编辑菜单：构造参数 = 方块坐标 + 当前文字 + 组件 id（-1 = 普通方块）
    public static final RegistryObject<MenuType<EditTextMenu>> EDIT_TEXT =
            MENU_TYPES.register("edit_text",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new EditTextMenu(windowId, data.readBlockPos(), data.readComponent(),
                                    data.readVarInt())));

    // 控制台表面文字专用菜单：总线坐标 + 文字 id/面/锚点坐标 + 当前样式 + 锚点/缩放 + 表面半宽高
    public static final RegistryObject<MenuType<ConsoleTextMenu>> CONSOLE_TEXT =
            MENU_TYPES.register("console_text",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            ConsoleTextMenu.read(windowId, data)));

    // 控制台表面组件设置菜单：总线坐标 + 组件 id/类型/面/位置/代号 + 表面半宽高
    public static final RegistryObject<MenuType<com.cc_rc.gui.ConsoleCompMenu>> CONSOLE_COMP =
            MENU_TYPES.register("console_comp",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            com.cc_rc.gui.ConsoleCompMenu.read(windowId, data)));

    // 控制柜表面绘图菜单：总线坐标 + 表面 + 表面半宽高
    public static final RegistryObject<MenuType<com.cc_rc.gui.ConsoleDrawMenu>> CONSOLE_DRAW =
            MENU_TYPES.register("console_draw",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            com.cc_rc.gui.ConsoleDrawMenu.read(windowId, data)));

    // 海报设置菜单：构造参数 = 方块坐标 + 当前图片索引 / 缩放倍率 / 垂直偏移 / 水平偏移
    public static final RegistryObject<MenuType<PosterMenu>> POSTER =
            MENU_TYPES.register("poster",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new PosterMenu(windowId, data.readBlockPos(), data.readVarInt(),
                                    data.readFloat(), data.readFloat(), data.readFloat())));

    // 控制台总线菜单：构造参数 = 总线方块坐标（其余数据由客户端直接读方块实体）
    public static final RegistryObject<MenuType<ConsoleMenu>> CONSOLE =
            MENU_TYPES.register("console",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ConsoleMenu(windowId, data.readBlockPos())));

    // 控制柜程序菜单：构造参数 = 总线方块坐标（辅助计算机右键打开）
    public static final RegistryObject<MenuType<ConsoleProgramMenu>> CONSOLE_PROGRAM =
            MENU_TYPES.register("console_program",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ConsoleProgramMenu(windowId, data.readBlockPos())));
}