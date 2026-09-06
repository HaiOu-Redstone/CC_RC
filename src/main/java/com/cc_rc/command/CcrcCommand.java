package com.cc_rc.command;

import com.cc_rc.CcRc;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dan200.computercraft.shared.computer.core.ServerContext;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /ccrc 指令：查看/强制修改 CC: Tweaked 外设编号计数器。
 *
 * - /ccrc get_count &lt;设备种类名称&gt;：查询该种类已分配到的最大编号（id 文件内存储值，
 *   未记录过则显示 0，即下一个新外设将获得编号 0）；参数带自动补全（候选 = ids.json
 *   中已记录的类型）。
 * - /ccrc set_count &lt;设备种类名称&gt; &lt;数字&gt;：强制把该种类计数值改为指定值
 *   （需 OP 权限），同时写入 CC 的 ids.json 与内存中的 IDAssigner.ids（保持两者一致，
 *   否则 getNextId 会继续按内存旧值分配并覆盖文件）。参数带自动补全；仅允许设置
 *   ids.json 中已存在（= 曾被有线调制解调器连接/分配过编号）的类型，防止误设不存在的外设。
 * - /ccrc list：仅查看 CC 计数文件 ids.json 的内容（类型 → 编号），不做世界扫描。
 *
 * CC 的计数文件位于 存档目录/computercraft/ids.json（ServerContext.storageDir() 定位），
 * 内存 Map 为 IDAssigner 私有字段，通过反射同步。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CcrcCommand {

    // 与 CC 内部一致：Gson 读写 ids.json（Map<String, Integer>）
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ccrc")
                // set_count 需要 OP 权限（2 级）；get_count 无权限要求
                .then(Commands.literal("set_count")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("设备种类名称", StringArgumentType.word())
                                .suggests(CcrcCommand::suggestTypes)
                                .then(Commands.argument("数字", IntegerArgumentType.integer(0))
                                        .executes(ctx -> runSetCount(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "设备种类名称"),
                                                IntegerArgumentType.getInteger(ctx, "数字"))))))
                .then(Commands.literal("get_count")
                        .then(Commands.argument("设备种类名称", StringArgumentType.word())
                                .suggests(CcrcCommand::suggestTypes)
                                .executes(ctx -> runGetCount(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "设备种类名称")))))
                // list：仅查看 ids.json 计数文件内容（不扫描世界）
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> runList(ctx.getSource()))));
    }

    /**
     * 设备种类名称参数的自动补全：候选 = ids.json 中已记录的全部类型。
     * 与 set_count 的"仅允许已存在类型"校验保持一致。
     */
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTypes(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        try {
            ServerContext context = ServerContext.get(ctx.getSource().getServer());
            for (String type : readIdsFile(context.storageDir().resolve("ids.json")).keySet()) {
                builder.suggest(type);
            }
        } catch (Exception ignored) {
            // 补全失败不影响指令正常使用
        }
        return builder.buildFuture();
    }

    /** /ccrc set_count <类型> <值>：强制改写 CC 该种类计数值（文件 + 内存）。 */
    private static int runSetCount(CommandSourceStack source, String type, int value) {
        ServerContext context = ServerContext.get(source.getServer());
        Path idFile = context.storageDir().resolve("ids.json");

        // 1) 读取现有 id 文件（不存在则视为空）
        Map<String, Integer> fileIds = readIdsFile(idFile);

        // 2) 校验：仅允许设置已存在的类型（ids.json 有记录 = 曾被有线调制解调器连接/分配过编号）
        if (!fileIds.containsKey(type)) {
            source.sendFailure(Component.literal("设备种类 \"" + type + "\" 不存在或从未被连接过，"
                    + "无法设置计数（可用 /ccrc list 查看已记录的类型）"));
            return 0;
        }

        fileIds.put(type, value);

        // 3) 写回文件
        writeIdsFile(idFile, fileIds);

        // 4) 同步内存（反射 IDAssigner.ids；若尚未懒加载则直接用文件结果放入）
        Map<String, Integer> memoryIds = getMemoryIds(context);
        if (memoryIds == null) {
            memoryIds = fileIds;
        } else {
            memoryIds.put(type, value);
        }
        setMemoryIds(context, memoryIds);

        source.sendSuccess(() -> Component.literal("已将设备种类 \"" + type + "\" 的计数值设为 "
                + value + "（下一个该种类新外设将从 " + value + " 继续递增分配）"), true);
        return 1;
    }

    /** /ccrc get_count <类型>：查询该种类当前计数值。 */
    private static int runGetCount(CommandSourceStack source, String type) {
        ServerContext context = ServerContext.get(source.getServer());
        Integer value = null;

        // 优先读内存（若已初始化），否则读文件
        Map<String, Integer> memoryIds = getMemoryIds(context);
        if (memoryIds != null) {
            value = memoryIds.get(type);
        }
        if (value == null) {
            value = readIdsFile(context.storageDir().resolve("ids.json")).get(type);
        }

        int count = value == null ? 0 : value;
        boolean existed = value != null;
        source.sendSuccess(() -> Component.literal("设备种类 \"" + type + "\" 当前计数值：" + count
                + (existed ? "" : "（此前未分配过，下一个新外设将从 0 开始）")), false);
        return 1;
    }

    /**
     * /ccrc list：仅查看 CC 计数文件 ids.json 的内容（类型 → 编号），不做世界扫描。
     * 输出格式与文件保持一致，便于与 get_count/set_count 的补全候选对照。
     */
    private static int runList(CommandSourceStack source) {
        ServerContext context = ServerContext.get(source.getServer());
        Map<String, Integer> idFile = readIdsFile(context.storageDir().resolve("ids.json"));

        source.sendSuccess(() -> Component.literal("=== CC 外设计数文件 ids.json ==="), true);
        if (idFile.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  （文件为空或不存在，尚无已编号的外设类型）"), false);
        } else {
            idFile.forEach((type, id) -> source.sendSuccess(() -> Component.literal(
                    "  " + type + " → " + id), false));
        }
        return 1;
    }

    // ---------- 文件读写 ----------

    /** 读取 ids.json，文件缺失/损坏时返回空 Map。 */
    private static Map<String, Integer> readIdsFile(Path idFile) {
        if (!Files.isRegularFile(idFile)) return new HashMap<>();
        try {
            Map<String, Integer> result = GSON.fromJson(
                    Files.newBufferedReader(idFile, StandardCharsets.UTF_8), MAP_TYPE);
            return result == null ? new HashMap<>() : new HashMap<>(result);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /** 将 Map 以与 CC 相同格式写入 ids.json。 */
    private static void writeIdsFile(Path idFile, Map<String, Integer> ids) {
        try {
            if (idFile.getParent() != null) Files.createDirectories(idFile.getParent());
            GSON.toJson(ids, MAP_TYPE, Files.newBufferedWriter(idFile, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 写文件失败不阻塞命令反馈
        }
    }

    // ---------- 内存反射 ----------

    /** 反射获取 ServerContext.idAssigner 的 ids 字段；未初始化时返回 null。 */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> getMemoryIds(ServerContext context) {
        try {
            Field assignerField = ServerContext.class.getDeclaredField("idAssigner");
            assignerField.setAccessible(true);
            Object assigner = assignerField.get(context);
            if (assigner == null) return null;
            Field idsField = assigner.getClass().getDeclaredField("ids");
            idsField.setAccessible(true);
            return (Map<String, Integer>) idsField.get(assigner);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** 反射写回 ServerContext.idAssigner.ids，保证后续 getNextId 使用新值。 */
    private static void setMemoryIds(ServerContext context, Map<String, Integer> ids) {
        try {
            Field assignerField = ServerContext.class.getDeclaredField("idAssigner");
            assignerField.setAccessible(true);
            Object assigner = assignerField.get(context);
            if (assigner == null) return;
            Field idsField = assigner.getClass().getDeclaredField("ids");
            idsField.setAccessible(true);
            idsField.set(assigner, ids);
        } catch (ReflectiveOperationException e) {
            // 反射失败仅影响内存同步，文件已写入
        }
    }
}