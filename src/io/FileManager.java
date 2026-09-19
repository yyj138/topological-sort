package io;

import java.io.*;
import java.util.List;

/**
 * 文件管理工具类。
 * 负责打开文件（读入文本）、保存当前数据、导出拓扑排序结果（txt/csv），
 * 并记录最近打开路径便于再次打开。
 *
 * @author huxixi19
 */
public class FileManager {

    /** 最近一次打开的文件路径 */
    private static String lastOpenedPath = null;

    /**
     * 从文件读取文本内容。
     *
     * @param file 要读取的文件
     * @return 文件内容字符串
     * @throws IOException 读取失败时抛出
     */
    public static String readFile(File file) throws IOException {
        if (file == null) {
            throw new IOException("未选择文件，请选择一个文件后再试");
        }
        if (!file.exists()) {
            throw new IOException("文件不存在：" + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IOException("路径不是文件：" + file.getAbsolutePath());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        lastOpenedPath = file.getAbsolutePath();
        return sb.toString();
    }

    /**
     * 将文本内容保存到文件。
     * <p>
     * 保存前统一做全角转半角归一化，换行统一使用 {@code \n}。
     * 全角转换规则与 {@link DataParser}、{@link util.InputValidator} 保持一致。
     * </p>
     *
     * @param file    目标文件
     * @param content 要保存的文本内容
     * @throws IOException 保存失败时抛出
     */
    public static void saveFile(File file, String content) throws IOException {
        if (file == null) {
            throw new IOException("未指定保存路径，请选择保存位置后再试");
        }
        if (content == null) {
            content = "";
        }

        // 归一化：全角转半角，统一 \n 换行
        // 全角转换规则与 DataParser/InputValidator 保持一致
        String[] lines = content.split("\\r\\n|\\r|\\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i]
                    .replace('＜', '<')
                    .replace('＞', '>')
                    .replace('，', ',');
            sb.append(l);
            if (i != lines.length - 1) {
                sb.append("\n");
            }
        }
        content = sb.toString();

        // 确保父目录存在
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            bw.write(content);
            bw.flush();
        }
    }

    /**
     * 导出拓扑排序结果为 txt 格式（UTF-8，每条序列一行）。
     *
     * @param file    目标文件
     * @param results 拓扑排序结果列表，每个元素是一条完整序列
     * @throws IOException 写入失败时抛出
     */
    public static void exportTxt(File file, List<String> results) throws IOException {
        if (file == null) {
            throw new IOException("未指定导出路径，请选择保存位置后再试");
        }
        if (results == null || results.isEmpty()) {
            throw new IOException("没有可导出的拓扑排序结果");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            bw.write("# 拓扑排序结果");
            bw.newLine();
            bw.write("# 共 " + results.size() + " 条序列");
            bw.newLine();
            bw.newLine();
            for (int i = 0; i < results.size(); i++) {
                bw.write("序列 " + (i + 1) + ": " + results.get(i));
                bw.newLine();
            }
            bw.flush();
        }
    }

    /**
     * 导出拓扑排序结果为 csv 格式（UTF-8 BOM，可被 Excel 正常打开）。
     * 格式：每行一条序列，顶点用逗号分隔。
     *
     * @param file    目标文件
     * @param results 拓扑排序结果列表，每个元素是一条完整序列
     * @throws IOException 写入失败时抛出
     */
    public static void exportCsv(File file, List<String> results) throws IOException {
        if (file == null) {
            throw new IOException("未指定导出路径，请选择保存位置后再试");
        }
        if (results == null || results.isEmpty()) {
            throw new IOException("没有可导出的拓扑排序结果");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            // 写入 UTF-8 BOM，确保 Excel 正确识别编码
            bos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

            OutputStreamWriter osw = new OutputStreamWriter(bos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            // 表头
            bw.write("序号,拓扑排序序列");
            bw.newLine();

            for (int i = 0; i < results.size(); i++) {
                String sequence = results.get(i);
                // csv 中如果顶点名包含逗号或双引号，需要加引号包裹并转义内部双引号
                if (sequence.contains(",") || sequence.contains("\"")) {
                    bw.write((i + 1) + ",\"" + sequence.replace("\"", "\"\"") + "\"");
                } else {
                    bw.write((i + 1) + "," + sequence);
                }
                bw.newLine();
            }

            bw.flush();
        }
    }

    /**
     * 获取最近一次打开的文件路径。
     *
     * @return 文件路径字符串，如果没有打开过文件则返回 null
     */
    public static String getLastOpenedPath() {
        return lastOpenedPath;
    }

    /**
     * 获取最近打开路径对应的目录，便于下次打开时定位。
     *
     * @return 目录路径，如果没有记录则返回 null
     */
    public static String getLastOpenedDirectory() {
        if (lastOpenedPath == null) {
            return null;
        }
        File file = new File(lastOpenedPath);
        File parent = file.getParentFile();
        return parent != null ? parent.getAbsolutePath() : null;
    }

    /**
     * 仅供单元测试使用，生产业务代码禁止调用。
     * <p>
     * 重置最近打开路径记录，避免测试用例之间互相污染。
     * 如果 TestFileManager 不在 io 包，请修改为 public static。
     * </p>
     */
    public static void resetLastOpenedPathForTest() {
        lastOpenedPath = null;
    }
}