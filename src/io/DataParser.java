package io;

import model.Graph;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 {@code <from,to>} 格式的文本，直接通过 Graph 方法建立图。
 * <p>
 * 功能说明：
 * <ul>
 * <li>跳过空行和以 {@code #} 开头的注释行</li>
 * <li>容忍行首尾空格，兼容全角尖括号和全角逗号</li>
 * <li>自动去除重复边并记录警告</li>
 * <li>识别到自环边并保留入图；环路路径识别由 CycleDetector 模块完成</li>
 * <li>解析失败记录行号与原因，返回结构化错误列表</li>
 * <li>名称首尾空白使用 {@code String.strip()} 清理，与 A 的 Vertex.normalizeName 保持一致</li>
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

        if (text == null || text.strip().isEmpty()) {
            return new ParseResult(graph, errors, warnings);
        }

        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].strip();

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

            // 通过 Graph.addEdge 建立关系，自动补齐缺失端点
            // 自环（from.equals(to)）按正常边处理，由环检测模块提示
            // addEdge 返回 false 表示重复边，不重复增加入度
            // try-catch 兜底：即使 containsFormatDelimiter 漏检，也不让 parse 抛异常
            try {
                boolean added = graph.addEdge(from, to);
                if (!added) {
                    warnings.add(new ParseIssue(lineNumber,
                            "重复的关系 <" + from + "," + to + ">，已忽略"));
                }
            } catch (IllegalArgumentException e) {
                errors.add(new ParseIssue(lineNumber,
                        "名称不符合图结构规则：" + e.getMessage()));
            }
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
     * <p>
     * 名称处理顺序（与 A 的 Vertex.normalizeName 保持一致）：
     * <ol>
     * <li>不提前 strip 括号内容，保留原始字符；</li>
     * <li>用 {@code content.strip().isEmpty()} 判断括号内是否为空，
     *     不改变 content 本身；</li>
     * <li>提取 fromRaw / toRaw 时不做 strip；</li>
     * <li>先对原始名称检查非法字符（{@code <}、{@code >}、{@code ,}、
     *     换行符、Unicode 行分隔符）；</li>
     * <li>再对原始名称使用 {@code String.strip()} 去除首尾空白
     *     （含 U+2003 等 Unicode 空白）；</li>
     * <li>最后判断清理后的名称是否为空。</li>
     * </ol>
     * 不能先 strip 再检查非法字符，否则首尾的 U+2028、U+2029 等
     * 行分隔符会被 strip 清掉，导致非法字符漏检。
     * </p>
     *
     * @param line       已去除首尾空白并完成全角转换的行
     * @param lineNumber 行号（从 1 开始）
     * @param errors     错误列表，解析失败时向此列表追加
     * @return 成功时返回 [from, to]，失败返回 null
     */

    // ⚠️【重要】本方法逻辑必须与 util.InputValidator.extractPair 完全一致！
    // 修改此处，必须同步修改 InputValidator 的同名方法；
    // 修改完成运行 TestDataParser + TestInputValidator。
    private String[] extractPair(String line, int lineNumber, List<ParseIssue> errors) {
        int left = line.indexOf('<');
        int right = line.lastIndexOf('>');

        if (left == -1) {
            errors.add(new ParseIssue(lineNumber, "格式错误：缺少左括号 '<'"));
            return null;
        }
        if (right == -1) {
            errors.add(new ParseIssue(lineNumber, "格式错误：缺少右括号 '>'"));
            return null;
        }
        if (left >= right) {
            errors.add(new ParseIssue(lineNumber, "格式错误：尖括号顺序错误"));
            return null;
        }

        // 不提前 strip 括号内容，保留原始字符，避免首尾 Unicode 行分隔符被清掉
        String content = line.substring(left + 1, right);

        // 用 content.strip() 判断是否为空，但不改变 content 本身
        if (content.strip().isEmpty()) {
            errors.add(new ParseIssue(lineNumber, "格式错误：括号内为空"));
            return null;
        }

        int comma = content.indexOf(',');
        if (comma == -1) {
            errors.add(new ParseIssue(lineNumber, "格式错误：缺少逗号分隔符"));
            return null;
        }

        // 提取原始名称，不 strip
        String fromRaw = content.substring(0, comma);
        String toRaw = content.substring(comma + 1);

        // 先检查非法字符（含 Unicode 行分隔符），不能先 strip
        if (containsFormatDelimiter(fromRaw)) {
            errors.add(new ParseIssue(lineNumber, "起点名称包含非法字符：" + fromRaw));
            return null;
        }
        if (containsFormatDelimiter(toRaw)) {
            errors.add(new ParseIssue(lineNumber, "终点名称包含非法字符：" + toRaw));
            return null;
        }

        // 再 strip，去除首尾空白（含 U+2003 等 Unicode 空白）
        String from = fromRaw.strip();
        String to = toRaw.strip();

        if (from.isEmpty()) {
            errors.add(new ParseIssue(lineNumber, "格式错误：起点名称为空"));
            return null;
        }
        if (to.isEmpty()) {
            errors.add(new ParseIssue(lineNumber, "格式错误：终点名称为空"));
            return null;
        }

        return new String[] { from, to };
    }

    /**
     * 检查名称中是否包含格式分隔符（{@code <}、{@code >}、{@code ,}）、
     * 换行符（{@code \n}、{@code \r}）或 Unicode 行分隔符
     * （{@code \u0085}、{@code \u2028}、{@code \u2029}）。
     * <p>
     * 接口契约规定：格式分隔符、换行和空名称不作为合法名称。
     * 规则与 {@link model.Vertex#normalizeName(String)} 保持一致。
     * </p>
     */
    private boolean containsFormatDelimiter(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '<' || c == '>' || c == ','
                    || c == '\n' || c == '\r'
                    || c == '\u0085' || c == '\u2028' || c == '\u2029') {
                return true;
            }
        }
        return false;
    }
}