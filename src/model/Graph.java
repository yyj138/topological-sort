package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

// 图（A 包 T-A1）：邻接表，按接口契约 V1.0 与图数据结构设计实现
// 不维护独立 Edge 列表，遍历节点后继即可得全部边；边数单独计数
public class Graph {

    private final LinkedHashMap<String, Vertex> vertices = new LinkedHashMap<>();
    private int edgeCount = 0;

    // 新顶点返回 true；已存在返回 false
    public boolean addVertex(String name) {
        String normalized = validateName(name);
        if (vertices.containsKey(normalized)) return false;
        vertices.put(normalized, new Vertex(normalized));
        return true;
    }

    // 新边返回 true；重复边返回 false 且不重复增加入度
    // 两个名称先验证，再变更图
    public boolean addEdge(String from, String to) {
        String f = validateName(from);
        String t = validateName(to);

        Vertex vf = vertices.get(f);
        if (vf == null) {
            vf = new Vertex(f);
            vertices.put(f, vf);
        }
        Vertex vt = vertices.get(t);
        if (vt == null) {
            vt = new Vertex(t);
            vertices.put(t, vt);
        }

        // Set.add 去重：重复后继即重复边
        if (!vf.addSuccessor(t)) return false;
        vt.addPredecessor(f);
        edgeCount++;
        return true;
    }

    // 删除已有边返回 true；关系不存在返回 false；节点保留
    public boolean removeEdge(String from, String to) {
        String f = validateName(from);
        String t = validateName(to);

        Vertex vf = vertices.get(f);
        Vertex vt = vertices.get(t);
        if (vf == null || vt == null) return false;
        if (!vf.removeSuccessor(t)) return false;
        vt.removePredecessor(f);
        edgeCount--;
        return true;
    }

    // 全部节点名称（独立副本，按录入顺序）
    public List<String> getVertexNames() {
        return new ArrayList<>(vertices.keySet());
    }

    // 直接后继名称（独立副本）
    public List<String> getSuccessors(String name) {
        Vertex v = vertices.get(validateName(name));
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return new ArrayList<>(v.getSuccessors());
    }

    public int getInDegree(String name) {
        Vertex v = vertices.get(validateName(name));
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return v.inDegree();
    }

    public int getOutDegree(String name) {
        Vertex v = vertices.get(validateName(name));
        if (v == null) {
            throw new IllegalArgumentException("未知节点: " + name);
        }
        return v.outDegree();
    }

    public int getVertexCount() { return vertices.size(); }
    public int getEdgeCount() { return edgeCount; }

    // 名称校验：trim 后保留内部空格，区分大小写；拒绝格式分隔符与控制字符
    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        String n = name.trim();
        if (n.isEmpty()) {
            throw new IllegalArgumentException("顶点名不能为空");
        }
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == '<' || c == '>' || c == ','
                    || c == '\uFF1C' || c == '\uFF1E' || c == '\uFF0C'
                    || Character.isISOControl(c)) {
                throw new IllegalArgumentException("顶点名含非法字符: " + c);
            }
        }
        return n;
    }

    @Override
    public String toString() {
        return "Graph{顶点=" + getVertexCount() + ", 边=" + getEdgeCount() + "}";
    }
}
