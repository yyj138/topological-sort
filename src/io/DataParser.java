package io;

import model.Graph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 解析 {@code <from,to>} 格式的文本，直接通过 Graph 方法建立图。
 * <p>
 * 功能说明：
 * <ul>
 * <li>跳过空行和以 {@code #} 开头的注释行</li>
 * <li>容忍行首尾空格，兼容全角尖括号和全角逗号</li>
 * <li>自动去除重复边并记录警告</li>
 * <li>自环保留在图中，由环检测模块统一提示</li>
 * <li>解析失败记录行号与原因，返回结构化错误列表</li>
 * </ul>
 * </p>
 */
public class DataParser {

    /**
     * 解析文本内容，构建图并返回解析结果。
     * <p>
     * 每行应包含一个 {@code <from,to>} 关系。名称去除首尾空白后作为标识，
     * 区分大小写，允许内部空格（如 {@code MA 141}）。空引用和空白名称
     * 被拒绝，格式分隔符（{@code <}、{@code >}、{@code ,}）和换行符
     * 不能出现在名称中。
     * </p>
     *
     * @param text 待解析的文本，为 null 或空白时返回空图
     * @return 包含图、错误列表和警告列表的解析结果
     */
    public ParseResult parse(String text) {
        Graph graph = new Graph();
        List<ParseIssue> errors = new ArrayList<>();
        List<ParseIssue> warnings = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return new ParseResult(graph, errors, warnings);
        }

        // 用于在同一份输入内检测重复关系
        Set<String> seenEdges = new HashSet<>();

        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].trim();

            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }

            // 跳过 # 注释行
            if (line.startsWith("#")) {
                continue;
            }

            // 全角尖括号和全角逗号转半角
            line = normalizeFullWidth(line);

            // 解析单行的 <from,to> 格式
            String[] pair = extractPair(line, lineNumber, errors);
            if (pair == null) {
                // 格式错误，已记录到 errors，跳过本行
                continue;
            }

            String from = pair[0];
            String to = pair[1];

            // 检查本份输入内是否已出现相同关系
            String edgeKey = from + "\t" + to;
            if (seenEdges.contains(edgeKey)) {
                warnings.add(new ParseIssue(lineNumber,
                        "重复的关系 <" + from + "," + to + ">，已忽略"));
                continue;
            }
            seenEdges.add(edgeKey);

            // 通过 Graph.addEdge 建立关系，自动补齐缺失端点
            // 自环（from.equals(to)）按正常边处理，由环检测模块提示
            graph.addEdge(from, to);
        }

        return new ParseResult(graph, errors, warnings);
    }

    /**
     * 将全角格式符号转为半角。
     */
    private String normalizeFullWidth(String line) {
        return line.replace('＜', '<')
                .replace('＞', '>')
                .replace('，', ',');
    }

    /**
     * 从一行中提取 {@code <from,to>} 的两个名称。
     * <p>
     * 解析失败时向 errors 添加对应行号的错误并返回 null。
     * </p>
     *
     * @param line       已去除首尾空白并完成全角转换的行
     * @param lineNumber 行号（从 1 开始）
     * @param errors     错误列表，解析失败时向此列表追加
     * @return 成功时返回 [from, to]，失败返回 null
     */
    private String[] extractPair(String line, int lineNumber, List<ParseIssue> errors) {
        // 查找尖括号
        int left = line.indexOf('<');
        int right = line.lastIndexOf('>');

        if (left == -1 || right == -1 || left >= right) {
            errors.add(new ParseIssue(lineNumber,
                    "格式错误：缺少尖括号，无法识别为关系"));
            return null;
        }

        // 提取尖括号内的内容
        String content = line.substring(left + 1, right).trim();

        // 查找逗号
        int comma = content.indexOf(',');
        if (comma == -1) {
            errors.add(new ParseIssue(lineNumber,
                    "格式错误：缺少逗号分隔符"));
            return null;
        }

        String from = content.substring(0, comma).trim();
        String to = content.substring(comma + 1).trim();

        // 校验起点名称
        if (from.isEmpty()) {
            errors.add(new ParseIssue(lineNumber,
                    "格式错误：起点名称为空"));
            return null;
        }
        if (containsFormatDelimiter(from)) {
            errors.add(new ParseIssue(lineNumber,
                    "起点名称包含非法字符：" + from));
            return null;
        }

        // 校验终点名称
        if (to.isEmpty()) {
            errors.add(new ParseIssue(lineNumber,
                    "格式错误：终点名称为空"));
            return null;
        }
        if (containsFormatDelimiter(to)) {
            errors.add(new ParseIssue(lineNumber,
                    "终点名称包含非法字符：" + to));
            return null;
        }

        return new String[] { from, to };
    }

    /**
     * 检查名称中是否包含格式分隔符（{@code <}、{@code >}、{@code ,}）或换行符。
     * <p>
     * 接口契约规定：格式分隔符、换行和空名称不作为合法名称。
     * </p>
     */
    private boolean containsFormatDelimiter(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '<' || c == '>' || c == ',' || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }
}