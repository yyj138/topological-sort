package model;

import java.util.LinkedHashMap;
import java.util.List;

<<<<<<< HEAD
/**
 * 按录入顺序保存节点和直接有向关系的可变图，允许孤立节点和环。
 * 名称规则与 Vertex 相同，区分大小写。返回的列表都是只读快照。
 * 本类不保证线程安全；计算期间调用方应保证图稳定。
 *
 * @author A
 */
=======
>>>>>>> origin/dev-c
public final class Graph {
    private final LinkedHashMap<String, Vertex> vertices = new LinkedHashMap<>();
    private int edgeCount;

<<<<<<< HEAD
    /** 创建节点数和边数均为 0 的空图。 */
    public Graph() {
    }

    /**
     * 添加节点；重复添加不改变已有关系。
     * @param name 节点名称
     * @return 新增返回 true，已存在返回 false
     * @throws IllegalArgumentException 名称无效
     */
    public boolean addVertex(String name) {
        String normalized = Vertex.normalizeName(name);
        if (vertices.containsKey(normalized)) {
            return false;
        }
=======
    public Graph() {}

    public boolean addVertex(String name) {
        String normalized = Vertex.normalizeName(name);
        if (vertices.containsKey(normalized)) return false;
>>>>>>> origin/dev-c
        vertices.put(normalized, new Vertex(normalized));
        return true;
    }

<<<<<<< HEAD
    /**
     * 添加 from 指向 to 的边，按起点、终点顺序自动补齐缺失节点。
     * 重复边不计数，自环按一条真实关系保存。
     * @param from 起点名称
     * @param to 终点名称
     * @return 新增返回 true，边已存在返回 false
     * @throws IllegalArgumentException 任一名称无效，此时图保持不变
     */
    public boolean addEdge(String from, String to) {
        String sourceName = Vertex.normalizeName(from);
        String targetName = Vertex.normalizeName(to);
        // 两个参数都有效后才变更图，防止无效终点留下部分新增节点。
        Vertex source = vertices.computeIfAbsent(sourceName, Vertex::new);
        Vertex target = vertices.computeIfAbsent(targetName, Vertex::new);
        if (!source.addSuccessor(targetName)) {
            return false;
        }
=======
    public boolean addEdge(String from, String to) {
        String sourceName = Vertex.normalizeName(from);
        String targetName = Vertex.normalizeName(to);
        Vertex source = vertices.computeIfAbsent(sourceName, Vertex::new);
        Vertex target = vertices.computeIfAbsent(targetName, Vertex::new);
        if (!source.addSuccessor(targetName)) return false;
>>>>>>> origin/dev-c
        target.addPredecessor(sourceName);
        edgeCount++;
        return true;
    }

<<<<<<< HEAD
    /**
     * 删除指定关系，保留两端节点，包括删除后产生的孤立节点。
     * @param from 起点名称
     * @param to 终点名称
     * @return 已删除返回 true，两端存在但边不存在返回 false
     * @throws IllegalArgumentException 名称无效或任一端点不存在；图保持不变
     */
=======
>>>>>>> origin/dev-c
    public boolean removeEdge(String from, String to) {
        String sourceName = Vertex.normalizeName(from);
        String targetName = Vertex.normalizeName(to);
        Vertex source = requireVertex(sourceName);
        Vertex target = requireVertex(targetName);
<<<<<<< HEAD
        if (!source.removeSuccessor(targetName)) {
            return false;
        }
=======
        if (!source.removeSuccessor(targetName)) return false;
>>>>>>> origin/dev-c
        target.removePredecessor(sourceName);
        edgeCount--;
        return true;
    }

<<<<<<< HEAD
    /** @return 按节点首次录入顺序排列的只读快照，包含孤立节点 */
=======
>>>>>>> origin/dev-c
    public List<String> getVertexNames() {
        return List.copyOf(vertices.keySet());
    }

<<<<<<< HEAD
    /**
     * 读取直接后继，不添加间接可达关系。
     * @param name 节点名称
     * @return 按边录入顺序排列的后继名称只读快照
     * @throws IllegalArgumentException 名称无效或节点不存在
     */
=======
>>>>>>> origin/dev-c
    public List<String> getSuccessors(String name) {
        return requireVertex(Vertex.normalizeName(name)).getSuccessors();
    }

<<<<<<< HEAD
    /**
     * @param name 节点名称
     * @return 直接前驱数量，自环贡献 1
     * @throws IllegalArgumentException 名称无效或节点不存在
     */
=======
>>>>>>> origin/dev-c
    public int getInDegree(String name) {
        return requireVertex(Vertex.normalizeName(name)).getInDegree();
    }

<<<<<<< HEAD
    /**
     * @param name 节点名称
     * @return 直接后继数量，自环贡献 1
     * @throws IllegalArgumentException 名称无效或节点不存在
     */
=======
>>>>>>> origin/dev-c
    public int getOutDegree(String name) {
        return requireVertex(Vertex.normalizeName(name)).getOutDegree();
    }

<<<<<<< HEAD
    /** @return 节点数量，包含孤立节点 */
=======
>>>>>>> origin/dev-c
    public int getVertexCount() {
        return vertices.size();
    }

<<<<<<< HEAD
    /** @return 去重后的有向边数量，自环计一条 */
=======
>>>>>>> origin/dev-c
    public int getEdgeCount() {
        return edgeCount;
    }

    private Vertex requireVertex(String normalizedName) {
        Vertex vertex = vertices.get(normalizedName);
<<<<<<< HEAD
        if (vertex == null) {
            throw new IllegalArgumentException("节点不存在：" + normalizedName);
        }
        return vertex;
    }
}
=======
        if (vertex == null) throw new IllegalArgumentException("节点不存在：" + normalizedName);
        return vertex;
    }
}
>>>>>>> origin/dev-c
