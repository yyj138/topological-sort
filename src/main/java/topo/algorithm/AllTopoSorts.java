package topo.algorithm;

import topo.model.Graph;
import topo.model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 全拓扑枚举（A 包桩，T-A3）：DFS 回溯，默认上限 1000，0 表示全部
public class AllTopoSorts {

    private final Graph graph;
    private final int maxResults;
    private final List<List<String>> results = new ArrayList<>();
    private boolean computed = false;

    public AllTopoSorts(Graph graph) { this(graph, 1000); }

    public AllTopoSorts(Graph graph, int maxResults) {
        this.graph = graph;
        this.maxResults = maxResults;
    }

    public List<List<String>> compute() {
        if (computed) return Collections.unmodifiableList(results);
        if (graph == null || graph.isEmpty()) {
            computed = true;
            return results;
        }
        java.util.Map<String, Integer> inDegree = new java.util.LinkedHashMap<>();
        for (Vertex v : graph.getVertices()) {
            inDegree.put(v.getName(), v.inDegree());
        }
        backtrack(inDegree, new LinkedHashSet<>(), new ArrayList<>());
        computed = true;
        return Collections.unmodifiableList(results);
    }

    private void backtrack(java.util.Map<String, Integer> inDegree,
                           Set<String> visited, List<String> current) {
        if (maxResults > 0 && results.size() >= maxResults) return;
        if (current.size() == graph.vertexCount()) {
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
                for (String next : v.getOutEdges()) {
                    inDegree.put(next, inDegree.get(next) - 1);
                    decremented.add(next);
                }
            }
            backtrack(inDegree, visited, current);
            for (String next : decremented) {
                inDegree.put(next, inDegree.get(next) + 1);
            }
            visited.remove(pick);
            current.remove(current.size() - 1);
        }
    }

    public int getTotalCount() { return results.size(); }
    public boolean isTruncated() { return maxResults > 0 && results.size() >= maxResults; }
    public int getMaxResults() { return maxResults; }
}
