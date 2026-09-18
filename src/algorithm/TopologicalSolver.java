package algorithm;

import model.Graph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// Kahn 拓扑排序（A 包 T-A2）：O(V+E)，按契约 V1.0
// 只通过 Graph 公开方法读图，入度使用局部副本，不改变原图
public class TopologicalSolver {

    public static TopoResult kahnSort(Graph graph) {
        if (graph == null || graph.getVertexCount() == 0) {
            return new TopoResult(new ArrayList<>(), false);
        }

        List<String> names = graph.getVertexNames();

        // 局部入度表
        java.util.Map<String, Integer> inDegree = new java.util.LinkedHashMap<>();
        Queue<String> queue = new LinkedList<>();
        for (String name : names) {
            int d = graph.getInDegree(name);
            inDegree.put(name, d);
            if (d == 0) queue.add(name);
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            result.add(cur);
            for (String next : graph.getSuccessors(cur)) {
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
