package io;

import model.Graph;
import java.util.List;

/**
 * DataParser 的解析结果，包含解析建立的图、错误列表和警告列表。
 * <p>
 * 有错误时 B 和 E 不应启动计算；警告不影响图的使用，但应提示用户。
 * </p>
 */
public class ParseResult {

    private final Graph graph;
    private final List<ParseIssue> errors;
    private final List<ParseIssue> warnings;

    /**
     * @param graph    解析建立的图，即使有错误也返回已建立的部分图
     * @param errors   解析错误列表
     * @param warnings 解析警告列表
     */
    public ParseResult(Graph graph, List<ParseIssue> errors, List<ParseIssue> warnings) {
        this.graph = graph;
        this.errors = errors;
        this.warnings = warnings;
    }

    /**
     * @return 解析建立的图
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @return 解析错误列表，错误行的关系未加入图
     */
    public List<ParseIssue> getErrors() {
        return errors;
    }

    /**
     * @return 解析警告列表，如重复关系
     */
    public List<ParseIssue> getWarnings() {
        return warnings;
    }

    /**
     * @return 是否存在解析错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}