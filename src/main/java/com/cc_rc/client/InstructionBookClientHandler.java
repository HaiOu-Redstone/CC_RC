package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.item.InstructionBook2Item;
import com.cc_rc.item.InstructionBookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * 说明书成书的客户端事件处理。
 *
 * 原版打开成书的链路（ServerPlayer.openItemGui 与客户端 handleOpenBook）均用
 * is(Items.WRITTEN_BOOK) 精确匹配，自定义子类无法触发，因此需要在客户端直接
 * setScreen(BookViewScreen) 打开真书界面。
 *
 * 服务端兼容说明：物品类 use() 中不得引用任何 net.minecraft.client.* 类，
 * 否则专用服务器在类加载/校验时抛 ClassNotFoundException 崩溃，故打开界面的
 * 逻辑统一放在本客户端监听类中（Dist.CLIENT 注册）。
 *
 * 右键处理：
 *   - 只处理 LogicalSide.CLIENT 触发的事件（单机下客户端/服务端逻辑都会发
 *     布同一事件，若不判断会重复 setScreen）；
 *   - 主手/副手持说明书右键时打开对应书籍界面。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InstructionBookClientHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // 仅客户端侧处理，避免单机集成服务器下客户端/服务端两侧同时触发
        if (event.getSide() != LogicalSide.CLIENT) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof InstructionBookItem
                || stack.getItem() instanceof InstructionBook2Item) {
            // 直接打开原版书籍阅读界面（WrittenBookAccess 读取成书 NBT 内容）
            Minecraft.getInstance().setScreen(
                    new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));
        }
    }
}