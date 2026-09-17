package topo.view;

import topo.algorithm.CycleResult;
import topo.model.Edge;
import topo.model.Graph;
import topo.model.Vertex;

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
import java.util.List;
import java.util.Map;

// 关系图绘制（C 包桩，T-C1~T-C5）：简单圆形+箭头渲染，便于 B 独立运行
public class GraphPanel extends JPanel {

    private Graph graph;
    private CycleResult cycleResult;
    private List<String> selectedOrder = new ArrayList<>();
    private final Map<String, Point> nodePositions = new HashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 500));
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.selectedOrder.clear();
        this.cycleResult = null;
        layoutNodes();
        repaint();
    }

    public void setCycleHighlight(CycleResult cycleResult) {
        this.cycleResult = cycleResult;
        repaint();
    }

    public void setSelectedOrder(List<String> order) {
        this.selectedOrder = order == null ? new ArrayList<>() : new ArrayList<>(order);
        repaint();
    }

    public void clear() {
        this.graph = null;
        this.selectedOrder.clear();
        this.cycleResult = null;
        nodePositions.clear();
        repaint();
    }

    // 环形布局（桩）
    private void layoutNodes() {
        nodePositions.clear();
        if (graph == null || graph.isEmpty()) return;
        int n = graph.vertexCount();
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
        if (nodePositions.isEmpty() || nodePositions.size() != graph.vertexCount()) {
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
        java.util.Set<String> cycleEdges = collectCycleEdges();

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
        java.util.Set<String> cycleNodes = collectCycleNodes();
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

    private java.util.Set<String> collectCycleEdges() {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (cycleResult == null || !cycleResult.hasCycle()) return set;
        List<String> path = cycleResult.getCyclePath();
        for (int i = 0; i + 1 < path.size(); i++) {
            set.add(path.get(i) + "|" + path.get(i + 1));
        }
        return set;
    }

    private java.util.Set<String> collectCycleNodes() {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (cycleResult == null || !cycleResult.hasCycle()) return set;
        set.addAll(cycleResult.getCyclePath());
        return set;
    }

    // 导出画布为 PNG（C 包 T-C5/T-C8 调用）
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
