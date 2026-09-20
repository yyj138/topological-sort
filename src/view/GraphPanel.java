package view;

import model.Graph;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系图绘制组件（组员 C）
 * ------------------------------------------------------------
 * T-C1：圆角矩形节点 + 宽度自适应 + 有向箭头 + 三种高亮 + 双击查看信息
 * T-C2：分层布局委托 LayoutManager，按画布尺寸自适应
 * T-C3：环形布局兜底 + switchLayout 布局切换入口
 * T-C5：exportPNG 导出画布（含背景、节点、边、图例），中文不乱码
 * ------------------------------------------------------------
 * 接口对齐 MainController：
 *   setGraph / setHighlightedCycle / setSelectedOrder / exportPNG
 * 供 B 调用扩展接口：
 *   setSelectedNode / switchLayout
 */
public class GraphPanel extends JPanel {

    public static final int NODE_RADIUS = 26;

    private static final Color NODE_FILL    = new Color(232, 240, 254);
    private static final Color NODE_BORDER  = new Color(70, 130, 180);
    private static final Color CYCLE_FILL   = new Color(255, 225, 225);
    private static final Color CYCLE_BORDER = new Color(214, 48, 49);
    private static final Color ORDER_FILL   = new Color(225, 255, 225);
    private static final Color ORDER_BORDER = new Color(39, 174, 96);
    private static final Color SELECT_FILL  = new Color(255, 249, 196);
    private static final Color SELECT_BORDER= new Color(243, 156, 18);
    private static final Color EDGE_COLOR   = new Color(140, 140, 140);
    private static final Color CYCLE_EDGE   = new Color(214, 48, 49);
    private static final Color TEXT_COLOR   = new Color(33, 33, 33);

    private static final String FONT_NAME = "Microsoft YaHei";

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    private List<String> selectedOrder    = new ArrayList<>();
    private String selectedNode = null;

    private final LayoutManager layoutManager = new LayoutManager();
    private final Map<String, Point2D.Double> nodePositions = new LinkedHashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 650));
        setFont(new Font(FONT_NAME, Font.PLAIN, 13));

        // 双击节点显示详细信息
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showNodeInfo(e.getX(), e.getY());
                }
            }
        });
    }

    // ============================================================
    // ============== MainController 对接的公共方法 ================
    // ============================================================

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
        this.selectedNode = null;
        this.nodePositions.clear();
        repaint();
    }

    public void setHighlightedCycle(List<String> cycle) {
        this.highlightedCycle = (cycle != null) ? new ArrayList<>(cycle) : new ArrayList<>();
        repaint();
    }

    public void setSelectedOrder(List<String> order) {
        this.selectedOrder = (order != null) ? new ArrayList<>(order) : new ArrayList<>();
        repaint();
    }

    /** 供 ResultPanel 单击某条序列时调用，高亮单个节点 */
    public void setSelectedNode(String nodeName) {
        this.selectedNode = nodeName;
        repaint();
    }

    /** 供工具栏切换布局：LayoutManager.LAYOUT_LAYERED / LAYOUT_CIRCULAR */
    public void switchLayout(int mode) {
        layoutManager.setMode(mode);
        repaint();
    }

    public void exportPNG(File file) throws IOException {
        int w = getWidth()  > 0 ? getWidth()  : getPreferredSize().width;
        int h = getHeight() > 0 ? getHeight() : getPreferredSize().height;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            drawGraph(g2, w, h);
        } finally {
            g2.dispose();
        }
        ImageIO.write(img, "png", file);
    }

    // ============================================================
    // ========== 节点宽度估算（GraphPanel 与 LayoutManager 共用）===
    // ============================================================

    /** 中文按 14px、英文数字按 8px，最少 NODE_RADIUS*2 */
    public static int estimateNodeWidth(String name) {
        int w = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            w += (c > 127) ? 14 : 8;
        }
        return Math.max(NODE_RADIUS * 2, w + 24);
    }

    // ============================================================
    // ======================= 绘制入口 ==========================
    // ============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawGraph(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    private void drawGraph(Graphics2D g2, int w, int h) {
        if (graph == null) {
            g2.setColor(Color.GRAY);
            g2.setFont(getFont().deriveFont(14f));
            String tip = "暂无数据：请先导入关系并点击计算";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tip, (w - fm.stringWidth(tip)) / 2, h / 2);
            return;
        }

        recomputeLayout(w, h);
        drawEdges(g2);
        drawNodes(g2);
        drawLegend(g2, w, h);   // T-C5 要求画布含图例
    }

    // ============================================================
    // ====================== 布局计算 ===========================
    // ============================================================

    private void recomputeLayout(int w, int h) {
        nodePositions.clear();
        if (graph == null) return;
        Map<String, Point2D.Double> pos = layoutManager.computeLayout(graph, w, h, NODE_RADIUS);
        nodePositions.putAll(pos);
    }

    // ============================================================
    // ====================== 绘制边 =============================
    // ============================================================

    private void drawEdges(Graphics2D g2) {
        List<String> names = graph.getVertexNames();
        for (String from : names) {
            Point2D.Double p1 = nodePositions.get(from);
            if (p1 == null) continue;

            List<String> successors;
            try {
                successors = graph.getSuccessors(from);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            for (String to : successors) {
                boolean selfLoop = from.equals(to);
                boolean inCycle  = isEdgeInCycle(from, to);

                g2.setColor(inCycle ? CYCLE_EDGE : EDGE_COLOR);
                g2.setStroke(new BasicStroke(inCycle ? 2.5f : 1.4f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                if (selfLoop) {
                    drawSelfLoop(g2, p1);
                } else {
                    Point2D.Double p2 = nodePositions.get(to);
                    if (p2 == null) continue;
                    drawArrow(g2, p1, p2, from, to);
                }
            }
        }
    }

    /** 从源/目标矩形的边缘出发画箭头，避免穿过节点本体 */
    private void drawArrow(Graphics2D g2, Point2D.Double from, Point2D.Double to,
                           String fromName, String toName) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;

        double ux = dx / len;
        double uy = dy / len;

        double fromHalfW = estimateNodeWidth(fromName) / 2.0;
        double toHalfW   = estimateNodeWidth(toName) / 2.0;
        double fromHalfH = NODE_RADIUS;
        double toHalfH   = NODE_RADIUS;

        double fromOffset = halfExtent(ux, uy, fromHalfW, fromHalfH);
        double toOffset   = halfExtent(ux, uy, toHalfW,   toHalfH);

        double sx = from.x + ux * fromOffset;
        double sy = from.y + uy * fromOffset;
        double ex = to.x   - ux * toOffset;
        double ey = to.y   - uy * toOffset;

        g2.draw(new Line2D.Double(sx, sy, ex, ey));

        double arrowLen = 11;
        double angle = Math.atan2(ey - sy, ex - sx);
        int x1 = (int) Math.round(ex - arrowLen * Math.cos(angle - Math.PI / 7));
        int y1 = (int) Math.round(ey - arrowLen * Math.sin(angle - Math.PI / 7));
        int x2 = (int) Math.round(ex - arrowLen * Math.cos(angle + Math.PI / 7));
        int y2 = (int) Math.round(ey - arrowLen * Math.sin(angle + Math.PI / 7));
        g2.fillPolygon(new int[]{(int) Math.round(ex), x1, x2},
                new int[]{(int) Math.round(ey), y1, y2}, 3);
    }

    /** 沿单位方向 (ux,uy) 从矩形中心到矩形边缘的距离 */
    private double halfExtent(double ux, double uy, double halfW, double halfH) {
        double tx = (Math.abs(ux) < 1e-6) ? Double.MAX_VALUE : halfW / Math.abs(ux);
        double ty = (Math.abs(uy) < 1e-6) ? Double.MAX_VALUE : halfH / Math.abs(uy);
        return Math.min(tx, ty);
    }

    private void drawSelfLoop(Graphics2D g2, Point2D.Double center) {
        double r = NODE_RADIUS + 8;
        double cx = center.x;
        double cy = center.y - NODE_RADIUS - 14;

        Arc2D arc = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
                45, 270, Arc2D.OPEN);
        g2.draw(arc);

        double endAngle = Math.toRadians(45);
        double ex = cx + r * Math.cos(endAngle);
        double ey = cy - r * Math.sin(endAngle);
        double tangent = endAngle - Math.PI / 2;
        double arrowLen = 9;
        int x1 = (int) Math.round(ex - arrowLen * Math.cos(tangent - Math.PI / 6));
        int y1 = (int) Math.round(ey - arrowLen * Math.sin(tangent - Math.PI / 6));
        int x2 = (int) Math.round(ex - arrowLen * Math.cos(tangent + Math.PI / 6));
        int y2 = (int) Math.round(ey - arrowLen * Math.sin(tangent + Math.PI / 6));
        g2.fillPolygon(new int[]{(int) Math.round(ex), x1, x2},
                new int[]{(int) Math.round(ey), y1, y2}, 3);
    }

    // ============================================================
    // ====================== 绘制节点 ===========================
    // ============================================================

    private void drawNodes(Graphics2D g2) {
        Font font = getFont();
        if (font == null) font = new Font(FONT_NAME, Font.PLAIN, 13);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
            String name = entry.getKey();
            Point2D.Double p = entry.getValue();

            Color fill   = NODE_FILL;
            Color border = NODE_BORDER;
            int   stroke = 2;

            if (highlightedCycle.contains(name)) {
                fill = CYCLE_FILL; border = CYCLE_BORDER; stroke = 3;
            }
            if (selectedOrder.contains(name)) {
                fill = ORDER_FILL; border = ORDER_BORDER; stroke = 3;
            }
            if (name.equals(selectedNode)) {
                fill = SELECT_FILL; border = SELECT_BORDER; stroke = 4;
            }

            int dWidth  = estimateNodeWidth(name);
            int dHeight = NODE_RADIUS * 2;
            int x = (int) Math.round(p.x - dWidth / 2.0);
            int y = (int) Math.round(p.y - dHeight / 2.0);
            int arc = 14;

            g2.setColor(fill);
            g2.fillRoundRect(x, y, dWidth, dHeight, arc, arc);

            g2.setColor(border);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawRoundRect(x, y, dWidth, dHeight, arc, arc);

            g2.setColor(TEXT_COLOR);
            int tw = fm.stringWidth(name);
            int tx = (int) Math.round(p.x) - tw / 2;
            int ty = (int) Math.round(p.y) + fm.getAscent() / 2 - 2;
            g2.drawString(name, tx, ty);
        }
    }

    // ============================================================
    // ====================== 绘制图例 ===========================
    // ============================================================

    /** 右上角绘制图例，说明颜色含义（T-C5 要求画布含图例） */
    private void drawLegend(Graphics2D g2, int w, int h) {
        int boxW = 160, boxH = 118;
        int x = w - boxW - 15;
        int y = 15;

        // 半透明白底
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);

        Font font = getFont() != null ? getFont().deriveFont(12f)
                : new Font(FONT_NAME, Font.PLAIN, 12);
        g2.setFont(font);

        int swatchX = x + 12;
        int swatchW = 18;
        int swatchH = 14;
        int lineY = y + 22;
        int lineGap = 24;

        // 1) 普通节点
        g2.setColor(NODE_FILL);
        g2.fillRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(NODE_BORDER);
        g2.drawRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(TEXT_COLOR);
        g2.drawString("普通节点", swatchX + swatchW + 8, lineY);

        // 2) 环路径
        lineY += lineGap;
        g2.setColor(CYCLE_FILL);
        g2.fillRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(CYCLE_BORDER);
        g2.drawRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(TEXT_COLOR);
        g2.drawString("环路径", swatchX + swatchW + 8, lineY);

        // 3) 拓扑序
        lineY += lineGap;
        g2.setColor(ORDER_FILL);
        g2.fillRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(ORDER_BORDER);
        g2.drawRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(TEXT_COLOR);
        g2.drawString("拓扑序", swatchX + swatchW + 8, lineY);

        // 4) 选中节点
        lineY += lineGap;
        g2.setColor(SELECT_FILL);
        g2.fillRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(SELECT_BORDER);
        g2.drawRoundRect(swatchX, lineY - swatchH + 2, swatchW, swatchH, 5, 5);
        g2.setColor(TEXT_COLOR);
        g2.drawString("选中节点", swatchX + swatchW + 8, lineY);
    }

    // ============================================================
    // =================== 环高亮的边判断 =========================
    // ============================================================

    private boolean isEdgeInCycle(String from, String to) {
        if (highlightedCycle == null || highlightedCycle.size() < 2) return false;
        int n = highlightedCycle.size();
        for (int i = 0; i < n; i++) {
            String a = highlightedCycle.get(i);
            String b = highlightedCycle.get((i + 1) % n);
            if (a.equals(from) && b.equals(to)) return true;
        }
        return false;
    }

    // ============================================================
    // ================= 双击节点显示信息 =========================
    // ============================================================

    private void showNodeInfo(int mouseX, int mouseY) {
        if (graph == null || nodePositions.isEmpty()) return;

        String hit = null;
        for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
            Point2D.Double p = entry.getValue();
            double halfW = estimateNodeWidth(entry.getKey()) / 2.0;
            double halfH = NODE_RADIUS;
            if (Math.abs(mouseX - p.x) <= halfW && Math.abs(mouseY - p.y) <= halfH) {
                hit = entry.getKey();
                break;
            }
        }
        if (hit == null) return;

        int inDeg  = graph.getInDegree(hit);
        int outDeg = graph.getOutDegree(hit);
        List<String> succ = graph.getSuccessors(hit);

        // 自己算先修（A 的 Graph 尚未提供 getPredecessors）
        List<String> preds = new ArrayList<>();
        for (String other : graph.getVertexNames()) {
            if (graph.getSuccessors(other).contains(hit)) {
                preds.add(other);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(hit).append("\n");
        sb.append("入度（先修数）：").append(inDeg).append("\n");
        sb.append("出度（后继数）：").append(outDeg).append("\n");
        sb.append("先修课程：");
        sb.append(preds.isEmpty() ? "无" : String.join("、", preds)).append("\n");
        sb.append("后继课程：");
        sb.append(succ.isEmpty() ? "无" : String.join("、", succ));

        JOptionPane.showMessageDialog(this, sb.toString(),
                "节点信息", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    // ======================== 测试入口 =========================
    // ============================================================

    public static void main(String[] args) {
        Graph g = new Graph();
        // 任务书图1 的课程关系
        g.addEdge("MA140", "MA141");
        g.addEdge("MA140", "CS150");
        g.addEdge("MA141", "CS150");
        g.addEdge("CS150", "CS155");
        g.addEdge("CS155", "CS200");
        g.addEdge("CS155", "CS225");
        g.addEdge("CS200", "CS230");
        g.addEdge("CS225", "CS230");
        g.addEdge("CS225", "CS300");
        g.addEdge("CS225", "CS250");
        g.addEdge("CS230", "CS301");
        g.addEdge("CS300", "CS340");
        g.addEdge("CS340", "CS345");
        g.addEdge("CS345", "CS350");
        g.addEdge("CS350", "CS360");
        g.addEdge("CS360", "CS390");

        GraphPanel panel = new GraphPanel();
        panel.setGraph(g);
        panel.setSelectedOrder(Arrays.asList("MA140", "MA141", "CS150"));

        JButton btnLayered  = new JButton("分层布局");
        JButton btnCircular = new JButton("环形布局");
        btnLayered.addActionListener(e  -> panel.switchLayout(LayoutManager.LAYOUT_LAYERED));
        btnCircular.addActionListener(e -> panel.switchLayout(LayoutManager.LAYOUT_CIRCULAR));

        JPanel top = new JPanel();
        top.add(btnLayered);
        top.add(btnCircular);

        JFrame frame = new JFrame("GraphPanel 测试");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 750);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}