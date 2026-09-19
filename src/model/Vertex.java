package model;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 一个名称固定的顶点；邻接关系仅由 model 包中的 Graph 维护。
 *
 * @author A
 */
public final class Vertex {
    private final String name;
    private final LinkedHashSet<String> successors = new LinkedHashSet<>();
    private final LinkedHashSet<String> predecessors = new LinkedHashSet<>();

    /**
     * 创建没有邻接关系的顶点。
     * @param name 节点名称，使用 String.strip() 去除首尾空白，保留内部空格
     * @throws IllegalArgumentException 名称为空引用、空白或含格式分隔符、换行
     */
    public Vertex(String name) {
        this.name = normalizeName(name);
    }

    /** @return 不可变的节点名称 */
    public String getName() {
        return name;
    }

    /** @return 按录入顺序排列的直接后继名称的只读快照 */
    public List<String> getSuccessors() {
        return List.copyOf(successors);
    }

    /** @return 直接前驱数量 */
    public int getInDegree() {
        return predecessors.size();
    }

    /** @return 直接后继数量 */
    public int getOutDegree() {
        return successors.size();
    }

    boolean addSuccessor(String name) {
        return successors.add(name);
    }

    void addPredecessor(String name) {
        predecessors.add(name);
    }

    boolean removeSuccessor(String name) {
        return successors.remove(name);
    }

    void removePredecessor(String name) {
        predecessors.remove(name);
    }

    // Graph、Vertex 和 Edge 共用名称规则；先拒绝换行，再去除首尾空白。
    static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("节点名称不能为 null");
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if ("<>,＜＞".indexOf(ch) >= 0 || ch == '\n' || ch == '\r'
                    || ch == '\u0085' || ch == '\u2028' || ch == '\u2029') {
                throw new IllegalArgumentException("节点名称不能包含格式分隔符或换行");
            }
        }
        String normalized = name.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("节点名称不能为空或仅含空白");
        }
        return normalized;
    }
}
