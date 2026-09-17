package io;

import model.Graph;

import java.util.Collections;
import java.util.List;

// 解析结果（契约 §六）：直接返回 Graph + 错误清单 + 警告清单
// 有错误时 B 不启动计算，不把部分解析结果当完整输入
public class ParseResult {

    private final Graph graph;
    private final List<ParseIssue> errors;
    private final List<ParseIssue> warnings;

    public ParseResult(Graph graph, List<ParseIssue> errors, List<ParseIssue> warnings) {
        this.graph = graph;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
    }

    public Graph getGraph() { return graph; }
    public List<ParseIssue> getErrors() { return errors; }
    public List<ParseIssue> getWarnings() { return warnings; }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public int getErrorCount() { return errors.size(); }
    public int getWarningCount() { return warnings.size(); }
}
