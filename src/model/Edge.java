package model;

import java.util.Objects;

// 有向边（A 包桩，T-A1）：起点 -> 终点
public class Edge {

    private final String from;
    private final String to;

    public Edge(String from, String to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("边的端点不能为空");
        }
        this.from = from.trim();
        this.to = to.trim();
        if (this.from.isEmpty() || this.to.isEmpty()) {
            throw new IllegalArgumentException("边的端点不能为空字符串");
        }
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public boolean isSelfLoop() { return from.equals(to); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() { return Objects.hash(from, to); }

    @Override
    public String toString() { return from + " -> " + to; }
}
