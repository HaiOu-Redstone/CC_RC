package com.cc_rc.block.graffiti;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 涂鸦形状方块实体：保存「图案类型 + 预设贴图 + 颜色 + 大小 + 线宽 + 旋转 + 水平/垂直偏移」，
 * 供客户端 {@link GraffitiShapeBER} 直接绘制到墙面上。
 *
 * 数据含义：
 *  - type      ：图形类型（预设图案 / 实心矩形 / 空心矩形 / 线段 / 箭头）；
 *  - preset    ：预设图案 id（仅 PRESET 使用，见 ConsoleDrawPresets）；
 *  - color     ：颜色（0xRRGGBB）；
 *  - size      ：大小（图案边长，单位格，1.0 = 一格；0.1~16.0）；
 *  - width     ：线宽（表面像素，仅空心矩形 / 线段 / 箭头使用，0.1~4.0）；
 *  - rotation  ：旋转角度（度，0~359.9，绕图案中心）；
 *  - dx / dy   ：水平 / 垂直偏移（单位格，观者视角向右 / 向上为正）。
 *
 * 同步：getUpdateTag / getUpdatePacket 把配置随方块更新下发客户端；
 * GUI 提交（GraffitiShapeConfigPacket）在服务端改值后由服务端调用 sendBlockUpdated 触发同步。
 */
public class GraffitiShapeBlockEntity extends BlockEntity {

    /** 图案大小（边长，单位格）允许范围：最大 16 格。 */
    public static final float MIN_SIZE = 0.1F;
    public static final float MAX_SIZE = 16.0F;
    /** 偏移量绝对值上限（单位：格）。 */
    public static final float MAX_OFFSET = 16.0F;
    /** 预设 id 最大长度。 */
    public static final int MAX_PRESET_LENGTH = 64;

    /** 默认图案：默认预设，大小 1 格，白色，线宽 1 像素。 */
    public static final ConsoleShapeType DEFAULT_TYPE = ConsoleShapeType.PRESET;
    public static final String DEFAULT_PRESET = ConsoleDrawPresets.DEFAULT_ID;
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public static final float DEFAULT_SIZE = 1.0F;
    public static final float DEFAULT_WIDTH = 1.0F;

    private ConsoleShapeType type = DEFAULT_TYPE;
    private String preset = DEFAULT_PRESET;
    private int color = DEFAULT_COLOR;
    private float size = DEFAULT_SIZE;
    private float width = DEFAULT_WIDTH;
    private float rotation;
    private float dx;
    private float dy;

    public GraffitiShapeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAFFITI_SHAPE_BE.get(), pos, state);
    }

    public ConsoleShapeType getShapeType() {
        return this.type;
    }

    public String getPreset() {
        return this.preset;
    }

    public int getColor() {
        return this.color;
    }

    public float getSize() {
        return this.size;
    }

    public float getWidth() {
        return this.width;
    }

    public float getRotation() {
        return this.rotation;
    }

    public float getDx() {
        return this.dx;
    }

    public float getDy() {
        return this.dy;
    }

    /** 设置全部配置（越界值统一夹取），并标记存档需要保存。 */
    public void setConfig(int typeOrdinal, String preset, int color, float size, float width,
                          float rotation, float dx, float dy) {
        this.type = ConsoleShapeType.byOrdinal(typeOrdinal);
        String normalized = preset == null ? "" : preset;
        this.preset = normalized.length() > MAX_PRESET_LENGTH
                ? normalized.substring(0, MAX_PRESET_LENGTH) : normalized;
        if (ConsoleDrawPresets.byId(this.preset) == null) {
            this.preset = DEFAULT_PRESET;
        }
        this.color = color & 0xFFFFFF;
        this.size = clampSize(size);
        this.width = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH,
                        Float.isFinite(width) ? width : DEFAULT_WIDTH)));
        // 旋转角度归一到 [0, 360) 并量化到 0.1 度
        float rot = Float.isFinite(rotation) ? rotation : 0.0F;
        this.rotation = ConsoleShape.quantize((rot % 360.0F + 360.0F) % 360.0F);
        this.dx = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, Float.isFinite(dx) ? dx : 0.0F));
        this.dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, Float.isFinite(dy) ? dy : 0.0F));
        this.setChanged();
    }

    private static float clampSize(float value) {
        float v = Float.isFinite(value) ? value : DEFAULT_SIZE;
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, v));
    }

    /**
     * 构造供渲染器使用的图形（像素坐标以图案中心为原点，1 像素 = 1/16 格）。
     *
     * 旋转不写进 ConsoleShape（它的 rotation 只对 PRESET 生效），
     * 而是由渲染器在像素平面上统一旋转，这样所有类型都能任意角度旋转。
     */
    public ConsoleShape toShape() {
        Direction face = this.getBlockState().getBlock() instanceof GraffitiShapeBlock
                ? this.getBlockState().getValue(GraffitiShapeBlock.FACING) : Direction.NORTH;
        float half = this.size * 8.0F;
        float u1;
        float v1;
        float u2;
        float v2;
        if (this.type == ConsoleShapeType.PRESET) {
            // 预设图案由 renderPreset 按贴图尺寸自行计算半边长，顶点用中心即可
            u1 = 0.0F;
            v1 = 0.0F;
            u2 = 0.0F;
            v2 = 0.0F;
        } else if (this.type == ConsoleShapeType.RECT
                || this.type == ConsoleShapeType.RECT_OUTLINE) {
            u1 = -half;
            v1 = -half;
            u2 = half;
            v2 = half;
        } else {
            // 线段 / 箭头：水平方向，从 -half 到 +half，整体绕中心旋转
            u1 = -half;
            v1 = 0.0F;
            u2 = half;
            v2 = 0.0F;
        }
        return new ConsoleShape(0, face, 0, this.type, this.color, this.width,
                u1, v1, u2, v2, this.preset, 0.0F, this.size);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Type", this.type.ordinal());
        tag.putString("Preset", this.preset);
        tag.putInt("Color", this.color);
        tag.putFloat("Size", this.size);
        tag.putFloat("Width", this.width);
        tag.putFloat("Rot", this.rotation);
        tag.putFloat("Dx", this.dx);
        tag.putFloat("Dy", this.dy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.type = ConsoleShapeType.byOrdinal(tag.getInt("Type"));
        this.preset = tag.getString("Preset");
        if (ConsoleDrawPresets.byId(this.preset) == null) {
            this.preset = DEFAULT_PRESET;
        }
        this.color = tag.contains("Color") ? (tag.getInt("Color") & 0xFFFFFF) : DEFAULT_COLOR;
        this.size = clampSize(tag.contains("Size") ? tag.getFloat("Size") : DEFAULT_SIZE);
        this.width = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH,
                        tag.contains("Width") ? tag.getFloat("Width") : DEFAULT_WIDTH)));
        this.rotation = ConsoleShape.quantize(
                (tag.getFloat("Rot") % 360.0F + 360.0F) % 360.0F);
        this.dx = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, tag.getFloat("Dx")));
        this.dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, tag.getFloat("Dy")));
    }

    /** 客户端同步用：数据包/区块更新读取的完整配置。 */
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 图案可以超出单格（大尺寸 / 大偏移），扩大渲染包围盒避免视锥剔除时凭空消失。 */
    @Override
    public AABB getRenderBoundingBox() {
        double span = this.size * 1.5D + Math.abs(this.dx) + Math.abs(this.dy) + 2.0D;
        return new AABB(this.getBlockPos()).inflate(span);
    }
}
