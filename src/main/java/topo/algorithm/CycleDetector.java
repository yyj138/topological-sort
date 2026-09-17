package topo.algorithm;

import topo.model.Graph;
import topo.model.Vertex;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 环检测（A 包桩，T-A4）：Kahn 计数法 + DFS 三色标记定位环路径
public class CycleDetector {

    public static boolean hasCycle(Graph graph) {
        return detect(graph).hasCycle();
    }

    public static CycleResult detect(Graph graph) {
        if (graph == null || graph.isEmpty()) return CycleResult.noCycle();
        boolean kahnHasCycle = TopologicalSolver.kahnSort(graph).size() < graph.vertexCount();
        if (!kahnHasCycle) return CycleResult.noCycle();
        return new CycleResult(true, findCycleByDfs(graph));
    }

    // DFS 三色标记：0=未访问, 1=访问中, 2=已完成
    private static List<String> findCycleByDfs(Graph graph) {
        java.util.Map<String, Integer> color = new java.util.HashMap<>();
        java.util.Map<String, String> parent = new java.util.HashMap<>();
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
                                         java.util.Map<String, Integer> color,
                                         java.util.Map<String, String> parent,
                                         String fromParent) {
        color.put(u, 1);
        Vertex v = graph.getVertex(u);
        if (v != null) {
            for (String next : v.getOutEdges()) {
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

    private static List<String> buildCyclePath(java.util.Map<String, String> parent,
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
        return path;
    }
}
