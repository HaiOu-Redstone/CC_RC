package com.cc_rc.block.graffiti;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 涂鸦文字方块实体：保存「文字内容 + 颜色 + 大小 + 水平/垂直偏移 + 字体格式」，
 * 供客户端 {@link GraffitiBER} 直接绘制到墙面上。
 *
 * 数据含义：
 *  - text    ：显示的文字（单行，最长 {@link #MAX_TEXT} 字符）；
 *  - color   ：文字颜色（0xRRGGBB）；
 *  - scale   ：文字大小，表示「一行文字占多少格高」（0.5 = 半格）；
 *  - dx      ：水平偏移（单位：格，观者视角向右为正）；
 *  - dy      ：垂直偏移（单位：格，向上为正）；
 *  - bold/italic/underline/strikethrough：字体格式。
 *
 * 同步：getUpdateTag / getUpdatePacket 把配置随方块更新下发客户端；
 * GUI 提交（GraffitiConfigPacket）在服务端改值后由服务端调用 sendBlockUpdated 触发同步。
 */
public class GraffitiBlockEntity extends BlockEntity {

    /** 文字大小（行高，单位格）允许范围。 */
    public static final float MIN_SCALE = 0.05F;
    public static final float MAX_SCALE = 4.0F;
    /** 偏移量绝对值上限（单位：格）。 */
    public static final float MAX_OFFSET = 16.0F;
    /** 文字最大长度（单行）。 */
    public static final int MAX_TEXT = 128;

    /** 默认文字大小：半格行高。 */
    public static final float DEFAULT_SCALE = 0.5F;
    /** 默认文字颜色：白色。 */
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    private String text = "";
    private int color = DEFAULT_COLOR;
    private float scale = DEFAULT_SCALE;
    private float dx;
    private float dy;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;

    public GraffitiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAFFITI_BE.get(), pos, state);
    }

    public String getText() {
        return this.text;
    }

    public int getColor() {
        return this.color;
    }

    public float getScale() {
        return this.scale;
    }

    public float getDx() {
        return this.dx;
    }

    public float getDy() {
        return this.dy;
    }

    public boolean isBold() {
        return this.bold;
    }

    public boolean isItalic() {
        return this.italic;
    }

    public boolean isUnderline() {
        return this.underline;
    }

    public boolean isStrikethrough() {
        return this.strikethrough;
    }

    /** 按当前字体格式构造可绘制的文字组件（BER 绘制用）。 */
    public Component toComponent() {
        Style style = Style.EMPTY
                .withBold(this.bold)
                .withItalic(this.italic)
                .withUnderlined(this.underline)
                .withStrikethrough(this.strikethrough);
        return Component.literal(this.text).withStyle(style);
    }

    /** 设置全部配置（越界值统一夹取），并标记存档需要保存。 */
    public void setConfig(String text, int color, float scale, float dx, float dy,
                          boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        // 单行显示：去掉换行符并截断超长内容
        String normalized = text == null ? "" : text.replace("\n", "").replace("\r", "");
        this.text = normalized.length() > MAX_TEXT ? normalized.substring(0, MAX_TEXT) : normalized;
        this.color = color & 0xFFFFFF;
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                Float.isFinite(scale) ? scale : DEFAULT_SCALE));
        this.dx = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, Float.isFinite(dx) ? dx : 0.0F));
        this.dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, Float.isFinite(dy) ? dy : 0.0F));
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeConfig(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readConfig(tag);
    }

    /** 客户端同步用：数据包/区块更新读取的完整配置（不含位置等元数据）。 */
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 扩大渲染包围盒：文字可远远超出单格（长文本 + 大字号），
     * 若仍用默认 1 格包围盒，视锥剔除会让文字在边缘处凭空消失。
     */
    @Override
    public AABB getRenderBoundingBox() {
        // 粗略估算文字宽度（每字符约 6 字体像素，行高 = scale 格 / 9 像素）
        double perFontPixel = this.scale / 9.0D;
        double textWidth = Math.max(1.0D, this.text.length() * 6.0D * perFontPixel);
        double span = Math.min(256.0D, textWidth) + MAX_OFFSET + 4.0D;
        return new AABB(this.getBlockPos()).inflate(span);
    }

    private void writeConfig(CompoundTag tag) {
        tag.putString("Text", this.text);
        tag.putInt("Color", this.color);
        tag.putFloat("Scale", this.scale);
        tag.putFloat("Dx", this.dx);
        tag.putFloat("Dy", this.dy);
        tag.putBoolean("Bold", this.bold);
        tag.putBoolean("Italic", this.italic);
        tag.putBoolean("Underline", this.underline);
        tag.putBoolean("Strike", this.strikethrough);
    }

    private void readConfig(CompoundTag tag) {
        this.text = tag.getString("Text");
        this.color = tag.contains("Color") ? (tag.getInt("Color") & 0xFFFFFF) : DEFAULT_COLOR;
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                tag.contains("Scale") ? tag.getFloat("Scale") : DEFAULT_SCALE));
        this.dx = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, tag.getFloat("Dx")));
        this.dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, tag.getFloat("Dy")));
        this.bold = tag.getBoolean("Bold");
        this.italic = tag.getBoolean("Italic");
        this.underline = tag.getBoolean("Underline");
        this.strikethrough = tag.getBoolean("Strike");
    }
}
