package com.cc_rc.entity;

import com.cc_rc.ModItems;
import com.cc_rc.gui.NoteMenu;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 便签实体：贴在任意方块表面的任意位置（不限于格子中心）的小纸条。
 *
 * 与海报 / 涂鸦方块不同，便签是实体，因此位置可以是浮点坐标：
 *  - 右键已有便签：服务端打开文字编辑界面（四行、每行可单独设置颜色与字体格式）；
 *  - 左键：移除便签，生存 / 冒险模式掉落带文字的便签物品，创造模式直接移除；
 *  - Shift + 右键：取下便签，把四行文字写进获得的物品 NBT，再次放置时还原；
 *  - 支撑方块被破坏后自动掉落。
 *
 * 同步：朝向与四行文字都放在 SynchedEntityData 里，随实体生成包 / 数据更新包
 * 自动同步到客户端，客户端 NoteRenderer 直接读取。
 */
public class NoteEntity extends Entity {

    /** 四行文字。 */
    public static final int LINES = 4;
    /** 每行最大字符数。 */
    public static final int MAX_CHARS = 48;
    /** 便签边长（格）：半格，比告示牌小。 */
    public static final float NOTE_SIZE = 0.5F;
    /** 便签包围盒厚度（格）：很薄，便于贴墙点选。 */
    public static final float NOTE_THICKNESS = 0.06F;
    /** 默认文字颜色：黑色（浅黄底纸上更清晰）。 */
    public static final int DEFAULT_COLOR = 0x000000;
    /** 字体格式位标志：bit0 粗体、bit1 斜体、bit2 下划线、bit3 删除线。 */
    public static final int FLAG_BOLD = 1;
    public static final int FLAG_ITALIC = 2;
    public static final int FLAG_UNDERLINE = 4;
    public static final int FLAG_STRIKE = 8;
    /** 物品 NBT 里保存便签内容的键名。 */
    public static final String ITEM_TAG = "Note";

    private static final EntityDataAccessor<Direction> DATA_DIRECTION =
            SynchedEntityData.defineId(NoteEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<CompoundTag> DATA_TEXT =
            SynchedEntityData.defineId(NoteEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** 当前朝向（便签所在面的外法线），同时决定包围盒的薄边方向。 */
    private Direction direction = Direction.NORTH;
    /** 客户端渲染用的四行拆解数据（由同步标签刷新）。 */
    private final String[] lines = new String[LINES];
    private final int[] colors = new int[LINES];
    private final int[] flags = new int[LINES];

    public NoteEntity(EntityType<? extends NoteEntity> type, Level level) {
        super(type, level);
        Arrays.fill(this.colors, DEFAULT_COLOR);
        this.applyTextTag(emptyTag());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DIRECTION, Direction.NORTH);
        this.entityData.define(DATA_TEXT, emptyTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_DIRECTION.equals(key)) {
            this.direction = this.entityData.get(DATA_DIRECTION);
            // 朝向改变会改变薄边所在的轴，重新计算包围盒
            this.setPos(this.getX(), this.getY(), this.getZ());
        } else if (DATA_TEXT.equals(key)) {
            this.applyTextTag(this.entityData.get(DATA_TEXT));
        }
    }

    public Direction getDirection() {
        return this.direction;
    }

    /** 设置朝向（外法线方向）：同时刷新同步数据与包围盒。 */
    public void setDirection(Direction direction) {
        this.direction = direction;
        this.entityData.set(DATA_DIRECTION, direction);
        this.setPos(this.getX(), this.getY(), this.getZ());
    }

    // ==================== 文字数据 ====================

    public String getLineText(int line) {
        return this.lines[line];
    }

    public int getLineColor(int line) {
        return this.colors[line];
    }

    public int getLineFlags(int line) {
        return this.flags[line];
    }

    /** 按当前行数据构造可绘制的组件（颜色与字体格式写进 Style）。 */
    public Component getLineComponent(int line) {
        return styledComponent(this.lines[line], this.colors[line], this.flags[line]);
    }

    /** 当前四行文字的完整标签（供物品 NBT、编辑界面初始化与同步使用）。 */
    public CompoundTag getTextTag() {
        return this.entityData.get(DATA_TEXT).copy();
    }

    /** 写入四行文字（越界内容统一夹取，随后同步给客户端）。 */
    public void setTextTag(CompoundTag tag) {
        CompoundTag safe = sanitize(tag);
        this.entityData.set(DATA_TEXT, safe);
        this.applyTextTag(safe);
    }

    /** 把标签拆解成渲染用的行文本 / 颜色 / 格式。 */
    private void applyTextTag(CompoundTag tag) {
        for (int i = 0; i < LINES; i++) {
            String text = tag.getString(lineKey(i));
            this.lines[i] = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
            this.colors[i] = tag.contains(colorKey(i))
                    ? (tag.getInt(colorKey(i)) & 0xFFFFFF) : DEFAULT_COLOR;
            this.flags[i] = tag.getInt(flagKey(i)) & (FLAG_BOLD | FLAG_ITALIC | FLAG_UNDERLINE | FLAG_STRIKE);
        }
    }

    public static String lineKey(int line) {
        return "L" + line;
    }

    public static String colorKey(int line) {
        return "C" + line;
    }

    public static String flagKey(int line) {
        return "F" + line;
    }

    /** 空标签（四行空白、默认颜色、无格式）。 */
    public static CompoundTag emptyTag() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < LINES; i++) {
            tag.putString(lineKey(i), "");
            tag.putInt(colorKey(i), DEFAULT_COLOR);
            tag.putInt(flagKey(i), 0);
        }
        return tag;
    }

    /** 清洗外来标签：截断每行长度、颜色限制为 0xRRGGBB、格式只保留低 4 位。 */
    public static CompoundTag sanitize(CompoundTag source) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < LINES; i++) {
            String text = source == null ? "" : source.getString(lineKey(i));
            if (text.length() > MAX_CHARS) {
                text = text.substring(0, MAX_CHARS);
            }
            tag.putString(lineKey(i), text);
            tag.putInt(colorKey(i), source != null && source.contains(colorKey(i))
                    ? (source.getInt(colorKey(i)) & 0xFFFFFF) : DEFAULT_COLOR);
            tag.putInt(flagKey(i), source == null ? 0
                    : source.getInt(flagKey(i)) & (FLAG_BOLD | FLAG_ITALIC | FLAG_UNDERLINE | FLAG_STRIKE));
        }
        return tag;
    }

    /** 由文字 + 颜色 + 格式位构造带样式的组件。 */
    public static Component styledComponent(String text, int color, int flagBits) {
        Style style = Style.EMPTY
                .withColor(color & 0xFFFFFF)
                .withBold((flagBits & FLAG_BOLD) != 0)
                .withItalic((flagBits & FLAG_ITALIC) != 0)
                .withUnderlined((flagBits & FLAG_UNDERLINE) != 0)
                .withStrikethrough((flagBits & FLAG_STRIKE) != 0);
        return Component.literal(text == null ? "" : text).withStyle(style);
    }

    // ==================== 交互 ====================

    /** 便签可被准星选中（右键编辑、左键移除都依赖这里返回 true）。 */
    @Override
    public boolean isPickable() {
        return true;
    }

    /** 不受水流推动，避免贴墙便签被冲走。 */
    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /**
     * 右键：Shift 取下（写进物品 NBT 后给玩家），否则打开文字编辑界面。
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            if (player.isShiftKeyDown()) {
                player.getInventory().placeItemBackInInventory(this.createNoteStack());
                this.discard();
            } else if (player instanceof ServerPlayer serverPlayer) {
                NoteMenu.open(serverPlayer, this);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * 左键：移除便签。创造模式只移除不掉落；其余情况（生存 / 冒险 / 其他来源）掉落物品。
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide) {
            boolean creative = source.getEntity() instanceof Player player
                    && player.getAbilities().instabuild;
            if (creative) {
                this.discard();
            } else {
                this.dropNote();
            }
        }
        return true;
    }

    /** 是否有任意一行写过文字：决定取下 / 破坏后得到「已书写的便签」还是空白便签。 */
    public boolean hasText() {
        for (String line : this.lines) {
            if (!line.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 构造带当前文字 NBT 的便签物品（有文字时使用「已书写的便签」）。 */
    public ItemStack createNoteStack() {
        ItemStack stack = new ItemStack(this.hasText()
                ? ModItems.WRITTEN_NOTE.get() : ModItems.NOTE.get());
        stack.getOrCreateTag().put(ITEM_TAG, this.getTextTag());
        return stack;
    }

    /** 生成掉落物并移除实体。 */
    public void dropNote() {
        if (!this.level().isClientSide) {
            this.spawnAtLocation(this.createNoteStack());
            this.discard();
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.canSurvive()) {
            this.dropNote();
        }
    }

    /** 支撑方块是否仍然结实（便签贴在它的外表面）。 */
    private boolean canSurvive() {
        Direction face = this.direction;
        // 从便签中心向墙内退一个厚度，落在支撑方块内部
        Vec3 inside = new Vec3(
                this.getX() - face.getStepX() * NOTE_THICKNESS,
                this.getY() - face.getStepY() * NOTE_THICKNESS,
                this.getZ() - face.getStepZ() * NOTE_THICKNESS);
        BlockPos supportPos = BlockPos.containing(inside);
        BlockState state = this.level().getBlockState(supportPos);
        return state.isFaceSturdy(this.level(), supportPos, face);
    }

    /** 包围盒：沿朝向轴很薄，另外两轴为便签边长；下沿外翻的部分也要包进来以便点选。 */
    @Override
    protected AABB makeBoundingBox() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        double half = NOTE_SIZE / 2.0D;
        double back = NOTE_THICKNESS / 2.0D;   // 中心靠墙一侧的厚度
        double front = 0.12D;                  // 纸面下沿外翻后离墙的最大深度（含余量）
        // 注意：Entity 父类构造器里会调用 setPos -> makeBoundingBox，
        // 此时本类字段初始化还没执行，direction 仍为 null，需要兜底
        Direction dir = this.direction == null ? Direction.NORTH : this.direction;
        return switch (dir) {
            case EAST -> new AABB(x - back, y - half, z - half, x + front, y + half, z + half);
            case WEST -> new AABB(x - front, y - half, z - half, x + back, y + half, z + half);
            case UP -> new AABB(x - half, y - back, z - half, x + half, y + front, z + half);
            case DOWN -> new AABB(x - half, y - front, z - half, x + half, y + back, z + half);
            case SOUTH -> new AABB(x - half, y - half, z - back, x + half, y + half, z + front);
            default -> new AABB(x - half, y - half, z - front, x + half, y + half, z + back); // NORTH
        };
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Dir", this.direction.get3DDataValue());
        tag.put(ITEM_TAG, this.getTextTag());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setDirection(Direction.from3DDataValue(tag.getInt("Dir")));
        this.setTextTag(tag.getCompound(ITEM_TAG));
    }
}
