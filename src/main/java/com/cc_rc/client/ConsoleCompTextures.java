package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.ConsoleCompType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * 控制台组件贴图的「按颜色重新着色」缓存。
 *
 * 组件颜色不能直接用顶点色相乘：那样会把原贴图本身的色相也乘进去。
 * 这里改成逐像素重算贴图：
 *  - 只处理「亮部或带彩度」的像素（表盘奶白底色、右上角红色区域、按键区、
 *    指示灯面板等），把它的色相替换为所选颜色，亮度按 luma / 最亮可着色像素
 *    的比例保留，于是同一区域里较暗的部分自然得到「所选颜色加深」的效果；
 *  - 深灰外壳、黑色刻度、透明像素原样保留，保证外观与模型预览一致；
 *  - 流量表的指针与表盘右上角的红色区域在贴图上是同一块红色（指针面只采样其中
 *    一个像素），无法按颜色区分。做法：解析模型时把「三顶点 UV 完全相同」的面
 *    标记为纯色面（ConsoleCompModels.Tri.solidColor）；生成着色贴图时另找一个
 *    **不被任何面采样**的像素（看不见），把它涂成原指针色，渲染时再把纯色面的
 *    UV 指过去 —— 指针保持原红色，红色区域与表盘其余部分一起正常着色，
 *    不会在表盘上留下与指针同色的小方块。
 *
 * 结果按 (类型, 颜色) 缓存成动态贴图，绘制时直接用它的 ResourceLocation。
 * 未设置颜色（-1）时调用方使用原始贴图，不进入这里。
 */
public final class ConsoleCompTextures {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 每种类型的原始像素（只读，作为着色输入）。 */
    private static final Map<ConsoleCompType, NativeImage> BASE =
            new EnumMap<>(ConsoleCompType.class);
    /** 每种类型「最亮可着色像素」的亮度，用于把亮度归一化到 1.0。 */
    private static final Map<ConsoleCompType, Integer> REF_LUMA =
            new EnumMap<>(ConsoleCompType.class);
    /** (类型:颜色) → 已注册的动态贴图。 */
    private static final Map<String, Dyed> CACHE = new HashMap<>();
    /** 每种类型「指针像素」：{列, 行, ARGB}；着色贴图里把该像素涂成原指针色。 */
    private static final Map<ConsoleCompType, int[]> NEEDLE =
            new EnumMap<>(ConsoleCompType.class);
    /** 缓存上限：超过后整批释放，避免长时间游戏累积显存。 */
    private static final int MAX_CACHE = 96;
    /** 判定「亮部」的亮度阈值（深灰外壳 luma≈80、黑色刻度 luma≈0 会被排除）。 */
    private static final int BRIGHT_LUMA = 96;
    /** 判定「带彩度」的通道差阈值（表盘红色区域 chroma≈163 会被包含）。 */
    private static final int CHROMA_THRESHOLD = 24;

    private ConsoleCompTextures() {
    }

    /**
     * 取某种类型在指定 RGB（0xRRGGBB）下的着色贴图。
     *
     * @param mesh 该类型的模型（用于找出纯色面采样的像素，使其保持原色；可为 null）
     * @return 着色贴图位置；类型不可着色、资源缺失或创建失败时返回 null（调用方退回原贴图）
     */
    public static ResourceLocation get(ConsoleCompType type, int rgb,
                                       ConsoleCompModels.ObjMesh mesh) {
        if (type == null || !type.recolorable) {
            return null;
        }
        int color = rgb & 0xFFFFFF;
        String key = type.id + ":" + color;
        Dyed cached = CACHE.get(key);
        if (cached != null) {
            return cached.loc;
        }
        NativeImage base = base(type);
        if (base == null) {
            return null;
        }
        int ref = Math.max(1, REF_LUMA.getOrDefault(type, 255));
        int width = base.getWidth();
        int height = base.getHeight();
        int[] needle = needleTexel(type, mesh, base);
        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = color & 0xFF;

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = base.getPixelRGBA(x, y);
                image.setPixelRGBA(x, y, recolor(pixel, cr, cg, cb, ref));
            }
        }
        // 指针像素：该像素不被任何普通面采样（看不见），在这里直接涂成原指针色，
        // 渲染时纯色面（指针）的 UV 会指向它，于是指针始终是红色
        if (needle != null) {
            image.setPixelRGBA(needle[0], needle[1], needle[2]);
        }

        try {
            DynamicTexture texture = new DynamicTexture(image);
            // 像素风贴图：关闭线性过滤与 mipmap，避免边缘糊化
            texture.setFilter(false, false);
            ResourceLocation loc = new ResourceLocation(CcRc.MODID,
                    "dynamic/console_comp/" + type.id + "_" + String.format("%06x", color));
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            if (CACHE.size() >= MAX_CACHE) {
                releaseAll();
            }
            CACHE.put(key, new Dyed(loc, texture));
            return loc;
        } catch (Exception e) {
            LOGGER.warn("控制台组件贴图着色失败：{}", type.id, e);
            image.close();
            return null;
        }
    }

    /**
     * 找出某种类型的「指针像素」，返回 {列, 行, ARGB}；没有指针时返回 null。
     *
     * 流量表的指针是一组三顶点 UV 完全相同的面（{@link ConsoleCompModels.Tri#solidColor}），
     * 整根指针只会采样到一个贴图像素。它原本指向的像素同时属于表盘右上角的红色
     * 区域：如果保留那个像素不着色，表盘上就会留下一块与指针同色的方块；而把它
     * 挪到红色区域里的另一块像素，仍然会留下同样的小方块（整块红色都被表盘面
     * 采样）。因此这里改为找一个**不被任何面采样**的像素（看不见），把它涂成原
     * 指针色，渲染时再把指针 UV 指过去 —— 指针保持红色，表盘整块正常着色。
     */
    private static int[] needleTexel(ConsoleCompType type, ConsoleCompModels.ObjMesh mesh,
                                     NativeImage base) {
        if (NEEDLE.containsKey(type)) {
            return NEEDLE.get(type);
        }
        int[] result = null;
        int width = base.getWidth();
        int height = base.getHeight();
        if (mesh != null) {
            int sampleX = -1;
            int sampleY = -1;
            for (ConsoleCompModels.Tri tri : mesh.tris) {
                if (tri.solidColor) {
                    sampleX = texelX(tri.uv[0], width);
                    sampleY = texelY(tri.uv[1], height);
                    break;
                }
            }
            if (sampleX >= 0) {
                int needleColor = base.getPixelRGBA(sampleX, sampleY);
                if (((needleColor >>> 24) & 0xFF) != 0) {
                    boolean[] covered = coverage(mesh, width, height);
                    // 优先使用透明像素：即使某个面的边缘意外踩到它也不会露出色块
                    int fallback = -1;
                    for (int i = 0; i < width * height && result == null; i++) {
                        if (covered[i]) {
                            continue;
                        }
                        int x = i % width;
                        int y = i / width;
                        if (x == sampleX && y == sampleY) {
                            continue;
                        }
                        if (((base.getPixelRGBA(x, y) >>> 24) & 0xFF) == 0) {
                            result = new int[]{x, y, needleColor | 0xFF000000};
                        } else if (fallback < 0) {
                            fallback = i;
                        }
                    }
                    // 没有透明像素时退而求其次：用任意未被覆盖的像素
                    if (result == null && fallback >= 0) {
                        result = new int[]{fallback % width, fallback / width,
                                needleColor | 0xFF000000};
                    }
                }
            }
        }
        NEEDLE.put(type, result);
        return result;
    }

    /**
     * 着色贴图中指针（纯色面）应使用的 UV；没有指针时返回 null。
     *
     * 渲染器在有颜色的情况下把纯色面的 UV 换成本方法返回值，配合着色贴图里
     * 被涂成原指针色的像素，让指针始终保持红色。
     */
    public static float[] needleUv(ConsoleCompType type, ConsoleCompModels.ObjMesh mesh) {
        if (type == null) {
            return null;
        }
        NativeImage base = base(type);
        if (base == null) {
            return null;
        }
        int[] needle = needleTexel(type, mesh, base);
        if (needle == null) {
            return null;
        }
        return new float[]{(needle[0] + 0.5F) / base.getWidth(),
                (needle[1] + 0.5F) / base.getHeight()};
    }

    /**
     * 找出被「非纯色面」采样到的贴图像素。
     *
     * 这里用每个三角形 UV 的包围盒做**保守**估计（实际采样范围一定落在包围盒内），
     * 因此落在包围盒外的像素可以安全挪用（例如作为指针色块）。
     */
    private static boolean[] coverage(ConsoleCompModels.ObjMesh mesh, int width, int height) {
        boolean[] covered = new boolean[width * height];
        for (ConsoleCompModels.Tri tri : mesh.tris) {
            if (tri.solidColor) {
                continue;
            }
            float minU = Math.min(tri.uv[0], Math.min(tri.uv[2], tri.uv[4]));
            float maxU = Math.max(tri.uv[0], Math.max(tri.uv[2], tri.uv[4]));
            float minV = Math.min(tri.uv[1], Math.min(tri.uv[3], tri.uv[5]));
            float maxV = Math.max(tri.uv[1], Math.max(tri.uv[3], tri.uv[5]));
            // 包围盒用「直接映射」而不是 GL_REPEAT：UV=1.0 属于最后一列/行
            int x0 = clampTexel((int) Math.floor(minU * width), width);
            int x1 = clampTexel((int) Math.ceil(maxU * width) - 1, width);
            int y0 = clampTexel((int) Math.floor(minV * height), height);
            int y1 = clampTexel((int) Math.ceil(maxV * height) - 1, height);
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    covered[y * width + x] = true;
                }
            }
        }
        return covered;
    }

    /** UV → 贴图像素列（GL_REPEAT：先取小数部分，如 -0.002 采样到最右列）。 */
    private static int texelX(float u, int width) {
        float f = u - (float) Math.floor(u);
        return clampTexel((int) Math.floor(f * width), width);
    }

    /** UV → 贴图像素行，规则同 {@link #texelX}。 */
    private static int texelY(float v, int height) {
        float f = v - (float) Math.floor(v);
        return clampTexel((int) Math.floor(f * height), height);
    }

    /** 把像素索引夹到 [0, size-1]。 */
    private static int clampTexel(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    /** 单个像素的着色：不改深灰外壳 / 黑色刻度 / 透明像素。 */
    private static int recolor(int pixel, int cr, int cg, int cb, int ref) {
        int a = (pixel >>> 24) & 0xFF;
        if (a == 0) {
            return pixel;
        }
        // NativeImage 的像素按 ABGR 打包
        int b = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int r = pixel & 0xFF;
        if (!isTintable(r, g, b)) {
            return pixel;
        }
        float factor = Math.min(1.0F, luma(r, g, b) / (float) ref);
        int nr = Math.round(cr * factor);
        int ng = Math.round(cg * factor);
        int nb = Math.round(cb * factor);
        return (a << 24) | (nb << 16) | (ng << 8) | nr;
    }

    /**
     * 该像素是否属于「需要换色」的区域：亮部或带彩度。
     *
     * 带彩度的像素即表盘上的红色区域（指针在贴图上复用同一块红色，
     * 由渲染层单独用原贴图绘制，见 ConsoleCompModels.Tri.solidColor）。
     */
    private static boolean isTintable(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        if (max == 0) {
            return false;
        }
        int min = Math.min(r, Math.min(g, b));
        return max - min > CHROMA_THRESHOLD || luma(r, g, b) > BRIGHT_LUMA;
    }

    /** 感知亮度（0~255）。 */
    private static int luma(int r, int g, int b) {
        return (299 * r + 587 * g + 114 * b) / 1000;
    }

    /** 读取并缓存某种类型的原始贴图像素，同时算出归一化亮度。 */
    private static NativeImage base(ConsoleCompType type) {
        if (BASE.containsKey(type)) {
            return BASE.get(type);
        }
        NativeImage image = null;
        try {
            // 贴图名用 textureId()：双色灯 / RGB 颜色控制器与三状态旋钮开关共用
            // t_switch.png（源模型 panel_props 共用 knob_switch.mtl 的同一张贴图），
            // 避免按 id 去找已删除的 lamp_d.png / rgb_color.png 而误报贴图缺失。
            ResourceLocation loc = new ResourceLocation(CcRc.MODID,
                    "textures/block/console_parts/" + type.textureId() + ".png");
            Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(loc).orElse(null);
            if (resource == null) {
                LOGGER.warn("控制台组件贴图缺失：{}", loc);
            } else {
                try (InputStream in = resource.open()) {
                    image = NativeImage.read(in);
                }
                REF_LUMA.put(type, maxTintableLuma(image));
            }
        } catch (Exception e) {
            LOGGER.warn("控制台组件贴图读取失败：{}", type.id, e);
            image = null;
        }
        BASE.put(type, image);
        return image;
    }

    /** 扫描出最亮的可着色像素亮度（表盘底色 / 白色面板），用于归一化。 */
    private static int maxTintableLuma(NativeImage image) {
        int best = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                if (((pixel >>> 24) & 0xFF) == 0) {
                    continue;
                }
                int b = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int r = pixel & 0xFF;
                if (isTintable(r, g, b)) {
                    best = Math.max(best, luma(r, g, b));
                }
            }
        }
        return best;
    }

    /** 释放全部着色贴图（含动态贴图持有的像素内存）。 */
    private static void releaseAll() {
        for (Dyed dyed : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(dyed.loc);
            dyed.texture.close();
        }
        CACHE.clear();
    }

    /** 一张着色贴图：注册位置 + 动态贴图本体（便于释放像素内存）。 */
    private record Dyed(ResourceLocation loc, DynamicTexture texture) {
    }
}
