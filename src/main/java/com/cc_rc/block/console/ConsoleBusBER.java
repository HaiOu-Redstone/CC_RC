package com.cc_rc.block.console;

import com.cc_rc.client.ConsoleCompModels;
import com.cc_rc.client.ConsoleCompTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 控制台总线方块实体渲染器：绘制整个结构表面上的全部文字。
 *
 * 文字数据都保存在总线方块实体里（一个结构一个数据源），渲染时以
 * {@link ConsoleSurface} 提供的表面坐标系（U = 右、V = 上、N = 外法线）
 * 构造一个仿射矩阵，把「像素」单位的文字直接贴到结构表面上：
 *  - 矩阵第 0 列 = U / 16（横向每像素 1/16 格）；
 *  - 矩阵第 1 列 = -V / 16（原版字体 y 轴向下，故取 V 的反向）；
 *  - 矩阵第 2 列 = N / 16；
 *  - 平移 = 文字中心的世界坐标（沿外法线略微外推，防止深度冲突）。
 *
 * 文字以 (u, v) 为中心绘制，因此「右键文字覆盖区域任意一点」即可再次编辑。
 */
public class ConsoleBusBER implements BlockEntityRenderer<ConsoleBusBlockEntity> {

    /**
     * 全亮光照（天空 15 / 方块 15）。
     * 表面文字若使用方块实体传入的 packedLight，当总线/表面处在无光位置
     * （贴着结构、室内暗处）时亮度为 0，文字会整体渲染成黑色；这里固定全亮，
     * 保证文字颜色始终正确显示（与本模组数字显示器/记录仪的做法一致）。
     */
    private static final int PACKED_LIGHT = 0xF000F0;

    /**
     * 组件模型相对方块表面的外推距离（格）。
     *
     * 所有 _surface 模型的最内侧 x 都是 0（即设计上就贴在面板表面），而控制台方块是
     * 完整立方体，其外表面与模型最内侧平面完全重合。两个面共面时插值出来的深度值
     * 只差几个 ULP，会与方块面互相抢深度，表现为密集条纹 / 撕裂花屏。
     * 这里统一沿外法线外推一点点（0.002 格 ≈ 1/500 格），对所有 OBJ 模型通用，
     * 不需要改动模型资源，后续新增模型也不会再出现该问题。
     */
    private static final double COMP_OUTSET = 0.002D;

    /**
     * 可染色面（底板 / 底座）使用的「纯白采样点」由模型加载时按贴图动态解析
     * （见 {@link ConsoleCompModels.ObjMesh#tintU}）：渲染可染色面时把 UV 指到那块
     * 纯白像素，再用顶点色乘出所选颜色，既保证显示为准确的所选颜色，
     * 又不需要为每种颜色重新生成贴图。
     */

    /** 按钮面板按下时，Button 分组沿外法线向内（模型 -x）的位移（模型单位）。 */
    private static final float KEYPAD_PRESS_DEPTH = 0.03F;

    /**
     * 旋转按钮「复位」（未按下）时 Rotate 分组的旋转枢轴（模型 y / z 坐标）。
     * 取旋转部件所在面板的几何中心；给出的模型烘焙为「按下」状态，
     * 未按下时整个分组绕面板法线（模型 x 轴）顺时针旋转 90°。
     */
    private static final float BUTTON_X_PIVOT_Y = 0.5F;
    private static final float BUTTON_X_PIVOT_Z = 0.5F;

    /** 弧形仪表模型中的指针烘焙数值（gauge_buttons.obj 的指针位于 35% 位置）。 */
    private static final float ARC_BAKED_VALUE = 35.0F;
    /** 弧形仪表每 1% 数值对应的指针转角（度）：0% = -60°，100% = +60°。 */
    private static final float ARC_DEG_PER_PERCENT = 1.2F;
    /** 弧形仪表指针的旋转枢轴（转换后的模型坐标）。 */
    private static final float ARC_PIVOT_Y = 0.4344F;
    private static final float ARC_PIVOT_Z = 0.5F;

    /** 流量表指针枢轴（模型坐标 y / z；刻度圆弧的圆心）。 */
    private static final float GAUGE_PIVOT_Y = 0.4375F;
    private static final float GAUGE_PIVOT_Z = -0.125F;
    /** 流量表每 1% 数值对应的指针转角（度）：刻度只占一个四分之一圆弧，全程 90°。 */
    private static final float GAUGE_DEG_PER_PERCENT = 0.9F;
    /**
     * 流量表 0% 时的指针转角（度）。
     * 模型里指针烘焙为「垂直向上」（对应刻度弧的竖直端，即 100%）；
     * 0% 应指向刻度弧的水平端（模型 +z，屏幕左侧），因此先转 -90°，
     * 再随数值顺时针扫回竖直端：数值增大时指针从左下水平位扫到顶部竖直位。
     */
    private static final float GAUGE_START_DEG = -90.0F;

    /**
     * 立式仪表箭头在中央竖槽内的半行程（模型坐标，单位格）。
     * 按截图标定：刻度 0~100 在模型中占 0.64 格，指针居中于 50%，
     * 因此半行程取 0.32（原 0.26 只扫到刻度约 10~92，两端对不上刻度线）。
     */
    private static final float GAUGE_V_HALF_TRAVEL = 0.32F;

    /**
     * 扇形仪表（gauge_f）指针在模型 (z, y) 平面上的烘焙角度（度）。
     * 模型里的指针指向右下方约 17°，渲染时以它为基准旋转。
     */
    private static final float GAUGE_F_BAKED_DEG = -17.0F;
    /** 扇形仪表数值 0 时指针的目标角度：左下 30°（模型空间 -30°）。 */
    private static final float GAUGE_F_START_DEG = -30.0F;
    /** 扇形仪表每 1% 数值对应的指针转角：0 → 100 顺时针扫过 240°。 */
    private static final float GAUGE_F_DEG_PER_PERCENT = 2.4F;
    /** 扇形仪表指针的旋转枢轴（转换后的模型坐标，源模型 0.5, 0.5, -0.1875）。 */
    private static final float GAUGE_F_PIVOT_Y = 0.5F;
    private static final float GAUGE_F_PIVOT_Z = 0.5F;

    /**
     * 同步指示器指针的烘焙角度（度，约定 0 = 正上方、正 = 顺时针）。
     * 按指针几何精确反算：针根中点 (y=0.5, z=0.5)、针尖中点 (y≈0.4251, z≈0.7451)，
     * 指针实际指向 +Z 偏下约 107°，故烘焙角取 -107（此前 -102.5 会让 0 位指针偏左约 4.5°）。
     */
    private static final float SYNCHRO_BAKED_DEG = -107.0F;
    /** 同步指示器指针的旋转枢轴（转换后的模型坐标，源模型 0.5, 0.5）。 */
    private static final float SYNCHRO_PIVOT_Y = 0.5F;
    private static final float SYNCHRO_PIVOT_Z = 0.5F;

    /** 指针 / 数值显示的允许范围：略超量程便于玩家观察，但不至于跑出面板。 */
    private static final float DISPLAY_MIN = -5.0F;
    private static final float DISPLAY_MAX = 105.0F;

    /** 指示灯熄灯时颜色变暗的系数（与流量表右上角暗部比例接近）。 */
    private static final float INDICATOR_OFF_FACTOR = 0.38F;
    /**
     * 指示灯未设置颜色时熄灯的顶点色灰度（255 × {@link #INDICATOR_OFF_FACTOR}）。
     * 未着色时直接使用原贴图，只能在渲染时用顶点色把灯面压暗。
     */
    private static final int INDICATOR_OFF_GRAY = 97;

    /**
     * 数字键盘（numpad）屏幕可绘制区域（结构像素，取自模型说明：宽 9.6、高 1.9，
     * 屏幕中心 y = 13.35，组件中心 y = 8）。
     */
    private static final float KEYPAD_SCREEN_W = 9.6F;
    private static final float KEYPAD_SCREEN_H = 1.9F;
    /** 屏幕中心相对组件锚点的垂直偏移（结构像素）。 */
    private static final float KEYPAD_SCREEN_V = 5.35F;
    /**
     * 屏幕前平面（设计单位，1/16 格）：模型屏幕在 2.975 处，
     * 数字画在略靠前的位置（2.99），既不穿进玻璃也不跑到外框之外。
     */
    private static final float KEYPAD_SCREEN_X = 2.99F;
    /** 数字键盘未设置颜色时的数字颜色（LED 橙色）。 */
    private static final int KEYPAD_DEFAULT_COLOR = 0xFFFFA040;

    /**
     * 组件附带文字与组件矩形之间的间距（结构像素）。
     * 右侧 / 下方排布的文字留一点空隙，避免紧贴模型边缘。
     */
    private static final float COMP_TEXT_GAP = 1.0F;

    public ConsoleBusBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleBusBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 未成型时没有结构范围，无法定位文字/组件
        if (be.getOrigin() == null || be.getFormState() == ConsoleBusBlockEntity.State.UNFORMED) {
            return;
        }
        // 绘图层先画（位于组件下方），再画组件、读数与文字
        ConsoleDrawRenderer.render(be, poseStack, bufferSource);
        renderComponents(be, be.getComps(), null, poseStack, bufferSource);
        renderCompReadouts(be, be.getComps(), null, poseStack, bufferSource);
        renderCompTexts(be, be.getComps(), null, poseStack, bufferSource);
        if (be.getTexts().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        for (ConsoleBusBlockEntity.TextEntry entry : be.getTexts()) {
            drawText(poseStack, bufferSource, font, frameOf(be, entry.face, null),
                    entry.u, entry.v,
                    entry.anchorH, entry.anchorV, entry.scale,
                    ConsoleSurface.TEXT_OUTSET, entry.toComponent(), 0xFFFFFFFF);
        }
    }

    /**
     * 斜面控制台的渲染入口：用斜面坐标系绘制「宿主格子内」的组件 / 读数 / 文字 / 绘图。
     *
     * 数据仍取自总线（按宿主面 + 坐标过滤），这里只是换了一套绘制坐标系，
     * 因此组件与文字会自动随坡面倾斜。
     */
    public static void renderMapped(ConsoleBusBlockEntity be, ConsoleFrame frame,
                                    List<ConsoleBusBlockEntity.CompEntry> comps,
                                    List<ConsoleBusBlockEntity.TextEntry> texts,
                                    List<com.cc_rc.block.console.draw.ConsoleShape> shapes,
                                    PoseStack poseStack, MultiBufferSource bufferSource) {
        if (be.getOrigin() == null) {
            return;
        }
        ConsoleDrawRenderer.renderInFrame(frame, shapes, poseStack, bufferSource);
        renderComponents(be, comps, frame, poseStack, bufferSource);
        renderCompReadouts(be, comps, frame, poseStack, bufferSource);
        renderCompTexts(be, comps, frame, poseStack, bufferSource);
        if (texts.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        for (ConsoleBusBlockEntity.TextEntry entry : texts) {
            drawText(poseStack, bufferSource, font, frame,
                    entry.u, entry.v,
                    entry.anchorH, entry.anchorV, entry.scale,
                    ConsoleSurface.TEXT_OUTSET, entry.toComponent(), 0xFFFFFFFF);
        }
    }

    /**
     * 取某个面的绘制坐标系。
     *
     * @param fixed 非 null 时直接使用（斜面渲染：所有内容共用斜面坐标系）
     */
    private static ConsoleFrame frameOf(ConsoleBusBlockEntity be, Direction face,
                                        ConsoleFrame fixed) {
        if (fixed != null) {
            return fixed;
        }
        return ConsoleFrame.ofFace(be.getBlockPos(), be.getOrigin(),
                be.getSizeX(), be.getSizeY(), be.getSizeZ(), face, be.getBusFacing());
    }

    /**
     * 在结构表面绘制一条文字。
     *
     * 局部坐标系：x = 文字向右，y = 文字向下（原版字体坐标），z = 外法线；
     * 每字体像素对应的世界长度 = scale / 16（1 结构像素 = 1/16 格）。
     *
     * @param anchorH 水平锚点：0 左 / 1 中 / 2 右
     * @param anchorV 垂直锚点：0 上 / 1 中 / 2 下
     * @param outset  沿外法线外推的距离（避免与方块表面/组件模型共面）
     * @param color   文字颜色（ARGB，0xFFFFFFFF = 白色）
     */
    private static void drawText(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                 ConsoleFrame frame,
                                 float u, float v, int anchorH, int anchorV, float scale,
                                 double outset, Component text, int color) {
        drawTextScaled(poseStack, bufferSource, font, frame,
                u, v, anchorH, anchorV, scale, scale,
                outset, text, color);
    }

    /**
     * 同 {@link #drawText}，但水平 / 垂直方向可用不同缩放（用于圆盘记录仪的细长柱）。
     *
     * @param scaleU 水平方向：1 字体像素 = scaleU 结构像素
     * @param scaleV 垂直方向：1 字体像素 = scaleV 结构像素
     */
    private static void drawTextScaled(PoseStack poseStack, MultiBufferSource bufferSource,
                                       Font font, ConsoleFrame frame,
                                       float u, float v, int anchorH, int anchorV,
                                       float scaleU, float scaleV,
                                       double outset, Component text, int color) {
        if (text.getString().isEmpty()) {
            return;
        }
        // 文字中心相对渲染方块的坐标（沿外法线外推，避免与方块面深度冲突）
        Vec3 center = frame.toLocal(u, v, outset);

        double perFontPixelU = scaleU / ConsoleSurface.PIXELS_PER_BLOCK;
        double perFontPixelV = scaleV / ConsoleSurface.PIXELS_PER_BLOCK;
        Vec3 uAxis = frame.u().scale(perFontPixelU);
        Vec3 vAxis = frame.v().scale(-perFontPixelV);
        Vec3 nAxis = frame.n().scale(perFontPixelU);

        // 用 16 参数构造（JOML 的矩阵字段并非公开，不能直接赋值）
        Matrix4f local = new Matrix4f(
                (float) uAxis.x(), (float) uAxis.y(), (float) uAxis.z(), 0.0F,
                (float) vAxis.x(), (float) vAxis.y(), (float) vAxis.z(), 0.0F,
                (float) nAxis.x(), (float) nAxis.y(), (float) nAxis.z(), 0.0F,
                (float) center.x, (float) center.y, (float) center.z, 1.0F);

        Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);

        // 按锚点计算文字框左上角相对锚点的偏移（字体坐标 y 轴向下）
        // 注意：packedLight 必须用全亮，否则无光位置会渲染成黑色
        float width = font.width(text);
        float height = font.lineHeight;
        float x0 = switch (anchorH) {
            case 0 -> 0.0F;              // 左对齐：锚点在左边
            case 2 -> -width;            // 右对齐：锚点在右边
            default -> -width / 2.0F;    // 居中
        };
        float y0 = switch (anchorV) {
            case 0 -> 0.0F;              // 上对齐：锚点在顶边
            case 2 -> -height;           // 下对齐：锚点在底边
            default -> -height / 2.0F;   // 居中
        };
        font.drawInBatch(text, x0, y0, color, false,
                matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, PACKED_LIGHT);
    }

    /**
     * 绘制结构表面上的全部组件模型。
     *
     * 组件模型（OBJ）原始坐标：x = 面板外法线、y = 向上、z = 横向，单位格。
     * 这里用仿射矩阵把「x→外法线、y→V 轴、z→U 轴」映射到结构表面，
     * 并把模型包围盒中心 (centerY, centerZ) 对齐到组件锚点 (u, v)，
     * 最内侧的 x（minX）贴到表面，避免嵌进方块。
     */
    private static void renderComponents(ConsoleBusBlockEntity be,
                                         List<ConsoleBusBlockEntity.CompEntry> comps,
                                         ConsoleFrame fixedFrame,
                                         PoseStack poseStack,
                                         MultiBufferSource bufferSource) {
        if (comps.isEmpty()) {
            return;
        }
        // 当前游戏时间：按钮按下 / 安全按钮开盖等状态需要按时刻判断
        long gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();

        // 两遍绘制：第一遍只画不透明面，第二遍只画玻璃罩面。
        // 玻璃罩用的是另一个 RenderType（半透明着色器 + 另一张贴图），若与不透明面
        // 在同一次遍历里交替写入同一个 MultiBufferSource，会破坏先创建的裁切批次的
        // 贴图 / 状态绑定（本项目此前「组件绘制中切换 RenderType 导致贴图整体失效」
        // 就是这个原因），表现为整个组件都套上玻璃贴图、整体变半透明。
        for (int pass = 0; pass < 2; pass++) {
            boolean glassPass = pass == 1;
            for (ConsoleBusBlockEntity.CompEntry comp : comps) {
                // RGB 颜色控制器有附带文字时隐藏模型，只显示该附带文字（见 renderCompTexts）；
                // 没有附带文字时正常显示模型（与是否填写代号无关）
                if (comp.type == ConsoleCompType.RGB_COLOR && !comp.text.isEmpty()) {
                    continue;
                }
            ConsoleCompModels.ObjMesh mesh = meshFor(comp, gameTime);
            if (mesh == null) {
                continue;
            }
            ConsoleFrame frame = frameOf(be, comp.face, fixedFrame);
            // 组件锚点的世界坐标（先算，用于取光照）。
            // 沿外法线外推 COMP_OUTSET，使模型最内侧的面离开方块表面，避免共面深度冲突
            Vec3 anchorWorld = frame.toWorld(comp.u, comp.v, COMP_OUTSET);
            // 方块实体渲染时 PoseStack 已平移到「方块坐标 - 摄像机」，改为相对方块坐标
            Vec3 anchor = frame.toLocal(comp.u, comp.v, COMP_OUTSET);

            Vec3 uAxis = frame.u();
            Vec3 vAxis = frame.v();
            Vec3 nAxis = frame.n();

            // 实际缩放 = 类型默认缩放 × 玩家设置的组件缩放（交互判定使用同一倍率）。
            // 缩放必须**以锚点为中心**：把三个基向量与平移量里所有与模型尺寸相关的项
            // 一起乘 s，等价于 anchor + s·(模型点 - 包围盒中心/最内侧面)。
            // 若只缩放基向量而平移量仍按 s=1 计算，模型会绕局部原点缩放，
            // 额外偏移 (1-s)·(N·minX + V·centerY - U·centerZ)——表现为模型跑偏，
            // 而点击判定矩形仍以锚点为中心（看上去「点击位置对、模型不对」）。
            float s = comp.effectiveScale();
            // 平移：把模型局部点 (minX, centerY, centerZ)（乘 s 后）移到锚点。
            // 第 2 列取 -U（见下），z 方向的分量要取反，才能让包围盒中心仍对齐锚点。
            Vec3 trans = anchor
                    .subtract(nAxis.scale(mesh.minX * s))
                    .subtract(vAxis.scale(mesh.centerY * s))
                    .add(uAxis.scale(mesh.centerZ * s));

            // 局部矩阵：第 0 列 = 外法线（模型 x），第 1 列 = V（模型 y），
            // 第 2 列 = -U（模型 z）。
            // 模型空间 x=外法线、y=向上 构成右手系，其 z 轴等于 N×V = -U；
            // 若这里直接用 +U，三列构成左手系（行列式 -1），模型会被水平镜像
            // （例如表盘上的红色指示会跑到错误的一侧）。
            Matrix4f local = new Matrix4f(
                    (float) nAxis.x() * s, (float) nAxis.y() * s, (float) nAxis.z() * s, 0.0F,
                    (float) vAxis.x() * s, (float) vAxis.y() * s, (float) vAxis.z() * s, 0.0F,
                    (float) -uAxis.x() * s, (float) -uAxis.y() * s, (float) -uAxis.z() * s, 0.0F,
                    (float) trans.x, (float) trans.y, (float) trans.z, 1.0F);
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);
            // 法线矩阵：只取 4x4 矩阵的旋转/缩放部分（1.20.1 的 normal 需要 Matrix3f）
            Matrix3f normalMatrix = new Matrix3f(matrix);
            int light = lightAt(be, anchorWorld, nAxis);
            // 组件颜色：可着色类型（表盘 / 按键 / 指示灯）按所选 RGB 重新着色贴图，
            // 未设置颜色时用原贴图；显示屏 / 发信终端的颜色暂不作用于外观（仅保存备用）。
            // 指示灯熄灯时使用「所选颜色 × 暗化系数」，得到同一色相下更暗的灯面。
            int effectiveColor = comp.color;
            // RGB 颜色控制器：成型前由「显示原色」开关直接决定是否着色
            // （成型后服务端结算会把 color 写成最终值，此时只看 color，见 applyRgbControllers）
            if (comp.type == ConsoleCompType.RGB_COLOR
                    && be.getFormState() == ConsoleBusBlockEntity.State.PARTIAL
                    && comp.showOriginal) {
                effectiveColor = ConsoleBusBlockEntity.CompEntry.NO_COLOR;
            }
            if (comp.type == ConsoleCompType.INDICATOR && comp.state == 0L
                    && comp.color != ConsoleBusBlockEntity.CompEntry.NO_COLOR) {
                effectiveColor = dimColor(comp.color, INDICATOR_OFF_FACTOR);
            }
            // 未设置颜色的指示灯直接用原贴图，无法通过重新着色变暗：
            // 熄灯时把灯面（Light 分组）的顶点色整体压暗，否则默认模型看起来永远是开灯
            boolean indicatorOffDefault = comp.type == ConsoleCompType.INDICATOR
                    && comp.state == 0L
                    && comp.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR;
            ResourceLocation textureId = mesh.texture;
            // 着色贴图里指针采样的像素被涂成原指针色，纯色面渲染时改用它（见下）
            float[] needleUv = null;
            boolean hasColor = effectiveColor != ConsoleBusBlockEntity.CompEntry.NO_COLOR;
            if (hasColor) {
                ResourceLocation dyed = ConsoleCompTextures.get(comp.type, effectiveColor, mesh);
                if (dyed != null) {
                    textureId = dyed;
                    needleUv = ConsoleCompTextures.needleUv(comp.type, mesh);
                }
            }
            // 底板 / 底座逐面染色（panel_props 模型）：可染色面改用贴图右上角纯白采样块 +
            // 顶点色，直接显示为所选颜色；其余面（旋钮、拉杆、表盘、按钮灯等）保持原贴图
            int tintR = (effectiveColor >> 16) & 0xFF;
            int tintG = (effectiveColor >> 8) & 0xFF;
            int tintB = effectiveColor & 0xFF;
            // 第一遍只创建不透明批次；第二遍只创建玻璃半透明批次，两遍互不干扰
            VertexConsumer consumer = glassPass ? null
                    : bufferSource.getBuffer(RenderType.entityCutoutNoCull(textureId));
            // 玻璃罩（usemtl glass）单独用半透明批次绘制：
            // 贴图 button_s_glass.png 带 alpha（浅蓝 + 白色高光条纹），必须用半透明批次
            // 才会真正透出后面的按钮；用裁切批次会把半透明像素当成不透明整片涂蓝。
            // 用 entityTranslucentCull（带背面剔除）：玻璃罩由多块「薄板盒」拼成，
            // 每块板正反两面若都画，多层 alpha 叠加会把罩子糊成一片雾。
            VertexConsumer glassConsumer = !glassPass || mesh.glassTexture == null ? null
                    : bufferSource.getBuffer(RenderType.entityTranslucentCull(mesh.glassTexture));

            // 组件局部动画参数：
            //  - 按钮面板：state 为「弹起时刻」或「按住直到松手」，按下期间 Button 分组向内位移；
            //  - 弧形仪表：state 为 0~100，Needle 分组绕枢轴转动（模型指针烘焙在 35% 位置）；
            //  - 流量表：value 为数值，Needle 分组绕刻度圆心转动（0% 指向水平端，100% 指向烘焙的竖直端）；
            //  - 立式仪表：value 为数值，Needle 分组沿中央竖槽上下平移（50% 居中）。
            boolean keypadPressed = comp.isKeypadPressed(gameTime);
            // 旋转按钮：模型烘焙的是「按下」状态，未按下时 Rotate 分组需顺时针旋转 90° 复位
            boolean rotateButtonX = comp.type == ConsoleCompType.BUTTON_X && !keypadPressed;
            float arcAngle = comp.type == ConsoleCompType.GAUGE_ARC
                    ? (float) Math.toRadians(
                            ARC_DEG_PER_PERCENT * (comp.state - ARC_BAKED_VALUE))
                    : 0.0F;
            float arcCos = (float) Math.cos(arcAngle);
            float arcSin = (float) Math.sin(arcAngle);
            float gaugeAngle = comp.type == ConsoleCompType.GAUGE
                    ? (float) Math.toRadians(GAUGE_START_DEG
                            + GAUGE_DEG_PER_PERCENT * clampDisplay(comp.value))
                    : 0.0F;
            float gaugeCos = (float) Math.cos(gaugeAngle);
            float gaugeSin = (float) Math.sin(gaugeAngle);
            float gaugeVOffset = comp.type == ConsoleCompType.GAUGE_V
                    ? (clampDisplay(comp.value) / 100.0F - 0.5F) * GAUGE_V_HALF_TRAVEL * 2.0F
                    : 0.0F;
            // 扇形仪表：以烘焙角度为基准，先转到 0 位（左下 30°），再随数值顺时针扫 240°
            float gaugeFAngle = comp.type == ConsoleCompType.GAUGE_F
                    ? (float) Math.toRadians(GAUGE_F_START_DEG
                            + GAUGE_F_DEG_PER_PERCENT * clampDisplay(comp.value)
                            - GAUGE_F_BAKED_DEG)
                    : 0.0F;
            float gaugeFCos = (float) Math.cos(gaugeFAngle);
            float gaugeFSin = (float) Math.sin(gaugeFAngle);
            // 同步指示器：value 为指针角度（-180~180，正上方 0，正 = 顺时针），
            // 以模型烘焙角度为基准旋转到目标角度
            float synchroAngle = comp.type == ConsoleCompType.SYNCHROSCOPE
                    ? (float) Math.toRadians(wrapAngle180(comp.value) - SYNCHRO_BAKED_DEG)
                    : 0.0F;
            float synchroCos = (float) Math.cos(synchroAngle);
            float synchroSin = (float) Math.sin(synchroAngle);
            // 圆盘记录仪 L：右侧的笔分组只按最新采样值做纵向平移（横向不动）
            float penOffsetY = comp.type == ConsoleCompType.GRAPH_L ? penTravel(comp) : 0.0F;

            for (ConsoleCompModels.Tri tri : mesh.tris) {
                // 分遍绘制：第一遍跳过玻璃面，第二遍只画玻璃面
                if (tri.glass != glassPass) {
                    continue;
                }
                // entityCutoutNoCull 的顶点模式是 QUADS（每图元 4 个顶点），不是 TRIANGLES。
                // OBJ 已在解析阶段三角化，这里必须把每个三角形补足成 4 个顶点：
                // 第 4 个顶点与第 3 个顶点完全重合，构成 (a,b,c,c) 四边形。
                // 上传时四边形按 (0,1,2) + (0,2,3) 拆成两个三角形，第二个 (a,c,c)
                // 面积为零、不可见，第一个就是原本的三角形。
                // 若只提交 3 个顶点，缓冲区会把「下一个三角形的首顶点」当成当前四边形的
                // 第 4 点，相邻三角形被错位缝合成虚假的斜面 —— 表现为密集斜纹花屏、
                // 撕裂折片和悬空平面（对所有 OBJ 通用，无需改模型资源）。
                // 纯色面（流量表指针）在着色贴图中改用「指针像素」的 UV，
                // 该像素被涂成原指针色，因此指针不会跟着表盘一起变色
                boolean tinted = hasColor && tri.tintable;
                boolean needle = !tinted && needleUv != null && tri.solidColor;
                // 未着色的指示灯熄灯：只压暗灯面（Light 分组），外壳保持原样
                boolean dimLight = indicatorOffDefault && "Light".equals(tri.group);
                int vR = dimLight ? INDICATOR_OFF_GRAY : (tinted ? tintR : 255);
                int vG = dimLight ? INDICATOR_OFF_GRAY : (tinted ? tintG : 255);
                int vB = dimLight ? INDICATOR_OFF_GRAY : (tinted ? tintB : 255);
                // 分组动画：按钮按下位移 / 指针旋转 / 立式仪表箭头平移
                //（在模型坐标系内处理，与后续模型矩阵、缩放、外推完全独立）
                // 安全按钮的按下状态已烘焙在 _2 模型里，不能再做 Button 分组位移
                boolean pressGroup = keypadPressed
                        && comp.type != ConsoleCompType.BUTTON_S
                        && "Button".equals(tri.group);
                // 玻璃罩走半透明批次；没有玻璃贴图时跳过（避免闪退 / 错乱）
                VertexConsumer out = tri.glass ? glassConsumer : consumer;
                if (out == null) {
                    continue;
                }
                // 旋转按钮复位：Rotate 分组的全部面绕面板法线旋转
                boolean rotateGroupX = rotateButtonX && "Rotate".equals(tri.group);
                boolean needleGroup = "Needle".equals(tri.group);
                boolean rotateArc = needleGroup && arcAngle != 0.0F;
                boolean rotateGauge = needleGroup && gaugeAngle != 0.0F;
                boolean rotateGaugeF = needleGroup && gaugeFAngle != 0.0F;
                boolean rotateSynchro = needleGroup && synchroAngle != 0.0F;
                boolean shiftGaugeV = needleGroup && gaugeVOffset != 0.0F;
                // 圆盘记录仪 L：Pen 分组（笔）随最新采样值上下平移
                boolean shiftPen = penOffsetY != 0.0F && "Pen".equals(tri.group);
                for (int i = 0; i < 4; i++) {
                    // 第 4 个顶点复用第 3 个顶点（索引夹到 2）
                    int corner = Math.min(i, 2);
                    float px = tri.pos[corner * 3];
                    float py = tri.pos[corner * 3 + 1];
                    float pz = tri.pos[corner * 3 + 2];
                    float nx = tri.normal[0];
                    float ny = tri.normal[1];
                    float nz = tri.normal[2];
                    if (pressGroup) {
                        // 沿外法线向内按入（模型 x 为外法线方向）
                        px -= KEYPAD_PRESS_DEPTH;
                    }
                    if (rotateGroupX) {
                        // 观察者视角顺时针 90°：(y, z) → 枢轴 + (z_rel, -y_rel)，法线同步变换
                        float bdy = py - BUTTON_X_PIVOT_Y;
                        float bdz = pz - BUTTON_X_PIVOT_Z;
                        py = BUTTON_X_PIVOT_Y + bdz;
                        pz = BUTTON_X_PIVOT_Z - bdy;
                        float rny = nz;
                        float rnz = -ny;
                        ny = rny;
                        nz = rnz;
                    }
                    if (rotateGauge) {
                        // 流量表指针：绕刻度圆弧圆心旋转，使指针沿刻度扫动
                        float dz = pz - GAUGE_PIVOT_Z;
                        float dy = py - GAUGE_PIVOT_Y;
                        py = GAUGE_PIVOT_Y + dz * gaugeSin + dy * gaugeCos;
                        pz = GAUGE_PIVOT_Z + dz * gaugeCos - dy * gaugeSin;
                        float rny = nz * gaugeSin + ny * gaugeCos;
                        float rnz = nz * gaugeCos - ny * gaugeSin;
                        ny = rny;
                        nz = rnz;
                    } else if (rotateGaugeF) {
                        // 扇形仪表指针：绕垂直于表盘的模型 X 轴旋转（转换后的 (z, y) 平面）
                        float dz = pz - GAUGE_F_PIVOT_Z;
                        float dy = py - GAUGE_F_PIVOT_Y;
                        py = GAUGE_F_PIVOT_Y + dz * gaugeFSin + dy * gaugeFCos;
                        pz = GAUGE_F_PIVOT_Z + dz * gaugeFCos - dy * gaugeFSin;
                        float rny = nz * gaugeFSin + ny * gaugeFCos;
                        float rnz = nz * gaugeFCos - ny * gaugeFSin;
                        ny = rny;
                        nz = rnz;
                    } else if (rotateArc) {
                        // 弧形仪表指针绕「垂直于表盘」的轴（转换后为 +X）旋转：
                        // 由模型坐标还原到源 OBJ 空间的 (z, y) 平面上绕枢轴旋转，
                        // 角度 = 每 1% 1.2°，并以烘焙位置 35% 为零点。
                        float dz = pz - ARC_PIVOT_Z;
                        float dy = py - ARC_PIVOT_Y;
                        py = ARC_PIVOT_Y + dz * arcSin + dy * arcCos;
                        pz = ARC_PIVOT_Z + dz * arcCos - dy * arcSin;
                        float rny = nz * arcSin + ny * arcCos;
                        float rnz = nz * arcCos - ny * arcSin;
                        ny = rny;
                        nz = rnz;
                    } else if (rotateSynchro) {
                        // 同步指示器指针：绕垂直于表盘的模型 X 轴旋转，
                        // 正角度在屏幕上为顺时针（与仪表读数方向约定一致）
                        float dz = pz - SYNCHRO_PIVOT_Z;
                        float dy = py - SYNCHRO_PIVOT_Y;
                        py = SYNCHRO_PIVOT_Y + dz * synchroSin + dy * synchroCos;
                        pz = SYNCHRO_PIVOT_Z + dz * synchroCos - dy * synchroSin;
                        float rny = nz * synchroSin + ny * synchroCos;
                        float rnz = nz * synchroCos - ny * synchroSin;
                        ny = rny;
                        nz = rnz;
                    } else if (shiftGaugeV) {
                        // 立式仪表箭头沿中央竖槽平移：数值越大越靠上
                        py += gaugeVOffset;
                    } else if (shiftPen) {
                        // 圆盘记录仪 L 的笔：只沿模型 y 轴平移，数值越大越靠上
                        py += penOffsetY;
                    }
                    // 可染色面：UV 指到贴图里的纯白采样块，用顶点色乘出所选颜色；
                    // 贴图没有纯白块时（mesh.tintU < 0，如安全按钮）保留原 UV 只乘顶点色，
                    // 这样按钮本身的白色区域会被直接染成所选颜色
                    boolean useTintUv = tinted && mesh.tintU >= 0.0F;
                    float texU = useTintUv ? mesh.tintU : (needle ? needleUv[0] : tri.uv[corner * 2]);
                    float texV = useTintUv ? mesh.tintV : (needle ? needleUv[1] : tri.uv[corner * 2 + 1]);
                    out.vertex(matrix, px, py, pz)
                            .color(vR, vG, vB, 255)
                            .uv(texU, texV)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(light)
                            .normal(normalMatrix, nx, ny, nz)
                            .endVertex();
                }
            }
            }
        }
    }

    /**
     * 取组件当前应使用的模型：
     *  - 三状态开关（t_switch / t_lever）：state 0/1/2 对应绿 / 橙 / 红三套模型；
     *  - 双色指示灯（lamp_d）：state 0 = 红亮、1 = 绿亮两套模型；
     *  - 旋钮 / 拉杆开关（含阀门式拉杆）在关闭状态使用 *_close_surface.obj 变体
     *    （灯色与把手角度已烘进模型），其余类型只有一份模型。
     *  - 安全按钮（button_s）：state 0 = 合盖、OPEN_COVER = 开盖、按下 = 压入三套模型。
     *
     * @param gameTime 当前游戏时间（判断按钮是否处于按下状态）
     */
    private static ConsoleCompModels.ObjMesh meshFor(ConsoleBusBlockEntity.CompEntry comp,
                                                     long gameTime) {
        if (comp.type == ConsoleCompType.BUTTON_S) {
            // 0 = 合盖、1 = 开盖、2 = 开盖且按钮按下（按下状态已烘焙在模型里）
            int variant = comp.state == ConsoleBusBlockEntity.CompEntry.OPEN_COVER ? 1
                    : (comp.isKeypadPressed(gameTime) ? 2 : 0);
            return ConsoleCompModels.get(comp.type, variant);
        }
        if (comp.type == ConsoleCompType.SWITCH_K) {
            // 钥匙开关：0 = 合盖无钥匙、1 = 开盖无钥匙、2 = 合盖有钥匙、3 = 开盖有钥匙
            int variant = (comp.hasKey ? 2 : 0) + (comp.state != 0L ? 1 : 0);
            return ConsoleCompModels.get(comp.type, variant);
        }
        if (comp.type == ConsoleCompType.ALARM) {
            // 警报器：0/1 = 灯灭/灯亮，+2 = 静音按钮按下，+4 = 重置按钮按下
            // 报警中未静音时灯闪烁（按游戏时间每 10 tick 切换一次），静音后常亮
            boolean active = comp.state != 0L;
            boolean lampOn = active && (comp.muted || (gameTime / 10L) % 2L == 0L);
            boolean pressedA = comp.pressButton == 1 && gameTime < comp.pressUntil;
            boolean pressedB = comp.pressButton == 2 && gameTime < comp.pressUntil;
            int variant = (lampOn ? 1 : 0) + (pressedA ? 2 : 0) + (pressedB ? 4 : 0);
            return ConsoleCompModels.get(comp.type, variant);
        }
        if (comp.type == ConsoleCompType.T_LEVER || comp.type == ConsoleCompType.T_SWITCH) {
            // 三状态组件的模型档位与逻辑档位相反：模型 0 档是「左 + 绿灯」、
            // 2 档是「右 + 红灯」（旋钮的灯从左到右为绿 / 橙 / 红，拉杆同理），
            // 而逻辑约定 state 0 = 右 + 红灯、2 = 左 + 绿灯，
            // 因此这里反转首尾档位（1 档居中对齐不用换）
            long variant = 2L - Math.max(0L, Math.min(2L, comp.state));
            return ConsoleCompModels.get(comp.type, (int) variant);
        }
        if (comp.type.isThreeState()) {
            // 三档：越界存档按 0~2 夹紧，避免取不到模型
            long variant = Math.max(0L, Math.min(2L, comp.state));
            return ConsoleCompModels.get(comp.type, (int) variant);
        }
        if (comp.type == ConsoleCompType.LAMP_D) {
            return ConsoleCompModels.get(comp.type, comp.state != 0L ? 1 : 0);
        }
        if (comp.type == ConsoleCompType.SYNCHROSCOPE) {
            // state 1 = 灯亮（基础模型），0 = 灯灭（变体 1）
            return ConsoleCompModels.get(comp.type, comp.state != 0L ? 0 : 1);
        }
        boolean closed = comp.state == 0L
                && (comp.type == ConsoleCompType.KNOB_SWITCH
                || comp.type == ConsoleCompType.LEVER_SWITCH
                || comp.type == ConsoleCompType.LEVER_F);
        return ConsoleCompModels.get(comp.type, closed);
    }

    /**
     * 圆盘记录仪 L（graph_l）的笔尖纵向位移（模型坐标，单位格）。
     *
     * 取历史序列最前面的采样（index 0 = 最近一次 push 进入的值）；没有历史时退回当前值。
     * 数值 0~100 按模型设计（0 → 2.836、100 → 10.112 设计单位，1 设计单位 = 1/16 格）
     * 映射到笔的纵向行程，再减去模型里烘焙的位置（按数值 50 建模），
     * 因此只有「相对烘焙位置的偏移」会作为平移量。
     */
    private static float penTravel(ConsoleBusBlockEntity.CompEntry comp) {
        float value;
        if (comp.history != null && comp.history.length > 0) {
            value = comp.history[0];
        } else {
            value = comp.value;
        }
        if (!Float.isFinite(value)) {
            value = 0.0F;
        }
        value = Math.max(0.0F, Math.min(PLOT_VALUE_MAX, value));
        float design = PEN_Y_AT_ZERO + PEN_Y_PER_VALUE * value - PEN_Y_BAKED;
        return (float) (design / ConsoleSurface.PIXELS_PER_BLOCK);
    }

    /**
     * 绘制所有组件的附带文字。
     *
     * 组件模型全部画完后再统一画文字：文字使用字体着色器（RenderType 与组件不同），
     * 集中放在模型批次之后可以避免打断组件贴图批次（历史问题是组件绘制中切换
     * RenderType 会导致着色贴图整体失效）。
     */
    private static void renderCompTexts(ConsoleBusBlockEntity be,
                                        List<ConsoleBusBlockEntity.CompEntry> comps,
                                        ConsoleFrame fixedFrame,
                                        PoseStack poseStack,
                                        MultiBufferSource bufferSource) {
        if (comps.isEmpty()) {
            return;
        }
        for (ConsoleBusBlockEntity.CompEntry comp : comps) {
            if (comp.type == ConsoleCompType.RGB_COLOR) {
                renderRgbText(be, fixedFrame, poseStack, bufferSource, comp);
                continue;
            }
            ConsoleCompModels.ObjMesh mesh = meshFor(comp,
                    be.getLevel() == null ? 0L : be.getLevel().getGameTime());
            if (mesh == null) {
                continue;
            }
            renderCompText(be, fixedFrame, poseStack, bufferSource, comp, mesh);
        }
    }

    /**
     * 绘制 RGB 颜色控制器隐藏模型后显示的「附带文字」。
     *
     * 只有配置了附带文字（文本编辑器点击组件设置）时才隐藏模型并在此处显示该文字；
     * 文字颜色取控制器当前灯色（未着色时为白色），用于指示当前颜色。
     * 文字大小与其它组件一致：默认文字大小 × 玩家组件缩放（缩放比 1 即默认大小）。
     */
    private static void renderRgbText(ConsoleBusBlockEntity be, ConsoleFrame fixedFrame,
                                      PoseStack poseStack,
                                      MultiBufferSource bufferSource,
                                      ConsoleBusBlockEntity.CompEntry comp) {
        if (comp.text.isEmpty()) {
            return;
        }
        int shown = comp.color;
        // 成型前由「显示原色」开关决定文字颜色；成型后服务端已把最终颜色写入 color
        if (be.getFormState() == ConsoleBusBlockEntity.State.PARTIAL && comp.showOriginal) {
            shown = ConsoleBusBlockEntity.CompEntry.NO_COLOR;
        }
        int color = shown == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                ? 0xFFFFFFFF : (0xFF000000 | shown);
        // 只保留粗体/斜体/下划线/删除线样式，颜色统一用控制器当前灯色
        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY
                .withBold(comp.text.bold).withItalic(comp.text.italic)
                .withUnderlined(comp.text.underline).withStrikethrough(comp.text.strikethrough);
        Component label = Component.literal(comp.text.text).withStyle(style);
        drawText(poseStack, bufferSource, Minecraft.getInstance().font,
                frameOf(be, comp.face, fixedFrame),
                comp.u, comp.v, 1, 1,
                // 缩放比 1 时为旧版默认文字大小，随玩家组件缩放同步放大 / 缩小
                com.cc_rc.Config.getConsoleTextDefaultScale() * comp.scale,
                ConsoleSurface.TEXT_OUTSET, label, color);
    }

    /**
     * 绘制组件的附带文字。
     *
     * 位置相对于组件矩形固定（组件移动时文字跟着移动），大小与间距随组件缩放一起变化
     * （基础大小取配置里的默认文字大小，再乘以玩家的组件缩放 comp.scale：
     *  缩放比 1 时的文字大小就是以前各组件的默认附带文字大小，不乘类型的 renderScale，
     *  否则本来就渲染得较小的组件文字会小到看不清）：
     *  - 指示灯：灯板表面居中（沿外法线推到模型最厚处之外，避免被模型遮挡）；
     *  - 按钮（按键区）：组件右侧、垂直居中、左对齐；
     *  - 其他（流量表 / 数码管 / 发信终端）：组件下方、水平居中、上对齐（文字向下展开）。
     */
    private static void renderCompText(ConsoleBusBlockEntity be, ConsoleFrame fixedFrame,
                                       PoseStack poseStack,
                                       MultiBufferSource bufferSource,
                                       ConsoleBusBlockEntity.CompEntry comp,
                                       ConsoleCompModels.ObjMesh mesh) {
        ConsoleBusBlockEntity.CompText text = comp.text;
        if (text == null || text.isEmpty()) {
            return;
        }
        float s = comp.scale;
        float gap = COMP_TEXT_GAP * s;
        float u = comp.u;
        float v = comp.v;
        // 默认居中：水平锚点 1 = 中，垂直锚点 1 = 中
        int anchorH = 1;
        int anchorV = 1;
        // 默认贴在方块表面外侧（与普通表面文字一致）
        double outset = ConsoleSurface.TEXT_OUTSET;
        switch (comp.type) {
            case INDICATOR -> {
                // 灯板表面居中：模型在表面外还有厚度（按类型缩放），文字放到最外侧面之外
                outset = COMP_OUTSET
                        + (mesh.maxX - mesh.minX) * comp.effectiveScale() + 0.001D;
            }
            case KEYPAD, BUTTON_X, BUTTON_S -> {
                // 右侧、左对齐（锚点落在组件右边缘外 1 像素，间距随组件缩放）
                u = comp.u + comp.halfWidthPx() + gap;
                anchorH = 0;
            }
            default -> {
                // 下方、居中（锚点落在组件下边缘外 1 像素，文字从锚点向下展开）
                v = comp.v - comp.halfHeightPx() - gap;
                anchorV = 0;
            }
        }
        drawText(poseStack, bufferSource, Minecraft.getInstance().font,
                frameOf(be, comp.face, fixedFrame),
                u, v, anchorH, anchorV,
                com.cc_rc.Config.getConsoleTextDefaultScale() * s, outset, text.toComponent(),
                0xFFFFFFFF);
    }

    /** 把组件数值限制在略超量程的显示范围内（NaN 按 0 处理）。 */
    private static float clampDisplay(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.max(DISPLAY_MIN, Math.min(DISPLAY_MAX, value));
    }

    /** 把角度（度）环绕到 -180~180（同步指示器：185 → -175，-185 → 175）。 */
    private static float wrapAngle180(float degrees) {
        if (!Float.isFinite(degrees)) {
            return 0.0F;
        }
        float value = degrees % 360.0F;
        if (value > 180.0F) {
            value -= 360.0F;
        } else if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    /** 按系数把 RGB 变暗（指示灯熄灯颜色），各通道四舍五入并夹在 0~255。 */
    private static int dimColor(int rgb, float factor) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /** 组件表面位置的光照值（沿外法线外推 0.05 格取样）。 */
    private static int lightAt(ConsoleBusBlockEntity be, Vec3 worldPoint, Vec3 normal) {
        Level level = be.getLevel();
        if (level == null) {
            return PACKED_LIGHT;
        }
        BlockPos pos = BlockPos.containing(worldPoint.add(normal.scale(0.05D)));
        return LevelRenderer.getLightColor(level, pos);
    }

    // ===================== 数码显示器 / 圆盘记录仪读数 =====================

    /** 数码管文字最大缩放（再大就放不进 13×5 像素的屏幕）。 */
    private static final float DISPLAY_TEXT_MAX_SCALE = 0.25F;
    /** 数码管文字可用宽度（结构像素，屏幕宽 13 像素留一点边距）。 */
    private static final float DISPLAY_TEXT_MAX_WIDTH = 11.5F;
    /** 圆盘记录仪图表可用宽度（结构像素；屏幕内宽 12 像素，左右各留一点边距）。 */
    private static final float PLOT_WIDTH = 11.8F;
    /** 图表左边界（相对组件中心，结构像素）。 */
    private static final float PLOT_LEFT_U = -PLOT_WIDTH / 2.0F;
    /** 图表基线（相对组件中心，结构像素；屏幕内下边缘为 -2.0）。 */
    private static final float PLOT_BASE_V = -1.9F;
    /** 图表最大柱高（结构像素）：比单独的数字圆盘记录仪略压缩，落在 4 像素高的屏幕内。 */
    private static final float PLOT_MAX_HEIGHT = 3.8F;
    /** 趋势线的线宽（结构像素）：只画一条细线，不填充线下方空间。 */
    private static final float PLOT_LINE_THICK = 0.4F;
    /** 每个采样点占用的列宽（结构像素），50 点均分整条屏幕宽度。 */
    private static final float PLOT_STEP_U = PLOT_WIDTH / ConsoleBusBlockEntity.CompEntry.HISTORY_LEN;
    /** 全角方块字符 █ 的字形宽度（字体像素），用于把列宽 / 柱高换算成缩放。 */
    private static final float FULL_BLOCK_PX = 8.0F;
    /** 图表 / 数码管读数相对组件中心的最大数值范围（与单独实现的记录仪一致）。 */
    private static final float PLOT_VALUE_MAX = 100.0F;

    // ===================== 圆盘记录仪 L（graph_l）：更高的纸面 + 右侧的笔 =====================
    /**
     * 曲线绘制区域左边界（相对组件中心，结构像素）。
     * 模型纸面横向为设计单位 4.0118…13.78（右侧 4.0118 处是刻度板与笔尖），
     * 换算到组件坐标：u 偏移 = (8 - 设计值) × 缩放，故左边界 = 8 - 13.78 = -5.78。
     */
    private static final float PLOT_L_LEFT_U = -5.78F;
    /** 曲线绘制区域总宽度（结构像素）：笔尖 4.0118 到纸面左端 13.78。 */
    private static final float PLOT_L_WIDTH = 9.7682F;
    /** 曲线基线（相对组件中心，结构像素）：数值 0 时笔所画的纵向位置。 */
    private static final float PLOT_L_BASE_V = -3.664F;
    /** 数值 0→100 对应的纵向行程（结构像素）：与笔的行程（7.276 设计单位）一致。 */
    private static final float PLOT_L_HEIGHT = 7.276F;
    /** 曲线线宽（结构像素）：纸面比旧记录仪高，线也略粗。 */
    private static final float PLOT_L_LINE_THICK = 0.5F;
    /** 每列宽度（结构像素）：50 个采样点均分绘制区。 */
    private static final float PLOT_L_STEP_U =
            PLOT_L_WIDTH / ConsoleBusBlockEntity.CompEntry.HISTORY_LEN;
    /** 笔的纵向行程（模型设计单位 = 1/16 格）：数值 0 → 2.836，每 1 点 0.07276。 */
    private static final float PEN_Y_AT_ZERO = 2.836F;
    private static final float PEN_Y_PER_VALUE = 0.07276F;
    /** 笔在模型中烘焙的纵向位置（设计单位）：源模型按数值 50 建模（6.474）。 */
    private static final float PEN_Y_BAKED = 6.4743F;
    /** 曲线所在的模型深度（设计单位）：纸面 2.800 与笔尖背面 2.828 之间。 */
    private static final float PLOT_L_PLANE_X = 2.815F;

    /**
     * 绘制数码显示器与圆盘记录仪的读数。
     *
     * 两者复用同一份屏幕模型（13×5 结构像素），读数画在模型最外侧面的前面：
     *  - 数码显示器：按设置的小数位格式化数值并附加单位，居中显示，过宽时自动缩小；
     *  - 圆盘记录仪：50 个历史采样按列画成细柱状趋势（索引 0 = 最新，位于最右列），
     *    列宽为屏幕宽度的 1/50（亚像素），柱高按 0~100 连续映射到 3.8 像素。
     *
     * 与组件附带文字一样放在模型批次之后绘制，避免打断组件贴图批次
     * （组件绘制中切换 RenderType 会让染色整体失效）。
     */
    private static void renderCompReadouts(ConsoleBusBlockEntity be,
                                          List<ConsoleBusBlockEntity.CompEntry> comps,
                                          ConsoleFrame fixedFrame,
                                          PoseStack poseStack,
                                          MultiBufferSource bufferSource) {
        if (comps.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        for (ConsoleBusBlockEntity.CompEntry comp : comps) {
            ConsoleCompModels.ObjMesh mesh = meshFor(comp,
                    be.getLevel() == null ? 0L : be.getLevel().getGameTime());
            if (mesh == null) {
                continue;
            }
            if (comp.type == ConsoleCompType.NUMITRON) {
                renderDisplayValue(be, fixedFrame, poseStack, bufferSource, font, comp, mesh);
            } else if (comp.type == ConsoleCompType.NUMPAD) {
                // 数字键盘：在模型上方的屏幕区域绘制当前数值
                renderNumpadValue(be, fixedFrame, poseStack, bufferSource, font, comp);
            } else if (comp.type == ConsoleCompType.GRAPH
                    || comp.type == ConsoleCompType.GRAPH_L) {
                renderPlotHistory(be, fixedFrame, poseStack, bufferSource, font, comp, mesh);
            }
        }
    }

    /** 数码显示器：格式化后的数值 + 单位，居中显示在屏幕上。 */
    private static void renderDisplayValue(ConsoleBusBlockEntity be, ConsoleFrame fixedFrame,
                                           PoseStack poseStack,
                                           MultiBufferSource bufferSource, Font font,
                                           ConsoleBusBlockEntity.CompEntry comp,
                                           ConsoleCompModels.ObjMesh mesh) {
        float value = Float.isFinite(comp.value) ? comp.value : 0.0F;
        String number = String.format(java.util.Locale.ROOT,
                "%." + comp.decimals + "f", value);
        String text = number + comp.unit;
        if (text.isEmpty()) {
            return;
        }
        // 按宽度自动缩放：保证读数完整落在屏幕内；屏幕本身随组件缩放一起变大，读数同步放大
        float k = comp.scale;
        float scale = Math.min(DISPLAY_TEXT_MAX_SCALE * k,
                DISPLAY_TEXT_MAX_WIDTH * k / Math.max(1.0F, (float) font.width(text)));
        // 未设置颜色时用白色，设置过颜色则作为读数颜色（数码管颜色从此生效）
        int color = comp.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                ? 0xFFFFFFFF : (0xFF000000 | comp.color);
        drawText(poseStack, bufferSource, font,
                frameOf(be, comp.face, fixedFrame),
                comp.u, comp.v, 1, 1, scale,
                screenOutset(comp, mesh), Component.literal(text), color);
    }

    /**
     * 数字键盘：在模型上方的屏幕区域绘制当前数值。
     *
     * 显示区只有 4 个字符位（取自模型说明），因此数值按「最多 4 个字符」格式化：
     * 优先保留小数，超长时减少小数位，仍超长时取整；数字颜色使用组件颜色，
     * 未设置颜色时用 LED 橙色。
     */
    private static void renderNumpadValue(ConsoleBusBlockEntity be, ConsoleFrame fixedFrame,
                                          PoseStack poseStack,
                                          MultiBufferSource bufferSource, Font font,
                                          ConsoleBusBlockEntity.CompEntry comp) {
        String text = formatNumpadValue(comp.value);
        if (text.isEmpty()) {
            return;
        }
        // 组件实际倍率（类型默认缩放 × 玩家缩放）：模型按该倍率绘制，
        // 屏幕的宽 / 高 / 中心偏移必须同样乘它，否则数字会跑到模型上方并偏大
        float k = comp.effectiveScale();
        int color = comp.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                ? KEYPAD_DEFAULT_COLOR : (0xFF000000 | comp.color);
        float fontW = Math.max(1.0F, (float) font.width(text));
        float scale = Math.min(KEYPAD_SCREEN_H * k / font.lineHeight,
                KEYPAD_SCREEN_W * k / fontW) * 0.92F;
        // 外推：屏幕前平面略靠内（2.99 / 16 格），避免与玻璃面 / 外框深度冲突
        double outset = COMP_OUTSET
                + (KEYPAD_SCREEN_X / ConsoleSurface.PIXELS_PER_BLOCK) * comp.effectiveScale();
        // 字体行高中心比数字字形中心低约 1 个字体像素，绘制时下移锚点做补偿
        drawText(poseStack, bufferSource, font,
                frameOf(be, comp.face, fixedFrame),
                comp.u, comp.v + KEYPAD_SCREEN_V * k - scale, 1, 1, scale,
                outset, Component.literal(text), color);
    }

    /**
     * 数字键盘的 4 字符显示格式。
     * 例：0 → "0"，12.5 → "12.5"，123.456 → "123"，超出 4 位时显示 "9999" / "-999"。
     */
    private static String formatNumpadValue(float raw) {
        float value = Float.isFinite(raw) ? raw : 0.0F;
        String text = trimTrailingZeros(String.format(java.util.Locale.ROOT, "%.2f", value));
        if (text.length() > 4) {
            text = trimTrailingZeros(String.format(java.util.Locale.ROOT, "%.1f", value));
        }
        if (text.length() > 4) {
            text = String.valueOf(Math.round(value));
        }
        if (text.length() > 4) {
            // 极端大值：屏幕装不下，显示能表示的最大 / 最小值
            text = value < 0.0F ? "-999" : "9999";
        }
        // 舍入后可能得到 "-0"，统一显示为 "0"
        return "-0".equals(text) ? "0" : text;
    }

    /** 去掉小数末尾多余的 0 与小数点（"12.50" → "12.5"，"12.00" → "12"）。 */
    private static String trimTrailingZeros(String text) {
        if (text.indexOf('.') < 0) {
            return text;
        }
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }

    /** 圆盘记录仪：把历史采样按列画成细柱状趋势（最新值在最右列）。 */
    private static void renderPlotHistory(ConsoleBusBlockEntity be, ConsoleFrame fixedFrame,
                                          PoseStack poseStack,
                                          MultiBufferSource bufferSource, Font font,
                                          ConsoleBusBlockEntity.CompEntry comp,
                                          ConsoleCompModels.ObjMesh mesh) {
        if (comp.history == null) {
            return;
        }
        // 圆盘记录仪 L 的纸面更高、可画区域更窄，且曲线画在纸面与笔之间
        boolean tall = comp.type == ConsoleCompType.GRAPH_L;
        double outset = tall ? penPlaneOutset(comp) : screenOutset(comp, mesh);
        int color = comp.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                ? 0xFFFF0000 : (0xFF000000 | comp.color);
        Component block = Component.literal("\u2588");
        // 屏幕随组件缩放一起变化：列宽 / 柱高 / 线宽都按组件缩放换算
        float k = comp.scale;
        // 一列 = 一个采样点，列宽不到 1 个结构像素，看起来是细线而不是大方块
        float stepU = (tall ? PLOT_L_STEP_U : PLOT_STEP_U) * k;
        float scaleU = stepU / FULL_BLOCK_PX;
        // 每列只画一个点（不是从基线堆到数值的实心柱），点与点横向相接连成一条线
        float scaleV = (tall ? PLOT_L_LINE_THICK : PLOT_LINE_THICK) * k / FULL_BLOCK_PX;
        // 绘制区左边界与数值 0 / 100 对应的纵向位置（相对组件锚点，乘缩放）
        float leftU = (tall ? PLOT_L_LEFT_U : PLOT_LEFT_U) * k;
        float baseV = (tall ? PLOT_L_BASE_V : PLOT_BASE_V) * k;
        float heightV = (tall ? PLOT_L_HEIGHT : PLOT_MAX_HEIGHT) * k;
        for (int i = 0; i < ConsoleBusBlockEntity.CompEntry.HISTORY_LEN; i++) {
            // 索引 0 = 最新值 → 画在最右列；越靠左的历史越旧
            int column = ConsoleBusBlockEntity.CompEntry.HISTORY_LEN - 1 - i;
            float value = Float.isFinite(comp.history[i]) ? comp.history[i] : 0.0F;
            value = Math.max(0.0F, Math.min(PLOT_VALUE_MAX, value));
            if (value <= 0.0F) {
                continue;
            }
            // 列中心坐标：以组件锚点为原点，从屏幕左边界开始逐列排布
            float u = comp.u + leftU + (column + 0.5F) * stepU;
            // 数值对应的线中心高度：0 在基线，100 在最高处
            float v = comp.v + baseV + heightV * value / PLOT_VALUE_MAX;
            // 垂直居中对齐时字形中心比锚点高 0.5 个字体像素，这里把锚点下移同样距离使点居中
            drawTextScaled(poseStack, bufferSource, font,
                    frameOf(be, comp.face, fixedFrame),
                    u, v - 0.5F * scaleV, 1, 1, scaleU, scaleV,
                    outset, block, color);
        }
    }

    /**
     * 读数相对方块表面的外推距离。
     *
     * 显示器 / 记录仪的屏幕在模型中间是凹进去的（内屏约在模型厚度的一半处），
     * 读数不能按最外侧框架外推，否则会明显悬浮在组件表面之前，因此只取一半厚度。
     */
    private static double screenOutset(ConsoleBusBlockEntity.CompEntry comp,
                                       ConsoleCompModels.ObjMesh mesh) {
        return COMP_OUTSET + mesh.maxX * comp.effectiveScale() * 0.5D + 0.001D;
    }

    /**
     * 圆盘记录仪 L 的曲线外推距离。
     *
     * 曲线要画在纸面（模型 x = 2.800 设计单位）之前、笔尖（2.828）之后，
     * 因此直接取 2.815 设计单位换算成格，不再按模型最外侧（3.0）计算。
     */
    private static double penPlaneOutset(ConsoleBusBlockEntity.CompEntry comp) {
        return COMP_OUTSET
                + (PLOT_L_PLANE_X / ConsoleSurface.PIXELS_PER_BLOCK) * comp.effectiveScale();
    }
}
