package io;

import view.GraphPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 图片导出工具（组员 C，T-C8）
 * ------------------------------------------------------------
 * 依赖 T-C5 的 GraphPanel.exportPNG(File)
 * 功能：
 *   1) 弹出目录选择框，导出当前画布为 PNG
 *   2) 文件名自动带时间戳，如 graph_20260922_153045.png
 *   3) 同秒内重复导出自动加序号，避免覆盖
 *   4) 导出失败给出中文提示（含原因）
 *   5) 目录不存在自动创建，创建失败有提示
 * ------------------------------------------------------------
 * 供 B 的"导出图片"菜单调用：ImageExporter.chooseAndExport(parent, panel)
 */
public class ImageExporter {

    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";

    /**
     * 弹出目录选择框，导出画布为 PNG（供 B 的菜单调用）
     * @param parent 父组件，用于对话框居中；可传 null
     * @param panel  关系图画布
     * @return 成功返回 File；取消或失败返回 null
     */
    public static File chooseAndExport(java.awt.Component parent, GraphPanel panel) {
        if (panel == null) {
            showError(parent, "画布为空，无法导出");
            return null;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择导出目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int r = chooser.showSaveDialog(parent);
        if (r != JFileChooser.APPROVE_OPTION) return null;

        File dir = chooser.getSelectedFile();
        try {
            File saved = exportWithTimestamp(panel, dir);
            JOptionPane.showMessageDialog(parent,
                    "导出成功：\n" + saved.getAbsolutePath(),
                    "导出图片", JOptionPane.INFORMATION_MESSAGE);
            return saved;
        } catch (IOException ex) {
            showError(parent, "导出失败：" + ex.getMessage());
            return null;
        } catch (SecurityException ex) {
            showError(parent, "没有写入权限：" + ex.getMessage());
            return null;
        } catch (RuntimeException ex) {
            showError(parent, "导出异常：" + ex.getMessage());
            return null;
        }
    }

    /**
     * 导出到指定目录，文件名带时间戳（无界面，供脚本/测试调用）
     */
    public static File exportWithTimestamp(GraphPanel panel, File dir) throws IOException {
        if (panel == null) throw new IllegalArgumentException("画布为空");
        if (dir == null) dir = new File(System.getProperty("user.home"));

        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) throw new IOException("无法创建目录：" + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IOException("目标不是目录：" + dir.getAbsolutePath());
        }

        File target = uniqueFile(dir);
        panel.exportPNG(target);
        return target;
    }

    /**
     * 生成不重复的文件：同秒内多次导出时自动加 _1, _2 ...
     */
    private static File uniqueFile(File dir) {
        String ts = new SimpleDateFormat(TIME_FORMAT).format(new Date());
        File f = new File(dir, "graph_" + ts + ".png");
        int idx = 1;
        while (f.exists()) {
            f = new File(dir, "graph_" + ts + "_" + idx + ".png");
            idx++;
        }
        return f;
    }

    private static void showError(java.awt.Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "导出错误",
                JOptionPane.ERROR_MESSAGE);
    }
}