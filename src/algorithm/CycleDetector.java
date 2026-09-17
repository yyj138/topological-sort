package algorithm;

import model.Graph;
import model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 环检测（A 包桩，T-A4）：按契约 §五
// findCycle 返回空列表表示无环；有环返回首尾相同的闭合序列如 [A,B,C,A]
// 自环返回 [A,A]
public class CycleDetector {

    public static List<String> findCycle(Graph graph) {
        if (graph == null || graph.isEmpty()) return Collections.emptyList();

        // 先用 Kahn 计数法快速判定
        TopoResult kahn = TopologicalSolver.kahnSort(graph);
        if (!kahn.hasCycle()) return Collections.emptyList();

        // DFS 三色标记定位环路径：0=未访问, 1=访问中, 2=已完成
        Map<String, Integer> color = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (Vertex v : graph.getVertices()) color.put(v.getName(), 0);

        for (Vertex v : graph.getVertices()) {
            if (color.get(v.getName()) == 0) {
                List<String> cycle = dfsVisit(graph, v.getName(), color, parent, null);
                if (cycle != null) return cycle;
            }
        }
        return Collections.emptyList();
    }

    private static List<String> dfsVisit(Graph graph, String u,
                                         Map<String, Integer> color,
                                         Map<String, String> parent,
                                         String fromParent) {
        color.put(u, 1);
        Vertex v = graph.getVertex(u);
        if (v != null) {
            for (String next : v.getSuccessors()) {
                int c = color.getOrDefault(next, 0);
                if (c == 0) {
                    parent.put(next, u);
                    List<String> cycle = dfsVisit(graph, next, color, parent, u);
                    if (cycle != null) return cycle;
                } else if (c == 1 && !next.equals(fromParent)) {
                    return buildCyclePath(parent, next, u);
                }
            }
        }
        color.put(u, 2);
        return null;
    }

    // 从 parent 链重构环路径：start -> ... -> end -> start
    private static List<String> buildCyclePath(Map<String, String> parent,
                                                String start, String end) {
        java.util.LinkedList<String> path = new java.util.LinkedList<>();
        path.addFirst(end);
        String cur = end;
        Set<String> seen = new HashSet<>();
        while (!cur.equals(start) && parent.containsKey(cur) && !seen.contains(cur)) {
            seen.add(cur);
            cur = parent.get(cur);
            path.addFirst(cur);
        }
        path.addLast(start);
        return new ArrayList<>(path);
    }
}
