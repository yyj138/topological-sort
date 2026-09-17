package topo.parser;

import topo.model.Edge;

import java.util.Collections;
import java.util.List;

// 解析结果（D 包桩，T-D1）
public class ParseResult {

    private final List<Edge> edges;
    private final List<ParseError> errors;
    private final boolean hasSelfLoop;
    private final int totalLines;

    public ParseResult(List<Edge> edges, List<ParseError> errors,
                       boolean hasSelfLoop, int totalLines) {
        this.edges = edges == null ? Collections.emptyList() : edges;
        this.errors = errors == null ? Collections.emptyList() : errors;
        this.hasSelfLoop = hasSelfLoop;
        this.totalLines = totalLines;
    }

    public List<Edge> getEdges() { return Collections.unmodifiableList(edges); }
    public List<ParseError> getErrors() { return Collections.unmodifiableList(errors); }
    public boolean isSuccess() { return errors.isEmpty(); }
    public boolean hasSelfLoop() { return hasSelfLoop; }
    public int getTotalLines() { return totalLines; }
    public int getEdgeCount() { return edges.size(); }
    public int getErrorCount() { return errors.size(); }
}
