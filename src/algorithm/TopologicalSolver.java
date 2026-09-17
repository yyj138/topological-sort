package algorithm;

import model.Graph;
import model.Vertex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// Kahn 拓扑排序（A 包桩，T-A2）：O(V+E)，按契约 §三
// 无环返回完整序列；含环 hasCycle=true 且 order 为空
public class TopologicalSolver {

    public static TopoResult kahnSort(Graph graph) {
        if (graph == null || graph.isEmpty()) {
            return new TopoResult(new ArrayList<>(), false);
        }

        java.util.Map<String, Integer> inDegree = new java.util.LinkedHashMap<>();
        for (Vertex v : graph.getVertices()) {
            inDegree.put(v.getName(), v.inDegree());
        }

        Queue<String> queue = new LinkedList<>();
        for (java.util.Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            result.add(cur);
            Vertex v = graph.getVertex(cur);
            if (v == null) continue;
            for (String next : v.getSuccessors()) {
                int d = inDegree.get(next) - 1;
                inDegree.put(next, d);
                if (d == 0) queue.add(next);
            }
        }

        if (result.size() == graph.getVertexCount()) {
            return new TopoResult(result, false);
        }
        // 含环：按契约返回空列表 + hasCycle=true
        return new TopoResult(new ArrayList<>(), true);
    }
}
