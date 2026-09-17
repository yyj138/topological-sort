package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 图（A 包桩，T-A1）：邻接表，按契约 §二
// addEdge 自动补齐节点、去重（重复返回 false），自环作为边保留
public class Graph {

    private final Map<String, Vertex> vertices = new LinkedHashMap<>();
    private final Set<Edge> edges = new LinkedHashSet<>();

    // 新顶点返回 true；已存在返回 false
    public boolean addVertex(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        name = name.trim();
        if (vertices.containsKey(name)) return false;
        vertices.put(name, new Vertex(name));
        return true;
    }

    // 新边返回 true；重复边返回 false 且不重复增加入度
    public boolean addEdge(String from, String to) {
        addVertex(from);
        addVertex(to);
        Edge edge = new Edge(from, to);
        if (!edges.add(edge)) return false;
        vertices.get(from).addSuccessor(to);
        vertices.get(to).addPredecessor(from);
        return true;
    }

    public boolean removeEdge(String from, String to) {
        Edge edge = new Edge(from, to);
        if (!edges.remove(edge)) return false;
        Vertex f = vertices.get(from);
        Vertex t = vertices.get(to);
        if (f != null) f.removeSuccessor(to);
        if (t != null) t.removePredecessor(from);
        return true;
    }

    public List<String> getVertexNames() {
        return new ArrayList<>(vertices.keySet());
    }

    // 后继列表（只读副本）
    public List<String> getSuccessors(String name) {
        Vertex v = vertices.get(name);
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return new ArrayList<>(v.getSuccessors());
    }

    public int getInDegree(String name) {
        Vertex v = vertices.get(name);
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return v.inDegree();
    }

    public int getOutDegree(String name) {
        Vertex v = vertices.get(name);
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return v.outDegree();
    }

    public int getVertexCount() { return vertices.size(); }
    public int getEdgeCount() { return edges.size(); }
    public boolean isEmpty() { return vertices.isEmpty(); }

    // 以下为桩内部使用，不在跨模块契约中，供算法层遍历
    public Vertex getVertex(String name) { return vertices.get(name); }
    public java.util.Collection<Vertex> getVertices() {
        return Collections.unmodifiableCollection(vertices.values());
    }
    public Set<Edge> getEdges() { return Collections.unmodifiableSet(edges); }

    @Override
    public String toString() {
        return "Graph{顶点=" + getVertexCount() + ", 边=" + getEdgeCount() + "}";
    }
}
