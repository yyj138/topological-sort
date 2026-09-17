package topo.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// 图（A 包桩，T-A1）：邻接表 + 入边表，自动去重与自环标记
public class Graph {

    private final Map<String, Vertex> vertices = new LinkedHashMap<>();
    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Set<String> selfLoops = new LinkedHashSet<>();

    public void addVertex(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        name = name.trim();
        if (!vertices.containsKey(name)) {
            vertices.put(name, new Vertex(name));
        }
    }

    public void addEdge(String from, String to) {
        addVertex(from);
        addVertex(to);
        Edge edge = new Edge(from, to);
        if (edges.contains(edge)) return;
        edges.add(edge);
        if (edge.isSelfLoop()) selfLoops.add(from);
        vertices.get(from).addOutEdge(to);
        vertices.get(to).addInEdge(from);
    }

    public void removeEdge(String from, String to) {
        Edge edge = new Edge(from, to);
        if (!edges.remove(edge)) return;
        Vertex f = vertices.get(from);
        Vertex t = vertices.get(to);
        if (f != null) f.removeOutEdge(to);
        if (t != null) t.removeInEdge(from);
        if (edge.isSelfLoop()) selfLoops.remove(from);
    }

    public Vertex getVertex(String name) { return vertices.get(name); }
    public Collection<Vertex> getVertices() {
        return Collections.unmodifiableCollection(vertices.values());
    }
    public Set<String> getVertexNames() {
        return Collections.unmodifiableSet(vertices.keySet());
    }
    public Set<Edge> getEdges() { return Collections.unmodifiableSet(edges); }
    public int vertexCount() { return vertices.size(); }
    public int edgeCount() { return edges.size(); }
    public boolean hasSelfLoop() { return !selfLoops.isEmpty(); }
    public Set<String> getSelfLoops() {
        return Collections.unmodifiableSet(selfLoops);
    }
    public int getInDegree(String name) {
        Vertex v = vertices.get(name);
        return v == null ? 0 : v.inDegree();
    }
    public int getOutDegree(String name) {
        Vertex v = vertices.get(name);
        return v == null ? 0 : v.outDegree();
    }
    public void clear() {
        vertices.clear();
        edges.clear();
        selfLoops.clear();
    }
    public boolean isEmpty() { return vertices.isEmpty(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph{顶点=").append(vertexCount())
          .append(", 边=").append(edgeCount()).append("}\n");
        for (Edge e : edges) sb.append("  ").append(e).append("\n");
        return sb.toString();
    }
}
