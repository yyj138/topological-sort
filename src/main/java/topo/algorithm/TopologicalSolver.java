package topo.algorithm;

import topo.model.Graph;
import topo.model.Vertex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// Kahn 拓扑排序（A 包桩，T-A2）：O(V+E)，含环时返回已排出的部分序列
public class TopologicalSolver {

    public static List<String> kahnSort(Graph graph) {
        List<String> result = new ArrayList<>();
        if (graph == null || graph.isEmpty()) return result;

        java.util.Map<String, Integer> inDegree = new java.util.LinkedHashMap<>();
        for (Vertex v : graph.getVertices()) {
            inDegree.put(v.getName(), v.inDegree());
        }

        Queue<String> queue = new LinkedList<>();
        for (java.util.Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            result.add(cur);
            Vertex v = graph.getVertex(cur);
            if (v == null) continue;
            for (String next : v.getOutEdges()) {
                int d = inDegree.get(next) - 1;
                inDegree.put(next, d);
                if (d == 0) queue.add(next);
            }
        }
        return result;
    }

    public static boolean isCompleteSort(Graph graph, List<String> sorted) {
        return graph != null && sorted.size() == graph.vertexCount();
    }
}
