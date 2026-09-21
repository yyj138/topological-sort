package view;

import model.Graph;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系图绘制组件（组员 C）
 * - 环形静态布局：所有顶点均匀分布在一个圆周上
 * - 支持高亮环路径（红）、高亮选中拓扑序（绿）
 * - 支持导出 PNG（中文不乱码）
 * 接口对齐 MainController：setGraph / setHighlightedCycle / setSelectedOrder / exportPNG
 */
public class GraphPanel extends JPanel {

    private static final int NODE_RADIUS = 26;
    // 交互规范三态：默认蓝、选中序列绿、环节点红，三者互不叠加
    private static final Color NODE_FILL    = new Color(135, 206, 250);
    private static final Color NODE_BORDER  = new Color(21, 101, 192);
    private static final Color CYCLE_FILL   = new Color(255, 129, 128);
    private static final Color CYCLE_BORDER = new Color(183, 28, 28);
    // 选中序列高亮色板：不同序列轮换颜色（节点填充/边框/徽章同色系），避开默认节点蓝
    private static final Color[] ORDER_FILLS = {
            new Color(144, 238, 144),   // 绿
            new Color(255, 205, 130),   // 橙
            new Color(206, 176, 246),   // 紫
            new Color(130, 216, 226),   // 青
            new Color(248, 168, 196),   // 粉
    };
    private static final Color[] ORDER_BORDERS = {
            new Color(27, 122, 62),
            new Color(191, 111, 18),
            new Color(106, 61, 168),
            new Color(17, 122, 141),
            new Color(182, 55, 105),
    };
    private static final Color EDGE_COLOR   = new Color(140, 140, 140);
    private static final Color CYCLE_EDGE   = new Color(211, 47, 47);
    private static final Color TEXT_COLOR   = new Color(33, 33, 33);

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    private List<String> selectedOrder    = new ArrayList<>();
    // 当前高亮色板下标：随选中结果的序号轮换，让点不同行时颜色不同
    private int selectedColorIndex;

    private final Map<String, Point2D.Double> nodePositions = new LinkedHashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
        // 逻辑字体保证中文节点名在任何系统上正常渲染
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
        this.selectedColorIndex = 0;
        this.nodePositions.clear();
        repaint();
    }

    public void setHighlightedCycle(List<String> cycle) {
        this.highlightedCycle = (cycle != null) ? new ArrayList<>(cycle) : new ArrayList<>();
        repaint();
    }

    // 原契约方法保留：默认使用第 0 号色板（绿色）
    public void setSelectedOrder(List<String> order) {
        setSelectedOrder(order, 0);
    }

    // 双参数版本：colorIndex 为颜色序号，按五种颜色循环
    public void setSelectedOrder(List<String> order, int colorIndex) {
        this.selectedOrder = (order != null) ? new ArrayList<>(order) : new ArrayList<>();
        this.selectedColorIndex =
                Math.floorMod(colorIndex, ORDER_FILLS.length);
        repaint();
    }

    // 预留接口：单节点黄色高亮，调用方传入当前图中的节点名称，传入 null 表示清除
    public void setSelectedNode(String nodeName) {
        // 预留，暂不实现
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
                g2.setStroke(new BasicStroke(inCycle ? 3.0f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

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
        g2.fillPolygon(new int[]{(int) Math.round(ex), x1, x2}, new int[]{(int) Math.round(ey), y1, y2}, 3);
    }

    private void drawSelfLoop(Graphics2D g2, Point2D.Double center) {
        double r = NODE_RADIUS + 8;
        double cx = center.x;
        double cy = center.y - NODE_RADIUS - 14;

        Arc2D arc = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 45, 270, Arc2D.OPEN);
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
        g2.fillPolygon(new int[]{(int) Math.round(ex), x1, x2}, new int[]{(int) Math.round(ey), y1, y2}, 3);
    }

    private void drawNodes(Graphics2D g2) {
        Font font = getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
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
                fill = ORDER_FILLS[selectedColorIndex];
                border = ORDER_BORDERS[selectedColorIndex];
                stroke = 3;
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

            // 选中序列的节点右上角叠加序号徽章：
            // 拓扑序列必含全部节点，仅颜色无法区分不同序列，用序号体现该节点在序列中的位置
            if (selectedOrder.contains(name)) {
                drawOrderBadge(g2, p, selectedOrder.indexOf(name) + 1);
            }
        }
    }

    // 在节点右上角画序号徽章（深绿圆底白字），临时改字体后必须恢复，避免污染后续节点绘制
    private void drawOrderBadge(Graphics2D g2, Point2D.Double p, int order) {
        Font oldFont = g2.getFont();
        String text = String.valueOf(order);
        int r = text.length() > 1 ? 12 : 10;
        int cx = (int) Math.round(p.x + NODE_RADIUS * 0.75);
        int cy = (int) Math.round(p.y - NODE_RADIUS * 0.75);

        g2.setColor(ORDER_BORDERS[selectedColorIndex]);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setFont(oldFont.deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2 - 1);

        g2.setFont(oldFont);
    }

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
}