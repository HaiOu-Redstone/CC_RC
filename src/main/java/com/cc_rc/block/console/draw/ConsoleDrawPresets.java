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
 * 当前登记的图案为 32×32 或 16×16，登记时 size 必须与贴图实际边长一致
 * （界面缩略图按 size 取 UV，不一致会取错区域）。
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
            preset(DEFAULT_ID, "sample", 32),
            // 储罐（四种造型）
            preset("tank1", "tank1", 32),
            preset("tank2", "tank2", 32),
            preset("tank3", "tank3", 32),
            preset("tank4", "tank4", 32),
            // 泵（两种造型）
            preset("pump1", "pump1", 32),
            preset("pump2", "pump2", 32),
            // 汽轮机（两种造型）
            preset("steam_tubine_1", "steam_tubine_1", 32),
            preset("steam_tubine_2", "steam_tubine_2", 32),
            // 反应堆
            preset("reactor", "reactor", 32),
            // 抑压池
            preset("suppression_pool", "suppression_pool", 32),
            // 换热器
            preset("heat_exchanger", "heat_exchanger", 32),
            // 汽水分离器
            preset("steam_separator", "steam_separator", 32),
            // 给水加热器
            preset("feedwater_heater", "feedwater_heater", 32),
            // 过滤器（16×16）
            preset("filter", "filter", 16),
            // 电池
            preset("battery", "battery", 32),
            // 变压器
            preset("transformer", "transformer", 32),
            // 整流器
            preset("rectifier", "rectifier", 32),
            // 电闸（16×16）
            preset("breaker", "breaker", 16),
            // 阀门：止回阀（16×16）
            preset("valve_check", "valve_check", 16),
            // 阀门：自动阀两种造型（16×16）
            preset("valve_auto_1", "valve_auto_1", 16),
            preset("valve_auto_2", "valve_auto_2", 16),
            // 阀门：手动阀两种造型（16×16）
            preset("valve_1", "valve_1", 16),
            preset("valve_2", "valve_2", 16)
    );

    /**
     * 构造一条预设：贴图路径固定为 textures/gui/draw_presets/{fileName}.png，
     * 像素尺寸取贴图实际边长，最大放大倍数统一为 3。
     */
    private static Preset preset(String id, String fileName, int size) {
        return new Preset(id,
                new ResourceLocation(CcRc.MODID, "textures/gui/draw_presets/" + fileName + ".png"),
                size, 3);
    }

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
