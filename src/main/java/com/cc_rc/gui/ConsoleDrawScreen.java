package com.cc_rc.gui;

import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.network.ConsoleDrawCreatedPacket;
import com.cc_rc.network.ConsoleDrawPacket;
import com.cc_rc.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 单个绘图图形的属性界面。
 *
 * 界面只负责设置参数，不在界面里绘图：
 *  - 类型：实心矩形 / 空心矩形 / 线段 / 箭头 / 预设图案；
 *  - 颜色（16 色色块 + #RRGGBB 输入）、线宽（0.1~4.0，支持一位小数）、图层（0~7）；
 *  - 位置：图案左下角在表面上的像素坐标；
 *  - 尺寸：矩形为宽 / 高，线段与箭头为长度 / 角度；
 *  - 预设图案：右侧贴图样式列表选择图案，另有旋转角度（度）与缩放（0.1~3.0，支持一位小数）；
 *  - 保存 / 删除 / 关闭。
 *
 * 刚打开时处于「未创建」状态：关闭界面不会留下任何图案，
 * 只有点击类型按钮或右侧贴图样式时才请求服务端创建图形。
 * 保存与删除通过 {@link ConsoleDrawPacket} 发给服务端校验后写入。
 */
public class ConsoleDrawScreen extends AbstractContainerScreen<ConsoleDrawMenu> {

    private static final int GUI_W = 386;
    private static final int GUI_H = 220;

    /** 类型按钮排布。 */
    private static final int TYPE_X = 6;
    private static final int TYPE_Y = 31;
    private static final int TYPE_W = 46;
    private static final int TYPE_H = 16;
    private static final int TYPE_STEP = 48;

    /** 颜色色块排布（8 列 × 2 行）。 */
    private static final int SWATCH_X = 8;
    private static final int SWATCH_Y = 60;
    private static final int SWATCH_W = 14;
    private static final int SWATCH_H = 12;
    private static final int SWATCH_GAP_X = 2;
    private static final int SWATCH_GAP_Y = 2;
    private static final int SWATCH_COLS = 8;

    /** 线宽按钮排布。 */
    private static final int WIDTH_Y = 99;
    /** 图层按钮排布。 */
    private static final int LAYER_X = 110;
    private static final int LAYER_Y = 99;

    /** 位置输入行。 */
    private static final int POS_Y = 129;
    /** 尺寸 / 长度角度行。 */
    private static final int GEOM_Y = 159;
    /** 底部按钮行。 */
    private static final int BUTTON_Y = 190;

    /** 右侧贴图样式列表：4 列 × 3 行（一次显示 12 个），格子 24×24，间距 4。 */
    private static final int PRESET_LIST_X = 260;
    private static final int PRESET_LIST_Y = 32;
    private static final int PRESET_CELL = 24;
    private static final int PRESET_GAP = 4;
    private static final int PRESET_COLS = 4;
    /** 右侧列表面板内最多显示的行数（图案多于 4×3 时用滚轮翻看）。 */
    private static final int PRESET_VISIBLE_ROWS = 3;
    /** 列表面板上下边界相对 GUI 左上角的 Y（与 renderPresetList 保持一致）。 */
    private static final int PRESET_PANEL_TOP = 14;
    private static final int PRESET_PANEL_BOTTOM = 118;
    /** 列表面板右边界（相对 GUI 左上角）：格子区右侧再留 6 像素。 */
    private static final int PRESET_PANEL_RIGHT =
            PRESET_LIST_X + PRESET_COLS * (PRESET_CELL + PRESET_GAP) + 6;

    /** 16 种染料色（RGB）。 */
    private static final int[] DYE_COLORS = {
            0xFFFFFF, 0xF9801D, 0xC74EBD, 0x3AB3DA,
            0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
            0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    /** 类型按钮顺序（与界面中的排列一致）。 */
    private static final ConsoleShapeType[] TYPES = {
            ConsoleShapeType.RECT, ConsoleShapeType.RECT_OUTLINE, ConsoleShapeType.LINE,
            ConsoleShapeType.ARROW, ConsoleShapeType.PRESET
    };

    /** 当前编辑中的参数（未选择类型时为 null）。 */
    private ConsoleShapeType type;
    private int color;
    private float lineWidth;
    private int layer;
    private String presetId;
    private float presetRotation;
    private float presetScale;
    /** 贴图样式列表的滚动行偏移（0 = 从第一行开始）。 */
    private int presetScroll;
    /** 图案锚点（左下角，像素）。 */
    private float anchorU;
    private float anchorV;
    /** 尺寸：矩形为宽 / 高；线段与箭头为长度 / 角度（度）。 */
    private float dimA;
    private float dimB;

    /** 图形 id（-1 = 尚未创建）。 */
    private int shapeId;
    /** 是否尚未创建：此时关闭界面不会留下图案。 */
    private boolean pending;
    /** 创建请求已发出、等待服务端回执。 */
    private boolean creating;

    /** 位置输入框。 */
    private EditBox posUBox;
    private EditBox posVBox;
    /** 尺寸输入框（随类型重建）。 */
    private EditBox dimABox;
    private EditBox dimBBox;
    /** 颜色输入框。 */
    private EditBox hexBox;
    /** 线宽输入框（支持一位小数）。 */
    private EditBox widthBox;
    /** 预设旋转角度输入框（随类型重建）。 */
    private EditBox rotationBox;
    /** 预设缩放输入框（随类型重建）。 */
    private EditBox scaleBox;
    /** 保存 / 删除按钮（未创建时禁用）。 */
    private Button saveButton;
    private Button deleteButton;
    /** 随类型变化的控件（重建时移除）。 */
    private final List<AbstractWidget> geometryWidgets = new ArrayList<>();

    public ConsoleDrawScreen(ConsoleDrawMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // 无物品栏，隐藏默认的「物品栏」标签
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        this.geometryWidgets.clear();
        this.shapeId = this.menu.shapeId;
        this.pending = this.menu.pending;
        this.creating = false;
        initValues();

        int x = this.leftPos;
        int y = this.topPos;

        // 类型按钮：未创建时点击即创建该类型图形
        for (int i = 0; i < TYPES.length; i++) {
            final ConsoleShapeType target = TYPES[i];
            addRenderableWidget(Button.builder(Component.translatable(typeKey(target)),
                            b -> selectType(target))
                    .bounds(x + TYPE_X + i * TYPE_STEP, y + TYPE_Y, TYPE_W, TYPE_H).build());
        }

        // 颜色输入框（#RRGGBB）
        this.hexBox = new EditBox(this.font, x + 146, y + 62, 64, 14,
                Component.translatable("gui.cc_rc.draw.color"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setValue(String.format("#%06X", this.color));
        this.hexBox.setResponder(text -> {
            try {
                String value = text.startsWith("#") ? text.substring(1) : text;
                this.color = Integer.parseInt(value, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
                // 输入未完成时忽略
            }
        });
        addRenderableWidget(this.hexBox);

        // 线宽 0.5~4.0（支持一位小数）
        this.widthBox = new EditBox(this.font, x + 8, y + WIDTH_Y, 50, 14,
                Component.translatable("gui.cc_rc.draw.width"));
        this.widthBox.setMaxLength(5);
        this.widthBox.setValue(fmt(this.lineWidth));
        addRenderableWidget(this.widthBox);

        // 图层 0~7
        for (int i = 0; i < ConsoleShape.LAYERS; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)),
                            b -> this.layer = target)
                    .bounds(x + LAYER_X + i * 17, y + LAYER_Y, 15, 14).build());
        }

        // 位置（图案左下角）
        this.posUBox = addNumberBox(x + 20, y + POS_Y, 50, fmt(this.anchorU), false);
        this.posVBox = addNumberBox(x + 88, y + POS_Y, 50, fmt(this.anchorV), false);

        // 保存 / 删除（未创建时禁用） / 关闭
        this.saveButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.cc_rc.draw.save"), b -> save())
                .bounds(x + 8, y + BUTTON_Y, 76, 20).build());
        this.saveButton.active = !this.pending && this.shapeId >= 0;
        this.deleteButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.cc_rc.draw.delete"), b -> delete())
                .bounds(x + 88, y + BUTTON_Y, 76, 20).build());
        this.deleteButton.active = !this.pending && this.shapeId >= 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.cc_rc.draw.close"),
                        b -> onClose())
                .bounds(x + 168, y + BUTTON_Y, 76, 20).build());

        rebuildGeometry();
    }

    /** 从菜单初始化编辑值（并统一成「左下角锚点」的表示）。 */
    private void initValues() {
        // 未创建时不预先选中任何类型
        this.type = this.menu.pending ? null : this.menu.type;
        this.color = this.menu.color;
        this.lineWidth = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, this.menu.width)));
        this.layer = Math.max(0, Math.min(ConsoleShape.LAYERS - 1, this.menu.layer));
        this.presetId = this.menu.preset.isEmpty()
                ? ConsoleDrawPresets.DEFAULT_ID : this.menu.preset;
        this.presetRotation = ConsoleShape.quantize(
                (this.menu.rotation % 360.0F + 360.0F) % 360.0F);
        this.presetScale = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                Math.min(ConsoleShape.MAX_SCALE, this.menu.scale)));

        if (this.type == ConsoleShapeType.PRESET) {
            float half = presetHalf();
            this.anchorU = this.menu.u1 - half;
            this.anchorV = this.menu.v1 - half;
            this.dimA = 0.0F;
            this.dimB = 0.0F;
        } else if (this.type == ConsoleShapeType.RECT
                || this.type == ConsoleShapeType.RECT_OUTLINE) {
            // 矩形统一成左下角 + 正尺寸
            this.anchorU = Math.min(this.menu.u1, this.menu.u2);
            this.anchorV = Math.min(this.menu.v1, this.menu.v2);
            this.dimA = Math.abs(this.menu.u2 - this.menu.u1);
            this.dimB = Math.abs(this.menu.v2 - this.menu.v1);
        } else if (this.type == ConsoleShapeType.LINE || this.type == ConsoleShapeType.ARROW) {
            this.anchorU = this.menu.u1;
            this.anchorV = this.menu.v1;
            float du = this.menu.u2 - this.menu.u1;
            float dv = this.menu.v2 - this.menu.v1;
            this.dimA = (float) Math.hypot(du, dv);
            this.dimB = (float) Math.toDegrees(Math.atan2(dv, du));
        } else {
            // 未创建：使用打开界面时的点击位置与默认尺寸
            this.anchorU = this.menu.u1;
            this.anchorV = this.menu.v1;
            this.dimA = Math.max(1.0F, Math.abs(this.menu.u2 - this.menu.u1));
            this.dimB = Math.max(1.0F, Math.abs(this.menu.v2 - this.menu.v1));
        }
    }

    /** 点击类型按钮：未创建时立即创建，已存在时只切换类型。 */
    private void selectType(ConsoleShapeType target) {
        if (this.creating) {
            return;
        }
        if (!this.pending) {
            setType(target);
            return;
        }
        captureInputs();
        this.type = target;
        // 新建线段 / 箭头时默认长度 8、角度 0（矩形沿用 16×8 默认值）
        if (target == ConsoleShapeType.LINE || target == ConsoleShapeType.ARROW) {
            this.dimA = 8.0F;
            this.dimB = 0.0F;
        }
        rebuildGeometry();
        sendCreate();
    }

    /** 点击右侧贴图样式：未创建时以该图案创建，已存在时切换为预设图案。 */
    private void selectPreset(String id) {
        if (this.creating) {
            return;
        }
        captureInputs();
        this.presetId = id;
        if (this.pending) {
            this.type = ConsoleShapeType.PRESET;
            rebuildGeometry();
            sendCreate();
        } else if (this.type != ConsoleShapeType.PRESET) {
            setType(ConsoleShapeType.PRESET);
        }
    }

    /** 首次选择类型 / 贴图时请求服务端创建图形。 */
    private void sendCreate() {
        if (this.type == null) {
            return;
        }
        captureInputs();
        float[] p = points();
        this.creating = true;
        ModNetwork.CHANNEL.sendToServer(ConsoleDrawPacket.create(this.menu.pos,
                this.menu.face(), this.layer, this.type, this.color, this.lineWidth,
                p[0], p[1], p[2], p[3], this.presetId, this.presetRotation,
                this.presetScale));
    }

    /** 按当前类型把界面参数换算成图形两端点（预设类型返回中心点）。 */
    private float[] points() {
        switch (this.type) {
            case RECT, RECT_OUTLINE -> {
                return new float[]{this.anchorU, this.anchorV,
                        this.anchorU + Math.max(1.0F, this.dimA),
                        this.anchorV + Math.max(1.0F, this.dimB)};
            }
            case LINE, ARROW -> {
                float[] end = currentEnd();
                return new float[]{this.anchorU, this.anchorV, end[0], end[1]};
            }
            default -> {
                // 预设图案以中心点存储：把左下角锚点换算成中心
                float half = presetHalf();
                float cu = this.anchorU + half;
                float cv = this.anchorV + half;
                return new float[]{cu, cv, cu, cv};
            }
        }
    }

    /** 按当前类型重建「尺寸 / 预设」行控件。 */
    private void rebuildGeometry() {
        for (AbstractWidget widget : this.geometryWidgets) {
            removeWidget(widget);
        }
        this.geometryWidgets.clear();
        this.dimABox = null;
        this.dimBBox = null;
        this.rotationBox = null;
        this.scaleBox = null;

        int x = this.leftPos;
        int y = this.topPos;
        if (this.type == null) {
            return;
        }
        switch (this.type) {
            case RECT, RECT_OUTLINE -> {
                this.dimABox = addNumberBox(x + 22, y + GEOM_Y, 50, fmt(this.dimA), true);
                this.dimBBox = addNumberBox(x + 94, y + GEOM_Y, 50, fmt(this.dimB), true);
            }
            case LINE, ARROW -> {
                this.dimABox = addNumberBox(x + 30, y + GEOM_Y, 50, fmt(this.dimA), true);
                this.dimBBox = addNumberBox(x + 110, y + GEOM_Y, 50, fmt(this.dimB), true);
            }
            case PRESET -> {
                // 图案选择由右侧贴图样式列表完成，这里保留旋转角度与缩放输入框
                this.rotationBox = new EditBox(this.font, x + 30, y + GEOM_Y, 55, 14,
                        Component.translatable("gui.cc_rc.draw.rotation"));
                this.rotationBox.setMaxLength(6);
                this.rotationBox.setValue(fmt(this.presetRotation));
                addGeometryWidget(this.rotationBox);

                // 缩放倍数 0.1~3.0（支持一位小数）
                this.scaleBox = new EditBox(this.font, x + 118, y + GEOM_Y, 55, 14,
                        Component.translatable("gui.cc_rc.draw.preset_label"));
                this.scaleBox.setMaxLength(5);
                this.scaleBox.setValue(fmt(this.presetScale));
                addGeometryWidget(this.scaleBox);
            }
            default -> {
            }
        }
    }

    private void addGeometryWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
        this.geometryWidgets.add(widget);
    }

    private EditBox addNumberBox(int absX, int absY, int width, String value,
                                 boolean geometry) {
        EditBox box = new EditBox(this.font, absX, absY, width, 14, Component.empty());
        box.setMaxLength(8);
        box.setValue(value);
        if (geometry) {
            addGeometryWidget(box);
        } else {
            addRenderableWidget(box);
        }
        return box;
    }

    /** 切换类型：先把当前几何换算成端点，再按新类型推导尺寸参数。 */
    private void setType(ConsoleShapeType target) {
        if (target == this.type) {
            return;
        }
        captureInputs();
        float[] end = currentEnd();
        this.type = target;
        if (target == ConsoleShapeType.RECT || target == ConsoleShapeType.RECT_OUTLINE) {
            this.dimA = Math.max(1.0F, Math.abs(end[0] - this.anchorU));
            this.dimB = Math.max(1.0F, Math.abs(end[1] - this.anchorV));
        } else if (target == ConsoleShapeType.LINE || target == ConsoleShapeType.ARROW) {
            float du = end[0] - this.anchorU;
            float dv = end[1] - this.anchorV;
            this.dimA = Math.max(1.0F, (float) Math.hypot(du, dv));
            this.dimB = (float) Math.toDegrees(Math.atan2(dv, du));
        }
        rebuildGeometry();
    }

    /** 用输入框当前内容更新内存值（非法输入保留原值）。 */
    private void captureInputs() {
        this.anchorU = parse(this.posUBox, this.anchorU);
        this.anchorV = parse(this.posVBox, this.anchorV);
        // 线宽 / 缩放：夹到允许范围并量化到 0.1
        float width = parse(this.widthBox, this.lineWidth);
        this.lineWidth = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, width)));
        if (this.scaleBox != null) {
            float scale = parse(this.scaleBox, this.presetScale);
            this.presetScale = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                    Math.min(ConsoleShape.MAX_SCALE, scale)));
        }
        if (this.rotationBox != null) {
            // 旋转角度归一化到 [0, 360)
            float rot = parse(this.rotationBox, this.presetRotation);
            this.presetRotation = ConsoleShape.quantize((rot % 360.0F + 360.0F) % 360.0F);
        }
        if (this.dimABox != null) {
            this.dimA = parse(this.dimABox, this.dimA);
        }
        if (this.dimBBox != null) {
            this.dimB = parse(this.dimBBox, this.dimB);
        }
    }

    /** 当前类型下第二点（矩形对角 / 线段终点）的像素坐标。 */
    private float[] currentEnd() {
        if (this.type == null) {
            return new float[]{this.anchorU, this.anchorV};
        }
        switch (this.type) {
            case RECT, RECT_OUTLINE -> {
                return new float[]{this.anchorU + Math.max(1.0F, this.dimA),
                        this.anchorV + Math.max(1.0F, this.dimB)};
            }
            case LINE, ARROW -> {
                float length = Math.max(1.0F, this.dimA);
                double radians = Math.toRadians(this.dimB);
                return new float[]{this.anchorU + (float) (length * Math.cos(radians)),
                        this.anchorV + (float) (length * Math.sin(radians))};
            }
            default -> {
                float half = presetHalf();
                return new float[]{this.anchorU + half * 2.0F, this.anchorV + half * 2.0F};
            }
        }
    }

    /** 保存：换算成图形参数并发给服务端，成功后关闭界面。 */
    private void save() {
        if (this.pending || this.shapeId < 0 || this.type == null) {
            // 未创建时保存没有任何可写的图形（关闭界面也不会创建图案）
            return;
        }
        captureInputs();
        float[] p = points();
        ModNetwork.CHANNEL.sendToServer(ConsoleDrawPacket.edit(this.menu.pos, this.menu.face(),
                this.shapeId, this.layer, this.type, this.color, this.lineWidth,
                p[0], p[1], p[2], p[3], this.presetId, this.presetRotation,
                this.presetScale));
        onClose();
    }

    /** 删除当前图案并关闭界面。 */
    private void delete() {
        if (this.pending || this.shapeId < 0) {
            onClose();
            return;
        }
        ModNetwork.CHANNEL.sendToServer(ConsoleDrawPacket.delete(
                this.menu.pos, this.menu.face(), this.shapeId));
        onClose();
    }

    /**
     * 每 tick 轮询服务端的新建回执。
     * 注意：AbstractContainerScreen 的 tick() 是 final，不能覆盖，
     * 它会在内部调用 containerTick()，因此在这里处理。
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        // 取走服务端的新建回执：成功后切换到编辑状态，失败则回到「未选择」
        ConsoleDrawCreatedPacket ack = ConsoleDrawCreatedPacket.consumePending();
        if (ack == null || !ack.getPos().equals(this.menu.pos)) {
            return;
        }
        this.creating = false;
        if (ack.getShapeId() >= 0) {
            this.shapeId = ack.getShapeId();
            this.menu.shapeId = this.shapeId;
            this.menu.pending = false;
            this.pending = false;
            if (this.saveButton != null) {
                this.saveButton.active = true;
            }
            if (this.deleteButton != null) {
                this.deleteButton.active = true;
            }
        } else {
            // 创建失败（数量上限 / 数据无效）：清除选择，允许重新点选
            this.pending = true;
            this.menu.pending = true;
            this.type = null;
            rebuildGeometry();
        }
    }

    private float presetHalf() {
        ConsoleDrawPresets.Preset preset = ConsoleDrawPresets.byId(this.presetId);
        if (preset == null) {
            return 8.0F;
        }
        return preset.size() * ConsoleDrawPresets.DISPLAY_SCALE
                * Math.max(ConsoleShape.MIN_SCALE, this.presetScale) / 2.0F;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 背景与边框（全部用绝对坐标，与控件坐标一致，避免点击错位）
        graphics.fill(x, y, x + GUI_W, y + GUI_H, 0xE0101010);
        graphics.fill(x, y, x + GUI_W, y + 1, 0xFF808080);
        graphics.fill(x, y + GUI_H - 1, x + GUI_W, y + GUI_H, 0xFF808080);
        graphics.fill(x, y, x + 1, y + GUI_H, 0xFF808080);
        graphics.fill(x + GUI_W - 1, y, x + GUI_W, y + GUI_H, 0xFF808080);

        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.type"),
                x + 8, y + 20, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.color"),
                x + 8, y + 50, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.width"),
                x + 8, y + 88, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.layer"),
                x + LAYER_X, y + 88, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.position"),
                x + 8, y + 117, 0xFFFFFFFF, false);
        if (this.type != null) {
            graphics.drawString(this.font, geometryLabel(),
                    x + 8, y + 149, 0xFFFFFFFF, false);
        }

        // 位置输入框前的 X / Y 标记
        graphics.drawString(this.font, "X", x + 8, y + 132, 0xFFB0B0B0, false);
        graphics.drawString(this.font, "Y", x + 76, y + 132, 0xFFB0B0B0, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.anchor_hint"),
                x + 146, y + 132, 0xFF808080, false);

        // 尺寸 / 长度角度输入框前的标记
        if (this.type == ConsoleShapeType.RECT || this.type == ConsoleShapeType.RECT_OUTLINE) {
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.size_w"),
                    x + 8, y + 162, 0xFFB0B0B0, false);
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.size_h"),
                    x + 80, y + 162, 0xFFB0B0B0, false);
        } else if (this.type == ConsoleShapeType.LINE || this.type == ConsoleShapeType.ARROW) {
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.length"),
                    x + 8, y + 162, 0xFFB0B0B0, false);
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.angle"),
                    x + 88, y + 162, 0xFFB0B0B0, false);
        } else if (this.type == ConsoleShapeType.PRESET) {
            // 旋转角度 / 缩放输入框前的标记
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.rotation"),
                    x + 8, y + 162, 0xFFB0B0B0, false);
            graphics.drawString(this.font, "x", x + 108, y + 162, 0xFFB0B0B0, false);
        }

        // 颜色色块
        for (int i = 0; i < DYE_COLORS.length; i++) {
            int sx = x + swatchX(i);
            int sy = y + swatchY(i);
            graphics.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, 0xFF000000 | DYE_COLORS[i]);
            if ((this.color & 0xFFFFFF) == DYE_COLORS[i]) {
                highlight(graphics, sx, sy, SWATCH_W, SWATCH_H);
            }
        }

        // 右侧贴图样式列表
        renderPresetList(graphics);

        // 当前选中项高亮（类型 / 线宽 / 图层）
        if (this.type != null) {
            highlight(graphics, x + TYPE_X + typeIndex() * TYPE_STEP, y + TYPE_Y,
                    TYPE_W, TYPE_H);
        }
        highlight(graphics, x + LAYER_X + this.layer * 17, y + LAYER_Y, 15, 14);
    }

    /** 右侧贴图样式列表（白色蒙版缩略图，点击即选用该图案）。 */
    private void renderPresetList(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int panelLeft = x + PRESET_LIST_X - 6;
        int panelRight = x + PRESET_PANEL_RIGHT;
        int panelTop = y + PRESET_PANEL_TOP;
        int panelBottom = y + PRESET_PANEL_BOTTOM;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF1E1E1E);
        border(graphics, panelLeft, panelTop, panelRight, panelBottom, 0xFF606060);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.preset_list"),
                x + PRESET_LIST_X, y + 18, 0xFFFFFFFF, false);

        List<ConsoleDrawPresets.Preset> all = ConsoleDrawPresets.all();
        // 超出可见行的部分裁掉，滚轮翻看（见 mouseScrolled）
        graphics.enableScissor(panelLeft, panelTop, panelRight, panelBottom);
        for (int i = 0; i < all.size(); i++) {
            int row = i / PRESET_COLS - this.presetScroll;
            if (row < 0 || row >= PRESET_VISIBLE_ROWS) {
                continue;
            }
            ConsoleDrawPresets.Preset preset = all.get(i);
            int cx = x + presetCellX(i);
            int cy = y + PRESET_LIST_Y + row * (PRESET_CELL + PRESET_GAP);
            graphics.fill(cx, cy, cx + PRESET_CELL, cy + PRESET_CELL, 0xFF2A2A2A);
            int inner = PRESET_CELL - 4;
            // 11 参重载：把 size×size 的纯白蒙版缩放到格子内
            graphics.blit(preset.texture(), cx + 2, cy + 2, inner, inner,
                    0.0F, 0.0F, preset.size(), preset.size(), preset.size(), preset.size());
            boolean selected = this.type == ConsoleShapeType.PRESET
                    && this.presetId.equals(preset.id());
            border(graphics, cx, cy, cx + PRESET_CELL, cy + PRESET_CELL,
                    selected ? 0xFFFFFFFF : 0xFF606060);
        }
        graphics.disableScissor();
        // 可滚动时在面板右侧画一条滚动条指示
        int maxScroll = presetMaxScroll();
        if (maxScroll > 0) {
            int trackTop = panelTop + 2;
            int trackBottom = panelBottom - 2;
            int trackHeight = trackBottom - trackTop;
            int rows = presetRowCount();
            int thumbHeight = Math.max(8, trackHeight * PRESET_VISIBLE_ROWS / rows);
            int thumbTop = trackTop + (trackHeight - thumbHeight) * this.presetScroll / maxScroll;
            graphics.fill(panelRight - 3, trackTop, panelRight - 1, trackBottom, 0xFF303030);
            graphics.fill(panelRight - 3, thumbTop, panelRight - 1, thumbTop + thumbHeight,
                    0xFFA0A0A0);
        }
    }

    /** 贴图样式总行数（向上取整）。 */
    private static int presetRowCount() {
        int count = ConsoleDrawPresets.all().size();
        return (count + PRESET_COLS - 1) / PRESET_COLS;
    }

    /** 贴图样式列表的最大滚动行偏移。 */
    private static int presetMaxScroll() {
        return Math.max(0, presetRowCount() - PRESET_VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelLeft = this.leftPos + PRESET_LIST_X - 6;
        int panelRight = this.leftPos + PRESET_PANEL_RIGHT;
        int panelTop = this.topPos + PRESET_PANEL_TOP;
        int panelBottom = this.topPos + PRESET_PANEL_BOTTOM;
        if (mouseX >= panelLeft && mouseX < panelRight
                && mouseY >= panelTop && mouseY < panelBottom
                && presetMaxScroll() > 0) {
            int next = this.presetScroll - (int) Math.signum(delta);
            this.presetScroll = Math.max(0, Math.min(presetMaxScroll(), next));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** 选中项的白色描边。 */
    private static void highlight(GuiGraphics graphics, int x, int y, int w, int h) {
        border(graphics, x - 1, y - 1, x + w + 1, y + h + 1, 0xFFFFFFFF);
    }

    /** 空心矩形边框（右 / 下边界不含在填充范围内）。 */
    private static void border(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    /** 色块相对 GUI 左上角的 X。 */
    private static int swatchX(int index) {
        return SWATCH_X + (index % SWATCH_COLS) * (SWATCH_W + SWATCH_GAP_X);
    }

    /** 色块相对 GUI 左上角的 Y。 */
    private static int swatchY(int index) {
        return SWATCH_Y + (index / SWATCH_COLS) * (SWATCH_H + SWATCH_GAP_Y);
    }

    /** 贴图样式格子相对 GUI 左上角的 X。 */
    private static int presetCellX(int index) {
        return PRESET_LIST_X + (index % PRESET_COLS) * (PRESET_CELL + PRESET_GAP);
    }

    private int typeIndex() {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == this.type) {
                return i;
            }
        }
        return 0;
    }

    private static String typeKey(ConsoleShapeType type) {
        return switch (type) {
            case RECT -> "gui.cc_rc.draw.tool.rect";
            case RECT_OUTLINE -> "gui.cc_rc.draw.tool.rect_outline";
            case LINE -> "gui.cc_rc.draw.tool.line";
            case ARROW -> "gui.cc_rc.draw.tool.arrow";
            case PRESET -> "gui.cc_rc.draw.tool.preset";
        };
    }

    /** 尺寸行标题（随类型变化）。 */
    private Component geometryLabel() {
        if (this.type == null) {
            return Component.empty();
        }
        return switch (this.type) {
            case RECT, RECT_OUTLINE -> Component.translatable("gui.cc_rc.draw.size");
            case LINE, ARROW -> Component.translatable("gui.cc_rc.draw.length_angle");
            case PRESET -> Component.translatable("gui.cc_rc.draw.preset_label");
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 贴图样式列表：手动绘制，命中区域与绘制坐标使用同一套换算
            List<ConsoleDrawPresets.Preset> all = ConsoleDrawPresets.all();
            for (int i = 0; i < all.size(); i++) {
                // 只命中当前滚动位置下可见的格子
                int row = i / PRESET_COLS - this.presetScroll;
                if (row < 0 || row >= PRESET_VISIBLE_ROWS) {
                    continue;
                }
                int cx = this.leftPos + presetCellX(i);
                int cy = this.topPos + PRESET_LIST_Y + row * (PRESET_CELL + PRESET_GAP);
                if (mouseX >= cx && mouseX < cx + PRESET_CELL
                        && mouseY >= cy && mouseY < cy + PRESET_CELL) {
                    selectPreset(all.get(i).id());
                    return true;
                }
            }
            // 颜色色块
            for (int i = 0; i < DYE_COLORS.length; i++) {
                int sx = this.leftPos + swatchX(i);
                int sy = this.topPos + swatchY(i);
                if (mouseX >= sx && mouseX < sx + SWATCH_W
                        && mouseY >= sy && mouseY < sy + SWATCH_H) {
                    this.color = DYE_COLORS[i];
                    if (this.hexBox != null) {
                        this.hexBox.setValue(String.format("#%06X", this.color));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 数值格式化：整数不带小数点，其余保留 1 位小数。 */
    private static String fmt(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05F) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** 读取输入框数值，非法时返回原值。 */
    private static float parse(EditBox box, float fallback) {
        if (box == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
