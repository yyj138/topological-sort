package model;

import java.util.LinkedHashSet;
import java.util.List;

public final class Vertex {
    private final String name;
    private final LinkedHashSet<String> successors = new LinkedHashSet<>();
    private final LinkedHashSet<String> predecessors = new LinkedHashSet<>();

    public Vertex(String name) {
        this.name = normalizeName(name);
    }

    public String getName() { return name; }
    public List<String> getSuccessors() { return List.copyOf(successors); }
    public int getInDegree() { return predecessors.size(); }
    public int getOutDegree() { return successors.size(); }

    boolean addSuccessor(String name) { return successors.add(name); }
    void addPredecessor(String name) { predecessors.add(name); }
    boolean removeSuccessor(String name) { return successors.remove(name); }
    void removePredecessor(String name) { predecessors.remove(name); }

    static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException("节点名称不能为 null");
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if ("<>,＜＞".indexOf(ch) >= 0 || ch == '\n' || ch == '\r'
                    || ch == '\u0085' || ch == '\u2028' || ch == '\u2029') {
                throw new IllegalArgumentException("节点名称不能包含格式分隔符或换行");
            }
        }
        String normalized = name.strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException("节点名称不能为空或仅含空白");
        return normalized;
    }
}