package algorithm;

import model.Graph;
import model.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

// 全拓扑枚举（A 包桩，T-A3）：DFS 回溯，按契约 §四
// enumerate 在展开搜索前拒绝含环图（返回 StopReason.CYCLE）
// 回溯必须恢复入度和候选状态，不污染原图
public class AllTopoSorts {

    public static EnumerationResult enumerate(Graph graph, int maxResults,
                                               long timeoutMillis, BooleanSupplier cancelled) {
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults 不能为负数");
        }
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis 不能为负数");
        }
        if (graph == null || graph.isEmpty()) {
            // 空图：约定有一个空序列
            List<List<String>> empty = new ArrayList<>();
            empty.add(new ArrayList<>());
            return new EnumerationResult(empty, true, StopReason.COMPLETED);
        }

        // 先判环：含环不进入枚举
        List<String> cycle = CycleDetector.findCycle(graph);
        if (!cycle.isEmpty()) {
            return new EnumerationResult(new ArrayList<>(), false, StopReason.CYCLE);
        }

        java.util.Map<String, Integer> inDegree = new java.util.LinkedHashMap<>();
        for (Vertex v : graph.getVertices()) {
            inDegree.put(v.getName(), v.inDegree());
        }

        List<List<String>> results = new ArrayList<>();
        long deadline = timeoutMillis > 0 ? System.currentTimeMillis() + timeoutMillis : 0;
        backtrack(graph, inDegree, new LinkedHashSet<>(), new ArrayList<>(),
                results, maxResults, deadline, cancelled);

        StopReason reason;
        boolean complete;
        if (maxResults > 0 && results.size() >= maxResults) {
            reason = StopReason.LIMIT_REACHED;
            complete = false;
        } else if (cancelled.getAsBoolean()) {
            reason = StopReason.CANCELLED;
            complete = false;
        } else if (deadline > 0 && System.currentTimeMillis() >= deadline) {
            reason = StopReason.TIMEOUT;
            complete = false;
        } else {
            reason = StopReason.COMPLETED;
            complete = true;
        }
        return new EnumerationResult(results, complete, reason);
    }

    private static void backtrack(Graph graph, java.util.Map<String, Integer> inDegree,
                                  Set<String> visited, List<String> current,
                                  List<List<String>> results,
                                  int maxResults, long deadline, BooleanSupplier cancelled) {
        if (maxResults > 0 && results.size() >= maxResults) return;
        if (cancelled.getAsBoolean()) return;
        if (deadline > 0 && System.currentTimeMillis() >= deadline) return;

        if (current.size() == graph.getVertexCount()) {
            results.add(new ArrayList<>(current));
            return;
        }

        List<String> candidates = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0 && !visited.contains(e.getKey())) {
                candidates.add(e.getKey());
            }
        }

        for (String pick : candidates) {
            current.add(pick);
            visited.add(pick);
            Vertex v = graph.getVertex(pick);
            List<String> decremented = new ArrayList<>();
            if (v != null) {
                for (String next : v.getSuccessors()) {
                    inDegree.put(next, inDegree.get(next) - 1);
                    decremented.add(next);
                }
            }
            backtrack(graph, inDegree, visited, current, results, maxResults, deadline, cancelled);
            for (String next : decremented) {
                inDegree.put(next, inDegree.get(next) + 1);
            }
            visited.remove(pick);
            current.remove(current.size() - 1);
        }
    }
}
