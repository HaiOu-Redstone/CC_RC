package com.cc_rc;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 自定义告示牌木种类型：为 16 色各注册一个 canvas_<颜色> 木种（搬运自农夫乐事）。
 * 名称带 "cc_rc:" 命名空间前缀，且每种颜色独立的 WoodType 名称（如 "cc_rc:canvas_white"），
 * 使原版 Sheets 解析告示牌面板贴图时：
 *   普通式：cc_rc:entity/signs/canvas_<颜色>.png
 *   悬挂式：cc_rc:entity/signs/hanging/canvas_<颜色>.png
 * 从而 16 种颜色各用各的贴图（源贴图位于 模型/农夫乐事/canvas_sign/signs）。
 * 原版 LayerDefinitions 会遍历所有已注册 WoodType 自动生成告示牌/悬挂告示牌模型层，
 * 因此无需在客户端额外注册模型层定义。
 */
public class ModWoodTypes {
    /** 16 种染料色（顺序同原版染料物品），与方块注册循环 ModBlocks.CANVAS_SIGN_COLORS 一致 */
    public static final String[] CANVAS_SIGN_COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    // 每种颜色一个 WoodType，顺序与颜色数组一一对应（实例自包含，避免类初始化循环依赖）
    public static final WoodType[] CANVAS_TYPES = new WoodType[CANVAS_SIGN_COLORS.length];

    static {
        for (int i = 0; i < CANVAS_SIGN_COLORS.length; i++) {
            String color = CANVAS_SIGN_COLORS[i];
            CANVAS_TYPES[i] = WoodType.register(new WoodType("cc_rc:canvas_" + color, BlockSetType.OAK));
        }
    }
}