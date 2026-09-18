package util;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.List;

// 异常处理工具类（T-B5）：统一弹窗、中文消息、行号定位
// 解析器各类型的问题先格式化为中文消息，再由此统一展示
public final class ExceptionHandler {

    private ExceptionHandler() {}

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    // 问题清单：消息由调用方按"第x行：原因"格式组织
    public static void showIssues(Component parent, List<String> messages, String title) {
        if (messages == null || messages.isEmpty()) {
            showInfo(parent, "无问题");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("共发现 ").append(messages.size()).append(" 处问题：\n\n");
        int show = Math.min(messages.size(), 20);
        for (int i = 0; i < show; i++) {
            sb.append(messages.get(i)).append("\n");
        }
        if (messages.size() > show) {
            sb.append("... 共 ").append(messages.size())
              .append(" 条，仅显示前 ").append(show).append(" 条");
        }
        JOptionPane.showMessageDialog(parent, sb.toString(),
                title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showParseErrors(Component parent, List<String> messages) {
        showIssues(parent, messages, "数据校验错误");
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
}
