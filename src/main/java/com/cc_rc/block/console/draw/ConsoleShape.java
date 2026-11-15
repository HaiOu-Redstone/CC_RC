package com.cc_rc.block.console.draw;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * 控制柜表面上的一个绘图图形（不可变，修改位置时生成新对象）。
 *
 * 坐标全部是「表面像素」：以该表面矩形中心为原点，U 向右、V 向上，1 像素 = 1/16 格。
 * 图形不单独保存表面尺寸，结构扩大/缩小时由总线在重新成型时统一平移。
 */
public final class ConsoleShape {

    /** 线宽允许范围（像素，支持一位小数）。 */
    public static final float MIN_WIDTH = 0.1F;
    public static final float MAX_WIDTH = 4.0F;
    /** 图层数量（0 在最底层）。 */
    public static final int LAYERS = 8;
    /** 预设图案允许的缩放范围（支持一位小数）。 */
    public static final float MIN_SCALE = 0.1F;
    public static final float MAX_SCALE = 3.0F;

    /**
     * 把数值量化到 0.1，避免浮点误差在存档 / 网络包 / 界面之间反复累积
     * （例如 0.30000001 之类）。
     */
    public static float quantize(float value) {
        return Math.round(value * 10.0F) / 10.0F;
    }

    /** 图形自增 id（同一总线内唯一，用于选中 / 移动 / 删除）。 */
    public final int id;
    /** 所在表面。 */
    public final Direction face;
    /** 图层（0~7），数字越大越靠外。 */
    public final int layer;
    public final ConsoleShapeType type;
    /** 颜色（0xRRGGBB）。 */
    public final int color;
    /** 线宽（像素，支持一位小数；仅 RECT_OUTLINE / LINE / ARROW 使用）。 */
    public final float width;
    /** 第一点：矩形对角 / 线段起点 / 箭头尾部 / 预设中心。 */
    public final float u1;
    public final float v1;
    /** 第二点：矩形对角 / 线段终点 / 箭头尖端（预设未使用）。 */
    public final float u2;
    public final float v2;
    /** 预设图案 id（仅 PRESET 使用）。 */
    public final String preset;
    /** 预设旋转角度（度，0~359.9，支持一位小数）。 */
    public final float rotation;
    /** 预设放大倍数（0.1~3，支持一位小数）。 */
    public final float scale;

    public ConsoleShape(int id, Direction face, int layer, ConsoleShapeType type, int color,
                        float width, float u1, float v1, float u2, float v2,
                        String preset, float rotation, float scale) {
        this.id = id;
        this.face = face;
        this.layer = layer;
        this.type = type;
        this.color = color & 0xFFFFFF;
        this.width = width;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.preset = preset == null ? "" : preset;
        this.rotation = rotation;
        this.scale = scale;
    }

    /** 生成平移后的新图形（坐标由调用方保证在表面范围内）。 */
    public ConsoleShape translated(float du, float dv) {
        return new ConsoleShape(this.id, this.face, this.layer, this.type, this.color,
                this.width, this.u1 + du, this.v1 + dv, this.u2 + du, this.v2 + dv,
                this.preset, this.rotation, this.scale);
    }

    /**
     * 图形的像素包围盒 {minU, minV, maxU, maxV}。
     * 用于整体平移时保证图形不会移出表面（把线宽 / 箭头头部也算进去）。
     */
    public float[] bounds() {
        float pad = 0.0F;
        if (this.type == ConsoleShapeType.RECT_OUTLINE || this.type == ConsoleShapeType.LINE) {
            pad = this.width / 2.0F;
        } else if (this.type == ConsoleShapeType.ARROW) {
            // 箭头头部半宽为 1.5 倍线宽（与渲染保持一致），平移时按最大外扩留边
            pad = this.width * 1.5F;
        }
        float minU;
        float maxU;
        float minV;
        float maxV;
        if (this.type == ConsoleShapeType.PRESET) {
            float half = presetHalfSize();
            minU = this.u1 - half;
            maxU = this.u1 + half;
            minV = this.v1 - half;
            maxV = this.v1 + half;
        } else {
            minU = Math.min(this.u1, this.u2) - pad;
            maxU = Math.max(this.u1, this.u2) + pad;
            minV = Math.min(this.v1, this.v2) - pad;
            maxV = Math.max(this.v1, this.v2) + pad;
        }
        return new float[]{minU, minV, maxU, maxV};
    }

    /** 预设图案的半边长（像素），未登记 id 时按 0 处理（不可见）。 */
    public float presetHalfSize() {
        ConsoleDrawPresets.Preset preset = ConsoleDrawPresets.byId(this.preset);
        if (preset == null) {
            return 0.0F;
        }
        return preset.size() * ConsoleDrawPresets.DISPLAY_SCALE
                * Math.max(MIN_SCALE, this.scale) / 2.0F;
    }

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", this.id);
        tag.putInt("Face", this.face.get3DDataValue());
        tag.putInt("Layer", this.layer);
        tag.putInt("Type", this.type.ordinal());
        tag.putInt("Color", this.color);
        tag.putFloat("Width", this.width);
        tag.putFloat("U1", this.u1);
        tag.putFloat("V1", this.v1);
        tag.putFloat("U2", this.u2);
        tag.putFloat("V2", this.v2);
        tag.putString("Preset", this.preset);
        tag.putFloat("Rot", this.rotation);
        tag.putFloat("Scale", this.scale);
        return tag;
    }

    /** 从 NBT 读取；字段缺失时使用安全默认值。 */
    public static ConsoleShape load(CompoundTag tag) {
        Direction[] faces = Direction.values();
        int faceOrd = tag.getInt("Face");
        Direction face = faces[Math.max(0, Math.min(faces.length - 1, faceOrd))];
        ConsoleShapeType type = ConsoleShapeType.byOrdinal(tag.getInt("Type"));
        // 线宽 / 倍率统一量化到 0.1 并夹到允许范围（旧存档里的 int 值也能被 getFloat 读取）
        float width = quantize(Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, tag.getFloat("Width"))));
        int layer = Math.max(0, Math.min(LAYERS - 1, tag.getInt("Layer")));
        // 旧存档的旋转是 0~3 的枚举（int），转换为角度；新格式直接存角度
        float rotation = tag.contains("Rot", 3)
                ? tag.getInt("Rot") * 90.0F : tag.getFloat("Rot");
        rotation = quantize((rotation % 360.0F + 360.0F) % 360.0F);
        float scale = quantize(Math.max(MIN_SCALE, Math.min(MAX_SCALE, tag.getFloat("Scale"))));
        return new ConsoleShape(tag.getInt("Id"), face, layer, type, tag.getInt("Color"),
                width, tag.getFloat("U1"), tag.getFloat("V1"), tag.getFloat("U2"),
                tag.getFloat("V2"), tag.getString("Preset"), rotation, scale);
    }
}
