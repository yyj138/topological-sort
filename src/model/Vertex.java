package model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// 顶点（A 包桩，T-A1）：名称唯一标识，维护入边/出边邻接
public class Vertex {

    private final String name;
    private final Set<String> successors = new LinkedHashSet<>();
    private final Set<String> predecessors = new LinkedHashSet<>();

    public Vertex(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        this.name = name.trim();
    }

    public String getName() { return name; }

    public boolean addSuccessor(String to) { return successors.add(to); }
    public boolean addPredecessor(String from) { return predecessors.add(from); }
    public boolean removeSuccessor(String to) { return successors.remove(to); }
    public boolean removePredecessor(String from) { return predecessors.remove(from); }

    public Set<String> getSuccessors() { return Collections.unmodifiableSet(successors); }
    public Set<String> getPredecessors() { return Collections.unmodifiableSet(predecessors); }
    public int inDegree() { return predecessors.size(); }
    public int outDegree() { return successors.size(); }

    @Override
    public String toString() { return name; }
}
