package com.cc_rc.block.console;

import net.minecraft.util.StringRepresentable;

/**
 * 控制柜颜色（结构方块 / 总线 / 辅助计算机共用）。
 *
 * - 默认色沿用原有资源（controller_block 等），蓝色与黑色各有独立贴图；
 * - 只有同色的结构方块才会互相连通、拼成同一个结构；
 * - 总线识别到结构颜色后，通过方块状态 COLOR 切换为对应颜色的贴图，
 *   辅助计算机跟随相邻总线 / 结构方块的颜色。
 */
public enum ConsoleColor implements StringRepresentable {
    /** 默认色（原有外观）。 */
    DEFAULT("default"),
    /** 蓝色。 */
    BLUE("blue"),
    /** 黑色。 */
    BLACK("black");

    private final String name;

    ConsoleColor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * 贴图名后缀：默认色沿用旧资源名（无后缀），其余为 _blue / _black。
     * 例如 controller_block + suffix + ".png"。
     */
    public String textureSuffix() {
        return this == DEFAULT ? "" : "_" + this.name;
    }
}
