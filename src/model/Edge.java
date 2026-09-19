package model;

import java.util.Objects;

/**
 * 不可变的有向边，两个端点名称及方向共同决定边是否相同。
 *
 * @author A
 */
public final class Edge {
    private final String from;
    private final String to;

    /**
     * 创建边对象，不向任何 Graph 添加关系。
     * @param from 起点名称，处理规则与 Vertex 相同
     * @param to 终点名称，处理规则与 Vertex 相同
     * @throws IllegalArgumentException 任一端点名称无效
     */
    public Edge(String from, String to) {
        this.from = Vertex.normalizeName(from);
        this.to = Vertex.normalizeName(to);
    }

    /** @return 起点名称 */
    public String getFrom() {
        return from;
    }

    /** @return 终点名称 */
    public String getTo() {
        return to;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Edge edge)) {
            return false;
        }
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
