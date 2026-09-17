package view;

import model.Edge;
import model.Graph;
import model.Vertex;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 关系图绘制（C 包桩，T-C1~T-C5）：按契约 §七
// setGraph 接收关系图；setHighlightedCycle 标记环路径节点与边
// exportPNG 导出完整关系图
public class GraphPanel extends JPanel {

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    // 选中序列高亮：B/C 另行对接（契约 §七未定义）
    private List<String> selectedOrder = new ArrayList<>();
    private final Map<String, Point> nodePositions = new HashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 500));
    }

    // 契约 §七：接收关系图
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
        layoutNodes();
        repaint();
    }

    // 契约 §七：环高亮，接收闭合节点序列如 [A,B,C,A]
    public void setHighlightedCycle(List<String> closedCycle) {
        this.highlightedCycle = closedCycle == null ? new ArrayList<>() : new ArrayList<>(closedCycle);
        repaint();
    }

    // B/C 另行对接：选中序列高亮（不在契约中，由 C 最终确认）
    public void setSelectedOrder(List<String> order) {
        this.selectedOrder = order == null ? new ArrayList<>() : new ArrayList<>(order);
        repaint();
    }

    public void clear() {
        this.graph = null;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
        nodePositions.clear();
        repaint();
    }

    // 环形布局（桩）
    private void layoutNodes() {
        nodePositions.clear();
        if (graph == null || graph.isEmpty()) return;
        int n = graph.getVertexCount();
        int radius = Math.min(getWidth(), getHeight()) / 2 - 60;
        if (radius < 80) radius = 80;
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int i = 0;
        for (Vertex v : graph.getVertices()) {
            double angle = 2 * Math.PI * i / n;
            int x = cx + (int) (radius * Math.cos(angle));
            int y = cy + (int) (radius * Math.sin(angle));
            nodePositions.put(v.getName(), new Point(x, y));
            i++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (graph == null || graph.isEmpty()) {
            drawEmptyHint(g2);
            return;
        }
        if (nodePositions.isEmpty() || nodePositions.size() != graph.getVertexCount()) {
            layoutNodes();
        }
        drawEdges(g2);
        drawNodes(g2);
    }

    private void drawEmptyHint(Graphics2D g2) {
        g2.setColor(new Color(150, 150, 150));
        g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        String hint = "请载入或输入 <a,b> 关系数据后点击「计算」";
        int w = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (getWidth() - w) / 2, getHeight() / 2);
    }

    private void drawEdges(Graphics2D g2) {
        Stroke solid = new BasicStroke(1.5f);
        Stroke cycleStroke = new BasicStroke(2.5f);
        Set<String> cycleEdges = collectCycleEdges();

        for (Edge e : graph.getEdges()) {
            Point from = nodePositions.get(e.getFrom());
            Point to = nodePositions.get(e.getTo());
            if (from == null || to == null) continue;
            boolean inCycle = cycleEdges.contains(e.getFrom() + "|" + e.getTo());
            g2.setStroke(inCycle ? cycleStroke : solid);
            g2.setColor(inCycle ? Color.RED : new Color(80, 80, 80));
            if (e.isSelfLoop()) drawSelfLoop(g2, from);
            else drawArrow(g2, from.x, from.y, to.x, to.y);
        }
    }

    private void drawNodes(Graphics2D g2) {
        Set<String> cycleNodes = new HashSet<>(highlightedCycle);
        for (Vertex v : graph.getVertices()) {
            Point p = nodePositions.get(v.getName());
            if (p == null) continue;
            String name = v.getName();
            boolean inCycle = cycleNodes.contains(name);
            boolean isSelected = selectedOrder.contains(name);

            int r = 22;
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillOval(p.x - r + 2, p.y - r + 2, 2 * r, 2 * r);

            Color fill;
            if (inCycle) fill = new Color(255, 235, 235);
            else if (isSelected) fill = new Color(220, 240, 255);
            else fill = new Color(245, 245, 250);
            g2.setColor(fill);
            g2.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);

            g2.setColor(inCycle ? Color.RED
                    : (isSelected ? new Color(50, 120, 200) : new Color(120, 120, 140)));
            g2.setStroke(new BasicStroke(inCycle ? 2.0f : 1.5f));
            g2.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);

            g2.setColor(new Color(50, 50, 50));
            g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            int tw = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, p.x - tw / 2, p.y + 5);
        }
    }

    private void drawSelfLoop(Graphics2D g2, Point p) {
        g2.drawOval(p.x + 18, p.y - 28, 20, 20);
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int r = 22;
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        int sx = (int) (x1 + ux * r);
        int sy = (int) (y1 + uy * r);
        int ex = (int) (x2 - ux * r);
        int ey = (int) (y2 - uy * r);
        g2.drawLine(sx, sy, ex, ey);
        double ang = Math.atan2(dy, dx);
        int aLen = 10;
        double a1 = ang + Math.PI - 0.4;
        double a2 = ang + Math.PI + 0.4;
        int[] xs = {ex, (int)(ex + aLen * Math.cos(a1)), (int)(ex + aLen * Math.cos(a2))};
        int[] ys = {ey, (int)(ey + aLen * Math.sin(a1)), (int)(ey + aLen * Math.sin(a2))};
        g2.fillPolygon(xs, ys, 3);
    }

    private Set<String> collectCycleEdges() {
        Set<String> set = new HashSet<>();
        for (int i = 0; i + 1 < highlightedCycle.size(); i++) {
            set.add(highlightedCycle.get(i) + "|" + highlightedCycle.get(i + 1));
        }
        return set;
    }

    // 契约 §七：导出完整关系图 PNG
    public void exportPNG(File file) throws IOException {
        int w = Math.max(getWidth(), 600);
        int h = Math.max(getHeight(), 500);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        paintComponent(g2);
        g2.dispose();
        ImageIO.write(img, "PNG", file);
    }
}
