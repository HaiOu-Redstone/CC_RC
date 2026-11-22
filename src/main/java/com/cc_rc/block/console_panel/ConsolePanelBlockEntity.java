package com.cc_rc.block.console_panel;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.ITextDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 控制面板方块实体基类
 * 存储和管理方块上显示的文本，通过 NBT 持久化，支持网络同步
 * 支持 ticksRemaining 倒计时功能（安全按钮使用）
 */
public class ConsolePanelBlockEntity extends BlockEntity implements ITextDisplay {
    private Component text = Component.empty();
    private int ticksRemaining = 0;

    /** 用于 console_panel 自身 */
    public ConsolePanelBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CONSOLE_PANEL_BE.get(), pos, state);
    }

    /** 用于子类传入自己的 BlockEntityType */
    protected ConsolePanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setText(Component text) {
        this.text = text;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Component getText() {
        return text;
    }

    /**
     * 应用染料颜色到显示文字：仅覆写文字颜色（保留原文内容与粗体/斜体等其他格式）。
     * 面板文字允许为空（先染色后写入文字），样式随空文字一并持久化。
     *
     * 注意这里必须用 {@link Component#plainCopy()} 而不是
     * {@code Component.literal(getString())}：后者会把任意 Component 拉平成纯字符串，
     * 再包一层 literal，于是 ItemStack 自定义名带来的斜体/粗体/颜色以及
     * translatable 翻译键全部丢失，客户端还会因为失去翻译键而显示成英文原文。
     * plainCopy 保留内容与 siblings，只是去掉根节点的样式，随后叠加染料颜色。
     */
    public void applyDyeColor(DyeColor dyeColor) {
        Style style = text.getStyle().withColor(TextColor.fromRgb(dyeColor.getTextColor()));
        setText(text.plainCopy().withStyle(style));
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void setTicksRemaining(int ticks) {
        this.ticksRemaining = ticks;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 空文本但有样式时也要存：先染色后写字是受支持的操作（见 applyDyeColor），
        // 只按内容判空会让这份颜色在重载后丢失
        if (!text.getString().isEmpty() || !text.getStyle().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
        if (ticksRemaining > 0) {
            tag.putInt("TicksRemaining", ticksRemaining);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Text")) {
            // fromJson 在 JSON 非法或字面量 "null" 时返回 null，后续 getString() 会 NPE；
            // 而 onDataPacket 会把网络收到的 tag 直接喂进来，等于可被外部同步包触发
            Component parsed = Component.Serializer.fromJson(tag.getString("Text"));
            text = parsed != null ? parsed : Component.empty();
        }
        ticksRemaining = tag.contains("TicksRemaining") ? tag.getInt("TicksRemaining") : 0;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
