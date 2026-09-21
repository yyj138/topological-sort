package view;

import model.Graph;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
 * T-C3：环形布局兜底 + switchLayout 切换入口 + 拓扑序 5 色轮换
 * T-C4：滚轮缩放（以鼠标为中心）、拖拽平移、拖动节点、显示缩放比例
 * T-C5：exportPNG 导出完整关系图（含图例），中文不乱码，且图例不遮挡节点
 * ------------------------------------------------------------
 * 接口对齐 MainController：
 *   setGraph / setHighlightedCycle / setSelectedOrder(list[, idx]) / exportPNG
 * 供 B 调用扩展接口：
 *   setSelectedNode / switchLayout / resetView / getScale
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

    /** T-C3 拓扑序 5 色轮换色板（B 的需求：点不同结果行颜色不同） */
    private static final Color[][] ORDER_PALETTE = {
        { new Color(225, 255, 225), new Color( 39, 174,  96) }, // 绿
        { new Color(255, 240, 220), new Color(230, 126,  34) }, // 橙
        { new Color(225, 235, 255), new Color( 41, 128, 185) }, // 蓝
        { new Color(245, 225, 255), new Color(142,  68, 173) }, // 紫
        { new Color(255, 225, 235), new Color(192,  57,  43) }, // 红
    };

    private static final String FONT_NAME = "Microsoft YaHei";
    private static final double MIN_SCALE = 0.3;
    private static final double MAX_SCALE = 3.0;

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    private List<String> selectedOrder    = new ArrayList<>();
    private String selectedNode = null;

    /** T-C3：当前拓扑序颜色序号 */
    private int selectedOrderColorIndex = 0;

    private final LayoutManager layoutManager = new LayoutManager();
    private final Map<String, Point2D.Double> nodePositions = new LinkedHashMap<>();

    // T-C4 视图变换
    private double scale    = 1.0;
    private double offsetX  = 0;
    private double offsetY  = 0;
    private boolean layoutDirty = true;

    // T-C4 拖拽状态
    private String           dragNode         = null;
    private Point            dragStartScreen  = null;
    private Point2D.Double   dragStartModel   = null;
    private Point2D.Double   dragNodeStartPos = null;

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 650));
        setFont(new Font(FONT_NAME, Font.PLAIN, 13));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (e.getClickCount() >= 2) return;
                Point2D.Double mp = toModel(e.getX(), e.getY());
                String hit = hitNode(mp);
                if (hit != null) {
                    dragNode = hit;
                    dragStartModel = mp;
                    dragNodeStartPos = nodePositions.get(hit);
                } else {
                    dragNode = null;
                    dragStartScreen = e.getPoint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragNode = null; dragStartScreen = null;
                dragStartModel = null; dragNodeStartPos = null;
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showNodeInfo(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragNode != null && dragStartModel != null && dragNodeStartPos != null) {
                    Point2D.Double mp = toModel(e.getX(), e.getY());
                    double nx = dragNodeStartPos.x + (mp.x - dragStartModel.x);
                    double ny = dragNodeStartPos.y + (mp.y - dragStartModel.y);
                    nodePositions.put(dragNode, new Point2D.Double(nx, ny));
                    repaint();
                } else if (dragStartScreen != null) {
                    offsetX += e.getX() - dragStartScreen.x;
                    offsetY += e.getY() - dragStartScreen.y;
                    dragStartScreen = e.getPoint();
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            double delta = -e.getWheelRotation() * 0.1;
            double oldScale = scale;
            scale = clamp(scale + delta, MIN_SCALE, MAX_SCALE);
            if (Math.abs(scale - oldScale) < 1e-9) return;
            double mx = e.getX(), my = e.getY();
            double factor = scale / oldScale;
            offsetX = mx - (mx - offsetX) * factor;
            offsetY = my - (my - offsetY) * factor;
            repaint();
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutDirty = true;
                repaint();
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
        this.selectedOrderColorIndex = 0;
        this.nodePositions.clear();
        this.layoutDirty = true;
        this.resetView();
    }

    public void setHighlightedCycle(List<String> cycle) {
        this.highlightedCycle = (cycle != null) ? new ArrayList<>(cycle) : new ArrayList<>();
        repaint();
    }

    public void setSelectedOrder(List<String> order) {
        this.selectedOrder = (order != null) ? new ArrayList<>(order) : new ArrayList<>();
        this.selectedOrderColorIndex = 0;
        repaint();
    }

    /** T-C3 重载：带颜色序号，B 的 MainController 调用契约 */
    public void setSelectedOrder(List<String> order, int colorIndex) {
        this.selectedOrder = (order != null) ? new ArrayList<>(order) : new ArrayList<>();
        this.selectedOrderColorIndex = colorIndex;
        repaint();
    }

    public void setSelectedNode(String nodeName) {
        this.selectedNode = nodeName;
        repaint();
    }

    public void switchLayout(int mode) {
        layoutManager.setMode(mode);
        layoutDirty = true;
        repaint();
    }

    public void resetView() {
        scale = 1.0; offsetX = 0; offsetY = 0;
        repaint();
    }

    public double getScale() { return scale; }

    public void zoomIn() {
        scale = clamp(scale + 0.2, MIN_SCALE, MAX_SCALE);
        repaint();
    }

    public void zoomOut() {
        scale = clamp(scale - 0.2, MIN_SCALE, MAX_SCALE);
        repaint();
    }

    /**
     * T-C5：导出完整关系图为 PNG（修复版：图例不遮挡节点）
     * ---------------------------------------------------------
     * 行为约定（契约要求）：导出“完整关系图”，而非仅当前视口。
     * 修复要点：
     *   1) 忽略当前 scale / offsetX / offsetY 视图变换；
     *   2) 计算节点包围盒；
     *   3) 图片高度 = 节点高度 + 图例高度 + 间距，为图例预留独立顶部空间；
     *   4) 节点整体向下平移，确保图例在右上角不会遮挡任何节点或自环；
     *   5) 中文使用微软雅黑，避免乱码。
     */
    public void exportPNG(File file) throws IOException {
        // 1) 确保布局已经基于当前面板尺寸计算完毕，保持用户当前看到的布局
        int panelW = getWidth()  > 0 ? getWidth()  : getPreferredSize().width;
        int panelH = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        recomputeLayoutIfNeeded(panelW, panelH);

        // 2) 计算包围盒
        final int PADDING = 40;
        final int SELF_LOOP_TOP_EXTENT = 2 * NODE_RADIUS + 22;
        final int LEGEND_WIDTH  = 160;
        final int LEGEND_HEIGHT = 118;
        final int LEGEND_GAP    = 20;

        double minX = 0, minY = 0, maxX = panelW, maxY = panelH;
        boolean hasNodes = (graph != null) && !nodePositions.isEmpty();

        if (hasNodes) {
            minX = Double.MAX_VALUE;
            minY = Double.MAX_VALUE;
            maxX = -Double.MAX_VALUE;
            maxY = -Double.MAX_VALUE;
            for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
                String name = entry.getKey();
                Point2D.Double p = entry.getValue();
                double halfW = estimateNodeWidth(name) / 2.0;
                double halfH = NODE_RADIUS;

                minX = Math.min(minX, p.x - halfW - PADDING);
                maxX = Math.max(maxX, p.x + halfW + PADDING);
                minY = Math.min(minY, p.y - halfH - SELF_LOOP_TOP_EXTENT - PADDING);
                maxY = Math.max(maxY, p.y + halfH + PADDING);
            }
        }

        // 3) 计算图片尺寸
        int nodesW = (int) Math.ceil(maxX - minX);
        int nodesH = (int) Math.ceil(maxY - minY);

        int imgW = Math.max(nodesW, LEGEND_WIDTH + 30);
        int imgH = nodesH + LEGEND_HEIGHT + LEGEND_GAP;

        imgW = Math.max(imgW, 500);
        imgH = Math.max(imgH, 400);

        // 4) 创建图片，1:1 完整绘制
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, imgW, imgH);

            Graphics2D gBody = (Graphics2D) g2.create();
            try {
                gBody.translate(-minX, -minY + LEGEND_HEIGHT + LEGEND_GAP);
                if (graph != null) {
                    drawEdges(gBody);
                    drawNodes(gBody);
                } else {
                    gBody.setColor(Color.GRAY);
                    gBody.setFont(getFont() != null
                            ? getFont().deriveFont(14f)
                            : new Font(FONT_NAME, Font.PLAIN, 14));
                    String tip = "暂无数据：请先导入关系并点击计算";
                    FontMetrics fm = gBody.getFontMetrics();
                    gBody.drawString(tip, (imgW - fm.stringWidth(tip)) / 2, imgH / 2);
                }
            } finally {
                gBody.dispose();
            }

            if (graph != null) {
                drawLegend(g2, imgW, imgH);
            }
        } finally {
            g2.dispose();
        }

        ImageIO.write(img, "png", file);
    }

    // ============================================================
    // ========== 节点宽度估算（GraphPanel 与 LayoutManager 共用）===
    // ============================================================

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
        if (graph == null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                drawGraph(g2, getWidth(), getHeight());
            } finally { g2.dispose(); }
            return;
        }
        Graphics2D gBody = (Graphics2D) g.create();
        try {
            gBody.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gBody.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gBody.translate(offsetX, offsetY);
            gBody.scale(scale, scale);
            drawGraph(gBody, getWidth(), getHeight());
        } finally { gBody.dispose(); }

        Graphics2D gOverlay = (Graphics2D) g.create();
        try {
            gOverlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gOverlay.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawLegend(gOverlay, getWidth(), getHeight());
            drawZoomIndicator(gOverlay, getWidth(), getHeight());
        } finally { gOverlay.dispose(); }
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
        recomputeLayoutIfNeeded(w, h);
        drawEdges(g2);
        drawNodes(g2);
    }

    private void recomputeLayoutIfNeeded(int w, int h) {
        if (!layoutDirty) return;
        nodePositions.clear();
        if (graph != null) {
            Map<String, Point2D.Double> pos = layoutManager.computeLayout(graph, w, h, NODE_RADIUS);
            nodePositions.putAll(pos);
        }
        layoutDirty = false;
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
            try { successors = graph.getSuccessors(from); }
            catch (IllegalArgumentException ex) { continue; }
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

    private void drawArrow(Graphics2D g2, Point2D.Double from, Point2D.Double to,
                           String fromName, String toName) {
        double dx = to.x - from.x, dy = to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;
        double ux = dx / len, uy = dy / len;
        double fromHalfW = estimateNodeWidth(fromName) / 2.0;
        double toHalfW   = estimateNodeWidth(toName) / 2.0;
        double fromOffset = halfExtent(ux, uy, fromHalfW, NODE_RADIUS);
        double toOffset   = halfExtent(ux, uy, toHalfW,   NODE_RADIUS);
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

    private double halfExtent(double ux, double uy, double halfW, double halfH) {
        double tx = (Math.abs(ux) < 1e-6) ? Double.MAX_VALUE : halfW / Math.abs(ux);
        double ty = (Math.abs(uy) < 1e-6) ? Double.MAX_VALUE : halfH / Math.abs(uy);
        return Math.min(tx, ty);
    }

    private void drawSelfLoop(Graphics2D g2, Point2D.Double center) {
        double r = NODE_RADIUS + 8;
        double cx = center.x;
        double cy = center.y - NODE_RADIUS - 14;
        g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 45, 270, Arc2D.OPEN));
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

            Color fill = NODE_FILL, border = NODE_BORDER;
            int stroke = 2;

            if (highlightedCycle.contains(name)) {
                fill = CYCLE_FILL; border = CYCLE_BORDER; stroke = 3;
            }
            if (selectedOrder.contains(name)) {
                int ci = Math.floorMod(selectedOrderColorIndex, ORDER_PALETTE.length);
                fill = ORDER_PALETTE[ci][0];
                border = ORDER_PALETTE[ci][1];
                stroke = 3;
            }
            if (name.equals(selectedNode)) {
                fill = SELECT_FILL; border = SELECT_BORDER; stroke = 4;
            }

            int dWidth = estimateNodeWidth(name);
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
            int ty = (int) Math.round(p.y) + (fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(name, tx, ty);
        }
    }

    // ============================================================
    // ====================== 绘制图例 ===========================
    // ============================================================

    private void drawLegend(Graphics2D g2, int w, int h) {
        int boxW = 160, boxH = 118;
        int x = w - boxW - 15, y = 15;

        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);

        Font font = getFont() != null ? getFont().deriveFont(12f)
                : new Font(FONT_NAME, Font.PLAIN, 12);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int swatchX = x + 14, swatchW = 18, swatchH = 14, textGap = 10;
        int firstLineY = y + 22, lineGap = 24;

        // 动态计算“拓扑序”图例的颜色和文本
        Color legendOrderFill = ORDER_FILL;
        Color legendOrderBorder = ORDER_BORDER;
        String legendOrderText = "拓扑序";
        
        if (selectedOrder != null && !selectedOrder.isEmpty()) {
            int ci = Math.floorMod(selectedOrderColorIndex, ORDER_PALETTE.length);
            legendOrderFill = ORDER_PALETTE[ci][0];
            legendOrderBorder = ORDER_PALETTE[ci][1];
            legendOrderText = "当前拓扑序"; // 或者 "当前拓扑序(" + (selectedOrderColorIndex + 1) + ")"
        }

        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY,
                textGap, NODE_FILL, NODE_BORDER, "普通节点");
        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY + lineGap,
                textGap, CYCLE_FILL, CYCLE_BORDER, "环路径");
        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY + lineGap * 2,
                textGap, legendOrderFill, legendOrderBorder, legendOrderText); // 动态颜色和文本
        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY + lineGap * 3,
                textGap, SELECT_FILL, SELECT_BORDER, "选中节点");
    }

    private void drawLegendItem(Graphics2D g2, FontMetrics fm,
                                int swatchX, int swatchW, int swatchH,
                                int centerY, int textGap,
                                Color fill, Color border, String text) {
        int swatchTop = centerY - swatchH / 2;
        g2.setColor(fill);
        g2.fillRoundRect(swatchX, swatchTop, swatchW, swatchH, 5, 5);
        g2.setColor(border);
        g2.drawRoundRect(swatchX, swatchTop, swatchW, swatchH, 5, 5);

        g2.setColor(TEXT_COLOR);
        int textX = swatchX + swatchW + textGap;
        int textY = centerY + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, textX, textY);
    }

    // ============================================================
    // ================== 绘制缩放指示器（T-C4） ==================
    // ============================================================

    private void drawZoomIndicator(Graphics2D g2, int w, int h) {
        String text = String.format("缩放：%.0f%%", scale * 100);
        Font font = getFont() != null ? getFont().deriveFont(12f)
                : new Font(FONT_NAME, Font.PLAIN, 12);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(text), padH = 14, padV = 8;
        int boxW = textW + padH * 2;
        int boxH = fm.getHeight() + padV * 2;
        int x = 15, y = h - boxH - 15;

        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(new Color(180, 180, 180));
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);

        int textX = x + (boxW - textW) / 2;
        int textY = y + (boxH + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(TEXT_COLOR);
        g2.drawString(text, textX, textY);
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
    // ==================== 坐标与命中工具 ========================
    // ============================================================

    private Point2D.Double toModel(int screenX, int screenY) {
        return new Point2D.Double((screenX - offsetX) / scale,
                                  (screenY - offsetY) / scale);
    }

    private String hitNode(Point2D.Double modelPt) {
        if (nodePositions.isEmpty()) return null;
        for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
            Point2D.Double p = entry.getValue();
            double halfW = estimateNodeWidth(entry.getKey()) / 2.0;
            double halfH = NODE_RADIUS;
            if (Math.abs(modelPt.x - p.x) <= halfW
                    && Math.abs(modelPt.y - p.y) <= halfH) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ============================================================
    // ================= 双击节点显示信息 =========================
    // ============================================================

    private void showNodeInfo(int screenX, int screenY) {
        if (graph == null || nodePositions.isEmpty()) return;
        Point2D.Double mp = toModel(screenX, screenY);
        String hit = hitNode(mp);
        if (hit == null) return;

        int inDeg = graph.getInDegree(hit);
        int outDeg = graph.getOutDegree(hit);
        List<String> succ = graph.getSuccessors(hit);
        List<String> preds = new ArrayList<>();
        for (String other : graph.getVertexNames()) {
            if (graph.getSuccessors(other).contains(hit)) preds.add(other);
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
        panel.setSelectedOrder(Arrays.asList("MA140", "MA141", "CS150"), 0);

        JButton btnLayered  = new JButton("分层布局");
        JButton btnCircular = new JButton("环形布局");
        JButton btnReset    = new JButton("重置视图");
        JButton btnExport   = new JButton("导出 PNG");
        btnLayered.addActionListener(e  -> panel.switchLayout(LayoutManager.LAYOUT_LAYERED));
        btnCircular.addActionListener(e -> panel.switchLayout(LayoutManager.LAYOUT_CIRCULAR));
        btnReset.addActionListener(e    -> panel.resetView());
        btnExport.addActionListener(e -> {
            try {
                File out = new File("test_export.png");
                panel.exportPNG(out);
                JOptionPane.showMessageDialog(null,
                        "图片已保存到：\n" + out.getAbsolutePath(),
                        "导出成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        JPanel top = new JPanel();
        top.add(btnLayered); top.add(btnCircular); top.add(btnReset); top.add(btnExport);

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