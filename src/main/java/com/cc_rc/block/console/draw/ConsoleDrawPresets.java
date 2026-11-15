package com.cc_rc.block.console.draw;

import com.cc_rc.CcRc;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 预设图案清单。
 *
 * 图案以「纯白 + 透明背景」的 PNG 蒙版保存：渲染时使用顶点色染色，
 * 因此同一张图可以被不同颜色复用。新增图案时只需在
 * {@code assets/cc_rc/textures/gui/draw_presets/} 下放入同规格 PNG，
 * 并在此登记 id / 贴图 / 默认像素尺寸即可（服务端按白名单校验）。
 *
 * 灰度用法：最终颜色 = 贴图 RGB × 选中颜色，所以
 *  - 纯白 0xFFFFFF = 显示为原色；
 *  - 灰度值越低，显示越暗（例如 0x7F7F7F 约为原色的一半亮度），用于区分明暗层次。
 * 乘法无法让颜色比原色更亮，因此贴图只做「压暗」，不需要也不应使用提亮值。
 *
 * 当前登记的图案均为 32×32，之后可按相同格式继续补充。
 */
public final class ConsoleDrawPresets {

    /** 预设图案：id（存档中保存的标识）、贴图、默认像素尺寸、允许的最大缩放倍数。 */
    public record Preset(String id, ResourceLocation texture, int size, float maxScale) {
    }

    /** 默认选中图案的 id。 */
    public static final String DEFAULT_ID = "sample";

    /**
     * 贴图显示缩放系数：32×32 贴图先缩小 0.5 倍，再乘以玩家的缩放倍数。
     * 这样换成更高分辨率贴图后，默认显示尺寸仍与旧 16×16 贴图一致。
     */
    public static final float DISPLAY_SCALE = 0.5F;

    /** 全部预设（顺序即界面中的展示顺序）。 */
    private static final List<Preset> ALL = List.of(
            // 示例图案：32×32，白色实心十字（透明背景），最多放大 3 倍
            new Preset(DEFAULT_ID,
                    new ResourceLocation(CcRc.MODID, "textures/gui/draw_presets/sample.png"),
                    32, 3),
            // 储罐（两种造型）
            new Preset("tank1",
                    new ResourceLocation(CcRc.MODID, "textures/gui/draw_presets/tank1.png"),
                    32, 3),
            new Preset("tank2",
                    new ResourceLocation(CcRc.MODID, "textures/gui/draw_presets/tank2.png"),
                    32, 3),
            // 汽轮机（两种造型）
            new Preset("steam_tubine_1",
                    new ResourceLocation(CcRc.MODID,
                            "textures/gui/draw_presets/steam_tubine_1.png"),
                    32, 3),
            new Preset("steam_tubine_2",
                    new ResourceLocation(CcRc.MODID,
                            "textures/gui/draw_presets/steam_tubine_2.png"),
                    32, 3)
    );

    private ConsoleDrawPresets() {
    }

    /** 全部预设（只读）。 */
    public static List<Preset> all() {
        return ALL;
    }

    /** 按 id 查找，找不到返回 null。 */
    public static Preset byId(String id) {
        if (id == null) {
            return null;
        }
        for (Preset preset : ALL) {
            if (preset.id().equals(id)) {
                return preset;
            }
        }
        return null;
    }
}
