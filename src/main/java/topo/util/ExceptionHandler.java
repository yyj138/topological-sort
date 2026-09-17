package topo.util;

import topo.parser.ParseError;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.List;

// 异常处理工具类（T-B5）：统一弹窗、中文消息、行号定位
public final class ExceptionHandler {

    private ExceptionHandler() {}

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static void showError(Component parent, String message, int lineNo) {
        showError(parent, "第 " + lineNo + " 行：" + message);
    }

    public static void showParseErrors(Component parent, List<ParseError> errors) {
        if (errors == null || errors.isEmpty()) {
            showInfo(parent, "无错误");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("共发现 ").append(errors.size()).append(" 处错误：\n\n");
        int show = Math.min(errors.size(), 20);
        for (int i = 0; i < show; i++) {
            sb.append(errors.get(i).toString()).append("\n");
        }
        if (errors.size() > show) {
            sb.append("... 共 ").append(errors.size()).append(" 条，仅显示前 ").append(show).append(" 条");
        }
        JOptionPane.showMessageDialog(parent, sb.toString(), "数据校验错误", JOptionPane.ERROR_MESSAGE);
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "警告", JOptionPane.WARNING_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "确认",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void handle(Component parent, Throwable t) {
        if (t == null) { showError(parent, "未知错误"); return; }
        String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) msg = t.getClass().getSimpleName();
        showError(parent, msg);
    }

    public static void safeRun(Component parent, Runnable task) {
        try { task.run(); }
        catch (Exception e) { handle(parent, e); }
    }
}
