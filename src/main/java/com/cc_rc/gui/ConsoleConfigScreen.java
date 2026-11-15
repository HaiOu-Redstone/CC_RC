package com.cc_rc.gui;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.network.ConsoleConfigUploadPacket;
import com.cc_rc.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 控制柜配置界面：保存 / 加载多方块结构配置。
 *
 * 保存：把总线（结构尺寸与朝向、面板组件、表面文字、变量表、辅助计算机程序）
 * 导出为可读 JSON，写入游戏目录下的 cc_rc_configs/&lt;名称&gt;.json。
 * 加载：读取本地 JSON → 若总线已有数据则先确认覆盖 → 上传服务端校验并整体替换。
 *
 * 服务器环境下文件始终保存在玩家客户端电脑上，服务端只接收上传的 JSON 文本。
 * 加载时允许「总线与结构一起旋转」：服务端按保存朝向与当前朝向的差值旋转配置，
 * 并按旋转后的 X/Z 尺寸校验结构是否匹配。
 */
public class ConsoleConfigScreen extends Screen {

    private static final int GUI_W = 300;
    private static final int GUI_H = 196;

    /** 列表区域：8 行，每行 12 像素。 */
    private static final int LIST_X = 10;
    private static final int LIST_Y = 44;
    private static final int LIST_W = GUI_W - 20;
    private static final int LIST_H = 96;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = LIST_H / ROW_H;

    private static final int NAME_LABEL_X = 10;
    private static final int NAME_LABEL_Y = 24;
    private static final int NAME_X = 52;
    private static final int NAME_Y = 22;
    private static final int NAME_W = GUI_W - 62;
    private static final int NAME_H = 16;

    private static final int STATUS_Y = 146;
    private static final int BTN_Y = 164;
    private static final int BTN_H = 20;

    private static final int COLOR_TEXT = 0xFF303030;
    private static final int COLOR_DIM = 0xFF666666;
    private static final int COLOR_ERROR = 0xFFB02020;
    private static final int COLOR_OK = 0xFF1E7A1E;
    private static final int COLOR_WARN = 0xFF9A6A00;
    private static final int COLOR_LIST_BG = 0xFF141414;
    private static final int COLOR_LIST_HIGHLIGHT = 0xFF4A4A4A;
    private static final int COLOR_ROW_SELECTED = 0xFF3A3A6A;
    private static final int COLOR_TEXT_LIGHT = 0xFFE0E0E0;
    private static final int COLOR_DIM_LIGHT = 0xFF9A9A9A;

    /** 返回按钮打开的父界面（控制柜总线界面）。 */
    private final Screen parent;
    /** 总线方块坐标。 */
    private final BlockPos pos;

    private int px;
    private int py;

    private EditBox nameBox;
    private Button saveButton;
    private Button loadButton;
    private Button confirmButton;
    private Button cancelButton;
    private Button backButton;

    private final List<String> files = new ArrayList<>();
    private int scroll;
    private int selected = -1;

    /** 状态提示文本与颜色。 */
    private String status = "";
    private int statusColor = COLOR_DIM;

    /** 等待服务端回执（期间禁用按钮，避免重复上传）。 */
    private boolean waiting;
    /** 待上传的 JSON 内容（确认覆盖后使用）。 */
    private String pendingJson;
    /** 待覆盖保存的文件名（确认覆盖后使用）。 */
    private String pendingSaveName;

    public ConsoleConfigScreen(Screen parent, BlockPos pos) {
        super(Component.literal("控制柜配置"));
        this.parent = parent;
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        this.px = (this.width - GUI_W) / 2;
        this.py = (this.height - GUI_H) / 2;

        // 文件名输入框
        this.nameBox = new EditBox(this.font, this.px + NAME_X, this.py + NAME_Y,
                NAME_W, NAME_H, Component.literal("文件名"));
        this.nameBox.setMaxLength(ConsoleConfigStore.MAX_NAME_LEN);
        this.nameBox.setHint(Component.literal("输入文件名（不含 .json）"));
        this.addRenderableWidget(this.nameBox);

        // 底部按钮
        this.saveButton = vanillaButton(this.px + 10, this.py + BTN_Y, 68, BTN_H,
                Component.translatable("gui.cc_rc.console.config.save"), b -> save());
        this.loadButton = vanillaButton(this.px + 82, this.py + BTN_Y, 68, BTN_H,
                Component.translatable("gui.cc_rc.console.config.load"), b -> load());
        this.confirmButton = vanillaButton(this.px + 82, this.py + BTN_Y, 68, BTN_H,
                Component.literal("确认覆盖"), b -> confirmOverwrite());
        this.cancelButton = vanillaButton(this.px + 154, this.py + BTN_Y, 52, BTN_H,
                Component.literal("取消"), b -> cancelConfirm());
        this.backButton = vanillaButton(this.px + GUI_W - 70, this.py + BTN_Y, 60, BTN_H,
                Component.literal("返回"), b -> this.minecraft.setScreen(this.parent));

        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.loadButton);
        this.addRenderableWidget(this.confirmButton);
        this.addRenderableWidget(this.cancelButton);
        this.addRenderableWidget(this.backButton);

        this.confirmButton.visible = false;
        this.cancelButton.visible = false;

        refreshFiles();
        if (this.selected >= this.files.size()) {
            this.selected = -1;
        }
    }

    /** 重新读取本地配置列表并夹紧滚动位置。 */
    private void refreshFiles() {
        this.files.clear();
        this.files.addAll(ConsoleConfigStore.list());
        int maxScroll = Math.max(0, this.files.size() - VISIBLE_ROWS);
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
    }

    /** 读取客户端方块实体。 */
    private ConsoleBusBlockEntity bus() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.pos) instanceof ConsoleBusBlockEntity be
                ? be : null;
    }

    // ---------------------------------------------------------------- 操作

    /** 保存：导出总线配置并写入本地文件；文件已存在时先请求玩家确认覆盖。 */
    private void save() {
        if (this.waiting) {
            return;
        }
        String name = ConsoleConfigStore.sanitize(this.nameBox.getValue());
        if (name.isEmpty()) {
            setStatus("请输入文件名", COLOR_ERROR);
            return;
        }
        ConsoleBusBlockEntity be = bus();
        if (be == null) {
            setStatus("找不到控制柜总线", COLOR_ERROR);
            return;
        }
        if (be.getFormState() == ConsoleBusBlockEntity.State.UNFORMED) {
            setStatus("结构未成型，无法保存配置", COLOR_ERROR);
            return;
        }
        if (ConsoleConfigStore.exists(name)) {
            // 覆盖已有文件前必须确认，避免误触覆盖手工编辑过的配置
            this.pendingSaveName = name;
            enterConfirm("文件 " + name + ".json 已存在，确认覆盖？");
            return;
        }
        doSave(name);
    }

    /** 实际导出并写入文件（新文件与确认覆盖后共用）。 */
    private void doSave(String name) {
        resetConfirm();
        ConsoleBusBlockEntity be = bus();
        if (be == null) {
            setStatus("找不到控制柜总线", COLOR_ERROR);
            return;
        }
        try {
            ConsoleConfigStore.write(name, be.exportConfigJson());
            this.nameBox.setValue(name);
            refreshFiles();
            setStatus("已保存到 " + ConsoleConfigStore.DIR_NAME + "/" + name + ".json", COLOR_OK);
        } catch (Exception ex) {
            setStatus("保存失败：" + ex.getMessage(), COLOR_ERROR);
        }
    }

    /** 加载：读取本地文件，必要时先请求玩家确认覆盖。 */
    private void load() {
        if (this.waiting) {
            return;
        }
        String name = ConsoleConfigStore.sanitize(this.nameBox.getValue());
        if (name.isEmpty()) {
            setStatus("请选择或输入要加载的文件名", COLOR_ERROR);
            return;
        }
        String json = ConsoleConfigStore.read(name);
        if (json == null) {
            setStatus("文件不存在：" + name + ".json", COLOR_ERROR);
            return;
        }
        this.pendingJson = json;
        ConsoleBusBlockEntity be = bus();
        if (be != null && be.hasConfigData()) {
            // 总线上已有组件 / 文字 / 变量 / 程序：先确认再覆盖
            enterConfirm("控制柜已有组件 / 文字 / 变量 / 程序，确认覆盖？");
        } else {
            sendConfig(json);
        }
    }

    /** 确认覆盖：按当前待处理动作执行保存或加载。 */
    private void confirmOverwrite() {
        String saveName = this.pendingSaveName;
        String json = this.pendingJson;
        if (saveName == null && json == null) {
            cancelConfirm();
            return;
        }
        if (saveName != null) {
            doSave(saveName);
        } else {
            sendConfig(json);
        }
    }

    /** 取消覆盖，回到普通状态。 */
    private void cancelConfirm() {
        resetConfirm();
        setStatus("已取消", COLOR_DIM);
    }

    /** 退出确认状态：清空待处理动作并恢复按钮显示。 */
    private void resetConfirm() {
        this.pendingJson = null;
        this.pendingSaveName = null;
        this.loadButton.visible = true;
        this.confirmButton.visible = false;
        this.cancelButton.visible = false;
        this.saveButton.active = true;
    }

    /** 进入「确认覆盖」状态。 */
    private void enterConfirm(String message) {
        this.loadButton.visible = false;
        this.confirmButton.visible = true;
        this.cancelButton.visible = true;
        this.saveButton.active = false;
        setStatus(message, COLOR_WARN);
    }

    /** 把 JSON 上传给服务端校验。 */
    private void sendConfig(String json) {
        resetConfirm();
        this.waiting = true;
        this.saveButton.active = false;
        this.loadButton.active = false;
        this.backButton.active = false;
        ModNetwork.CHANNEL.sendToServer(new ConsoleConfigUploadPacket(this.pos, json));
        setStatus("已上传，等待服务器校验…", COLOR_DIM);
    }

    /** 服务端回执：显示结果并恢复按钮。 */
    public void onServerResult(int result) {
        this.waiting = false;
        this.saveButton.active = true;
        this.loadButton.active = true;
        this.backButton.active = true;
        switch (result) {
            case ConsoleBusBlockEntity.CFG_OK ->
                    setStatus("配置加载成功", COLOR_OK);
            case ConsoleBusBlockEntity.CFG_BAD_JSON ->
                    setStatus("加载失败：文件不是合法的配置 JSON", COLOR_ERROR);
            case ConsoleBusBlockEntity.CFG_NOT_FORMED ->
                    setStatus("加载失败：需要先让结构部分成型", COLOR_ERROR);
            case ConsoleBusBlockEntity.CFG_SIZE_MISMATCH ->
                    setStatus("加载失败：结构尺寸（按旋转后计算）与保存时不一致", COLOR_ERROR);
            case ConsoleBusBlockEntity.CFG_FACING_MISMATCH ->
                    setStatus("加载失败：配置文件中的总线朝向无效", COLOR_ERROR);
            case ConsoleBusBlockEntity.CFG_LIMIT ->
                    setStatus("加载失败：组件 / 文字 / 变量 / 程序行数量超过上限", COLOR_ERROR);
            default -> setStatus("加载失败：配置内容非法或已被修改", COLOR_ERROR);
        }
    }

    private void setStatus(String text, int color) {
        this.status = text == null ? "" : text;
        this.statusColor = color;
    }

    /** Esc / 关闭时回到控制柜总线界面，而不是退回游戏。 */
    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    // ---------------------------------------------------------------- 输入

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 列表点击优先：选中文件并填入文件名输入框
        int index = listIndexAt(mouseX, mouseY);
        if (index >= 0) {
            this.selected = index;
            this.nameBox.setValue(this.files.get(index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inList(mouseX, mouseY)) {
            int maxScroll = Math.max(0, this.files.size() - VISIBLE_ROWS);
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** 鼠标所在行的文件下标；不在列表区域或有空行时返回 -1。 */
    private int listIndexAt(double mouseX, double mouseY) {
        if (!inList(mouseX, mouseY)) {
            return -1;
        }
        int row = (int) ((mouseY - (this.py + LIST_Y)) / ROW_H);
        int index = this.scroll + row;
        return index >= 0 && index < this.files.size() ? index : -1;
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= this.px + LIST_X && mouseX < this.px + LIST_X + LIST_W
                && mouseY >= this.py + LIST_Y && mouseY < this.py + LIST_Y + LIST_H;
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawInterface(graphics, mouseX, mouseY);
        // 按钮等控件最后绘制，保证在面板文字之上
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 绘制面板、列表与状态文字。 */
    private void drawInterface(GuiGraphics graphics, int mouseX, int mouseY) {
        // 浅灰凸起面板
        drawRaised(graphics, this.px, this.py, GUI_W, GUI_H, 0xFFC6C6C6);
        graphics.drawString(this.font, this.title, this.px + 8, this.py + 6, COLOR_TEXT, false);
        graphics.fill(this.px + 6, this.py + 17, this.px + GUI_W - 6, this.py + 18, 0xFF555555);

        graphics.drawString(this.font, "文件名", this.px + NAME_LABEL_X,
                this.py + NAME_LABEL_Y, COLOR_TEXT, false);

        // 列表：深色凹槽
        drawSunken(graphics, this.px + LIST_X - 2, this.py + LIST_Y - 2,
                LIST_W + 4, LIST_H + 4, COLOR_LIST_BG);
        int maxScroll = Math.max(0, this.files.size() - VISIBLE_ROWS);
        // 页码提示（列表右上角，位于凹槽外）
        if (maxScroll > 0) {
            int page = this.scroll / VISIBLE_ROWS + 1;
            int pages = (this.files.size() + VISIBLE_ROWS - 1) / VISIBLE_ROWS;
            String pageText = "第 " + page + " / " + pages + " 页";
            graphics.drawString(this.font, pageText,
                    this.px + LIST_X + LIST_W - this.font.width(pageText),
                    this.py + LIST_Y - 12, COLOR_DIM, false);
        }
        if (this.files.isEmpty()) {
            graphics.drawString(this.font, "（还没有配置文件，输入文件名后点击「保存配置」）",
                    this.px + LIST_X + 4, this.py + LIST_Y + 4, COLOR_DIM_LIGHT, false);
        } else {
            int hoverIndex = listIndexAt(mouseX, mouseY);
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = this.scroll + row;
                if (index >= this.files.size()) {
                    break;
                }
                int rowY = this.py + LIST_Y + row * ROW_H;
                if (index == this.selected || index == hoverIndex) {
                    graphics.fill(this.px + LIST_X, rowY,
                            this.px + LIST_X + LIST_W, rowY + ROW_H,
                            index == this.selected ? COLOR_ROW_SELECTED : 0xFF232323);
                }
                graphics.drawString(this.font, this.files.get(index),
                        this.px + LIST_X + 4, rowY + 2, COLOR_TEXT_LIGHT, false);
            }
        }

        // 状态提示
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font, this.status,
                    this.px + 10, this.py + STATUS_Y, this.statusColor, false);
        } else {
            graphics.drawString(this.font, "目录：" + ConsoleConfigStore.dir().getAbsolutePath(),
                    this.px + 10, this.py + STATUS_Y, COLOR_DIM, false);
        }
    }

    /** 原版凸起斜面（与程序 / 组件界面同一套配色）。 */
    private static void drawRaised(GuiGraphics graphics, int x, int y, int w, int h, int face) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** 原版凹陷槽（列表 / 输入框底）。 */
    private static void drawSunken(GuiGraphics graphics, int x, int y, int w, int h, int face) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF373737);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, COLOR_LIST_HIGHLIGHT);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, COLOR_LIST_HIGHLIGHT);
    }

    /** 原版风格按钮（浅灰凸起斜面 + 无阴影文字）。 */
    private static Button vanillaButton(int x, int y, int w, int h,
                                        Component text, Button.OnPress onPress) {
        return new VanillaButton(x, y, w, h, text, onPress);
    }

    private static final class VanillaButton extends Button {
        private VanillaButton(int x, int y, int w, int h,
                              Component message, OnPress onPress) {
            super(x, y, w, h, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hover = this.active && mouseX >= x && mouseX < x + w
                    && mouseY >= y && mouseY < y + h;
            drawRaised(graphics, x, y, w, h, this.active ? 0xFFC6C6C6 : 0xFF9E9E9E);
            if (hover) {
                graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFB00000);
                graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFFB00000);
                graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFB00000);
                graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFFB00000);
            }
            int color = !this.active ? 0xFF707070 : (hover ? 0xFF000000 : 0xFF303030);
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            Component message = this.getMessage();
            int tx = x + (w - font.width(message)) / 2;
            int ty = y + (h - 8) / 2;
            graphics.drawString(font, message, tx, ty, color, false);
        }
    }
}
