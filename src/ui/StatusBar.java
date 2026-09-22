package ui;

import util.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

// 状态栏（T-B5）：节点/边数/含环/序列总数/耗时/缩放比例
public class StatusBar extends JPanel {

    private final JLabel lblNodeCount  = new JLabel("节点: 0");
    private final JLabel lblEdgeCount  = new JLabel("边数: 0");
    private final JLabel lblCycle       = new JLabel("无环");
    private final JLabel lblTotalSorts = new JLabel("序列数: 0");
    private final JLabel lblCost        = new JLabel("耗时: -");
    private final JLabel lblStatus      = new JLabel("");
    private final JLabel lblScale       = new JLabel("缩放: 100%");
    private final JLabel lblTip        = new JLabel("就绪");

    public StatusBar() {
        UIStyle.styleStatusBar(this);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        add(lblNodeCount); addSeparator();
        add(lblEdgeCount); addSeparator();
        add(lblCycle);      addSeparator();
        add(lblTotalSorts); addSeparator();
        add(lblCost); addSeparator();
        add(lblStatus); addSeparator();
        add(lblScale);
        add(Box.createHorizontalGlue());
        add(lblTip);
        add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        styleLabels();
    }

    private void styleLabels() {
        Color onDark = UIStyle.FG_ON_DARK;
        lblNodeCount.setForeground(onDark);
        lblEdgeCount.setForeground(onDark);
        lblCycle.setForeground(onDark);
        lblTotalSorts.setForeground(onDark);
        lblCost.setForeground(onDark);
        lblScale.setForeground(onDark);
        lblTip.setForeground(new Color(180, 200, 220));
        lblNodeCount.setFont(UIStyle.FONT_STATUS);
        lblEdgeCount.setFont(UIStyle.FONT_STATUS);
        lblCycle.setFont(UIStyle.FONT_STATUS);
        lblTotalSorts.setFont(UIStyle.FONT_STATUS);
        lblCost.setFont(UIStyle.FONT_STATUS);
        lblScale.setFont(UIStyle.FONT_STATUS);
        lblTip.setFont(UIStyle.FONT_STATUS);
    }

    private void addSeparator() {
        add(Box.createHorizontalStrut(UIStyle.GAP_MEDIUM));
        JLabel sep = new JLabel("|");
        sep.setForeground(new Color(90, 95, 105));
        add(sep);
        add(Box.createHorizontalStrut(UIStyle.GAP_MEDIUM));
    }

    public void updateStats(int nodeCount, int edgeCount,
                            boolean hasCycle, int totalSorts, long costMs) {
        lblNodeCount.setText("节点: " + nodeCount);
        lblEdgeCount.setText("边数: " + edgeCount);
        lblCycle.setText(hasCycle ? "含环" : "无环");
        lblCycle.setForeground(hasCycle ? UIStyle.DANGER : UIStyle.SUCCESS);
        lblTotalSorts.setText("序列数: " + totalSorts);
        lblCost.setText("耗时: " + costMs + " ms");
    }

    public void reset() {
        lblNodeCount.setText("节点: 0");
        lblEdgeCount.setText("边数: 0");
        lblCycle.setText("无环");
        lblCycle.setForeground(UIStyle.FG_ON_DARK);
        lblTotalSorts.setText("序列数: 0");
        lblCost.setText("耗时: -");
    }

    public void setTip(String text) {
        lblTip.setText(text == null ? "" : text);
    }

    public void setStatus(String text) {
        lblStatus.setText(text == null ? "" : text);
        lblStatus.setForeground(text != null && !text.isEmpty() ? Color.ORANGE : UIStyle.FG_ON_DARK);
    }

    public void setScale(double scale) {
        lblScale.setText("缩放: " + Math.round(scale * 100) + "%");
    }
}
