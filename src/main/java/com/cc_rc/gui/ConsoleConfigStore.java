package com.cc_rc.gui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * 控制柜配置文件的本地读写（仅客户端）。
 *
 * 文件统一存放在游戏目录下的 cc_rc_configs/ 中，格式为可读 JSON，
 * 玩家可以直接用记事本打开查看或修改。服务器环境下这份文件只存在于玩家自己的电脑上，
 * 加载时由客户端读出内容再上传给服务端。
 */
public final class ConsoleConfigStore {

    /** 配置目录名（位于游戏根目录下）。 */
    public static final String DIR_NAME = "cc_rc_configs";
    /** 文件名最大长度（不含 .json 后缀）。 */
    public static final int MAX_NAME_LEN = 48;

    private ConsoleConfigStore() {
    }

    /** 配置目录（不保证存在）。 */
    public static File dir() {
        return new File(Minecraft.getInstance().gameDirectory, DIR_NAME);
    }

    /** 列出目录下所有配置文件名（不含 .json，按名称排序；目录不存在时返回空表）。 */
    public static List<String> list() {
        File dir = dir();
        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".json"));
        List<String> names = new ArrayList<>();
        if (files == null) {
            return names;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            String name = file.getName();
            names.add(name.substring(0, name.length() - ".json".length()));
        }
        return names;
    }

    /** 配置文件的完整路径。 */
    public static File fileOf(String name) {
        return new File(dir(), sanitize(name) + ".json");
    }

    /** 配置文件是否已存在（保存前用于提示覆盖）。 */
    public static boolean exists(String name) {
        return fileOf(name).isFile();
    }

    /** 读取文件内容（UTF-8）；文件不存在或读取失败时返回 null。 */
    public static String read(String name) {
        File file = fileOf(name);
        if (!file.isFile()) {
            return null;
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    /** 写入文件（UTF-8，自动创建目录）。 */
    public static void write(String name, String json) throws IOException {
        File dir = dir();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("无法创建目录 " + dir.getAbsolutePath());
        }
        Files.writeString(fileOf(name).toPath(), json, StandardCharsets.UTF_8);
    }

    /**
     * 清理玩家输入的文件名：去掉 .json 后缀与非法字符，限制长度。
     * 返回空字符串表示名称不可用。
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim();
        if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        // Windows 文件名非法字符统一替换为下划线
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (name.length() > MAX_NAME_LEN) {
            name = name.substring(0, MAX_NAME_LEN);
        }
        return name;
    }
}
