package model;

import java.util.Objects;

public final class Edge {
    private final String from;
    private final String to;

    public Edge(String from, String to) {
        this.from = Vertex.normalizeName(from);
        this.to = Vertex.normalizeName(to);
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Edge edge)) return false;
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() { return Objects.hash(from, to); }
}