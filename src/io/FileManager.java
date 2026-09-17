package io;

import algorithm.EnumerationResult;
import algorithm.StopReason;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// 文件读写（D 包桩，T-D2）：打开/保存数据、导出结果
// 导出需写明是否完整和停止原因（契约 §七）
public class FileManager {

    private static String lastOpenPath = System.getProperty("user.dir");

    public static String openFile(Component parent) {
        JFileChooser chooser = createChooser("文本文件", "txt", "csv");
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return openFileDirect(chooser.getSelectedFile());
        }
        return null;
    }

    public static String openFileDirect(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            lastOpenPath = file.getParent();
            return content;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    public static boolean saveFile(Component parent, String content) {
        JFileChooser chooser = createChooser("文本文件", "txt");
        chooser.setSelectedFile(new File("data.txt"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return saveFileDirect(ensureExt(chooser.getSelectedFile(), ".txt"), content);
        }
        return false;
    }

    public static boolean saveFileDirect(File file, String content) {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write(content == null ? "" : content);
            lastOpenPath = file.getParent();
            return true;
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
        }
    }

    // 导出枚举结果：写明完整性 + 停止原因
    public static boolean exportResults(Component parent, EnumerationResult result, String format) {
        String ext = "csv".equalsIgnoreCase(format) ? "csv" : "txt";
        JFileChooser chooser = createChooser(ext.toUpperCase() + " 文件", ext);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        chooser.setSelectedFile(new File("topo_results_" + timestamp + "." + ext));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return exportResultsDirect(ensureExt(chooser.getSelectedFile(), "." + ext), result, format);
        }
        return false;
    }

    public static boolean exportResultsDirect(File file, EnumerationResult result, String format) {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            List<List<String>> sequences = result.getSequences();
            StopReason reason = result.getStopReason();
            boolean complete = result.isComplete();

            if ("csv".equalsIgnoreCase(format)) {
                w.write("# 完整性=" + (complete ? "完整" : "不完整")
                        + ", 停止原因=" + reason + ", 生成数=" + result.getGeneratedCount());
                w.newLine();
                for (List<String> seq : sequences) {
                    w.write(String.join(",", seq));
                    w.newLine();
                }
            } else {
                w.write("# 拓扑排序结果");
                w.newLine();
                w.write("# 完整性: " + (complete ? "完整" : "不完整"));
                w.newLine();
                w.write("# 停止原因: " + reason);
                w.newLine();
                w.write("# 生成数: " + result.getGeneratedCount());
                w.newLine();
                w.newLine();
                for (int i = 0; i < sequences.size(); i++) {
                    w.write((i + 1) + ". " + String.join(" -> ", sequences.get(i)));
                    w.newLine();
                }
            }
            lastOpenPath = file.getParent();
            return true;
        } catch (IOException e) {
            throw new RuntimeException("导出结果失败: " + e.getMessage(), e);
        }
    }

    public static String getLastOpenPath() { return lastOpenPath; }
    public static void setLastOpenPath(String path) { lastOpenPath = path; }

    private static JFileChooser createChooser(String desc, String... exts) {
        JFileChooser chooser = new JFileChooser(lastOpenPath);
        chooser.setFileFilter(new FileNameExtensionFilter(desc, exts));
        return chooser;
    }

    private static File ensureExt(File file, String ext) {
        String name = file.getName();
        if (!name.toLowerCase().endsWith(ext)) {
            return new File(file.getParentFile(), name + ext);
        }
        return file;
    }
}
