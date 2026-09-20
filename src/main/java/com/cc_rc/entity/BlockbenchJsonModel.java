package com.cc_rc.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blockbench JSON 模型运行时加载器 + 渲染器（生物无动画，直接按 json 几何绘制）。
 *
 * 来源：Blockbench 导出的 *.json（format_version 1.9.0，面级独立 UV）：
 *  - elements[].from / to：盒的 AABB 角点（像素，16px=1 方块）；
 *  - elements[].rotation：绕 origin 沿 axis 轴旋转 angle 度（顶点先旋转再渲染）；
 *  - elements[].faces.<方向>.uv：该面在贴图上的像素区域 [u1, v1, u2, v2]
 *    （除以 texture_size 归一化为 0..1 纹理坐标）；
 *  - 贴图由外层 EntityRenderer.getTextureLocation 绑定（配合 RenderType.entity* 层）。
 *
 * 渲染方式：逐元素逐面绘制 quad（VertexConsumer 手动传点），ISE 无动画/无模型部件要求；
 * 法线按面方向（旋转元素先经旋转矩阵变换），支持任意角度旋转盒与透明镂空（无背面剔除）。
 */
public class BlockbenchJsonModel {

    /** 单个盒元素 */
    static class BbElement {
        float[] from = new float[3];
        float[] to = new float[3];
        boolean rotated = false;
        float angle = 0F;
        String axis = "y";
        float[] origin = new float[3];
        /** 方向名 -> uv[4]（像素） */
        Map<String, float[]> faces = new LinkedHashMap<>();
    }

    private final List<BbElement> elements;
    private final float texWidth;
    private final float texHeight;
    /** 模型几何包围盒：x/z 中心与 y 最小值（像素），渲染时平移使其居中于实体原点并贴地 */
    private final float centerX;
    private final float minY;
    private final float centerZ;

    private BlockbenchJsonModel(List<BbElement> elements, float texWidth, float texHeight,
                                float centerX, float minY, float centerZ) {
        this.elements = elements;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.centerX = centerX;
        this.minY = minY;
        this.centerZ = centerZ;
    }

    /**
     * 从资源包加载 Blockbench json（贴图单独由渲染器绑定）。
     *
     * @param rm      客户端 ResourceManager
     * @param jsonLoc json 的资源路径，如 cc_rc:models/entity/evil_gajin.json
     * @throws IOException 读取失败
     */
    public static BlockbenchJsonModel load(ResourceManager rm, ResourceLocation jsonLoc) throws IOException {
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(
                rm.getResource(jsonLoc).orElseThrow(
                        () -> new IOException("Blockbench model not found: " + jsonLoc)).open())) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        float texW = 64F;
        float texH = 64F;
        JsonArray ts = root.has("texture_size") ? root.getAsJsonArray("texture_size") : null;
        if (ts != null && ts.size() >= 2) {
            texW = ts.get(0).getAsFloat();
            texH = ts.get(1).getAsFloat();
        }
        List<BbElement> list = new ArrayList<>();
        JsonArray arr = root.has("elements") ? root.getAsJsonArray("elements") : null;
        // 遍历元素统计几何包围盒（渲染时用于居中+贴地）
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        if (arr != null) {
            for (JsonElement e : arr) {
                BbElement el = parseElement(e.getAsJsonObject());
                list.add(el);
                minX = Math.min(minX, el.from[0]); maxX = Math.max(maxX, el.to[0]);
                minY = Math.min(minY, el.from[1]); maxY = Math.max(maxY, el.to[1]);
                minZ = Math.min(minZ, el.from[2]); maxZ = Math.max(maxZ, el.to[2]);
            }
        }
        return new BlockbenchJsonModel(list, texW, texH,
                (minX + maxX) / 2F, minY, (minZ + maxZ) / 2F);
    }

    private static BbElement parseElement(JsonObject o) {
        BbElement el = new BbElement();
        JsonArray from = o.getAsJsonArray("from");
        JsonArray to = o.getAsJsonArray("to");
        for (int i = 0; i < 3; i++) {
            el.from[i] = from.get(i).getAsFloat();
            el.to[i] = to.get(i).getAsFloat();
        }
        if (o.has("rotation")) {
            JsonObject rot = o.getAsJsonObject("rotation");
            el.angle = rot.get("angle").getAsFloat();
            el.axis = rot.has("axis") ? rot.get("axis").getAsString() : "y";
            JsonArray ori = rot.has("origin") ? rot.getAsJsonArray("origin") : null;
            if (ori != null && ori.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    el.origin[i] = ori.get(i).getAsFloat();
                }
            } else {
                // 缺省 origin = 元素中心
                for (int i = 0; i < 3; i++) {
                    el.origin[i] = (el.from[i] + el.to[i]) / 2F;
                }
            }
            el.rotated = el.angle != 0F;
        }
        if (o.has("faces")) {
            JsonObject faces = o.getAsJsonObject("faces");
            for (String dir : new String[]{"north", "east", "south", "west", "up", "down"}) {
                if (faces.has(dir)) {
                    JsonArray uv = faces.getAsJsonObject(dir).getAsJsonArray("uv");
                    float[] uvArr = new float[4];
                    for (int i = 0; i < 4; i++) {
                        uvArr[i] = uv.get(i).getAsFloat();
                    }
                    el.faces.put(dir, uvArr);
                }
            }
        }
        return el;
    }

    /**
     * 绘制整个模型。
     *
     * @param pose        位置/姿态（已被渲染器 push 到实体坐标并按 yRot 旋转）
     * @param buffer      顶点消费者（RenderType.entityCutoutNoCull 等）
     * @param packedLight 打包光照
     * @param packedOverlay 打包遮罩（通常 OverlayTexture.NO_OVERLAY）
     * @param r/g/b/a     顶点色（实体渲染通常白色 1,1,1,1）
     */
    public void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay,
                       float r, float g, float b, float a) {
        pose.pushPose();
        // 像素坐标→格（16px=1 格）：平移使模型包围盒中心落在实体原点（脚底中心）并贴地。
        // PoseStack 后写先作用：此平移先于调用方施加的 yaw 旋转，模型中心保持在原点旋转。
        pose.translate(-centerX / 16.0F, -minY / 16.0F, -centerZ / 16.0F);
        PoseStack.Pose p = pose.last();
        Matrix4f mat = p.pose();
        Matrix3f normMat = p.normal();
        for (BbElement el : elements) {
            renderElement(el, mat, normMat, buffer, packedLight, packedOverlay, r, g, b, a);
        }
        pose.popPose();
    }

    private void renderElement(BbElement el, Matrix4f mat, Matrix3f normMat, VertexConsumer buffer,
                               int packedLight, int packedOverlay, float r, float g, float b, float a) {
        // 8 个角点（先应用元素旋转：v' = R·(v - origin) + origin）
        Vector3f[] corners = new Vector3f[8];
        Matrix3f rot = null;
        if (el.rotated) {
            // Matrix3f.rotation(angle, axis)：angle 为弧度、axis 为旋转轴
            rot = new Matrix3f().rotation((float) Math.toRadians(el.angle), toAxisAngle(el));
        }
        int idx = 0;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    float x = xi == 0 ? el.from[0] : el.to[0];
                    float y = yi == 0 ? el.from[1] : el.to[1];
                    float z = zi == 0 ? el.from[2] : el.to[2];
                    Vector3f v = new Vector3f(x, y, z);
                    if (rot != null) {
                        v.sub(el.origin[0], el.origin[1], el.origin[2]);
                        v.mul(rot);
                        v.add(el.origin[0], el.origin[1], el.origin[2]);
                    }
                    corners[idx++] = v;
                }
            }
        }
        // 索引表：corner index = xi*4 + yi*2 + zi (xi in 0..1, yi in 0..1, zi in 0..1)
        for (Map.Entry<String, float[]> entry : el.faces.entrySet()) {
            String dir = entry.getKey();
            float[] uv = entry.getValue();
            if (uv.length < 4) {
                continue;
            }
            int[] cornerIdx = FACE_CORNERS.get(dir);
            Vector3f[] fn = FACE_NORMALS.get(dir);
            if (cornerIdx == null) {
                continue;
            }
            // 面 4 顶点顺序（外看左上→右上→右下→左下），对应 uv 四角。
            // 依据原版 ModelPart$Cube 的 Polygon UV 映射（反编译字节码验证）：
            // 6 个面统一 (u1,v1)=贴图左上↔外看左上、(u2,v2)=贴图右下↔外看右下，
            // up/down 面贴图上方同样朝向模型 +Z（南），无需额外翻转 v。
            // UV 归一化基准：该模型（gaijin_t58）的每面 uv 数值基于 16 像素网格
            // （texture_size 为 32×32 但 uv 值 x/z 恰好为模型像素一半，即 1 纹理单位=2 模型像素），
            // 若按 texture_size=32 归一化则采样区域减半 → 贴图像素被放大 2 倍（颗粒明显变大）。
            // 故统一除以 16；侧面 v（模型高度）方向作者未严格按比例，与 Blockbench 预览一致。
            float u0 = uv[0] / 16.0F;
            float v0 = uv[1] / 16.0F;
            float u1 = uv[2] / 16.0F;
            float v1 = uv[3] / 16.0F;
            float[][] uvCorners = new float[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            // 法线（旋转元素随旋转矩阵变换）
            Vector3f n = new Vector3f(fn[0]);
            if (rot != null) {
                n.mul(rot);
            }
            float nx = n.x();
            float ny = n.y();
            float nz = n.z();
            for (int c = 0; c < 4; c++) {
                Vector3f corner = corners[cornerIdx[c]];
                // 法线先随元素旋转（模型空间），再经 pose.normal() 变换到观察坐标
                Vector3f vn = normMat.transform(new Vector3f(nx, ny, nz));
                // 顶点链顺序必须匹配 entityCutoutNoCull 的 VertexFormat：POSITION→COLOR→UV0→
                // UV1(overlay)→UV2(light)→NORMAL；BufferVertexConsumer 按元素游标推进，顺序错
                // 会导致后续元素写入错位/跳过（如 light 不写 → 全黑/不可见）。
                // 官方映射名：光照=uv2(int)（UV2）、覆盖层=overlayCoords(int)（UV1），均先于 normal。
                // 坐标必须从 Blockbench 像素换算为格：16px=1 格（原版 ModelPart$Cube.compile
                // 同样先 vertex.pos /16F 再提交；不除则模型被渲染成 16 倍大）。
                buffer.vertex(mat, corner.x() / 16.0F, corner.y() / 16.0F, corner.z() / 16.0F)
                        .color(r, g, b, a)
                        .uv(uvCorners[c][0], uvCorners[c][1])
                        .overlayCoords(packedOverlay)
                        .uv2(packedLight)
                        .normal(vn.x(), vn.y(), vn.z())
                        .endVertex();
            }
        }
    }

    private static Vector3f toAxisAngle(BbElement el) {
        // 返回旋转轴向量（角度存 el.angle，弧度在这里换算）
        switch (el.axis) {
            case "x": return new Vector3f(1, 0, 0);
            case "y": return new Vector3f(0, 1, 0);
            case "z": return new Vector3f(0, 0, 1);
            default:  return new Vector3f(0, 1, 0);
        }
    }

    /** 每面 4 个角点索引（对应 8 角数组，按 xi/y/zi 二进制 4/2/1 编码），顺序=外看左上→右上→右下→左下 */
    private static final Map<String, int[]> FACE_CORNERS = new LinkedHashMap<>();
    /** 每面朝向法线（仅一个元素，旋转时变换） */
    private static final Map<String, Vector3f[]> FACE_NORMALS = new LinkedHashMap<>();

    static {
        // corner index = xi*4 + yi*2 + zi
        //   xi=0/1 → from.x/to.x；yi=0/1 → from.y/to.y；zi=0/1 → from.z/to.z
        // 顶点顺序 = 从面外侧观察的 左上→右上→右下→左下
        // 角点顺序（左上→右上→右下→左下）依据 Blockbench 内部面数据（cube faces 顺序 +
        // UV 角点索引）逐角点验证：east/west/up/down 与常见"外看"直觉不完全一致，
        // 以 Blockbench 导出顺序为准（如 up 面贴图上方朝向 -Z、u 小端朝向 +X）。
        FACE_CORNERS.put("north", new int[]{2, 6, 4, 0});
        FACE_CORNERS.put("south", new int[]{7, 3, 1, 5});
        FACE_CORNERS.put("east",  new int[]{6, 7, 5, 4});
        FACE_CORNERS.put("west",  new int[]{3, 2, 0, 1});
        FACE_CORNERS.put("up",    new int[]{6, 2, 3, 7});
        FACE_CORNERS.put("down",  new int[]{5, 1, 0, 4});

        FACE_NORMALS.put("north", new Vector3f[]{new Vector3f(0, 0, -1)});
        FACE_NORMALS.put("south", new Vector3f[]{new Vector3f(0, 0, 1)});
        FACE_NORMALS.put("west",  new Vector3f[]{new Vector3f(-1, 0, 0)});
        FACE_NORMALS.put("east",  new Vector3f[]{new Vector3f(1, 0, 0)});
        FACE_NORMALS.put("up",    new Vector3f[]{new Vector3f(0, 1, 0)});
        FACE_NORMALS.put("down",  new Vector3f[]{new Vector3f(0, -1, 0)});
    }
}