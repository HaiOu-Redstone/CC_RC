package com.cc_rc.block;

import net.minecraft.network.chat.Component;

/**
 * 可显示名称（文字）的方块实体统一接口。
 *
 * 本模组中「放置时从物品自定义名称写入、渲染到方块表面」的方块实体
 * （控制面板类、数码显示器、数字调节器、数字圆盘记录仪等）都具备
 * setText/getText 能力。编辑工具（edit_tool）通过本接口统一识别与修改，
 * 无需逐个 instanceof 判断。
 *
 * 实现约定：所有实现类必须把文字通过 NBT 持久化并支持客户端同步
 * （各自已有 setText 会触发 setChanged + sendBlockUpdated）。
 */
public interface ITextDisplay {

    /** 设置方块表面显示的文字（单行；空文字表示不显示）。 */
    void setText(Component text);

    /** 获取当前显示的文字。 */
    Component getText();
}