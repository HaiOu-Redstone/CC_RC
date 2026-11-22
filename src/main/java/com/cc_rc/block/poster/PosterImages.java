package com.cc_rc.block.poster;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 海报可选图片表（单方块海报 poster 的「图片库」）。
 *
 * 每张图片对应一个贴图文件（位于 textures/block/poster/ 下），不再各自注册为独立方块，
 * 而是全部作为同一方块的可选图片。索引 = GUI 中缩略图的排列顺序（0 起始），
 * 保存在方块实体 NBT 中用于同步。**索引 0 = 无材质，是海报的默认材质**。
 *
 * 贴图文件名/图片 id 一律按原始图片名称命名（英文标识），GUI 中显示的 name 则为原始图片文件名。
 * 重复素材已合并：原 4 组竖旗各有「1 倍 / 2 倍」两份完全相同的贴图，现只保留一份（缩放倍率可调）。
 *
 * 宽高随表提供：客户端渲染器按「宽 = 16 * 缩放倍率 像素、高 = 宽 * (内容高 / 原图宽)」
 * 保持图片原始比例；GUI 缩略图也按该比例等比缩放显示。
 *
 * 部分竖版贴图（256x512 画布）的内容在画布中是「底对齐」的，顶部留有大片透明。
 * 为保证「默认定位 = 图片顶端对齐方块顶端」，这里记录内容顶部在画布中的像素位置 top：
 * 渲染时只采样 [top, height) 这段内容，几何高度按内容高计算。其余图片 top = 0（整张画布即内容）。
 */
public final class PosterImages {

    /**
     * 单张图片。
     *
     * @param id      贴图文件名（英文标识，不含路径与后缀）
     * @param name    原始图片名称（GUI 显示用，即素材文件名去掉后缀）
     * @param texture 贴图路径（含 textures/ 前缀与 .png 后缀，供 BER 与 GUI 直接使用）
     * @param width   画布宽（像素）
     * @param height  画布高（像素）
     * @param top     内容顶部在画布中的 y 像素（上方为透明留白，0 = 无留白）
     */
    public record Image(String id, String name, ResourceLocation texture,
                        int width, int height, int top) {
        /** 内容高（像素，已去掉顶部透明留白）。 */
        public int contentHeight() {
            return this.height - this.top;
        }

        /** 高宽比（内容高 / 画布宽）。 */
        public float aspect() {
            return (float) this.contentHeight() / (float) this.width;
        }

        /** 内容顶部对应的贴图 v 坐标（0~1）。 */
        public float v0() {
            return (float) this.top / (float) this.height;
        }
    }

    /**
     * 图片列表（共 47 张，其中 46 张可选）。
     * 索引 = 已保存到方块实体 NBT 的数值，**顺序一旦发布就不能再插入/调换**，
     * 新图片只能追加到列表末尾（否则存档里已放置的海报会变成别的图片）。
     * 当前顺序：0 = 无材质（默认材质）；1~23 = 之前已注册的图片（原顺序）；24~40 = CCNR / 其他 / 安全标识一批；41~46 = 新海报与模组图标。
     * 索引 45（水循环示意图）已改为独立物品 water_cycle_chart，这里保留低清条目仅为旧存档兼容，
     * 并从海报选择界面隐藏（见 HIDDEN_IDS），避免破坏索引顺序。
     */
    public static final List<Image> LIST = List.of(
            // 索引 0：默认材质「无材质」
            img("no_material", "无材质", 16, 16),
            // 方形海报 1~8（原 1.png ~ 8.png）
            img("poster_1", "1", 256, 256),
            img("poster_2", "2", 256, 256),
            img("poster_3", "3", 256, 256),
            img("poster_4", "4", 256, 256),
            img("poster_5", "5", 256, 256),
            img("poster_6", "6", 256, 256),
            img("poster_7", "7", 256, 256),
            img("poster_8", "8", 256, 256),
            // 竖版海报（256x512 画布，内容底对齐，top 为顶部透明留白）
            imgCrop("ask_yourself", "ask_yourself", 256, 512, 179),
            imgCrop("energy", "energy", 256, 512, 179),
            imgCrop("forgetting", "forgetting", 256, 512, 179),
            imgCrop("parking_lot", "parking_lot", 256, 512, 150),
            imgCrop("see_more", "see_more", 256, 512, 179),
            imgCrop("than_just_you", "than_just_you", 256, 512, 179),
            imgCrop("we_need_you", "we_need_you", 256, 512, 179),
            // 神秘标识
            img("mystery_mark", "神秘标识", 256, 386),
            // 四张量子竖旗（原 1 倍/2 倍重复，现只保留一份，倍率用 GUI 缩放调）
            img("flag_quantum_energy", "竖旗-量子能源公司", 256, 474),
            img("flag_quantum_science", "竖旗-量子科学", 256, 474),
            img("flag_quantum_lighting", "竖旗-量子灯具", 256, 474),
            img("flag_quantum_atomic", "竖旗-量子原子能源公司", 256, 474),
            // 塔塔鲁斯上层入口标志
            img("tartarus_gate", "塔塔鲁斯上层入口logo", 512, 114),
            // 最新追加（只能往列表末尾追加，不要插到中间）
            img("goggles_mark", "戴护目镜标识", 256, 341),
            img("mixue_sign", "蜜雪冰城招牌", 768, 256),
            // CCNR 宣传系列（源图 模型/其他物品/海报/新海报/CCNR图标，
            // 导入时已按最长边 512 等比缩小并裁掉四周透明留白）
            img("ccnr_main", "CCNR主图标", 404, 296),
            img("ccnr_logo_horizontal", "CCNR横版图标文字", 420, 99),
            img("ccnr_logo_vertical", "CCNR竖版图标文字", 416, 426),
            img("ccnr_admin_region", "CCNR行政区域图标", 451, 182),
            img("ccnr_character", "CCNR角色宣传画", 341, 512),
            img("ccnr_canteen", "CCNR食堂标语", 512, 204),
            img("ccnr_page_8", "第 8 頁", 394, 512),
            img("ccnr_page_9", "第 9 頁", 394, 512),
            // 其他
            img("milk_dragon", "奶龙", 236, 489),
            img("slippery_floor", "小心地滑", 300, 400),
            img("wear_helmet", "戴安全帽", 300, 400),
            img("decor_1", "装饰画1", 512, 512),
            img("decor_2", "装饰画2", 512, 512),
            img("decor_3", "装饰画3", 256, 384),
            // 安全标识
            img("danger_fall", "坠落危险", 448, 512),
            img("danger_electric", "触电危险", 448, 512),
            img("danger_radiation", "辐射危险", 454, 512),
            // 新海报（模型/其他物品/海报/新海报）+ 模组图标（源图已裁掉透明留白，最长边 512）
            img("nuclear_sketch", "核电站简笔画", 452, 389),
            img("blue_fish_portrait", "蓝色大肥鱼画像", 288, 512),
            img("facility_director_portrait", "设施总监画像", 307, 512),
            img("mod_icon", "模组图标", 256, 256),
            // 新海报（模型/其他物品/海报/新海报）
            img("water_cycle_diagram", "水循环示意图", 512, 362),
            img("facility_director_2", "设施总监照片2", 512, 512)
    );

    /** 不在海报选择界面中展示的图片 id（索引保留，旧存档里的该海报仍能正常显示）。 */
    private static final java.util.Set<String> HIDDEN_IDS = java.util.Set.of("water_cycle_diagram");

    /** 该索引的图片是否已隐藏（隐藏项不显示在 PosterScreen，但仍可被旧存档 NBT 引用）。 */
    public static boolean isHidden(int index) {
        return HIDDEN_IDS.contains(get(index).id());
    }

    /** 可选图片数量（不计隐藏项）。 */
    public static int visibleCount() {
        int count = 0;
        for (int i = 0; i < LIST.size(); i++) {
            if (!isHidden(i)) {
                count++;
            }
        }
        return count;
    }

    /** 第 pos 个可见图片在 LIST 中的实际索引。 */
    public static int visibleIndexAt(int pos) {
        int seen = -1;
        for (int i = 0; i < LIST.size(); i++) {
            if (!isHidden(i)) {
                seen++;
                if (seen == pos) {
                    return i;
                }
            }
        }
        return 0;
    }

    private PosterImages() {
    }

    private static Image img(String id, String name, int width, int height) {
        return imgCrop(id, name, width, height, 0);
    }

    /** 带内容裁切的图片：top = 内容顶部在画布中的像素 y（顶部透明留白高度）。 */
    private static Image imgCrop(String id, String name, int width, int height, int top) {
        // 注意：BER 的 RenderType 与 GUI blit 都需要「完整贴图路径」（不含命名空间前缀）
        return new Image(id, name, new ResourceLocation("cc_rc", "textures/block/poster/" + id + ".png"),
                width, height, top);
    }

    /** 图片总数。 */
    public static int size() {
        return LIST.size();
    }

    /** 取图片（索引越界时回退到第一张「无材质」，避免旧存档里的非法索引导致渲染异常）。 */
    public static Image get(int index) {
        if (index < 0 || index >= LIST.size()) {
            return LIST.get(0);
        }
        return LIST.get(index);
    }
}
