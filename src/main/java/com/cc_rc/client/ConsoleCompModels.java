package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.ConsoleCompType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * 控制台表面组件模型的客户端加载器。
 *
 * 资源来自 HBM 的 RoR 面板模型（只取 *_surface.obj 单件模型，不含 face_layout 排布）：
 *  - 模型：assets/cc_rc/models/console_parts/&lt;id&gt;_surface.obj
 *  - 贴图：assets/cc_rc/textures/block/console_parts/&lt;id&gt;.png
 *
 * OBJ 坐标约定（原 TESR 空间）：x = 面板外法线方向，y = 向上，z = 横向；
 * 1.0 = 1 格 = 16 像素。这里在加载时求出顶点包围盒，渲染时把模型中心
 * 对齐到组件锚点 (u, v)，并把 x 最小值贴到结构外表面。
 *
 * 解析结果按类型缓存（每种类型只读一次磁盘，失败也只尝试一次）。
 */
public final class ConsoleCompModels {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, ObjMesh> CACHE = new java.util.HashMap<>();

    private ConsoleCompModels() {
    }

    /** 取某种类型的默认（开 / 单状态）模型（失败返回 null，调用方跳过绘制）。 */
    public static ObjMesh get(ConsoleCompType type) {
        return get(type, false);
    }

    /**
     * 取某种类型的模型。
     *
     * @param closed 旋钮 / 拉杆开关的「关」状态使用 *_close_surface.obj 变体；其它类型忽略
     */
    public static ObjMesh get(ConsoleCompType type, boolean closed) {
        return getInternal(type, closed ? "_close" : "");
    }

    /**
     * 取某种类型的多状态模型变体。
     *
     * @param variant 0 = 基础模型（无后缀），1 / 2 = *_1_surface.obj / *_2_surface.obj
     *                （三状态开关的三个档位、双色指示灯的两个灯色）
     */
    public static ObjMesh get(ConsoleCompType type, int variant) {
        int v = Math.max(0, variant);
        return getInternal(type, v == 0 ? "" : "_" + v);
    }

    /** 按资源后缀取模型（后缀同时用于缓存 key、OBJ 文件名与可选贴图名）。 */
    private static ObjMesh getInternal(ConsoleCompType type, String suffix) {
        String key = type.name() + suffix;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        ObjMesh mesh = load(type, suffix);
        CACHE.put(key, mesh);
        return mesh;
    }

    /** 读取并解析一份 OBJ。 */
    private static ObjMesh load(ConsoleCompType type, String suffix) {
        ResourceLocation loc = new ResourceLocation(CcRc.MODID,
                "models/console_parts/" + type.modelId + suffix + "_surface.obj");
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(loc)
                    .orElse(null);
            if (resource == null) {
                LOGGER.warn("控制台组件模型缺失：{}", loc);
                return null;
            }
            List<float[]> verts = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();
            List<Tri> tris = new ArrayList<>();
            // 当前面是否属于「可染色面」（OBJ 中用 usemtl tint 标记，
            // 由模型转换脚本按分组/颜色写入，见 panel_props 组件）
            boolean tintMaterial = false;
            // 当前面是否属于「玻璃罩」面（OBJ 中用 usemtl glass 标记，
            // 安全按钮的罩子需用半透明批次单独绘制）
            boolean glassMaterial = false;
            // 当前分组名（OBJ 的 o 指令）：用于旋转指针 / 平移按钮等局部动画
            String currentGroup = "";

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    switch (parts[0]) {
                        case "v" -> verts.add(new float[]{f(parts[1]), f(parts[2]), f(parts[3])});
                        // OBJ 的 v 轴向上，Minecraft 贴图 v 轴向下 → 翻转
                        case "vt" -> uvs.add(new float[]{f(parts[1]), 1.0F - f(parts[2])});
                        case "vn" -> normals.add(new float[]{f(parts[1]), f(parts[2]), f(parts[3])});
                        // 材质名：tint = 底板 / 底座（可染色），glass = 玻璃罩（半透明批次）
                        case "usemtl" -> {
                            tintMaterial = parts.length > 1 && "tint".equals(parts[1]);
                            glassMaterial = parts.length > 1 && "glass".equals(parts[1]);
                        }
                        // 分组名：记录当前组，供渲染时做指针旋转 / 按钮位移
                        case "o" -> currentGroup = parts.length > 1 ? parts[1] : "";
                        case "f" -> addFace(parts, verts, uvs, normals, tris, tintMaterial,
                                glassMaterial, currentGroup);
                        default -> {
                            // s / mtllib 等忽略
                        }
                    }
                }
            }
            if (tris.isEmpty()) {
                return null;
            }

            // 顶点包围盒：用于把模型中心对齐到锚点、底面贴到表面
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE;
            float maxZ = -Float.MAX_VALUE;
            for (float[] v : verts) {
                minX = Math.min(minX, v[0]);
                maxX = Math.max(maxX, v[0]);
                minY = Math.min(minY, v[1]);
                maxY = Math.max(maxY, v[1]);
                minZ = Math.min(minZ, v[2]);
                maxZ = Math.max(maxZ, v[2]);
            }

            // 贴图：默认 <textureId>.png（多数类型与 modelId 相同；双色灯 / RGB 与
            // 三状态旋钮开关共用同一张 panel_props 调色板贴图，见 textureId()）；
            // 变体（开关「关」/ 三档位 / 双灯两色）若提供 <textureId><后缀>.png 则优先使用
            // （阀门式拉杆的开 / 关两套模型贴图不同；三档开关与双色灯复用同一张贴图，
            // 没有该文件时回退基础贴图）
            ResourceLocation texture = new ResourceLocation(CcRc.MODID,
                    "textures/block/console_parts/" + type.textureId() + ".png");
            if (!suffix.isEmpty()) {
                ResourceLocation variantTexture = new ResourceLocation(CcRc.MODID,
                        "textures/block/console_parts/" + type.textureId() + suffix + ".png");
                if (Minecraft.getInstance().getResourceManager()
                        .getResource(variantTexture).isPresent()) {
                    texture = variantTexture;
                }
            }
            // 可染色面的纯白采样点：按贴图实际内容动态查找（见 findTintUv）
            float[] tint = findTintUv(tris, texture);
            // 玻璃罩贴图：模型含 usemtl glass 面时使用 <modelId>_glass.png（半透明批次）
            boolean anyGlass = false;
            for (Tri tri : tris) {
                if (tri.glass) {
                    anyGlass = true;
                    break;
                }
            }
            ResourceLocation glassTexture = anyGlass
                    ? new ResourceLocation(CcRc.MODID,
                            "textures/block/console_parts/" + type.modelId + "_glass.png")
                    : null;
            // 贴图里找不到纯白采样块时（如安全按钮的调色板）用 -1 表示「保留原 UV、只乘顶点色」
            float tintU = tint == null ? -1.0F : tint[0];
            float tintV = tint == null ? -1.0F : tint[1];
            return new ObjMesh(tris, minX, maxX, (minY + maxY) / 2.0F, (minZ + maxZ) / 2.0F,
                    texture, tintU, tintV, glassTexture);
        } catch (Exception e) {
            LOGGER.warn("控制台组件模型解析失败：{}", loc, e);
            return null;
        }
    }

    /** 解析一个 f 指令：支持多边形，按扇形三角剖分。 */
    private static void addFace(String[] parts, List<float[]> verts, List<float[]> uvs,
                                List<float[]> normals, List<Tri> tris, boolean tintMaterial,
                                boolean glassMaterial, String group) {
        int count = parts.length - 1;
        if (count < 3) {
            return;
        }
        int[][] indices = new int[count][];
        for (int i = 0; i < count; i++) {
            indices[i] = parseIndex(parts[i + 1], verts.size(), uvs.size(), normals.size());
        }
        for (int i = 1; i < count - 1; i++) {
            tris.add(buildTri(indices[0], indices[i], indices[i + 1], verts, uvs, normals,
                    tintMaterial, glassMaterial, group));
        }
    }

    /** 解析 "v/vt/vn" 形式的索引（1 基，支持负数为相对索引）。 */
    private static int[] parseIndex(String token, int vertCount, int uvCount, int normalCount) {
        String[] pieces = token.split("/", -1);
        int vi = resolve(pieces.length > 0 ? pieces[0] : "", vertCount);
        int ti = resolve(pieces.length > 1 ? pieces[1] : "", uvCount);
        int ni = resolve(pieces.length > 2 ? pieces[2] : "", normalCount);
        return new int[]{vi, ti, ni};
    }

    private static int resolve(String raw, int size) {
        if (raw == null || raw.isEmpty()) {
            return -1;
        }
        int value = Integer.parseInt(raw);
        if (value < 0) {
            value = size + value;
        } else {
            value = value - 1;
        }
        return (value >= 0 && value < size) ? value : -1;
    }

    private static Tri buildTri(int[] a, int[] b, int[] c,
                                List<float[]> verts, List<float[]> uvs, List<float[]> normals,
                                boolean tintMaterial, boolean glassMaterial, String group) {
        Tri tri = new Tri();
        tri.tintable = tintMaterial;
        tri.glass = glassMaterial;
        tri.group = group;
        int[][] idx = {a, b, c};
        for (int i = 0; i < 3; i++) {
            float[] v = verts.get(idx[i][0]);
            tri.pos[i * 3] = v[0];
            tri.pos[i * 3 + 1] = v[1];
            tri.pos[i * 3 + 2] = v[2];
            if (idx[i][1] >= 0) {
                float[] uv = uvs.get(idx[i][1]);
                tri.uv[i * 2] = uv[0];
                tri.uv[i * 2 + 1] = uv[1];
            }
        }
        // 纯色面：三个顶点的 UV 完全相同（模型用「单像素贴图」表示纯色部件，
        // 如流量表的指针）。这类面渲染时要保留原贴图，否则会和表盘彩色区域
        // 一起被染色而看不清（指针与红色区域在贴图上是同一块红色）。
        if (a[1] >= 0 && b[1] >= 0 && c[1] >= 0) {
            float u0 = tri.uv[0];
            float v0 = tri.uv[1];
            tri.solidColor = u0 == tri.uv[2] && v0 == tri.uv[3]
                    && u0 == tri.uv[4] && v0 == tri.uv[5];
        }
        if (a[2] >= 0) {
            float[] n = normals.get(a[2]);
            tri.normal[0] = n[0];
            tri.normal[1] = n[1];
            tri.normal[2] = n[2];
        } else {
            // 没有法线时按三角形两边叉积计算
            float ux = tri.pos[3] - tri.pos[0];
            float uy = tri.pos[4] - tri.pos[1];
            float uz = tri.pos[5] - tri.pos[2];
            float vx = tri.pos[6] - tri.pos[0];
            float vy = tri.pos[7] - tri.pos[1];
            float vz = tri.pos[8] - tri.pos[2];
            tri.normal[0] = uy * vz - uz * vy;
            tri.normal[1] = uz * vx - ux * vz;
            tri.normal[2] = ux * vy - uy * vx;
        }
        return tri;
    }

    private static float f(String raw) {
        return Float.parseFloat(raw);
    }

    /**
     * 查找可染色面使用的「纯白采样块」UV（贴图坐标）。
     *
     * 渲染可染色面时把 UV 指到一块纯白像素，再用顶点色乘出所选颜色。转换模型时已经在
     * 贴图右下角预留了一块 4x4 纯白像素（不被任何面采样）；这里按实际贴图动态查找，
     * 兼容不同尺寸的调色板贴图，避免写死像素坐标。
     *
     * @return {u, v}；贴图里没有可挪用的纯白块时返回 null（见 {@link ObjMesh#tintU}）
     */
    private static float[] findTintUv(List<Tri> tris, ResourceLocation texture) {
        boolean anyTint = false;
        for (Tri tri : tris) {
            if (tri.tintable) {
                anyTint = true;
                break;
            }
        }
        if (!anyTint) {
            // 没有可染色面时该值不会被使用
            return null;
        }
        NativeImage image = null;
        try {
            Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(texture).orElse(null);
            if (resource == null) {
                return null;
            }
            try (InputStream in = resource.open()) {
                image = NativeImage.read(in);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            // 用每个三角形 UV 的包围盒做保守估计，落在包围盒外的像素才可挪用
            boolean[] covered = new boolean[width * height];
            for (Tri tri : tris) {
                float minU = Math.min(tri.uv[0], Math.min(tri.uv[2], tri.uv[4]));
                float maxU = Math.max(tri.uv[0], Math.max(tri.uv[2], tri.uv[4]));
                float minV = Math.min(tri.uv[1], Math.min(tri.uv[3], tri.uv[5]));
                float maxV = Math.max(tri.uv[1], Math.max(tri.uv[3], tri.uv[5]));
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
            // 从右下角向左上扫描：优先 4x4（抗 mipmap 混色），其次 2x2
            for (int size = 4; size >= 2; size -= 2) {
                for (int y = height - size; y >= 0; y--) {
                    for (int x = width - size; x >= 0; x--) {
                        if (!isWhiteBlock(image, covered, width, height, x, y, size)) {
                            continue;
                        }
                        // 取块中心，避免贴图过滤时采到块外像素
                        return new float[]{(x + size / 2.0F) / width,
                                (y + size / 2.0F) / height};
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("控制台组件贴图采样点解析失败：{}", texture, e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
        // 没找到可挪用的纯白块：交给渲染方保留原 UV、只乘顶点色（见 ObjMesh#tintU）
        return null;
    }

    /** 该 size×size 区域是否「未被任何面采样且全部为不透明白色」。 */
    private static boolean isWhiteBlock(NativeImage image, boolean[] covered,
                                        int width, int height, int x0, int y0, int size) {
        for (int dy = 0; dy < size; dy++) {
            for (int dx = 0; dx < size; dx++) {
                int x = x0 + dx;
                int y = y0 + dy;
                if (covered[y * width + x]) {
                    return false;
                }
                int pixel = image.getPixelRGBA(x, y);
                if (((pixel >>> 24) & 0xFF) < 250
                        || (pixel & 0xFF) < 250
                        || ((pixel >> 8) & 0xFF) < 250
                        || ((pixel >> 16) & 0xFF) < 250) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 把像素索引夹到 [0, size-1]。 */
    private static int clampTexel(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    /** 解析后的模型：三角形列表 + 对齐用的包围盒信息。 */
    public static final class ObjMesh {
        public final List<Tri> tris;
        /** 模型最内侧的 x（贴到结构外表面）。 */
        public final float minX;
        /** 模型最外侧的 x（最厚处，附带文字贴在其前方）。 */
        public final float maxX;
        /** 模型 y 中心（对齐锚点）。 */
        public final float centerY;
        /** 模型 z 中心（对齐锚点）。 */
        public final float centerZ;
        public final ResourceLocation texture;
        /** 玻璃罩贴图（模型含 usemtl glass 面时非 null，需用半透明批次绘制）。 */
        public final ResourceLocation glassTexture;
        /**
         * 可染色面使用的纯白采样点 UV（见 findTintUv）。
         * 为 -1 表示贴图里没有可挪用的纯白块：染色面保留原 UV、只用顶点色乘出颜色
         * （安全按钮的调色板就是这种情况，按钮本体的白色区域直接乘色即可）。
         */
        public final float tintU;
        public final float tintV;

        ObjMesh(List<Tri> tris, float minX, float maxX, float centerY, float centerZ,
                ResourceLocation texture, float tintU, float tintV,
                ResourceLocation glassTexture) {
            this.tris = tris;
            this.minX = minX;
            this.maxX = maxX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.texture = texture;
            this.glassTexture = glassTexture;
            this.tintU = tintU;
            this.tintV = tintV;
        }
    }

    /** 一个三角形：位置 / UV / 法线都是平铺数组，避免渲染时产生大量小对象。 */
    public static final class Tri {
        public final float[] pos = new float[9];
        public final float[] uv = new float[6];
        public final float[] normal = new float[3];
        /** 三个顶点 UV 相同（纯色面）：渲染时用原贴图，不参与颜色重新着色。 */
        public boolean solidColor;
        /**
         * 可染色面（底板 / 底座）：设置组件颜色时改用贴图里的白色采样块 + 顶点色，
         * 直接渲染为所选颜色；未设置颜色时仍使用原始贴图 UV，外观不变。
         */
        public boolean tintable;
        /** 玻璃罩面（usemtl glass）：用半透明批次单独绘制。 */
        public boolean glass;
        /** 所属 OBJ 分组名（o 指令），用于指针旋转 / 按钮按下位移等局部动画。 */
        public String group = "";
    }
}
