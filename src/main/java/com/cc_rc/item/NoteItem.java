package com.cc_rc.item;

import com.cc_rc.CcRc;
import com.cc_rc.ModEntities;
import com.cc_rc.entity.NoteEntity;
import com.cc_rc.gui.NoteMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 便签物品：右键任意结实方块表面，在命中点（浮点坐标）生成便签实体并立即打开编辑界面。
 *
 *  - 便签位置取「命中点 + 面法线 × 半厚」，再把贴面内的两个坐标夹到该方块面内，
 *    因此便签可以贴在面内任意位置而不仅是格子中心；
 *  - 生存 / 冒险模式消耗 1 个，创造模式不消耗；
 *  - 物品 NBT 中的 {@link NoteEntity#ITEM_TAG} 会写进新实体，实现取下再放置时文字还原。
 *
 * 冒险模式（`mayBuild = false`）的放置要注意：Forge 在 `ItemStack.useOn` 内部会先用
 * `ForgeHooks.onPlaceItemIntoWorld` 拦掉非 mayBuild 玩家的放置请求（除非物品 NBT 带
 * 匹配当前方块的 `CanPlaceOn`），所以冒险模式的 `useOn` 根本不会被调用；
 * 这里额外用 {@link AdventurePlaceHandler} 在右键方块事件里直接完成放置。
 */
public class NoteItem extends Item {

    public NoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return placeNote(context.getLevel(), context.getPlayer(), context.getItemInHand(),
                context.getClickLocation(), context.getClickedPos(), context.getClickedFace());
    }

    /**
     * 实际放置逻辑：服务端生成便签实体、还原文字、扣除物品并给放置者打开编辑界面。
     *
     * @param hit       右键命中点（浮点坐标）
     * @param clickedPos 被点击的方块坐标
     * @param face      被点击的面（便签朝向）
     */
    public InteractionResult placeNote(Level level, @Nullable Player player, ItemStack stack,
                                       Vec3 hit, BlockPos clickedPos, Direction face) {
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockState support = level.getBlockState(clickedPos);
        // 只允许贴在结实的面上（空气 / 非完整面不生成实体）
        if (!support.isFaceSturdy(level, clickedPos, face)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Vec3 spawn = computeSpawnPosition(hit, clickedPos, face);
            NoteEntity note = ModEntities.NOTE.get().create(level);
            if (note == null) {
                return InteractionResult.PASS;
            }
            // 先定朝向再定位置，保证包围盒按正确的薄边方向计算
            note.setDirection(face);
            note.setPos(spawn.x, spawn.y, spawn.z);
            // 还原物品里保存的文字（空物品则保持空白）
            CompoundTag saved = stack.getTagElement(NoteEntity.ITEM_TAG);
            if (saved != null) {
                note.setTextTag(saved);
            }
            level.addFreshEntity(note);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            // 放置后立即打开编辑界面
            if (player instanceof ServerPlayer serverPlayer) {
                NoteMenu.open(serverPlayer, note);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 计算便签中心位置：命中点沿面法线外移半厚，贴面内两个坐标夹到
     * [方块起点 + 半边长, 方块终点 - 半边长]，使便签完整落在被点击的方块面上。
     */
    private static Vec3 computeSpawnPosition(Vec3 hit, BlockPos clickedPos, Direction face) {
        double half = NoteEntity.NOTE_SIZE / 2.0D;
        double offset = NoteEntity.NOTE_THICKNESS / 2.0D + 0.001D;
        double x = hit.x + face.getStepX() * offset;
        double y = hit.y + face.getStepY() * offset;
        double z = hit.z + face.getStepZ() * offset;

        double minX = clickedPos.getX() + half;
        double maxX = clickedPos.getX() + 1.0D - half;
        double minY = clickedPos.getY() + half;
        double maxY = clickedPos.getY() + 1.0D - half;
        double minZ = clickedPos.getZ() + half;
        double maxZ = clickedPos.getZ() + 1.0D - half;

        // 只夹取贴面内的两个切向坐标，法线方向保持刚算出的偏移
        switch (face.getAxis()) {
            case X -> {
                y = clamp(y, minY, maxY);
                z = clamp(z, minZ, maxZ);
            }
            case Y -> {
                x = clamp(x, minX, maxX);
                z = clamp(z, minZ, maxZ);
            }
            default -> {
                x = clamp(x, minX, maxX);
                y = clamp(y, minY, maxY);
            }
        }
        return new Vec3(x, y, z);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 冒险模式放置支持：非 mayBuild 玩家的 `ItemStack.useOn` 会在 Forge 层被直接拦掉，
     * 因此改在右键方块事件里由服务端完成放置，并取消后续的原版交互。
     */
    @Mod.EventBusSubscriber(modid = CcRc.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AdventurePlaceHandler {

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            // 只在服务端处理：客户端照常走预测流程，真正的放置交给服务端
            if (event.getLevel().isClientSide) {
                return;
            }
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            // 生存 / 创造照常走原版 useOn 流程，这里只补冒险模式
            if (player.isSpectator() || player.getAbilities().mayBuild) {
                return;
            }
            if (!(stack.getItem() instanceof NoteItem noteItem)) {
                return;
            }
            BlockPos pos = event.getPos();
            BlockState state = event.getLevel().getBlockState(pos);
            // 带界面的方块（箱子、熔炉等）仍优先打开界面，不贴便签
            if (state.getMenuProvider(event.getLevel(), pos) != null) {
                return;
            }
            InteractionResult result = noteItem.placeNote(event.getLevel(), player, stack,
                    event.getHitVec().getLocation(), pos, event.getFace());
            if (result.consumesAction()) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }
}
