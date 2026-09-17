package topo.parser;

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

// 文件读写（D 包桩，T-D2）：打开/保存数据、导出结果(txt/csv)
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
            String content = new String(Files.readAllBytes(file.toPath()),
                    StandardCharsets.UTF_8);
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

    public static boolean exportResults(Component parent,
                                        List<List<String>> results, String format) {
        String ext = "csv".equalsIgnoreCase(format) ? "csv" : "txt";
        JFileChooser chooser = createChooser(ext.toUpperCase() + " 文件", ext);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        chooser.setSelectedFile(new File("topo_results_" + timestamp + "." + ext));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return exportResultsDirect(ensureExt(chooser.getSelectedFile(), "." + ext), results, format);
        }
        return false;
    }

    public static boolean exportResultsDirect(File file, List<List<String>> results, String format) {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (int i = 0; i < results.size(); i++) {
                if ("csv".equalsIgnoreCase(format)) {
                    w.write(String.join(",", results.get(i)));
                } else {
                    w.write((i + 1) + ". " + String.join(" -> ", results.get(i)));
                }
                w.newLine();
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
