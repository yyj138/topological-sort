package topo.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// 顶点（A 包桩，T-A1）
public class Vertex {

    private final String name;
    private final Set<String> outEdges = new LinkedHashSet<>();
    private final Set<String> inEdges = new LinkedHashSet<>();

    public Vertex(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public Set<String> getOutEdges() {
        return Collections.unmodifiableSet(outEdges);
    }

    public Set<String> getInEdges() {
        return Collections.unmodifiableSet(inEdges);
    }

    public void addOutEdge(String to) {
        outEdges.add(to);
    }

    public void addInEdge(String from) {
        inEdges.add(from);
    }

    public void removeOutEdge(String to) {
        outEdges.remove(to);
    }

    public void removeInEdge(String from) {
        inEdges.remove(from);
    }

    public int outDegree() {
        return outEdges.size();
    }

    public int inDegree() {
        return inEdges.size();
    }

    @Override
    public String toString() {
        return name + "(in=" + inEdges + ", out=" + outEdges + ")";
    }
}
