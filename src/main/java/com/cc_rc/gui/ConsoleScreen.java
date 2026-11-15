package com.cc_rc.gui;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.network.ConsoleStructurePacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 控制台总线界面（自绘深色面板，不使用背景贴图）。
 *
 * 第一期内容：
 *  - 显示成型状态、结构尺寸（x × y × z 格）、总格数；
 *  - 校验失败时显示红色原因文字；
 *  - 三个按钮：[检测并成型] [完全成型] [解除锁定]，按当前状态自动启用/禁用。
 *
 * 实时刷新：每帧直接从客户端方块实体读取数据（结构变化时服务端会 sendBlockUpdated），
 * 因此按钮点完立即反映结果，无需重开界面。
 */
public class ConsoleScreen extends AbstractContainerScreen<ConsoleMenu> {

    private static final int BG_WIDTH = 220;
    private static final int BG_HEIGHT = 172;

    private static final int LINE_STATE = 26;
    private static final int LINE_SIZE = 42;
    private static final int LINE_COUNT = 56;
    private static final int LINE_ERROR = 72;

    private static final int BTN_W = 100;
    private static final int BTN_H = 18;
    private static final int BTN_X0 = 10;
    private static final int BTN_X1 = 110;
    private static final int BTN_Y0 = 104;
    private static final int BTN_Y1 = 126;
    private static final int BTN_Y2 = 148;

    private Button formButton;
    private Button lockButton;
    private Button unlockButton;
    private Button configButton;

    public ConsoleScreen(ConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("gui.cc_rc.console.title"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        this.formButton = Button.builder(Component.translatable("gui.cc_rc.console.form"),
                        b -> send(ConsoleStructurePacket.ACTION_FORM))
                .bounds(cx + BTN_X0, cy + BTN_Y0, BTN_W, BTN_H).build();
        this.lockButton = Button.builder(Component.translatable("gui.cc_rc.console.lock"),
                        b -> send(ConsoleStructurePacket.ACTION_LOCK))
                .bounds(cx + BTN_X1, cy + BTN_Y0, BTN_W, BTN_H).build();
        this.unlockButton = Button.builder(Component.translatable("gui.cc_rc.console.unlock"),
                        b -> send(ConsoleStructurePacket.ACTION_UNLOCK))
                .bounds(cx + BTN_X0, cy + BTN_Y1, BTN_W, BTN_H).build();
        // 保存 / 加载配置：打开配置界面，里面再分「保存配置」「加载配置」两个按钮
        this.configButton = Button.builder(Component.translatable("gui.cc_rc.console.config"),
                        b -> openConfig())
                .bounds(cx + BTN_X0, cy + BTN_Y2, BTN_W * 2, BTN_H).build();

        this.addRenderableWidget(this.formButton);
        this.addRenderableWidget(this.lockButton);
        this.addRenderableWidget(this.unlockButton);
        this.addRenderableWidget(this.configButton);
    }

    /** 打开配置界面（保存与加载共用同一个界面）。 */
    private void openConfig() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ConsoleConfigScreen(this, this.menu.pos));
        }
    }

    private void send(int action) {
        ModNetwork.CHANNEL.sendToServer(new ConsoleStructurePacket(this.menu.pos, action));
    }

    /** 读取客户端方块实体（未加载或类型不符时返回 null）。 */
    private ConsoleBusBlockEntity bus() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.menu.pos) instanceof ConsoleBusBlockEntity be
                ? be : null;
    }

    /** 每个客户端 tick 按当前结构状态刷新按钮可用性。 */
    @Override
    protected void containerTick() {
        super.containerTick();
        ConsoleBusBlockEntity be = bus();
        if (be == null) {
            return;
        }
        boolean full = be.getFormState() == ConsoleBusBlockEntity.State.FULL;
        boolean partial = be.getFormState() == ConsoleBusBlockEntity.State.PARTIAL;
        boolean unformed = be.getFormState() == ConsoleBusBlockEntity.State.UNFORMED;
        this.formButton.active = !full;
        this.lockButton.active = partial;
        this.unlockButton.active = full;
        // 未成型没有可靠的尺寸 / 表面，保存与加载都没有意义；其余状态可打开配置界面
        this.configButton.active = !unformed;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 自绘深色面板 + 浅灰边框
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + BG_WIDTH;
        int y1 = y0 + BG_HEIGHT;
        graphics.fill(x0, y0, x1, y1, 0xFF1E1E1E);
        graphics.fill(x0, y0, x1, y0 + 1, 0xFF808080);
        graphics.fill(x0, y1 - 1, x1, y1, 0xFF808080);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF808080);
        graphics.fill(x1 - 1, y0, x1, y1, 0xFF808080);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);

        ConsoleBusBlockEntity be = bus();
        if (be == null) {
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.console.missing"),
                    BTN_X0, LINE_STATE, 0xFF5555, false);
            return;
        }

        // 成型状态
        graphics.drawString(this.font,
                Component.translatable("gui.cc_rc.console.state",
                        Component.translatable(stateKey(be.getFormState()))),
                BTN_X0, LINE_STATE, 0xE0E0E0, false);

        // 结构尺寸与格数（未成型时为「-」）
        if (be.getFormState() == ConsoleBusBlockEntity.State.UNFORMED) {
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.console.size_none"),
                    BTN_X0, LINE_SIZE, 0x9A9A9A, false);
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.console.count_none"),
                    BTN_X0, LINE_COUNT, 0x9A9A9A, false);
        } else {
            graphics.drawString(this.font,
                    Component.translatable("gui.cc_rc.console.size",
                            be.getSizeX(), be.getSizeY(), be.getSizeZ()),
                    BTN_X0, LINE_SIZE, 0xE0E0E0, false);
            graphics.drawString(this.font,
                    Component.translatable("gui.cc_rc.console.count", be.getMemberCount()),
                    BTN_X0, LINE_COUNT, 0xE0E0E0, false);
        }

        // 校验失败原因（红色）
        if (be.getError() != ConsoleStructureHelper.Error.NONE) {
            graphics.drawString(this.font,
                    Component.translatable(errorKey(be.getError())),
                    BTN_X0, LINE_ERROR, 0xFF5555, false);
        }

        // 完全成型提示（绿色）
        if (be.getFormState() == ConsoleBusBlockEntity.State.FULL) {
            graphics.drawString(this.font, Component.translatable("gui.cc_rc.console.full_hint"),
                    BTN_X0, LINE_ERROR, 0x55FF55, false);
        }
    }

    private static String stateKey(ConsoleBusBlockEntity.State state) {
        return switch (state) {
            case UNFORMED -> "gui.cc_rc.console.state.unformed";
            case PARTIAL -> "gui.cc_rc.console.state.partial";
            case FULL -> "gui.cc_rc.console.state.full";
        };
    }

    private static String errorKey(ConsoleStructureHelper.Error error) {
        return switch (error) {
            case NO_BLOCK -> "gui.cc_rc.console.error.no_block";
            case MULTIPLE -> "gui.cc_rc.console.error.multiple";
            case TOO_LARGE -> "gui.cc_rc.console.error.too_large";
            case NOT_RECTANGLE -> "gui.cc_rc.console.error.not_rectangle";
            case NONE -> "gui.cc_rc.console.error.none";
        };
    }
}
