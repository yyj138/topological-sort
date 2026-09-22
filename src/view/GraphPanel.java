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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系图绘制组件（组员 C）
 * ------------------------------------------------------------
 * T-C1：圆角矩形节点 + 宽度自适应 + 有向箭头 + 多种高亮 + 双击查看信息
 * T-C2：分层布局委托 LayoutManager，按画布尺寸自适应
 * T-C3：环形布局兜底 + switchLayout 切换入口 + 拓扑序 5 色轮换
 * T-C4：滚轮缩放、按钮缩放、拖拽平移、拖动节点、单击节点高亮、显示缩放比例
 * T-C5：exportPNG 导出完整关系图（含图例），中文不乱码，且图例不遮挡节点
 * ------------------------------------------------------------
 * 本次优化（2026-09-22，按 B 的需求清单）：
 *  1) 图例精简：缩到 105x68、字体 10f、仅保留"当前拓扑序"和"选中节点"两项
 *  2) 节点右上角序号徽章：显示该节点在当前拓扑序中的位置（1,2,3...）
 *  3) 边跟随拓扑序高亮：属于当前拓扑序的边颜色跟随节点边框色，粗细 2.2f
 *  4) 右下角悬浮按钮：+ / - / 重置视图（滚轮缩放保留）
 *  5) 左上角"放大查看"按钮：弹出新窗口独立显示大图
 * ------------------------------------------------------------
 * 接口对齐 MainController（未改变）：
 *   setGraph / setHighlightedCycle / setSelectedOrder(list[, idx]) / exportPNG
 * 供 B 调用扩展接口：
 *   setSelectedNode / switchLayout / resetView / getScale / zoomIn / zoomOut
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

    /** 拓扑序 5 色轮换色板 */
    private static final Color[][] ORDER_PALETTE = {
        { new Color(225, 255, 225), new Color( 39, 174,  96) },
        { new Color(255, 240, 220), new Color(230, 126,  34) },
        { new Color(225, 235, 255), new Color( 41, 128, 185) },
        { new Color(245, 225, 255), new Color(142,  68, 173) },
        { new Color(255, 225, 235), new Color(192,  57,  43) },
    };

    private static final String FONT_NAME = "Microsoft YaHei";
    private static final double MIN_SCALE = 0.3;
    private static final double MAX_SCALE = 3.0;

    // ===== 图例尺寸常量（导出与界面绘制共用，保证一致） =====
    private static final int LEGEND_WIDTH  = 105;
    private static final int LEGEND_HEIGHT = 68;
    private static final int LEGEND_MARGIN = 15; // 距离面板右侧/顶部的边距

    private Graph graph;
    private List<String> highlightedCycle = new ArrayList<>();
    private List<String> selectedOrder    = new ArrayList<>();
    private String selectedNode = null;
    private int selectedOrderColorIndex = 0;

    /** 节点名 -> 在 selectedOrder 中的位置（1-based），用于快速绘制序号徽章 */
    private final Map<String, Integer> orderPositionMap = new HashMap<>();

    private final LayoutManager layoutManager = new LayoutManager();
    private final Map<String, Point2D.Double> nodePositions = new LinkedHashMap<>();

    // 视图变换
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean layoutDirty = true;

    // 拖拽状态
    private String dragNode = null;
    private Point dragStartScreen = null;
    private Point2D.Double dragStartModel = null;
    private Point2D.Double dragNodeStartPos = null;

    // 悬浮按钮
    private final JButton btnZoomIn   = new JButton("+");
    private final JButton btnZoomOut  = new JButton("-");
    private final JButton btnResetView = new JButton("重置"); // ⟲
    private final JButton btnPopout   = new JButton("<>");

    /** popout 窗口里的实例不再显示"放大查看"按钮，避免无限递归 */
    private final boolean isPopoutInstance;

    public GraphPanel() {
        this(false);
    }

    public GraphPanel(boolean isPopoutInstance) {
        this.isPopoutInstance = isPopoutInstance;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 650));
        setFont(new Font(FONT_NAME, Font.PLAIN, 13));
        setLayout(null); // 手动定位悬浮按钮

        initOverlayButtons();
        initMouseListeners();
    }

    // ============================================================
    // ==================== 悬浮按钮初始化 ========================
    // ============================================================

    private void initOverlayButtons() {
        styleSmallButton(btnZoomIn, 15);
        styleSmallButton(btnZoomOut, 15);
        styleSmallButton(btnResetView, 13);
        styleSmallButton(btnPopout, 11);

        btnZoomIn.addActionListener(e -> zoomIn());
        btnZoomOut.addActionListener(e -> zoomOut());
        btnResetView.addActionListener(e -> resetView());
        btnPopout.addActionListener(e -> showPopoutView());

        add(btnZoomIn);
        add(btnZoomOut);
        add(btnResetView);
        if (!isPopoutInstance) {
            add(btnPopout);
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutDirty = true;
                layoutOverlayButtons();
                repaint();
            }
        });
    }

    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font(util.UIStyle.FONT_FAMILY, Font.PLAIN, 11));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(new Dimension(56, 28));
        b.setSize(new Dimension(56, 28));
    }

    /** 根据面板当前尺寸，摆放右下角与左上角的悬浮按钮 */
    private void layoutOverlayButtons() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        int size = 30;
        int margin = 12;
        int gap = 6;
        int y = h - size - margin;
        // 从右往左摆
        int x = w - margin;
        btnResetView.setBounds(x - 56, y, 56, size); x -= 56 + gap;
        btnZoomOut.setBounds(x - size, y, size, size); x -= size + gap;
        btnZoomIn.setBounds(x - size, y, size, size);
        if (!isPopoutInstance) {
            btnPopout.setBounds(15, 15, 50, 30);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::layoutOverlayButtons);
    }

    // ============================================================
    // ==================== 鼠标监听初始化 ========================
    // ============================================================

    private void initMouseListeners() {
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
                    selectedNode = hit;
                    repaint();
                } else {
                    dragNode = null;
                    dragStartScreen = e.getPoint();
                    selectedNode = null;
                    repaint();
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
    }

    // ============================================================
    // ============== MainController 对接的公共方法 ================
    // ============================================================

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.highlightedCycle = new ArrayList<>();
        this.selectedOrder = new ArrayList<>();
        this.orderPositionMap.clear();
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
        rebuildOrderPositionMap();
        repaint();
    }

    /** 带颜色序号的重载，B 的 MainController 调用契约 */
    public void setSelectedOrder(List<String> order, int colorIndex) {
        this.selectedOrder = (order != null) ? new ArrayList<>(order) : new ArrayList<>();
        this.selectedOrderColorIndex = colorIndex;
        rebuildOrderPositionMap();
        repaint();
    }

    private void rebuildOrderPositionMap() {
        orderPositionMap.clear();
        for (int i = 0; i < selectedOrder.size(); i++) {
            orderPositionMap.putIfAbsent(selectedOrder.get(i), i + 1);
        }
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

    /** T-C4 按钮缩放：放大 */
    public void zoomIn()  { adjustScale( 0.1); }

    /** T-C4 按钮缩放：缩小 */
    public void zoomOut() { adjustScale(-0.1); }

    /** 以画布中心为基准调整缩放 */
    private void adjustScale(double delta) {
        double oldScale = scale;
        scale = clamp(scale + delta, MIN_SCALE, MAX_SCALE);
        if (Math.abs(scale - oldScale) < 1e-9) return;
        double cx = getWidth()  / 2.0;
        double cy = getHeight() / 2.0;
        double factor = scale / oldScale;
        offsetX = cx - (cx - offsetX) * factor;
        offsetY = cy - (cy - offsetY) * factor;
        repaint();
    }

    /** 弹出独立窗口放大查看当前关系图 */
    private void showPopoutView() {
        if (graph == null) {
            JOptionPane.showMessageDialog(this,
                    "暂无数据可查看，请先导入关系并点击计算",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        GraphPanel popout = new GraphPanel(true);
        popout.setGraph(graph);
        popout.setHighlightedCycle(new ArrayList<>(highlightedCycle));
        popout.setSelectedOrder(new ArrayList<>(selectedOrder), selectedOrderColorIndex);
        if (selectedNode != null) popout.setSelectedNode(selectedNode);
        popout.resetView();

        JFrame f = new JFrame("关系图 - 放大查看");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setSize(1100, 800);
        f.setLocationRelativeTo(this);
        f.setLayout(new BorderLayout());
        f.add(popout, BorderLayout.CENTER);
        f.setVisible(true);
    }

    /**
     * T-C5：导出完整关系图为 PNG（含图例预留空间）
     */
    public void exportPNG(File file) throws IOException {
        int panelW = getWidth()  > 0 ? getWidth()  : getPreferredSize().width;
        int panelH = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        recomputeLayoutIfNeeded(panelW, panelH);

        final int PADDING = 40;
        final int SELF_LOOP_TOP_EXTENT = 2 * NODE_RADIUS + 22;
        final int LEGEND_GAP = 20;

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

        int nodesW = (int) Math.ceil(maxX - minX);
        int nodesH = (int) Math.ceil(maxY - minY);

        int imgW = Math.max(nodesW, LEGEND_WIDTH + 30);
        int imgH = nodesH + LEGEND_HEIGHT + LEGEND_GAP;

        imgW = Math.max(imgW, 500);
        imgH = Math.max(imgH, 400);

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
        boolean hasOrder = selectedOrder != null && selectedOrder.size() >= 2;
        Color orderEdgeColor = EDGE_COLOR;
        if (hasOrder) {
            int ci = Math.floorMod(selectedOrderColorIndex, ORDER_PALETTE.length);
            orderEdgeColor = ORDER_PALETTE[ci][1]; // 与节点边框色一致
        }

        for (String from : names) {
            Point2D.Double p1 = nodePositions.get(from);
            if (p1 == null) continue;
            List<String> successors;
            try { successors = graph.getSuccessors(from); }
            catch (IllegalArgumentException ex) { continue; }
            for (String to : successors) {
                boolean selfLoop = from.equals(to);
                boolean inCycle  = isEdgeInCycle(from, to);
                boolean inOrder  = hasOrder && isEdgeInOrder(from, to);

                Color edgeColor;
                float strokeWidth;
                if (inCycle) {
                    edgeColor = CYCLE_EDGE;
                    strokeWidth = 2.5f;
                } else if (inOrder) {
                    edgeColor = orderEdgeColor;
                    strokeWidth = 2.2f;
                } else {
                    edgeColor = EDGE_COLOR;
                    strokeWidth = 1.4f;
                }

                g2.setColor(edgeColor);
                g2.setStroke(new BasicStroke(strokeWidth,
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

    /** 判断 from->to 这条边是否属于当前拓扑序（from 出现在 to 之前） */
    private boolean isEdgeInOrder(String from, String to) {
        Integer fi = orderPositionMap.get(from);
        Integer ti = orderPositionMap.get(to);
        return fi != null && ti != null && fi < ti;
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

        boolean hasOrder = selectedOrder != null && !selectedOrder.isEmpty();
        int paletteIdx = Math.floorMod(selectedOrderColorIndex, ORDER_PALETTE.length);

        for (Map.Entry<String, Point2D.Double> entry : nodePositions.entrySet()) {
            String name = entry.getKey();
            Point2D.Double p = entry.getValue();

            Color fill = NODE_FILL, border = NODE_BORDER;
            int stroke = 2;

            if (highlightedCycle.contains(name)) {
                fill = CYCLE_FILL; border = CYCLE_BORDER; stroke = 3;
            }
            if (hasOrder && selectedOrder.contains(name)) {
                fill = ORDER_PALETTE[paletteIdx][0];
                border = ORDER_PALETTE[paletteIdx][1];
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

            // === 节点序号徽章 ===
            if (hasOrder) {
                Integer pos = orderPositionMap.get(name);
                if (pos != null) {
                    drawOrderBadge(g2, p, dWidth, dHeight, pos,
                            ORDER_PALETTE[paletteIdx][1]);
                }
            }
        }
    }

    /** 绘制节点右上角的序号徽章 */
    private void drawOrderBadge(Graphics2D g2, Point2D.Double center,
                                int dWidth, int dHeight, int position,
                                Color badgeColor) {
        int r = 11;
        int cx = (int) Math.round(center.x + dWidth / 2.0 - 4);
        int cy = (int) Math.round(center.y - dHeight / 2.0 + 4);

        g2.setColor(Color.WHITE);
        g2.fillOval(cx - r - 1, cy - r - 1, (r + 1) * 2, (r + 1) * 2);
        g2.setColor(badgeColor);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        String num = String.valueOf(position);
        Font badgeFont = new Font(FONT_NAME, Font.BOLD, 11);
        g2.setFont(badgeFont);
        FontMetrics bfm = g2.getFontMetrics();
        int nw = bfm.stringWidth(num);
        int nx = cx - nw / 2;
        int ny = cy + (bfm.getAscent() - bfm.getDescent()) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(num, nx, ny);

        Font restore = getFont();
        if (restore == null) restore = new Font(FONT_NAME, Font.PLAIN, 13);
        g2.setFont(restore);
    }

    // ============================================================
    // ====================== 绘制图例（紧凑版） ==================
    // ============================================================

    private void drawLegend(Graphics2D g2, int w, int h) {
        // 紧凑版图例：105x68、字体 10f、只保留 2 项
        int boxW = LEGEND_WIDTH, boxH = LEGEND_HEIGHT;
        int x = w - boxW - LEGEND_MARGIN;
        int y = LEGEND_MARGIN;

        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(x, y, boxW, boxH, 8, 8);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, boxW, boxH, 8, 8);

        Font font = getFont() != null ? getFont().deriveFont(10f)
                : new Font(FONT_NAME, Font.PLAIN, 10);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int swatchX = x + 9;
        int swatchW = 14, swatchH = 11;
        int textGap = 7;
        int firstLineY = y + 21;
        int lineGap = 22;

        Color legendOrderFill = ORDER_FILL;
        Color legendOrderBorder = ORDER_BORDER;
        if (selectedOrder != null && !selectedOrder.isEmpty()) {
            int ci = Math.floorMod(selectedOrderColorIndex, ORDER_PALETTE.length);
            legendOrderFill = ORDER_PALETTE[ci][0];
            legendOrderBorder = ORDER_PALETTE[ci][1];
        }

        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY,
                textGap, legendOrderFill, legendOrderBorder, "当前拓扑序");
        drawLegendItem(g2, fm, swatchX, swatchW, swatchH, firstLineY + lineGap,
                textGap, SELECT_FILL, SELECT_BORDER, "选中节点");
    }

    private void drawLegendItem(Graphics2D g2, FontMetrics fm,
                                int swatchX, int swatchW, int swatchH,
                                int centerY, int textGap,
                                Color fill, Color border, String text) {
        int swatchTop = centerY - swatchH / 2;
        g2.setColor(fill);
        g2.fillRoundRect(swatchX, swatchTop, swatchW, swatchH, 4, 4);
        g2.setColor(border);
        g2.drawRoundRect(swatchX, swatchTop, swatchW, swatchH, 4, 4);

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
        panel.setSelectedOrder(Arrays.asList(
                "MA140", "MA141", "CS150", "CS155", "CS200", "CS230",
                "CS250", "CS300", "CS301", "CS225",
                "CS340", "CS345", "CS350", "CS360", "CS390"), 0);

        JFrame frame = new JFrame("GraphPanel 测试");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 750);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}