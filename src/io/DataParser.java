package io;

import model.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 数据解析器（D 包桩，T-D1）：按契约 §六
// 解析器直接通过 Graph 方法建图，返回问题清单
// 空白行与 # 注释跳过；重复边去重并产生警告；自环作为边保留
public class DataParser {

    // 匹配 <起,终>，兼容全角尖括号与空格
    private static final Pattern RELATION_PATTERN =
            Pattern.compile("[<＜]\\s*([^,，<>＞\\s]+)\\s*[,，]\\s*([^,，<>＞\\s]+)\\s*[>＞]");

    // 实例方法，按契约 §六
    public ParseResult parse(String text) {
        List<ParseIssue> errors = new ArrayList<>();
        List<ParseIssue> warnings = new ArrayList<>();
        Graph graph = new Graph();

        if (text == null || text.trim().isEmpty()) {
            return new ParseResult(graph, errors, warnings);
        }

        String[] lines = text.split("\\r\\n|\\n|\\r");
        Set<String> seenEdges = new HashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            int lineNo = i + 1;
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            Matcher m = RELATION_PATTERN.matcher(trimmed);
            if (!m.find()) {
                errors.add(new ParseIssue(lineNo, "格式错误：应为 <a,b>", trimmed));
                continue;
            }
            String from = m.group(1).trim();
            String to = m.group(2).trim();
            if (from.isEmpty() || to.isEmpty()) {
                errors.add(new ParseIssue(lineNo, "顶点名不能为空", trimmed));
                continue;
            }
            String key = from + "|" + to;
            if (seenEdges.contains(key)) {
                warnings.add(new ParseIssue(lineNo, "重复边已自动跳过", trimmed));
                continue;
            }
            seenEdges.add(key);
            boolean added = graph.addEdge(from, to);
            if (!added) {
                warnings.add(new ParseIssue(lineNo, "重复边已自动跳过", trimmed));
            }
            if (from.equals(to)) {
                // 自环作为边保留，由环检测统一提示
            }
        }
        return new ParseResult(graph, errors, warnings);
    }

    // 将边列表回写为 <a,b> 文本（用于保存）
    public static String toText(Graph graph) {
        StringBuilder sb = new StringBuilder();
        for (model.Edge e : graph.getEdges()) {
            sb.append("<").append(e.getFrom()).append(",")
              .append(e.getTo()).append(">\n");
        }
        return sb.toString();
    }
}
