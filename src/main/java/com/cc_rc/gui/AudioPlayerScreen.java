package com.cc_rc.gui;

import com.cc_rc.block.audio_player.AudioPlayerBlockEntity;
import com.cc_rc.client.SoundDurations;
import com.cc_rc.network.AudioPlayerConfigPacket;
import com.cc_rc.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 音频播放器设置界面（自绘白色面板，风格与 PosterScreen 一致）。
 *
 * 布局：
 *  - 音频名称输入框 + 自动补全下拉（参考控制柜程序界面的设备名补全）；
 *  - 所选音效时长（客户端解析 OGG 得到）；
 *  - 模式循环按钮（触发 / 反触发 / 脉冲 / 无红石）；
 *  - 范围、衰减、起始点、截至点、间隔、音量输入框；
 *  - 保存按钮通过 AudioPlayerConfigPacket 写回方块实体。
 */
public class AudioPlayerScreen extends AbstractContainerScreen<AudioPlayerMenu> {

    private static final int BG_WIDTH = 220;
    private static final int BG_HEIGHT = 224;

    // 名称输入框
    private static final int SOUND_X = 10;
    private static final int SOUND_Y = 28;
    private static final int SOUND_W = 200;
    private static final int SOUND_H = 16;
    private static final int DURATION_Y = 47;

    // 模式按钮
    private static final int MODE_X = 100;
    private static final int MODE_Y = 56;
    private static final int MODE_W = 110;
    private static final int MODE_H = 18;

    // 两列输入框（范围/衰减、起始/截至）
    private static final int COL1_X = 10;
    private static final int COL2_X = 115;
    private static final int COL_W = 95;
    private static final int ROW1_LABEL_Y = 79;
    private static final int ROW1_BOX_Y = 88;
    private static final int ROW2_LABEL_Y = 113;
    private static final int ROW2_BOX_Y = 122;

    // 间隔 / 音量输入框与提示
    private static final int INTERVAL_LABEL_Y = 147;
    private static final int INTERVAL_BOX_Y = 156;
    /** 间隔提示行（输入框下方一行小字）。 */
    private static final int INTERVAL_HINT_Y = 174;

    // 保存按钮与状态文本
    private static final int SAVE_X = 70;
    private static final int SAVE_Y = 190;
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 18;
    private static final int STATUS_Y = 212;

    // 补全下拉（与 ConsoleProgramScreen 保持一致的行高与配色）
    private static final int SUGGEST_ROW_H = 12;
    private static final int SUGGEST_ROWS = 5;
    private static final int COLOR_SUGGEST_BG = 0xFF141414;
    private static final int COLOR_SUGGEST_BORDER = 0xFF000000;
    private static final int COLOR_SUGGEST_TEXT = 0xFFCFCFCF;
    private static final int COLOR_SUGGEST_HOVER = 0xFFFFE060;
    private static final int COLOR_SUGGEST_HOVER_BG = 0xFF3A3A6A;

    private static final int COLOR_TEXT = 0xFF303030;
    private static final int COLOR_DIM = 0xFF707070;
    private static final int COLOR_OK = 0xFF307030;
    private static final int COLOR_ERROR = 0xFFB02020;

    private static final Component[] MODE_NAMES = {
            Component.translatable("gui.cc_rc.audio_player.mode.trigger"),
            Component.translatable("gui.cc_rc.audio_player.mode.inverse"),
            Component.translatable("gui.cc_rc.audio_player.mode.pulse"),
            Component.translatable("gui.cc_rc.audio_player.mode.always")
    };

    private EditBox soundBox;
    private EditBox rangeBox;
    private EditBox attenBox;
    private EditBox startBox;
    private EditBox stopBox;
    private EditBox intervalBox;
    private EditBox volumeBox;
    private Button modeButton;

    private int mode;
    /** 时长显示文本（如「时长：263.0 秒」或「时长：未知」）。 */
    private String durationText = "";
    private String statusText = "";
    private int statusColor = COLOR_OK;

    /** 全部已注册音效 id（排序后作为补全候选）。 */
    private final List<String> allSounds = new ArrayList<>();
    /** 当前过滤后的候选。 */
    private final List<String> suggestions = new ArrayList<>();
    private boolean suggestOpen;
    private int suggestOffset;
    /** 点击候选回填时置 true，避免 responder 又把下拉重新打开。 */
    private boolean applyingSuggestion;

    public AudioPlayerScreen(AudioPlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("gui.cc_rc.audio_player.title"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.mode = menu.settings.mode();
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        for (ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            this.allSounds.add(key.toString());
        }
        this.allSounds.sort(String.CASE_INSENSITIVE_ORDER);

        // 音频名称：先填初值，再挂 responder（避免初始化时就弹出下拉）
        this.soundBox = makeBox(cx + SOUND_X, cy + SOUND_Y, SOUND_W,
                menu.settings.soundId(), AudioPlayerBlockEntity.MAX_SOUND_ID_LENGTH);
        this.soundBox.setResponder(value -> {
            updateSuggestions();
            updateDuration();
            if (!this.applyingSuggestion) {
                this.suggestOpen = this.soundBox.isFocused();
                this.suggestOffset = 0;
            }
        });
        this.setInitialFocus(this.soundBox);

        this.modeButton = Button.builder(MODE_NAMES[this.mode], b -> cycleMode())
                .bounds(cx + MODE_X, cy + MODE_Y, MODE_W, MODE_H)
                .build();
        this.addRenderableWidget(this.modeButton);

        this.rangeBox = makeBox(cx + COL1_X, cy + ROW1_BOX_Y, COL_W,
                format(menu.settings.range()), 10);
        this.attenBox = makeBox(cx + COL2_X, cy + ROW1_BOX_Y, COL_W,
                String.valueOf(menu.settings.attenuation()), 10);
        this.startBox = makeBox(cx + COL1_X, cy + ROW2_BOX_Y, COL_W,
                format(menu.settings.start()), 10);
        this.stopBox = makeBox(cx + COL2_X, cy + ROW2_BOX_Y, COL_W,
                format(menu.settings.stop()), 10);
        this.intervalBox = makeBox(cx + COL1_X, cy + INTERVAL_BOX_Y, COL_W,
                format(menu.settings.interval()), 10);
        // 音量百分比（0~500，可超过 100）
        this.volumeBox = makeBox(cx + COL2_X, cy + INTERVAL_BOX_Y, COL_W,
                String.valueOf(menu.settings.volume()), 10);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.cc_rc.audio_player.save"), b -> save())
                .bounds(cx + SAVE_X, cy + SAVE_Y, SAVE_W, SAVE_H)
                .build());

        updateSuggestions();
        updateDuration();
    }

    private EditBox makeBox(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(this.font, x, y, width, SOUND_H, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setBordered(true);
        this.addWidget(box);
        return box;
    }

    private void cycleMode() {
        this.mode = (this.mode + 1) & 3;
        this.modeButton.setMessage(MODE_NAMES[this.mode]);
    }

    /** 按当前输入刷新补全候选（空输入列出全部）。 */
    private void updateSuggestions() {
        this.suggestions.clear();
        String typed = this.soundBox == null ? ""
                : this.soundBox.getValue().trim().toLowerCase(Locale.ROOT);
        for (String id : this.allSounds) {
            if (typed.isEmpty() || id.toLowerCase(Locale.ROOT).startsWith(typed)) {
                this.suggestions.add(id);
            }
        }
        // 前缀无结果时退回「包含」匹配，方便只记得名字中间片段时查找
        if (this.suggestions.isEmpty() && !typed.isEmpty()) {
            for (String id : this.allSounds) {
                if (id.toLowerCase(Locale.ROOT).contains(typed)) {
                    this.suggestions.add(id);
                }
            }
        }
        int max = Math.max(0, this.suggestions.size() - SUGGEST_ROWS);
        this.suggestOffset = Math.max(0, Math.min(max, this.suggestOffset));
    }

    /** 重新解析所选音效并刷新时长文本。 */
    private void updateDuration() {
        ResourceLocation id = parseSoundId(this.soundBox == null ? "" : this.soundBox.getValue());
        int ticks = id == null ? -1 : SoundDurations.getTicks(id);
        if (ticks > 0) {
            this.durationText = Component.translatable("gui.cc_rc.audio_player.duration",
                    String.format(Locale.ROOT, "%.1f", ticks / 20.0F)).getString();
        } else {
            this.durationText = Component.translatable("gui.cc_rc.audio_player.duration_unknown")
                    .getString();
        }
    }

    /** 音效 id 解析：缺命名空间时按 cc_rc: 补齐；非法返回 null。 */
    private static ResourceLocation parseSoundId(String raw) {
        String value = AudioPlayerBlockEntity.normalizeSoundId(raw);
        return value.isEmpty() ? null : ResourceLocation.tryParse(value);
    }

    private void save() {
        ResourceLocation id = parseSoundId(this.soundBox.getValue());
        if (id == null || !ForgeRegistries.SOUND_EVENTS.containsKey(id)) {
            this.statusText = Component.translatable("gui.cc_rc.audio_player.invalid").getString();
            this.statusColor = COLOR_ERROR;
            return;
        }
        float range = parse(this.rangeBox.getValue(), menu.settings.range());
        int attenuation = Math.round(parse(this.attenBox.getValue(), menu.settings.attenuation()));
        float start = parse(this.startBox.getValue(), menu.settings.start());
        float stop = parse(this.stopBox.getValue(), menu.settings.stop());
        float interval = parse(this.intervalBox.getValue(), menu.settings.interval());
        int volume = Math.round(parse(this.volumeBox.getValue(), menu.settings.volume()));

        ModNetwork.CHANNEL.sendToServer(new AudioPlayerConfigPacket(menu.pos, id.toString(),
                this.mode, range, attenuation, start, stop, interval, volume));
        this.statusText = Component.translatable("gui.cc_rc.audio_player.saved").getString();
        this.statusColor = COLOR_OK;
    }

    /** 浮点数格式化：去掉多余的 .0，便于阅读与再次编辑。 */
    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 1.0E-4F) {
            return String.valueOf(Math.round(value));
        }
        return String.valueOf(value);
    }

    /** 解析输入框数值（允许逗号小数点；非法输入回退为原值）。 */
    private static float parse(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---------------------------------------------------------------- 补全下拉

    private int suggestTop() {
        return this.topPos + SOUND_Y + SOUND_H + 1;
    }

    private int suggestRowCount() {
        return Math.min(SUGGEST_ROWS, this.suggestions.size());
    }

    private int suggestHeight() {
        return suggestRowCount() * SUGGEST_ROW_H + 2;
    }

    private boolean isInsideSuggest(double mouseX, double mouseY) {
        if (!this.suggestOpen || this.suggestions.isEmpty()) {
            return false;
        }
        int x = this.leftPos + SOUND_X;
        int y = suggestTop();
        return mouseX >= x && mouseX < x + SOUND_W
                && mouseY >= y && mouseY < y + suggestHeight();
    }

    /** 补全下拉是否正在显示。 */
    private boolean isSuggestVisible() {
        return this.suggestOpen && !this.suggestions.isEmpty();
    }

    /**
     * 判断控件矩形是否被补全下拉遮挡。
     * 被遮挡的按钮 / 输入框 / 文字直接跳过渲染，避免下拉面板上透出下层文字。
     */
    private boolean isCoveredBySuggest(int x, int y, int w, int h) {
        if (!isSuggestVisible()) {
            return false;
        }
        int sx = this.leftPos + SOUND_X;
        int sy = suggestTop();
        return x < sx + SOUND_W && x + w > sx
                && y < sy + suggestHeight() && y + h > sy;
    }

    /** 鼠标命中的候选下标（含滚动偏移）；未命中返回 -1。 */
    private int suggestionAt(double mouseX, double mouseY) {
        if (!isInsideSuggest(mouseX, mouseY)) {
            return -1;
        }
        int rel = (int) (mouseY - suggestTop() - 1);
        if (rel < 0) {
            return -1;
        }
        int row = rel / SUGGEST_ROW_H + this.suggestOffset;
        return row >= 0 && row < this.suggestions.size() ? row : -1;
    }

    private void applySuggestion(String id) {
        this.applyingSuggestion = true;
        this.soundBox.setValue(id);
        this.applyingSuggestion = false;
        this.suggestOpen = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.suggestOpen) {
            int index = suggestionAt(mouseX, mouseY);
            if (index >= 0) {
                applySuggestion(this.suggestions.get(index));
                return true;
            }
            // 点在下拉底板内但没命中候选：吞掉这次点击
            if (isInsideSuggest(mouseX, mouseY)) {
                return true;
            }
            if (!this.soundBox.isMouseOver(mouseX, mouseY)) {
                this.suggestOpen = false;
            }
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // 点击名称输入框即展开全部候选
        if (this.soundBox.isFocused()) {
            this.suggestOpen = true;
            this.suggestOffset = 0;
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.suggestOpen && isInsideSuggest(mouseX, mouseY)) {
            int max = Math.max(0, this.suggestions.size() - SUGGEST_ROWS);
            this.suggestOffset = Math.max(0,
                    Math.min(max, this.suggestOffset - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC 优先收起下拉，避免直接关闭整个界面
        if (this.suggestOpen && keyCode == 256) {
            this.suggestOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 自绘白色面板 + 灰色边框（与海报设置界面一致）
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + BG_WIDTH;
        int y1 = y0 + BG_HEIGHT;
        graphics.fill(x0, y0, x1, y1, 0xFFFFFFFF);
        graphics.fill(x0, y0, x1, y0 + 1, 0xFF808080);
        graphics.fill(x0, y1 - 1, x1, y1, 0xFF808080);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF808080);
        graphics.fill(x1 - 1, y0, x1, y1, 0xFF808080);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabel(graphics, this.title, this.titleLabelX, this.titleLabelY, COLOR_TEXT);

        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.sound"),
                SOUND_X, SOUND_Y - 10, COLOR_TEXT);
        drawLabel(graphics, this.durationText, SOUND_X, DURATION_Y, COLOR_TEXT);

        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.mode"),
                SOUND_X, MODE_Y + 5, COLOR_TEXT);

        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.range"),
                COL1_X, ROW1_LABEL_Y, COLOR_TEXT);
        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.attenuation"),
                COL2_X, ROW1_LABEL_Y, COLOR_TEXT);

        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.start"),
                COL1_X, ROW2_LABEL_Y, COLOR_TEXT);
        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.stop"),
                COL2_X, ROW2_LABEL_Y, COLOR_TEXT);

        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.interval"),
                COL1_X, INTERVAL_LABEL_Y, COLOR_TEXT);
        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.volume"),
                COL2_X, INTERVAL_LABEL_Y, COLOR_TEXT);
        drawLabel(graphics, Component.translatable("gui.cc_rc.audio_player.interval_hint"),
                COL1_X, INTERVAL_HINT_Y, COLOR_DIM);

        if (!this.statusText.isEmpty()) {
            drawLabel(graphics, this.statusText, SAVE_X, STATUS_Y, this.statusColor);
        }
    }

    /** 绘制 Component 标签；被补全下拉遮挡时跳过（坐标为本界面内的相对坐标）。 */
    private void drawLabel(GuiGraphics graphics, Component text, int x, int y, int color) {
        if (isCoveredBySuggest(this.leftPos + x, this.topPos + y, this.font.width(text), 9)) {
            return;
        }
        graphics.drawString(this.font, text, x, y, color, false);
    }

    /** 绘制纯字符串标签；被补全下拉遮挡时跳过。 */
    private void drawLabel(GuiGraphics graphics, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (isCoveredBySuggest(this.leftPos + x, this.topPos + y, this.font.width(text), 9)) {
            return;
        }
        graphics.drawString(this.font, text, x, y, color, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = this.leftPos;
        int cy = this.topPos;
        // 下拉展开时隐藏被遮挡的模式按钮，避免按钮文字从下拉面板上透出
        this.modeButton.visible = !isCoveredBySuggest(cx + MODE_X, cy + MODE_Y, MODE_W, MODE_H);

        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // EditBox 不是 renderable widget，需要手动渲染；被下拉遮挡的输入框跳过
        renderBox(this.soundBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.rangeBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.attenBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.startBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.stopBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.intervalBox, graphics, mouseX, mouseY, partialTick);
        renderBox(this.volumeBox, graphics, mouseX, mouseY, partialTick);

        renderSuggestions(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderSuggestionTooltip(graphics, mouseX, mouseY);
    }

    /** 渲染输入框；若其矩形被补全下拉遮挡则跳过，防止文字透出。 */
    private void renderBox(EditBox box, GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
        if (isCoveredBySuggest(box.getX(), box.getY(), box.getWidth(), box.getHeight())) {
            return;
        }
        box.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 鼠标悬浮在补全候选上时，在指针旁显示该音效的中文翻译。
     * 翻译来源：cc_rc.sound.&lt;id&gt;，找不到时退回 cc_rc.subtitle.&lt;path&gt;；
     * 两者都没有（如原版音效）则不显示，避免重复展示 id。
     */
    private void renderSuggestionTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.suggestOpen) {
            return;
        }
        int index = suggestionAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        Component name = soundTranslation(this.suggestions.get(index));
        if (name != null) {
            graphics.renderTooltip(this.font, name, mouseX, mouseY);
        }
    }

    /** 查询音效翻译；cc_rc 命名空间省略前缀，路径中的 / 转为 . 组成语言键。 */
    private static Component soundTranslation(String raw) {
        ResourceLocation id = parseSoundId(raw);
        if (id == null) {
            return null;
        }
        String path = id.getPath().replace('/', '.');
        String key = "cc_rc".equals(id.getNamespace())
                ? "cc_rc.sound." + path
                : "cc_rc.sound." + id.getNamespace() + "." + path;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        String subtitle = "cc_rc.subtitle." + path;
        return I18n.exists(subtitle) ? Component.translatable(subtitle) : null;
    }

    /** 绘制补全下拉（深色底 + 悬停高亮，覆盖在其它控件之上）。 */
    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.suggestOpen || this.suggestions.isEmpty()) {
            return;
        }
        int x = this.leftPos + SOUND_X;
        int y = suggestTop();
        int rows = suggestRowCount();
        int h = suggestHeight();
        graphics.fill(x, y, x + SOUND_W, y + h, COLOR_SUGGEST_BG);
        graphics.fill(x, y, x + SOUND_W, y + 1, COLOR_SUGGEST_BORDER);
        graphics.fill(x, y + h - 1, x + SOUND_W, y + h, COLOR_SUGGEST_BORDER);
        graphics.fill(x, y, x + 1, y + h, COLOR_SUGGEST_BORDER);
        graphics.fill(x + SOUND_W - 1, y, x + SOUND_W, y + h, COLOR_SUGGEST_BORDER);

        for (int row = 0; row < rows; row++) {
            int index = this.suggestOffset + row;
            if (index >= this.suggestions.size()) {
                break;
            }
            int rowY = y + 1 + row * SUGGEST_ROW_H;
            boolean hover = mouseX >= x + 1 && mouseX < x + SOUND_W - 1
                    && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H;
            if (hover) {
                graphics.fill(x + 1, rowY, x + SOUND_W - 1, rowY + SUGGEST_ROW_H,
                        COLOR_SUGGEST_HOVER_BG);
            }
            graphics.drawString(this.font, this.suggestions.get(index), x + 3, rowY + 2,
                    hover ? COLOR_SUGGEST_HOVER : COLOR_SUGGEST_TEXT, false);
        }
    }
}
