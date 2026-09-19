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
 * - 环形静态布局：所有顶点均匀分布在一个圆周上
 * - 支持高亮环路径（红）、高亮选中拓扑序（绿）
 * - 支持导出 PNG（中文不乱码）
 * - 双击节点显示入度/出度/后继课程信息
 * 接口对齐 MainController：setGraph / setHighlightedCycle / setSelectedOrder / exportPNG
 */
public class GraphPanel extends JPanel {

    private static final int NODE_RADIUS = 26;
    private static final Color NODE_FILL    = new Color(232, 240, 254);
    private static final Color NODE_BORDER  = new Color(70, 130, 180);
    private static final Color CYCLE_FILL   = new Color(255, 225, 225);
    private static final Color CYCLE_BORDER = new Color(214, 48, 49);
    private static final Color ORDER_FILL   = new Color(225, 255, 225);
    private static final Color ORDER_BORDER = new Color(39, 174, 96);
    private static final Color EDGE_COLOR   = new Color(140, 140, 140);
    private static final Color CYCLE_EDGE   = new Color(214, 48, 49);
    private static final Color TEXT_COLOR   = new Color(33, 33, 33);

    private static final String FONT_NAME = "Microsoft YaHei";

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    private List<String> selectedOrder    = new ArrayList<>();

    private final Map<String, Point2D.Double> nodePositions = new LinkedHashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
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
    // ============== MainController 对接的四个公共方法 ==========
    // ============================================================

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
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
    }

    // ============================================================
    // ====================== 布局计算 ===========================
    // ============================================================

    private void recomputeLayout(int w, int h) {
        nodePositions.clear();
        List<String> names = graph.getVertexNames();
        int n = names.size();
        if (n == 0) return;

        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(w, h) / 2.0 - NODE_RADIUS - 40;
        radius = Math.max(radius, 80);

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            nodePositions.put(names.get(i), new Point2D.Double(x, y));
        }
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
                    drawArrow(g2, p1, p2);
                }
            }
        }
    }

    private void drawArrow(Graphics2D g2, Point2D.Double from, Point2D.Double to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;

        double ux = dx / len;
        double uy = dy / len;

        double sx = from.x + ux * NODE_RADIUS;
        double sy = from.y + uy * NODE_RADIUS;
        double ex = to.x   - ux * NODE_RADIUS;
        double ey = to.y   - uy * NODE_RADIUS;

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

            int x = (int) Math.round(p.x - NODE_RADIUS);
            int y = (int) Math.round(p.y - NODE_RADIUS);
            int d = NODE_RADIUS * 2;

            g2.setColor(fill);
            g2.fillOval(x, y, d, d);

            g2.setColor(border);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawOval(x, y, d, d);

            g2.setColor(TEXT_COLOR);
            int maxW = d - 6;
            if (fm.stringWidth(name) > maxW) {
                g2.setFont(font.deriveFont((float) Math.max(9, font.getSize() - 2)));
                fm = g2.getFontMetrics();
            }
            int tw = fm.stringWidth(name);
            int tx = (int) Math.round(p.x) - tw / 2;
            int ty = (int) Math.round(p.y) + fm.getAscent() / 2 - 2;
            g2.drawString(name, tx, ty);

            g2.setFont(font);
            fm = g2.getFontMetrics();
        }
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

        // 找到被双击的节点
        String hit = null;
        for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
            Point2D.Double p = entry.getValue();
            double dist = Math.hypot(mouseX - p.x, mouseY - p.y);
            if (dist <= NODE_RADIUS) {
                hit = entry.getKey();
                break;
            }
        }
        if (hit == null) return;

        int inDeg  = graph.getInDegree(hit);
        int outDeg = graph.getOutDegree(hit);
        List<String> succ = graph.getSuccessors(hit);

        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(hit).append("\n");
        sb.append("入度（先修数）：").append(inDeg).append("\n");
        sb.append("出度（后继数）：").append(outDeg).append("\n");
        sb.append("后继课程：");
        if (succ.isEmpty()) {
            sb.append("无");
        } else {
            sb.append(String.join("、", succ));
        }

        JOptionPane.showMessageDialog(this, sb.toString(),
                "节点信息", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    // ======================== 测试入口 =========================
    // ============================================================

    public static void main(String[] args) {
        Graph g = new Graph();
        g.addEdge("MA140", "MA141");
        g.addEdge("MA141", "CS150");
        g.addEdge("MA141", "CS225");
        g.addEdge("CS150", "CS155");
        g.addEdge("CS155", "CS200");
        g.addEdge("CS155", "CS225");
        g.addEdge("CS200", "CS230");
        g.addEdge("CS225", "CS300");

        GraphPanel panel = new GraphPanel();
        panel.setGraph(g);
        panel.setSelectedOrder(Arrays.asList("MA140", "MA141", "CS150"));

        JFrame frame = new JFrame("GraphPanel 测试");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(panel);
        frame.setVisible(true);
    }
}