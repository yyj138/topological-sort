package algorithm;

import model.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 环检测（A 包 T-A4）：按契约 V1.0
// findCycle 返回空列表表示无环；有环返回首尾相同的闭合序列如 [A,B,C,A]
// 自环返回 [A,A]；两节点互指返回 [A,B,A]
public class CycleDetector {

    public static List<String> findCycle(Graph graph) {
        if (graph == null || graph.getVertexCount() == 0) {
            return Collections.emptyList();
        }

        // Kahn 计数法快速判定
        TopoResult kahn = TopologicalSolver.kahnSort(graph);
        if (!kahn.hasCycle()) return Collections.emptyList();

        List<String> names = graph.getVertexNames();

        // DFS 三色标记：0=未访问, 1=访问中, 2=已完成
        Map<String, Integer> color = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (String name : names) color.put(name, 0);

        for (String name : names) {
            if (color.get(name) == 0) {
                List<String> cycle = dfsVisit(graph, name, color, parent);
                if (cycle != null) return cycle;
            }
        }
        return Collections.emptyList();
    }

    private static List<String> dfsVisit(Graph graph, String u,
                                         Map<String, Integer> color,
                                         Map<String, String> parent) {
        color.put(u, 1);
        for (String next : graph.getSuccessors(u)) {
            int c = color.getOrDefault(next, 0);
            if (c == 0) {
                parent.put(next, u);
                List<String> cycle = dfsVisit(graph, next, color, parent);
                if (cycle != null) return cycle;
            } else if (c == 1) {
                // 指向访问中节点：回边，有向图中即真实环（含自环与两节点互指）
                return buildCyclePath(parent, next, u);
            }
        }
        color.put(u, 2);
        return null;
    }

    // 从 parent 链重构环路径：start -> ... -> end -> start
    private static List<String> buildCyclePath(Map<String, String> parent,
                                               String start, String end) {
        LinkedList<String> path = new LinkedList<>();
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
