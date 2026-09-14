package com.cc_rc.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

/**
 * 说明书1（instruction_book_1）
 *
 * 采用原版成书（WrittenBookItem）的样式与阅读界面。
 * 本类在 createBook() 中把固定内容（目录 + 各内容页）写入成书 NBT：
 *   - title / author / resolved / generation / pages（每页为 JSON 文本组件字符串）。
 * 内容页包括：控制面板类、圆盘记录仪、断路器、刷卡机、红石信号发射/接收器。
 *
 * 注意（服务端兼容）：
 *   - 原版打开成书的链路（ServerPlayer.openItemGui 与客户端 handleOpenBook）
 *     均用 is(Items.WRITTEN_BOOK) 精确匹配，自定义子类无法触发；
 *   - 打开书籍界面必须在客户端进行，但本类会被专用服务器加载，
 *     因此 use() 中不得引用任何 net.minecraft.client.* 类（否则服务端
 *     ClassNotFoundException 崩溃）。
 *   - 打开界面的逻辑已移交给客户端事件监听器 InstructionBookClientHandler
 *     （com.cc_rc.client 包，Dist.CLIENT），它监听右键该物品事件并直接
 *     setScreen(BookViewScreen)。
 */
public class InstructionBookItem extends WrittenBookItem {

    public InstructionBookItem(Properties properties) {
        super(properties);
    }

    /**
     * 生成一本已写好固定内容的说明书（带完整成书 NBT）。
     * 供创造标签等处直接展示/发放，避免空 NBT 成书打不开内容。
     */
    public ItemStack createBook() {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, "说明书1");
        tag.putString(WrittenBookItem.TAG_AUTHOR, "海鸥的红石");
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);

        ListTag pages = new ListTag();
        // 封面
        pages.add(page("说明书 1\n\n--CC: 反应堆控制台--\n\n本说明书收录本模组常用红石相关模块的用途与基本操作说明。"));
        // 目录
        pages.add(page("【目录】\n1. 控制面板类(含编辑工具)\n2. 圆盘记录仪\n3. 断路器\n4. 刷卡机\n5. 红石信号收发"));
        // 控制面板类
        pages.add(page("3. 控制面板类\n\n控制面板可以显示文字，在铁砧上将物品命名然后放在，即可显示对应文字，同类操作/显示件同理。\n\n特殊件:\n-仪表：显示0~15强度\n-安全按钮：shift+右键点击开关盖，右键按下"));
        // 编辑工具（控制面板类条目）
        pages.add(page("3. 控制面板类(续)\n\n-编辑工具： 可修改显示文字。\n\n主手右键可显示名称的方块（控制面板/拉杆/数码显示器等）打开文字编辑界面，输入文字并确认后写入方块表面。\n\n副手持编辑工具放置此类方块时，放置完成后自动打开编辑界面。"));
        // 圆盘记录仪
        pages.add(page("4. 圆盘记录仪\n\n在面板上画趋势线。\n\n-圆盘记录仪： 多功能工具切换模式及时间间隔，按间隔采样24点画线\n\n-记录仪时钟： 红石上升沿触发正上方8格内模式为1的圆盘记录仪划线"));
        // 断路器
        pages.add(page("5. 断路器\n\n模拟电路跳闸。\n\n-右键切换开/关\n-开启时向后、上方输出 15红石信号\n-底面收到信号自动跳闸"));
        // 刷卡机
        pages.add(page("6. 刷卡机\n\n门禁识别设备。\n\n-贴墙放置\n-用对应等级门禁卡激活， 向后输出15信号\n-定时自动关闭\n-A~E五级，高级机可识别低级卡"));
        // 红石信号发射/接收器
        pages.add(page("7. 红石信号发射/接收器\n\n无线传输红石信号。\n\n-发射器：可向六方向放置，受到红石信号激活时沿面向方向无线传输\n-接收器：接受无线传输并向相邻方块输出相同信号强度\n-默认传输距离8格，可在配置中调整"));
        tag.put(WrittenBookItem.TAG_PAGES, pages);

        return stack;
    }

    /**
     * 右键使用：本方法不引用任何客户端类（保证服务端可正常加载）。
     * 打开原版书籍界面的逻辑由客户端事件监听器 InstructionBookClientHandler
     * 在 Dist.CLIENT 侧完成，详见 com.cc_rc.client.InstructionBookClientHandler。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 将纯文本页转成成书要求的 JSON 文本组件字符串（含换行转义）。 */
    private static StringTag page(String text) {
        return StringTag.valueOf(Component.Serializer.toJson(Component.literal(text)));
    }
}