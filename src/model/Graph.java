package model;

import java.util.LinkedHashMap;
import java.util.List;

public final class Graph {
    private final LinkedHashMap<String, Vertex> vertices = new LinkedHashMap<>();
    private int edgeCount;

    public Graph() {}

    public boolean addVertex(String name) {
        String normalized = Vertex.normalizeName(name);
        if (vertices.containsKey(normalized)) return false;
        vertices.put(normalized, new Vertex(normalized));
        return true;
    }

    public boolean addEdge(String from, String to) {
        String sourceName = Vertex.normalizeName(from);
        String targetName = Vertex.normalizeName(to);
        Vertex source = vertices.computeIfAbsent(sourceName, Vertex::new);
        Vertex target = vertices.computeIfAbsent(targetName, Vertex::new);
        if (!source.addSuccessor(targetName)) return false;
        target.addPredecessor(sourceName);
        edgeCount++;
        return true;
    }

    public boolean removeEdge(String from, String to) {
        String sourceName = Vertex.normalizeName(from);
        String targetName = Vertex.normalizeName(to);
        Vertex source = requireVertex(sourceName);
        Vertex target = requireVertex(targetName);
        if (!source.removeSuccessor(targetName)) return false;
        target.removePredecessor(sourceName);
        edgeCount--;
        return true;
    }

    public List<String> getVertexNames() {
        return List.copyOf(vertices.keySet());
    }

    public List<String> getSuccessors(String name) {
        return requireVertex(Vertex.normalizeName(name)).getSuccessors();
    }

    public int getInDegree(String name) {
        return requireVertex(Vertex.normalizeName(name)).getInDegree();
    }

    public int getOutDegree(String name) {
        return requireVertex(Vertex.normalizeName(name)).getOutDegree();
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    private Vertex requireVertex(String normalizedName) {
        Vertex vertex = vertices.get(normalizedName);
        if (vertex == null) throw new IllegalArgumentException("节点不存在：" + normalizedName);
        return vertex;
    }
}