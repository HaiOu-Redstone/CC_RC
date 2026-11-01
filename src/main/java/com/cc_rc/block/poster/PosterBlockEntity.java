package com.cc_rc.block.poster;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 海报方块实体：保存「当前图片 + 缩放倍率 + 两个偏移量」，供客户端 PosterBER 绘制。
 *
 * 数据含义：
 *  - image  ：PosterImages.LIST 的索引（选择哪张海报图片）；
 *  - scale  ：缩放倍率，1.0 = 图片宽 1 格（16 像素），2.0 = 宽 2 格，依此类推；
 *  - dy     ：垂直偏移（单位：格，向上为正，可输入 0.5 逐步微调）；
 *  - dz     ：水平偏移（单位：格，观者视角向右为正）。
 *
 * 同步：getUpdateTag / getUpdatePacket 把配置随方块更新一并下发客户端，
 * GUI 提交（PosterConfigPacket）在服务端改值后由服务端调用 sendBlockUpdated 触发同步。
 */
public class PosterBlockEntity extends BlockEntity {

    /** 缩放倍率下限 / 上限（防止 0 或极端值导致渲染异常）。 */
    private static final float MIN_SCALE = 0.05F;
    private static final float MAX_SCALE = 20.0F;
    /** 偏移量绝对值上限（单位：格）。 */
    private static final float MAX_OFFSET = 16.0F;

    private int image;
    private float scale = 1.0F;
    private float dy;
    private float dz;

    public PosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POSTER_BE.get(), pos, state);
    }

    public int getImage() {
        return this.image;
    }

    public float getScale() {
        return this.scale;
    }

    public float getDy() {
        return this.dy;
    }

    public float getDz() {
        return this.dz;
    }

    /** 设置全部配置（越界值统一夹取），并标记存档需要保存。 */
    public void setConfig(int image, float scale, float dy, float dz) {
        this.image = Math.max(0, Math.min(PosterImages.size() - 1, image));
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        this.dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, dy));
        this.dz = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, dz));
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
     * 扩大渲染包围盒：海报可远超单格（缩放上限 20 格、偏移上限 16 格），
     * 若仍用默认 1 格包围盒，视锥剔除会让大图在边缘处凭空消失。
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).inflate(MAX_SCALE + MAX_OFFSET + 4.0D);
    }

    private void writeConfig(CompoundTag tag) {
        tag.putInt("Image", this.image);
        tag.putFloat("Scale", this.scale);
        tag.putFloat("Dy", this.dy);
        tag.putFloat("Dz", this.dz);
    }

    private void readConfig(CompoundTag tag) {
        this.image = Math.max(0, Math.min(PosterImages.size() - 1, tag.getInt("Image")));
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F));
        this.dy = tag.getFloat("Dy");
        this.dz = tag.getFloat("Dz");
    }
}
