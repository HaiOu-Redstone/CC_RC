package com.cc_rc.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 便签渲染器：按实体朝向把一张半格大小的贴图四边形画在墙面上，并在纸面绘制四行小字。
 *
 * 朝向基向量（与 GraffitiBER 的四向约定一致，另加顶 / 底面）：
 *  - right  ：观者视角向右；
 *  - up     ：纸面向上（贴墙时即世界向上）；
 *  - normal ：right × up，指向观者（纸面外法线）。
 * 纸面绕上边沿（right 轴）外翻一个角度：上边沿贴住墙面，下边沿离开墙面。
 * 倾斜后的基向量直接解析求出：
 *  - upTilt     = up·cos − normal·sin（向墙内倒）；
 *  - normalTilt = up·sin + normal·cos（朝观者抬起）。
 *
 * 纸面与文字都用这同一组倾斜基向量绘制，因此倾斜角度严格一致；
 * 文字沿 normalTilt 外推，保证位于纸面正面（观者一侧）而不是背面。
 */
public class NoteRenderer extends EntityRenderer<NoteEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("cc_rc", "textures/entity/note.png");
    /** 上边沿铰链相对实体中心沿墙面法线的偏移（负值 = 更贴近墙面）。 */
    private static final float HINGE_OUTSET = -0.026F;
    /** 纸面外翻角度（度）：上沿贴墙、下沿离墙。 */
    private static final float TILT_DEGREES = 14.0F;
    /** 文字沿倾斜法线的外推（格），确保文字在纸面前方且不与纸面共面。 */
    private static final float TEXT_OUTSET = 0.01F;
    /** 全亮光照：墙面暗处文字与纸面颜色也应保持一致。 */
    private static final int PACKED_LIGHT = 0xF000F0;

    public NoteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(NoteEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(NoteEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        Direction direction = entity.getDirection();
        Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        // 观者视角向右 / 纸面向上
        Vec3 right;
        Vec3 up;
        switch (direction) {
            case NORTH -> {
                right = new Vec3(-1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case SOUTH -> {
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case EAST -> {
                right = new Vec3(0.0D, 0.0D, -1.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case WEST -> {
                right = new Vec3(0.0D, 0.0D, 1.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case UP -> {
                // 顶面：从南侧观看时文字正向
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 0.0D, -1.0D);
            }
            default -> {   // DOWN
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 0.0D, 1.0D);
            }
        }

        float angle = (float) Math.toRadians(TILT_DEGREES);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        // 倾斜后的纸面上方向与外法线（绕 right 轴外翻）
        Vec3 upTilt = up.scale(cos).subtract(normal.scale(sin));
        Vec3 normalTilt = up.scale(sin).add(normal.scale(cos));
        // 铰链 = 纸面上边沿中点，紧贴墙面
        Vec3 hinge = up.scale(NoteEntity.NOTE_SIZE / 2.0D).add(normal.scale(HINGE_OUTSET));

        // 实体渲染只做了平移，这里的矩阵即「实体位置」坐标系
        Matrix4f matrix = poseStack.last().pose();
        drawPaper(matrix, bufferSource, hinge, right, upTilt, normalTilt);
        drawLines(entity, matrix, bufferSource, hinge, right, upTilt, normalTilt);
    }

    /** 绘制纸面四边形：上沿经过铰链，向下沿 upTilt 展开。 */
    private static void drawPaper(Matrix4f matrix, MultiBufferSource bufferSource, Vec3 hinge,
                                  Vec3 right, Vec3 upTilt, Vec3 normalTilt) {
        float half = NoteEntity.NOTE_SIZE / 2.0F;
        float size = NoteEntity.NOTE_SIZE;
        Vec3 topLeft = hinge.subtract(right.scale(half));
        Vec3 topRight = hinge.add(right.scale(half));
        Vec3 bottomRight = topRight.subtract(upTilt.scale(size));
        Vec3 bottomLeft = topLeft.subtract(upTilt.scale(size));
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        // 贴图 u=0 在观者左侧，v=0 在纸面上沿
        vertex(vc, matrix, bottomLeft, 0.0F, 1.0F, normalTilt);
        vertex(vc, matrix, bottomRight, 1.0F, 1.0F, normalTilt);
        vertex(vc, matrix, topRight, 1.0F, 0.0F, normalTilt);
        vertex(vc, matrix, topLeft, 0.0F, 0.0F, normalTilt);
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix,
                               Vec3 pos, float u, float v, Vec3 normal) {
        vc.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    /** 绘制四行文字：逐行居中，超宽时自动缩小以适配纸面宽度。 */
    private static void drawLines(NoteEntity entity, Matrix4f matrix,
                                  MultiBufferSource bufferSource, Vec3 hinge,
                                  Vec3 right, Vec3 upTilt, Vec3 normalTilt) {
        Font font = Minecraft.getInstance().font;
        // 四行文字只占纸面中间约 82% 的高度，上下各留白，避免顶 / 底行超出纸面
        float contentHeight = NoteEntity.NOTE_SIZE * 0.82F;
        float lineHeight = contentHeight / NoteEntity.LINES;
        float glyphHeight = lineHeight * 0.82F;
        float topMargin = (NoteEntity.NOTE_SIZE - contentHeight) / 2.0F;
        float maxWidth = NoteEntity.NOTE_SIZE * 0.9F;

        for (int i = 0; i < NoteEntity.LINES; i++) {
            Component text = entity.getLineComponent(i);
            if (text.getString().isEmpty()) {
                continue;
            }
            int textWidth = font.width(text);
            if (textWidth <= 0) {
                continue;
            }
            // 每字体像素对应的世界长度；长文本按纸面宽度等比缩小
            float perPixel = glyphHeight / Math.max(1, font.lineHeight);
            if (textWidth * perPixel > maxWidth) {
                perPixel = maxWidth / textWidth;
            }

            // 行起点 = 铰链 + 向右居中偏移 − 向下偏移 + 沿纸面外法线外推（观者一侧）
            float left = -textWidth * perPixel / 2.0F;
            float top = topMargin + i * lineHeight + (lineHeight - glyphHeight) / 2.0F;
            Vec3 start = hinge.add(right.scale(left))
                    .subtract(upTilt.scale(top))
                    .add(normalTilt.scale(TEXT_OUTSET));

            // 仿射矩阵：字体 x 沿观者向右，字体 y 向下（取 upTilt 的反方向），z 沿纸面外法线
            Matrix4f local = new Matrix4f(
                    (float) (right.x * perPixel), (float) (right.y * perPixel),
                    (float) (right.z * perPixel), 0.0F,                                // 第 1 列：向右
                    (float) (-upTilt.x * perPixel), (float) (-upTilt.y * perPixel),
                    (float) (-upTilt.z * perPixel), 0.0F,                              // 第 2 列：向下
                    (float) (normalTilt.x * perPixel), (float) (normalTilt.y * perPixel),
                    (float) (normalTilt.z * perPixel), 0.0F,                           // 第 3 列：外法线
                    (float) start.x, (float) start.y, (float) start.z, 1.0F);          // 第 4 列：平移
            Matrix4f textMatrix = new Matrix4f(matrix).mul(local);
            // 颜色由组件 Style 提供，这里传白色仅作兜底
            font.drawInBatch(text, 0.0F, 0.0F, 0xFFFFFFFF, false, textMatrix, bufferSource,
                    Font.DisplayMode.POLYGON_OFFSET, 0, PACKED_LIGHT);
        }
    }
}
