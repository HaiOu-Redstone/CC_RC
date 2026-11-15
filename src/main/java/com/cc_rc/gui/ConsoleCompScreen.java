package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.network.ConsoleCompPacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 控制台表面组件的设置界面。
 *
 * 界面内容：
 *  - 组件类型与所在表面（只读展示）；
 *  - 组件代号输入框（英文 / 数字 / 下划线）；
 *  - 组件位置 X / Y（结构像素，相对表面中心，可负值）；
 *  - 组件颜色：16 个染料色块 + 手动 RGB 输入框（#RRGGBB）+「原色」恢复贴图原样；
 *  - 「完成」写回代号、位置与颜色，「删除」移除该组件。
 *
 * 颜色的作用范围由组件类型决定（见 ConsoleCompType.recolorable）：
 *  - 流量表：表盘底色（红色指针保持原色）；按键 / 指示灯：中间的按键区与灯面；
 *  - 显示屏 / 发信终端：暂不改变外观，颜色保存下来供之后显示文字使用。
 *
 * 背景使用与文字编辑界面同款的专属贴图（圆角描边 + 上/左白高光 + 下/右深灰阴影 + 内部淡灰），
 * 仅标题下的分隔线与颜色预览块仍用纯色绘制。
 */
public class ConsoleCompScreen extends AbstractContainerScreen<ConsoleCompMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 174;
    /** RGB 颜色控制器的界面更高：需要同时容纳颜色区与「连接 / 通道 / 显示原色」区。 */
    private static final int RGB_BG_HEIGHT = 212;
    /** 背景贴图：与文字编辑界面同一套边框样式（1px 圆角黑描边 + 上/左白高光 + 下/右深灰阴影）。 */
    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_comp_gui.png");
    /** 加高版背景贴图（176×212）：由标准背景纵向拉伸中部得到，外观与标准背景一致。 */
    private static final ResourceLocation GUI_BG_TALL =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_comp_gui_tall.png");

    // 控件布局（相对 GUI 左上角）
    private static final int TYPE_Y = 20;
    private static final int CODE_Y = 34;
    private static final int CODE_X = 44;
    private static final int CODE_W = 120;
    private static final int BOX_H = 16;
    private static final int POS_Y = 56;
    private static final int X_LABEL_X = 12;
    private static final int X_BOX_X = 22;
    private static final int Y_LABEL_X = 60;
    private static final int Y_BOX_X = 68;
    private static final int POS_BOX_W = 36;
    // 组件缩放（1.0 = 模型默认大小）
    private static final int SCALE_LABEL_X = 108;
    private static final int SCALE_BOX_X = 124;
    private static final int SCALE_BOX_W = 36;
    // 颜色色块区：8 列 × 2 行染料色
    private static final int COLOR_X = 12;
    private static final int COLOR_Y = 78;
    private static final int COLOR_W = 16;
    private static final int COLOR_H = 10;
    private static final int COLOR_GAP_X = 2;
    private static final int COLOR_GAP_Y = 2;
    private static final int COLOR_COLS = 8;
    // RGB 输入行：标签 + 输入框 + 原色按钮 + 预览块
    private static final int HEX_Y = 104;
    private static final int HEX_LABEL_X = 12;
    private static final int HEX_BOX_X = 34;
    private static final int HEX_BOX_W = 58;
    private static final int RESET_X = 96;
    private static final int RESET_W = 34;
    private static final int PREVIEW_X = 134;
    private static final int PREVIEW_W = 16;
    // 数字显示屏专用：小数位 + 单位（仅类型为数码显示器时显示）
    private static final int DISPLAY_Y = 124;
    private static final int DEC_LABEL_X = 12;
    private static final int DEC_BOX_X = 44;
    private static final int DEC_BOX_W = 24;
    private static final int UNIT_LABEL_X = 84;
    private static final int UNIT_BOX_X = 104;
    private static final int UNIT_BOX_W = 60;
    // 按钮专用：弹起时间（仅类型为按钮面板时显示；0 = 松手立刻弹起）
    private static final int HOLD_LABEL_X = 12;
    private static final int HOLD_BOX_X = 66;
    private static final int HOLD_BOX_W = 40;
    // 圆盘记录仪专用：第二名称（推送触发名称，仅类型为圆盘记录仪时显示）
    private static final int CODE2_LABEL_X = 12;
    private static final int CODE2_BOX_X = 56;
    private static final int CODE2_BOX_W = 108;
    // 同步指示器专用：转速变量名（第二个浮点变量；留空 = 自动使用 代号 + "_s"）
    private static final int SPEED_LABEL_X = 12;
    private static final int SPEED_BOX_X = 64;
    private static final int SPEED_BOX_W = 100;
    // 同步指示器专用：灯亮状态变量名（第三行；留空 = 自动使用 代号 + "_b"）
    private static final int LAMP_Y = 146;
    private static final int LAMP_LABEL_X = 12;
    private static final int LAMP_BOX_X = 64;
    private static final int LAMP_BOX_W = 100;
    // 安全按钮专用：盖子自动关闭时间（第二行；0 = 不自动关闭）
    private static final int COVER_LABEL_X = 12;
    private static final int COVER_BOX_X = 88;
    private static final int COVER_BOX_W = 44;
    // RGB 颜色控制器专用：连接的组件（多个用「;」分隔）
    private static final int LINKS_Y = 124;
    private static final int LINKS_LABEL_X = 12;
    private static final int LINKS_BOX_X = 44;
    private static final int LINKS_BOX_W = 120;
    // RGB 颜色控制器专用：R / G / B 三个通道变量名
    private static final int RGB_Y = 146;
    private static final int RGB_LABEL_R_X = 12;
    private static final int RGB_BOX_R_X = 22;
    private static final int RGB_LABEL_G_X = 62;
    private static final int RGB_BOX_G_X = 72;
    private static final int RGB_LABEL_B_X = 112;
    private static final int RGB_BOX_B_X = 122;
    private static final int RGB_BOX_W = 36;
    // RGB 颜色控制器专用：是否显示原色（= 停用控制）开关
    private static final int SHOW_ORIGINAL_Y = 168;
    private static final int SHOW_ORIGINAL_X = 12;
    private static final int SHOW_ORIGINAL_W = 100;
    // 底部按钮（RGB 界面更高，按钮整体下移）
    private static final int BUTTON_Y = 148;
    private static final int RGB_BUTTON_Y = 188;
    private static final int DONE_X = 12;
    private static final int DONE_W = 100;
    private static final int DELETE_X = 118;
    private static final int DELETE_W = 46;
    private static final int BUTTON_H = 20;

    private EditBox codeBox;
    private EditBox uBox;
    private EditBox vBox;
    /** 组件缩放（1.0 = 模型默认大小，实际大小还会乘以模型默认缩放）。 */
    private EditBox scaleBox;
    private EditBox colorBox;
    /** 数字显示屏：保留小数位（仅数码显示器存在该输入框）。 */
    private EditBox decimalsBox;
    /** 数字显示屏：单位（仅数码显示器存在该输入框）。 */
    private EditBox unitBox;
    /** 按钮面板：弹起时间（tick；0 = 松手立刻弹起）。 */
    private EditBox holdBox;
    /** 安全按钮：盖子自动关闭时间（tick；0 = 不自动关闭）。 */
    private EditBox coverBox;
    /** 圆盘记录仪：第二名称（推送触发名称，可留空）。 */
    private EditBox code2Box;
    /** 同步指示器：转速变量名（默认 = 代号 + "_s"）。 */
    private EditBox speedBox;
    /** 同步指示器：灯亮状态变量名（默认 = 代号 + "_b"）。 */
    private EditBox lampBox;
    /** 警报器：静音状态变量名（默认 = 代号 + "_Q"）。 */
    private EditBox muteBox;
    /** RGB 颜色控制器：连接的组件代号（多个用「;」分隔，可留空）。 */
    private EditBox linksBox;
    /** RGB 颜色控制器：R / G / B 三个通道变量名（可留空）。 */
    private EditBox rgbRBox;
    private EditBox rgbGBox;
    private EditBox rgbBBox;
    /** RGB 颜色控制器：是否显示原色（= 停用控制）。 */
    private boolean showOriginal;
    /** RGB 颜色控制器：「显示原色」开关按钮（需要随点击更新文字）。 */
    private Button showOriginalButton;
    /** 当前颜色（0xRRGGBB）；-1 表示保持贴图原样。 */
    private int color;
    /** 钥匙开关：钥匙模式（0 = 开闭，1 = 常开，2 = 常闭）。 */
    private int keyMode;
    /** 钥匙开关：「钥匙模式」循环切换按钮（需要随点击更新文字）。 */
    private Button keyModeButton;

    public ConsoleCompScreen(ConsoleCompMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("组件设置"));
        this.imageWidth = BG_WIDTH;
        // RGB 颜色控制器与同步指示器多出一行以上的设置区，使用加高版界面
        this.imageHeight = tallLayout() ? RGB_BG_HEIGHT : BG_HEIGHT;
    }

    /** 是否需要加高界面：RGB 控制器（连接 / 通道 / 显示原色）、同步指示器（两行）、安全按钮与警报器。 */
    private boolean tallLayout() {
        return menu.type == com.cc_rc.block.console.ConsoleCompType.RGB_COLOR
                || menu.type.hasLampVariable()
                || menu.type == com.cc_rc.block.console.ConsoleCompType.BUTTON_S
                // 警报器：多一行「静音变量」设置
                || menu.type.hasMuteVariable()
                // 钥匙开关：多一行「钥匙模式」设置
                || menu.type.hasKeyMode();
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;
        this.color = menu.color;
        this.keyMode = menu.keyMode;

        // 代号输入框
        this.codeBox = new EditBox(this.font, cx + CODE_X, cy + CODE_Y, CODE_W, BOX_H,
                Component.literal("组件代号"));
        this.codeBox.setMaxLength(32);
        this.codeBox.setValue(menu.code);
        if (menu.type == com.cc_rc.block.console.ConsoleCompType.RGB_COLOR) {
            // RGB 颜色控制器的名称可留空（未命名时显示模型；命名后隐藏模型、只显示同色名称）
            this.codeBox.setHint(Component.literal("可留空"));
            this.codeBox.setTooltip(Tooltip.create(Component.literal(
                    "代号：英文字母 / 数字 / 下划线，1~32 位；留空时只显示模型，"
                            + "填写后隐藏模型并在该位置显示同色名称。"
                            + "该名称对应一个布尔「启用」变量")));
        } else {
            this.codeBox.setTooltip(Tooltip.create(
                    Component.literal("代号：英文字母 / 数字 / 下划线，1~32 位")));
        }
        this.addWidget(this.codeBox);
        this.setInitialFocus(this.codeBox);

        // 位置 X / Y（像素）
        this.uBox = new EditBox(this.font, cx + X_BOX_X, cy + POS_Y, POS_BOX_W, BOX_H,
                Component.literal("X"));
        this.uBox.setMaxLength(8);
        this.uBox.setValue(formatFloat(menu.u));
        this.uBox.setTooltip(Tooltip.create(Component.literal("位置 X（像素，向右为正）")));
        this.addWidget(this.uBox);

        this.vBox = new EditBox(this.font, cx + Y_BOX_X, cy + POS_Y, POS_BOX_W, BOX_H,
                Component.literal("Y"));
        this.vBox.setMaxLength(8);
        this.vBox.setValue(formatFloat(menu.v));
        this.vBox.setTooltip(Tooltip.create(Component.literal("位置 Y（像素，向上为正）")));
        this.addWidget(this.vBox);

        // 组件缩放：所有设备默认 1.0（等于模型默认大小）
        this.scaleBox = new EditBox(this.font, cx + SCALE_BOX_X, cy + POS_Y, SCALE_BOX_W, BOX_H,
                Component.literal("缩放"));
        this.scaleBox.setMaxLength(4);
        this.scaleBox.setValue(formatFloat(menu.scale));
        this.scaleBox.setTooltip(Tooltip.create(Component.literal(
                "组件缩放：默认 1.0，范围 0.25~4.0；实际大小 = 该值 × 模型默认缩放")));
        this.addWidget(this.scaleBox);

        // 颜色输入框：所有类型都保留（RGB 控制器用它手动设置当前灯色）
        this.colorBox = new EditBox(this.font, cx + HEX_BOX_X, cy + HEX_Y, HEX_BOX_W, BOX_H,
                Component.literal("颜色"));
        this.colorBox.setMaxLength(7);
        this.colorBox.setValue(menu.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                ? "" : hex(menu.color));
        this.colorBox.setHint(Component.literal("#RRGGBB"));
        this.colorBox.setTooltip(Tooltip.create(
                Component.literal("颜色：填写 #RRGGBB，如 #E01F1F；留空表示保持原样")));
        this.addWidget(this.colorBox);

        // RGB 颜色控制器额外提供「连接组件 + 三个通道变量 + 显示原色」
        boolean rgb = menu.type == com.cc_rc.block.console.ConsoleCompType.RGB_COLOR;
        if (rgb) {
            // 连接的组件（可写多个，使用「;」隔开）
            this.linksBox = new EditBox(this.font, cx + LINKS_BOX_X, cy + LINKS_Y,
                    LINKS_BOX_W, BOX_H, Component.literal("连接的组件"));
            this.linksBox.setMaxLength(512);
            this.linksBox.setValue(menu.links);
            this.linksBox.setHint(Component.literal("组件代号，多个用 ; 隔开"));
            this.linksBox.setTooltip(Tooltip.create(Component.literal(
                    "连接的组件：填写其它组件代号，多个用「;」隔开；"
                            + "启用时这些组件的颜色会跟随本控制器")));
            this.addWidget(this.linksBox);

            // R / G / B 三个通道变量名（点完成即注册为浮点变量）
            this.rgbRBox = createChannelBox(cx + RGB_BOX_R_X, cy + RGB_Y, menu.rgbR, "R");
            this.rgbGBox = createChannelBox(cx + RGB_BOX_G_X, cy + RGB_Y, menu.rgbG, "G");
            this.rgbBBox = createChannelBox(cx + RGB_BOX_B_X, cy + RGB_Y, menu.rgbB, "B");

            // 是否显示原色：手动启用 / 停用该控制器
            this.showOriginal = menu.showOriginal;
            this.showOriginalButton = Button.builder(showOriginalLabel(), b -> {
                        this.showOriginal = !this.showOriginal;
                        b.setMessage(showOriginalLabel());
                    })
                    .bounds(cx + SHOW_ORIGINAL_X, cy + SHOW_ORIGINAL_Y,
                            SHOW_ORIGINAL_W, BOX_H)
                    .build();
            this.showOriginalButton.setTooltip(Tooltip.create(Component.literal(
                    "是否显示原色：选「是」时该控制器停用（只显示原色，也不再控制连接的组件）；"
                            + "注册了同名布尔变量时以变量值为准")));
            this.addRenderableWidget(this.showOriginalButton);
        }

        // 数码显示器专用：保留小数位 + 显示单位（其它组件不显示这两个输入框）
        if (menu.type == com.cc_rc.block.console.ConsoleCompType.NUMITRON) {
            this.decimalsBox = new EditBox(this.font, cx + DEC_BOX_X, cy + DISPLAY_Y,
                    DEC_BOX_W, BOX_H, Component.literal("小数位"));
            this.decimalsBox.setMaxLength(1);
            this.decimalsBox.setValue(Integer.toString(menu.decimals));
            this.decimalsBox.setTooltip(Tooltip.create(Component.literal("保留小数位：0~6")));
            this.addWidget(this.decimalsBox);

            this.unitBox = new EditBox(this.font, cx + UNIT_BOX_X, cy + DISPLAY_Y,
                    UNIT_BOX_W, BOX_H, Component.literal("单位"));
            this.unitBox.setMaxLength(ConsoleBusBlockEntity.CompEntry.MAX_UNIT_LEN);
            this.unitBox.setValue(menu.unit);
            this.unitBox.setHint(Component.literal("可留空"));
            this.unitBox.setTooltip(Tooltip.create(
                    Component.literal("显示单位，如 % / kPa / m/s；最多 8 个字符")));
            this.addWidget(this.unitBox);
        }

        // 按钮 / 旋转按钮 / 安全按钮专用：弹起时间（-1 = 跟随配置文件，界面里显示配置文件的有效值）
        if (menu.type == com.cc_rc.block.console.ConsoleCompType.KEYPAD
                || menu.type == com.cc_rc.block.console.ConsoleCompType.BUTTON_X
                || menu.type == com.cc_rc.block.console.ConsoleCompType.BUTTON_S) {
            int hold = menu.holdTicks == ConsoleBusBlockEntity.CompEntry.DEFAULT_HOLD_TICKS
                    ? com.cc_rc.Config.getConsoleButtonHoldTicks() : menu.holdTicks;
            this.holdBox = new EditBox(this.font, cx + HOLD_BOX_X, cy + DISPLAY_Y,
                    HOLD_BOX_W, BOX_H, Component.literal("弹起时间"));
            this.holdBox.setMaxLength(3);
            this.holdBox.setValue(Integer.toString(hold));
            this.holdBox.setTooltip(Tooltip.create(Component.literal(
                    "弹起时间（tick，20 tick = 1 秒）：0 = 按住期间一直按下、松开右键才弹起，范围 0~"
                            + ConsoleBusBlockEntity.CompEntry.MAX_HOLD_TICKS)));
            this.addWidget(this.holdBox);
        }

        // 安全按钮专用：盖子自动关闭时间（0 = 不自动关闭）
        if (menu.type == com.cc_rc.block.console.ConsoleCompType.BUTTON_S) {
            this.coverBox = new EditBox(this.font, cx + COVER_BOX_X, cy + LAMP_Y,
                    COVER_BOX_W, BOX_H, Component.literal("盖子自动关闭"));
            this.coverBox.setMaxLength(5);
            this.coverBox.setValue(Integer.toString(menu.coverTicks));
            this.coverBox.setTooltip(Tooltip.create(Component.literal(
                    "盖子自动关闭时间（tick，20 tick = 1 秒）：0 = 不自动关闭，范围 0~"
                            + ConsoleBusBlockEntity.CompEntry.MAX_COVER_TICKS)));
            this.addWidget(this.coverBox);
        }

        // 钥匙开关专用：钥匙模式（开闭 / 常开 / 常闭，点一次循环一次）
        if (menu.type.hasKeyMode()) {
            this.keyModeButton = Button.builder(keyModeLabel(), b -> {
                        this.keyMode = (this.keyMode + 1) % 3;
                        b.setMessage(keyModeLabel());
                    })
                    .bounds(cx + SHOW_ORIGINAL_X, cy + SHOW_ORIGINAL_Y,
                            SHOW_ORIGINAL_W, BOX_H)
                    .build();
            this.keyModeButton.setTooltip(Tooltip.create(Component.literal(
                    "钥匙模式：开闭 = 开 / 关都能拔钥匙；常开 = 只有开时能拔；常闭 = 只有关时能拔")));
            this.addRenderableWidget(this.keyModeButton);
        }

        // 圆盘记录仪专用：第二名称（用于绑定「本 tick 是否记一笔」的布尔触发变量）
        if (menu.type.supportsHistory()) {
            this.code2Box = new EditBox(this.font, cx + CODE2_BOX_X, cy + DISPLAY_Y,
                    CODE2_BOX_W, BOX_H, Component.literal("第二名称"));
            this.code2Box.setMaxLength(32);
            this.code2Box.setValue(menu.code2);
            this.code2Box.setHint(Component.literal("可留空"));
            this.code2Box.setTooltip(Tooltip.create(Component.literal(
                    "第二名称：对应一个布尔触发变量，该变量为 true 时本 tick 才记录一笔；留空表示不设触发名称")));
            this.addWidget(this.code2Box);
        }

        // 同步指示器专用：转速变量名（第二个浮点变量，默认 = 代号 + "_s"）
        if (menu.type.hasSpeedVariable()) {
            this.speedBox = new EditBox(this.font, cx + SPEED_BOX_X, cy + DISPLAY_Y,
                    SPEED_BOX_W, BOX_H, Component.literal("转速变量"));
            this.speedBox.setMaxLength(32);
            // 未自定义时显示自动名称，便于玩家看到默认绑定
            this.speedBox.setValue(menu.speedVar.isEmpty() ? menu.code + "_s" : menu.speedVar);
            this.speedBox.setTooltip(Tooltip.create(Component.literal(
                    "转速变量：浮点，单位「度/tick」，正值顺时针、负值逆时针；"
                            + "默认 = 代号 + _s，可改成其它名称")));
            this.addWidget(this.speedBox);
        }

        // 同步指示器专用：灯亮状态变量名（第三个变量，布尔；默认 = 代号 + "_b"）
        if (menu.type.hasLampVariable()) {
            this.lampBox = new EditBox(this.font, cx + LAMP_BOX_X, cy + LAMP_Y,
                    LAMP_BOX_W, BOX_H, Component.literal("灯状态变量"));
            this.lampBox.setMaxLength(32);
            // 未自定义时显示自动名称，便于玩家看到默认绑定
            this.lampBox.setValue(menu.lampVar.isEmpty() ? menu.code + "_b" : menu.lampVar);
            this.lampBox.setTooltip(Tooltip.create(Component.literal(
                    "灯状态变量：布尔，只读输出——灯亮为 true、灯灭为 false（手动写入会在下一 tick 被覆盖）；"
                            + "默认 = 代号 + _b，可改成其它名称")));
            this.addWidget(this.lampBox);
        }

        // 警报器专用：静音状态变量名（布尔；默认 = 代号 + "_Q"）
        if (menu.type.hasMuteVariable()) {
            this.muteBox = new EditBox(this.font, cx + LAMP_BOX_X, cy + LAMP_Y,
                    LAMP_BOX_W, BOX_H, Component.literal("静音变量"));
            this.muteBox.setMaxLength(32);
            // 未自定义时显示自动名称，便于玩家看到默认绑定
            this.muteBox.setValue(menu.muteVar.isEmpty() ? menu.code + "_Q" : menu.muteVar);
            this.muteBox.setTooltip(Tooltip.create(Component.literal(
                    "静音变量：布尔，只读输出——已静音为 true、未静音为 false（手动写入会在下一 tick 被覆盖）；"
                            + "默认 = 代号 + _Q，可改成其它名称")));
            this.addWidget(this.muteBox);
        }

        // 16 个染料色块（与文字编辑工具同一套颜色；RGB 控制器也可用它手动设置当前灯色）
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int dyeRgb = dyes[i].getTextColor();
            int x = cx + COLOR_X + (i % COLOR_COLS) * (COLOR_W + COLOR_GAP_X);
            int y = cy + COLOR_Y + (i / COLOR_COLS) * (COLOR_H + COLOR_GAP_Y);
            Component name = Component.translatable("color.minecraft." + dyes[i].getName());
            ColorSwatch swatch = new ColorSwatch(x, y, dyeRgb, name);
            swatch.setTooltip(Tooltip.create(name));
            this.addRenderableWidget(swatch);
        }

        // 恢复原样（RGB 颜色控制器不提供：它只有「显示原色」一个开关，
        // 着色与否由该开关决定，颜色值本身需要保留）
        if (!rgb) {
            Button reset = Button.builder(Component.literal("原色"), b -> {
                        this.color = ConsoleBusBlockEntity.CompEntry.NO_COLOR;
                        this.colorBox.setValue("");
                    })
                    .bounds(cx + RESET_X, cy + HEX_Y, RESET_W, BOX_H)
                    .build();
            reset.setTooltip(Tooltip.create(Component.literal("恢复贴图原色（不着色）")));
            this.addRenderableWidget(reset);
        }

        // 完成 / 删除（加高界面按钮随之下移）
        int buttonY = cy + (tallLayout() ? RGB_BUTTON_Y : BUTTON_Y);
        Button done = Button.builder(Component.literal("完成"), b -> this.submit())
                .bounds(cx + DONE_X, buttonY, DONE_W, BUTTON_H)
                .build();
        this.addRenderableWidget(done);

        Button delete = Button.builder(Component.literal("删除组件"), b -> this.deleteComp())
                .bounds(cx + DELETE_X, buttonY, DELETE_W, BUTTON_H)
                .build();
        this.addRenderableWidget(delete);
    }

    /** 创建 RGB 通道变量名输入框。 */
    private EditBox createChannelBox(int x, int y, String value, String channel) {
        EditBox box = new EditBox(this.font, x, y, RGB_BOX_W, BOX_H,
                Component.literal(channel + " 通道"));
        box.setMaxLength(32);
        box.setValue(value);
        box.setHint(Component.literal(channel));
        box.setTooltip(Tooltip.create(Component.literal(
                channel + " 通道变量名：填写名称后点「完成」即注册为浮点变量（可留空）")));
        this.addWidget(box);
        return box;
    }

    /** 「显示原色」开关按钮的文字。 */
    private Component showOriginalLabel() {
        return Component.literal("显示原色：" + (this.showOriginal ? "是" : "否"));
    }

    /** 「钥匙模式」循环按钮的文字。 */
    private Component keyModeLabel() {
        String name = switch (this.keyMode) {
            case ConsoleBusBlockEntity.CompEntry.KEY_MODE_ALWAYS_OPEN -> "常开";
            case ConsoleBusBlockEntity.CompEntry.KEY_MODE_ALWAYS_CLOSED -> "常闭";
            default -> "开闭";
        };
        return Component.literal("钥匙模式：" + name);
    }

    /** 数字格式化：整数不带小数点。 */
    private static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static float parseFloat(EditBox box, float fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 颜色格式化为 #RRGGBB。 */
    private static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** 解析 #RRGGBB / RRGGBB；留空或非法时沿用当前颜色。 */
    private static int parseColor(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String text = raw.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(text, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 提交代号、位置、颜色与显示设置（位置夹在表面范围内）。 */
    private void submit() {
        float u = parseFloat(this.uBox, menu.u);
        float v = parseFloat(this.vBox, menu.v);
        u = Math.max(-menu.halfW, Math.min(menu.halfW, u));
        v = Math.max(-menu.halfH, Math.min(menu.halfH, v));
        int newColor = this.colorBox == null
                ? menu.color : parseColor(this.colorBox.getValue(), this.color);
        // 数码显示器：小数位 / 单位（其他类型沿用原值，服务端也会忽略）
        int decimals = menu.decimals;
        String unit = menu.unit;
        if (this.decimalsBox != null) {
            try {
                decimals = Integer.parseInt(this.decimalsBox.getValue().trim());
            } catch (NumberFormatException e) {
                decimals = menu.decimals;
            }
            decimals = Math.max(0, Math.min(ConsoleBusBlockEntity.CompEntry.MAX_DECIMALS, decimals));
        }
        if (this.unitBox != null) {
            unit = this.unitBox.getValue().trim();
        }
        // 按钮：弹起时间（0 = 松手立刻弹起；其它类型沿用原值，服务端也会忽略）
        int holdTicks = menu.holdTicks;
        if (this.holdBox != null) {
            try {
                holdTicks = Integer.parseInt(this.holdBox.getValue().trim());
            } catch (NumberFormatException e) {
                holdTicks = menu.holdTicks;
            }
            if (holdTicks == ConsoleBusBlockEntity.CompEntry.DEFAULT_HOLD_TICKS) {
                holdTicks = com.cc_rc.Config.getConsoleButtonHoldTicks();
            }
            holdTicks = Math.max(0,
                    Math.min(ConsoleBusBlockEntity.CompEntry.MAX_HOLD_TICKS, holdTicks));
        }
        // 安全按钮：盖子自动关闭时间（0 = 不自动关闭；其它类型服务端会忽略）
        int coverTicks = menu.coverTicks;
        if (this.coverBox != null) {
            try {
                coverTicks = Integer.parseInt(this.coverBox.getValue().trim());
            } catch (NumberFormatException e) {
                coverTicks = menu.coverTicks;
            }
            coverTicks = Math.max(0,
                    Math.min(ConsoleBusBlockEntity.CompEntry.MAX_COVER_TICKS, coverTicks));
        }

        // 组件缩放：夹在允许范围内（服务端还会再校验一次）
        float scale = parseFloat(this.scaleBox, menu.scale);
        if (!Float.isFinite(scale)) {
            scale = ConsoleBusBlockEntity.CompEntry.DEFAULT_SCALE;
        }
        scale = Math.max(ConsoleBusBlockEntity.CompEntry.MIN_SCALE,
                Math.min(ConsoleBusBlockEntity.CompEntry.MAX_SCALE, scale));

        // RGB 颜色控制器：连接的组件 / 三个通道变量 / 显示原色（其它类型保持空值）
        String links = this.linksBox == null ? "" : this.linksBox.getValue();
        String rgbR = this.rgbRBox == null ? "" : this.rgbRBox.getValue().trim();
        String rgbG = this.rgbGBox == null ? "" : this.rgbGBox.getValue().trim();
        String rgbB = this.rgbBBox == null ? "" : this.rgbBBox.getValue().trim();
        boolean showOriginal = this.linksBox != null && this.showOriginal;
        // 通道变量名必须是合法的变量名（英文字母 / 数字 / 下划线，1~32 位），
        // 且不能与主代号或彼此重名；不合法时留在界面并提示，避免保存后变量没出现
        String compCode = this.codeBox.getValue().trim();
        for (String channel : new String[]{rgbR, rgbG, rgbB}) {
            if (!channel.isEmpty() && !channel.matches("[A-Za-z0-9_]{1,32}")) {
                localHint("RGB 通道变量名只能用英文字母 / 数字 / 下划线（1~32 位）");
                return;
            }
            if (!channel.isEmpty() && channel.equalsIgnoreCase(compCode)) {
                localHint("RGB 通道变量名不能与组件代号相同");
                return;
            }
        }
        if ((!rgbR.isEmpty() && (rgbR.equalsIgnoreCase(rgbG) || rgbR.equalsIgnoreCase(rgbB)))
                || (!rgbG.isEmpty() && rgbG.equalsIgnoreCase(rgbB))) {
            localHint("R / G / B 三个通道变量名不能重复");
            return;
        }

        // 同步指示器：转速变量名（等于默认的「代号 + _s」或留空时发送空串，改代号后自动跟随）
        String speedVar = "";
        if (this.speedBox != null) {
            String candidate = this.speedBox.getValue().trim();
            if (!candidate.isEmpty() && !candidate.equalsIgnoreCase(compCode + "_s")) {
                if (!candidate.matches("[A-Za-z0-9_]{1,32}")) {
                    localHint("转速变量名只能用英文字母 / 数字 / 下划线（1~32 位）");
                    return;
                }
                if (candidate.equalsIgnoreCase(compCode)) {
                    localHint("转速变量名不能与组件代号相同");
                    return;
                }
                speedVar = candidate;
            }
        }

        // 同步指示器：灯亮状态变量名（等于默认的「代号 + _b」或留空时发送空串，改代号后自动跟随）
        String lampVar = "";
        if (this.lampBox != null) {
            String candidate = this.lampBox.getValue().trim();
            if (!candidate.isEmpty() && !candidate.equalsIgnoreCase(compCode + "_b")) {
                if (!candidate.matches("[A-Za-z0-9_]{1,32}")) {
                    localHint("灯状态变量名只能用英文字母 / 数字 / 下划线（1~32 位）");
                    return;
                }
                if (candidate.equalsIgnoreCase(compCode)) {
                    localHint("灯状态变量名不能与组件代号相同");
                    return;
                }
                // 两个附加变量不能重名（转速名按实际生效值比较）
                String speedName = speedVar.isEmpty() ? compCode + "_s" : speedVar;
                if (candidate.equalsIgnoreCase(speedName)) {
                    localHint("灯状态变量名不能与转速变量名相同");
                    return;
                }
                lampVar = candidate;
            }
        }

        // 警报器：静音状态变量名（等于默认的「代号 + _Q」或留空时发送空串，改代号后自动跟随）
        String muteVar = "";
        if (this.muteBox != null) {
            String candidate = this.muteBox.getValue().trim();
            if (!candidate.isEmpty() && !candidate.equalsIgnoreCase(compCode + "_Q")) {
                if (!candidate.matches("[A-Za-z0-9_]{1,32}")) {
                    localHint("静音变量名只能用英文字母 / 数字 / 下划线（1~32 位）");
                    return;
                }
                if (candidate.equalsIgnoreCase(compCode)) {
                    localHint("静音变量名不能与组件代号相同");
                    return;
                }
                muteVar = candidate;
            }
        }

        ModNetwork.CHANNEL.sendToServer(new ConsoleCompPacket(
                menu.pos, menu.compId, this.codeBox.getValue(),
                this.code2Box == null ? "" : this.code2Box.getValue(),
                u, v, newColor,
                decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, speedVar, lampVar, coverTicks,
                keyMode, muteVar, false));
        this.onClose();
    }

    /** 在聊天栏显示一条本地提示（界面保持打开，便于修改后重新提交）。 */
    private void localHint(String message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
    }

    /** 删除该组件。 */
    private void deleteComp() {
        ModNetwork.CHANNEL.sendToServer(new ConsoleCompPacket(
                menu.pos, menu.compId, menu.code, menu.code2, menu.u, menu.v, this.color,
                menu.decimals, menu.unit, menu.holdTicks, menu.scale, true));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int border = 0xFF555555;
        // RGB 控制器与同步指示器使用加高版背景（176×212），其它组件保持标准高度
        boolean tall = tallLayout();
        ResourceLocation bg = tall ? GUI_BG_TALL : GUI_BG;
        int bgHeight = tall ? RGB_BG_HEIGHT : BG_HEIGHT;
        // 整张背景贴图（用带 textureWidth/textureHeight 的 11 参数重载，避免按 256x256 计算 UV 被裁边）
        graphics.blit(bg, this.leftPos, this.topPos, BG_WIDTH, bgHeight,
                0.0F, 0.0F, BG_WIDTH, bgHeight, BG_WIDTH, bgHeight);
        // 标题分隔线
        graphics.fill(this.leftPos + 6, this.topPos + 16, this.leftPos + BG_WIDTH - 6,
                this.topPos + 17, border);

        // 颜色预览块：已选颜色直接显示，「原色」用外壳灰表示不影响外观（RGB 控制器没有固定颜色）
        if (this.colorBox != null) {
            int px = this.leftPos + PREVIEW_X;
            int py = this.topPos + HEX_Y;
            int preview = this.color == ConsoleBusBlockEntity.CompEntry.NO_COLOR
                    ? 0xFF808080 : (0xFF000000 | this.color);
            graphics.fill(px, py, px + PREVIEW_W, py + BOX_H, preview);
            graphics.fill(px, py, px + PREVIEW_W, py + 1, border);
            graphics.fill(px, py + BOX_H - 1, px + PREVIEW_W, py + BOX_H, border);
            graphics.fill(px, py, px + 1, py + BOX_H, border);
            graphics.fill(px + PREVIEW_W - 1, py, px + PREVIEW_W, py + BOX_H, border);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int color = 0x404040;
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, color, false);
        graphics.drawString(this.font, "类型：" + menu.typeName().getString(),
                12, TYPE_Y, color, false);
        graphics.drawString(this.font, "代号", 12, CODE_Y + 4, color, false);
        graphics.drawString(this.font, "X", X_LABEL_X, POS_Y + 4, color, false);
        graphics.drawString(this.font, "Y", Y_LABEL_X, POS_Y + 4, color, false);
        graphics.drawString(this.font, "缩放", SCALE_LABEL_X, POS_Y + 4, color, false);
        if (this.colorBox != null) {
            graphics.drawString(this.font, "RGB", HEX_LABEL_X, HEX_Y + 4, color, false);
        }
        // RGB 颜色控制器：连接组件 / 三通道变量 / 显示原色标签
        if (this.linksBox != null) {
            graphics.drawString(this.font, "连接", LINKS_LABEL_X, LINKS_Y + 4, color, false);
            graphics.drawString(this.font, "R", RGB_LABEL_R_X, RGB_Y + 4, color, false);
            graphics.drawString(this.font, "G", RGB_LABEL_G_X, RGB_Y + 4, color, false);
            graphics.drawString(this.font, "B", RGB_LABEL_B_X, RGB_Y + 4, color, false);
        }
        // 数码显示器：小数位 / 单位标签
        if (this.decimalsBox != null) {
            graphics.drawString(this.font, "小数位", DEC_LABEL_X, DISPLAY_Y + 4, color, false);
            graphics.drawString(this.font, "单位", UNIT_LABEL_X, DISPLAY_Y + 4, color, false);
        }
        // 按钮面板：弹起时间标签
        if (this.holdBox != null) {
            graphics.drawString(this.font, "弹起时间", HOLD_LABEL_X, DISPLAY_Y + 4, color, false);
        }
        // 圆盘记录仪：第二名称标签
        if (this.code2Box != null) {
            graphics.drawString(this.font, "第二名称", CODE2_LABEL_X, DISPLAY_Y + 4, color, false);
        }
        // 同步指示器：转速变量标签
        if (this.speedBox != null) {
            graphics.drawString(this.font, "转速变量", SPEED_LABEL_X, DISPLAY_Y + 4, color, false);
        }
        // 同步指示器：灯状态变量标签
        if (this.lampBox != null) {
            graphics.drawString(this.font, "灯状态变量", LAMP_LABEL_X, LAMP_Y + 4, color, false);
        }
        // 安全按钮：盖子自动关闭时间标签
        if (this.coverBox != null) {
            graphics.drawString(this.font, "盖子自动关闭", COVER_LABEL_X, LAMP_Y + 4, color, false);
        }
        // 警报器：静音变量标签
        if (this.muteBox != null) {
            graphics.drawString(this.font, "静音变量", LAMP_LABEL_X, LAMP_Y + 4, color, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // EditBox 不会被 AbstractContainerScreen 自动渲染
        this.codeBox.render(graphics, mouseX, mouseY, partialTick);
        this.uBox.render(graphics, mouseX, mouseY, partialTick);
        this.vBox.render(graphics, mouseX, mouseY, partialTick);
        this.scaleBox.render(graphics, mouseX, mouseY, partialTick);
        if (this.colorBox != null) {
            this.colorBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.linksBox != null) {
            this.linksBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.rgbRBox != null) {
            this.rgbRBox.render(graphics, mouseX, mouseY, partialTick);
            this.rgbGBox.render(graphics, mouseX, mouseY, partialTick);
            this.rgbBBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.decimalsBox != null) {
            this.decimalsBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.unitBox != null) {
            this.unitBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.holdBox != null) {
            this.holdBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.code2Box != null) {
            this.code2Box.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.speedBox != null) {
            this.speedBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.lampBox != null) {
            this.lampBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.coverBox != null) {
            this.coverBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.muteBox != null) {
            this.muteBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        boolean editing = this.codeBox.isFocused() || this.uBox.isFocused()
                || this.vBox.isFocused() || this.scaleBox.isFocused()
                || (this.colorBox != null && this.colorBox.isFocused())
                || (this.linksBox != null && this.linksBox.isFocused())
                || (this.rgbRBox != null && (this.rgbRBox.isFocused()
                        || this.rgbGBox.isFocused() || this.rgbBBox.isFocused()))
                || (this.decimalsBox != null && this.decimalsBox.isFocused())
                || (this.unitBox != null && this.unitBox.isFocused())
                || (this.holdBox != null && this.holdBox.isFocused())
                || (this.code2Box != null && this.code2Box.isFocused())
                || (this.speedBox != null && this.speedBox.isFocused())
                || (this.lampBox != null && this.lampBox.isFocused())
                || (this.muteBox != null && this.muteBox.isFocused())
                || (this.coverBox != null && this.coverBox.isFocused());
        if (enter && editing) {
            this.submit();
            return true;
        }
        // 拦截背包键：编辑界面不应被背包键关闭
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 染料色块按钮：自绘纯色方块 + 边框，白色边框 = 当前选中颜色。 */
    private class ColorSwatch extends Button {

        private final int rgb;

        ColorSwatch(int x, int y, int rgb, Component message) {
            super(x, y, COLOR_W, COLOR_H, message, b -> {
                color = rgb;
                colorBox.setValue(hex(rgb));
            }, DEFAULT_NARRATION);
            this.rgb = rgb;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int argb = 0xFF000000 | this.rgb;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width,
                    this.getY() + this.height, argb);
            int border = color == this.rgb ? 0xFFFFFFFF : 0xFF303030;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width,
                    this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1,
                    this.getY() + this.height, border);
            graphics.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, border);
        }
    }
}
