package view;

import model.Graph;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * 布局管理器（组员 C）
 * - 分层布局（T-C2）：按拓扑层分配纵向位置，同层横向分布
 *   · 重心法排序同层节点，减少边交叉
 *   · 用节点实际宽度 + 最小间距定位，100 节点不重叠
 *   · 按画布尺寸自适应
 * - 环形布局（T-C3 兜底）：所有节点均匀分布在圆周上
 */
public class LayoutManager {

    public static final int LAYOUT_LAYERED  = 0;
    public static final int LAYOUT_CIRCULAR = 1;

    private static final int H_GAP    = 30;  // 同层最小横向间距
    private static final int V_PADDING = 50;  // 上下留白
    private static final int H_PADDING = 40;  // 左右留白

    private int mode = LAYOUT_LAYERED;

    public void setMode(int mode) { this.mode = mode; }
    public int  getMode()         { return mode; }

    public Map<String, Point2D.Double> computeLayout(Graph graph, int w, int h, int nodeRadius) {
        if (mode == LAYOUT_CIRCULAR) {
            return circularLayout(graph, w, h, nodeRadius);
        }
        return layeredLayout(graph, w, h, nodeRadius);
    }

    // ---------- 环形布局 ----------
    private Map<String, Point2D.Double> circularLayout(Graph graph, int w, int h, int nodeRadius) {
        Map<String, Point2D.Double> result = new LinkedHashMap<>();
        List<String> names = graph.getVertexNames();
        int n = names.size();
        if (n == 0) return result;

        double cx = w / 2.0, cy = h / 2.0;
        double radius = Math.max(Math.min(w, h) / 2.0 - nodeRadius - 40, 80);

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            result.put(names.get(i),
                    new Point2D.Double(cx + radius * Math.cos(angle),
                                       cy + radius * Math.sin(angle)));
        }
        return result;
    }

    // ---------- 分层布局（T-C2 核心） ----------
    private Map<String, Point2D.Double> layeredLayout(Graph graph, int w, int h, int nodeRadius) {
        Map<String, Point2D.Double> result = new LinkedHashMap<>();
        List<String> names = graph.getVertexNames();
        if (names.isEmpty()) return result;

        // 1) 计算层号
        Map<String, Integer> layerMap = computeLayer(graph);

        // 2) 按层号归组
        TreeMap<Integer, List<String>> byLayer = new TreeMap<>();
        for (String name : names) {
            int lv = layerMap.getOrDefault(name, 0);
            byLayer.computeIfAbsent(lv, k -> new ArrayList<>()).add(name);
        }

        int maxLayer   = byLayer.lastKey();
        int layerCount = maxLayer + 1;

        // 3) 纵向位置
        double usableH = Math.max(50, h - 2.0 * V_PADDING);
        double yStep   = (layerCount > 1) ? usableH / (layerCount - 1) : 0;
        double usableW = Math.max(100, w - 2.0 * H_PADDING);

        // 4) 逐层计算横向位置（重心排序 -> 减少边交叉）
        Map<String, Double> xPos = new HashMap<>();

        for (Map.Entry<Integer, List<String>> entry : byLayer.entrySet()) {
            int lv = entry.getKey();
            List<String> layerNodes = entry.getValue();
            int count = layerNodes.size();

            // 排序：第 0 层按名称；其余层按前驱平均 x（重心法）
            if (lv == 0) {
                layerNodes.sort(Comparator.naturalOrder());
            } else {
                layerNodes.sort(Comparator.comparingDouble(
                        name -> avgPredecessorX(name, graph, xPos)));
            }

            // 节点宽度 + 最小间距
            int[] widths = new int[count];
            double totalMinW = 0;
            for (int i = 0; i < count; i++) {
                widths[i] = GraphPanel.estimateNodeWidth(layerNodes.get(i));
                totalMinW += widths[i];
            }
            double minTotal = totalMinW + Math.max(0, count - 1) * H_GAP;
            double extraSpace = Math.max(0, usableW - minTotal);
            double gapEach    = H_GAP + (count > 1 ? extraSpace / (count - 1) : 0);

            double y = (layerCount == 1) ? h / 2.0 : V_PADDING + lv * yStep;

            // 单节点：水平居中
            if (count == 1) {
                double cx = w / 2.0;
                result.put(layerNodes.get(0), new Point2D.Double(cx, y));
                xPos.put(layerNodes.get(0), cx);
                continue;
            }

            double cursorX = H_PADDING;
            for (int i = 0; i < count; i++) {
                double cx = cursorX + widths[i] / 2.0;
                result.put(layerNodes.get(i), new Point2D.Double(cx, y));
                xPos.put(layerNodes.get(i), cx);
                cursorX += widths[i] + gapEach;
            }
        }
        return result;
    }

    /** 所有前驱节点的平均 x 坐标（重心法） */
    private double avgPredecessorX(String node, Graph graph, Map<String, Double> xPos) {
        double sum = 0;
        int cnt = 0;
        for (String other : graph.getVertexNames()) {
            if (graph.getSuccessors(other).contains(node)) {
                Double ox = xPos.get(other);
                if (ox != null) { sum += ox; cnt++; }
            }
        }
        return cnt > 0 ? sum / cnt : Double.MAX_VALUE / 2;
    }

    /** Kahn 变体：计算每个节点的拓扑层号；含环节点放到最末层兜底 */
    private Map<String, Integer> computeLayer(Graph graph) {
        Map<String, Integer> layer = new HashMap<>();
        Map<String, Integer> inDeg = new HashMap<>();
        List<String> names = graph.getVertexNames();

        for (String name : names) {
            inDeg.put(name, graph.getInDegree(name));
            layer.put(name, 0);
        }

        Queue<String> queue = new LinkedList<>();
        for (String name : names) {
            if (inDeg.get(name) == 0) queue.offer(name);
        }

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int curLayer = layer.get(cur);
            for (String next : graph.getSuccessors(cur)) {
                if (layer.get(next) < curLayer + 1) {
                    layer.put(next, curLayer + 1);
                }
                inDeg.put(next, inDeg.get(next) - 1);
                if (inDeg.get(next) == 0) queue.offer(next);
            }
        }

        // 含环兜底：仍有入度的节点放到最末层
        int maxLayer = 0;
        for (int lv : layer.values()) maxLayer = Math.max(maxLayer, lv);
        int fallbackLayer = maxLayer + 1;
        for (String name : names) {
            if (inDeg.get(name) > 0) layer.put(name, fallbackLayer);
        }
        return layer;
    }
}