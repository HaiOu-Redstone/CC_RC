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
 * 说明书2（instruction_book_2）
 *
 * 采用原版成书（WrittenBookItem）的样式与阅读界面，记录 CC: Tweaked 配件外设的使用方法。
 * 本类在 createBook() 中把固定内容写入成书 NBT：
 *   - title / author / resolved / generation / pages（每页为 JSON 文本组件字符串）。
 * 内容页包括：数码显示器、数字调节器、数字圆盘记录仪、
 * 扩展红石继电器/总线、方块探测器，并精确记录各外设注册的
 * 每一个 Lua 函数及其参数/返回类型（与各 Peripheral 中的
 * @LuaFunction 一一对应）。
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
public class InstructionBook2Item extends WrittenBookItem {

    public InstructionBook2Item(Properties properties) {
        super(properties);
    }

    /**
     * 生成一本已写好固定内容的说明书2（带完整成书 NBT）。
     * 供创造标签等处直接展示/发放，避免空 NBT 成书打不开内容。
     */
    public ItemStack createBook() {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, "说明书2");
        tag.putString(WrittenBookItem.TAG_AUTHOR, "海鸥的红石");
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);

        ListTag pages = new ListTag();
        // 封面
        pages.add(page("说明书 2\n\n--CC: 反应堆控制台--\n\n本说明书收录本模组 CC 相关配件的 Lua 外设使用方法。"));
        // 目录
        pages.add(page("【目录】\n1. 数码显示器\n2. 数字调节器\n3. 数字圆盘记录仪\n4. 扩展红石继电器/总线\n5. 方块探测器"));
        // 数码显示器 - 外设与 setStatus
        pages.add(page("1. 数码显示器\n\n外设类型: digital_display\n通过 CC 调制解调器连接后即可调用。\n\nsetStatus(text: string)\n  -> string\n设置橙色状态文字，参数为字符串。\n返回设置后的文字。"));
        // 数码显示器 - getStatus 与示例
        pages.add(page("1. 数码显示器(续)\n\ngetStatus()\n  -> string\n读取当前显示的橙色状态文字。\n\n示例(Lua):\np=peripheral.find(\"digital_display\")\np.setStatus(\"欢迎使用\")\nprint(p.getStatus())"));
        // 数字调节器 - 外设与 setValue
        pages.add(page("2. 数字调节器\n\n外设类型: digital_knob\n数值范围 0~1000，越界自动钳制。\n\nsetValue(value: int)\n  -> int\n设置整数数值，小于0按0计，\n大于1000按1000计。\n返回钳制后的整数。"));
        // 数字调节器 - getValue 与示例
        pages.add(page("2. 数字调节器(续)\n\ngetValue()\n  -> int\n读取当前整数数值(0~1000)。\n\n示例(Lua):\np=peripheral.find(\"digital_knob\")\np.setValue(500)\nprint(p.getValue())"));
        // 数字圆盘记录仪 - 外设与函数总览
        pages.add(page("3. 数字圆盘记录仪\n\n外设类型: digital_plotter\n列表长度50，值域0~100。\n索引采用 Lua 惯例 1~50。\n\n函数: push / setValue /\ngetValue / getList"));
        // 数字圆盘记录仪 - push 与 setValue
        pages.add(page("3. 数字圆盘记录仪(续)\n\npush(value: int)\n  -> int\n写入新值(自动删除末位并移位)，\n返回写入后的值(0~100)。\n\nsetValue(index: int, value: int)\n  -> boolean\n设置第 index 个位置的值，\n索引越界返回 false。"));
        // 数字圆盘记录仪 - getValue / getList 与示例
        pages.add(page("3. 数字圆盘记录仪(续)\n\ngetValue(index: int)\n  -> int | nil\n读取第 index 个位置的值，\n索引越界返回 nil。\n\ngetList()\n  -> table\n读取整个列表(50个整数)。\n\n示例(Lua):\np=peripheral.find(\"digital_plotter\")\np.push(30)\nprint(p.getList()[1])"));
        // 扩展红石继电器/总线 - 概述
        pages.add(page("4. 扩展红石继电器/总线\n\n由总线(relay_bus)沿自身朝向搜索前方\ndistance 格处的继电器(extended_relay)，\n向对应侧读出/写入红石信号。\n\n距离：紧贴=1，最大可在配置\nrelay_bus.max_distance 中调整\n(默认16，范围1~64)。\n\nside 以继电器自身朝向为基准：\ntop/bottom/left/right/front/back"));
        // 扩展红石继电器/总线 - 输出函数
        pages.add(page("4. 扩展红石继电器/总线(续)\n\nisRelay(distance)\n  -> boolean 判断该处是否为继电器\n\nsetOutput(distance, side, on)\n  布尔输出：on=true 输出15，false 输出0\n\ngetOutput(distance, side)\n  -> boolean 读取布尔输出\n\nsetAnalogOutput(distance, side, value)\n  模拟输出 0~15（越界报错）"));
        // 扩展红石继电器/总线 - 输入函数与示例
        pages.add(page("4. 扩展红石继电器/总线(续)\n\ngetAnalogOutput(distance, side)\n  -> int 读取输出强度 0~15\n\ngetInput(distance, side)\n  -> boolean 该侧是否收到信号\n\ngetAnalogInput(distance, side)\n  -> int 该侧读入强度 0~15\n\n示例(Lua):\np=peripheral.find(\"redstone_relay_bus\")\np.setAnalogOutput(3,\"front\",15)\nprint(p.getAnalogInput(3,\"back\"))"));
        // 方块探测器 - 概述
        pages.add(page("5. 方块探测器\n\n外设类型: block_detector\n探测器沿放置朝向探测前方一格的\n方块信息，只读不可修改。\n\n注意：探测需访问主线程世界数据，\n每次调用有 1 tick 延迟。\n\n函数: getFacing / getBlockInfo /\ngetBlockEntityData"));
        // 方块探测器 - getFacing 与 getBlockInfo
        pages.add(page("5. 方块探测器(续)\n\ngetFacing()\n  -> string\n返回探测方向字符串：\nnorth/south/west/east/up/down\n\ngetBlockInfo()\n  -> table | nil\n返回面向方块信息表：\n{x, y, z, id, name, mod,\nisBlockEntity}\n目标区块未加载返回 nil。"));
        // 方块探测器 - getBlockEntityData 与示例
        pages.add(page("5. 方块探测器(续)\n\ngetBlockEntityData()\n  -> table | nil\n目标为方块实体时返回其完整 NBT\n数据(类似 /data get block，\n含 id 与坐标)，不可修改；\n无方块实体或未加载返回 nil。\n\n示例(Lua):\np=peripheral.find(\"block_detector\")\nlocal t=p.getBlockInfo()\nprint(t.id, t.mod, t.x, t.y, t.z)"));
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