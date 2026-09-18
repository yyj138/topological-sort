package io;

import model.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 数据解析器（T-D1 契约桩，A 提供，待 D 交付正式实现替换）：按接口契约 §六
// 实例方法 parse 直接通过 Graph 公开方法建图，返回 io.ParseResult（graph + errors + warnings）
// 规则：空白行与 # 注释跳过；英文半角 <a,b> 正式格式；兼容全角 ＜＞，；
// 节点名 trim 首尾、保留内部空格（如 MA 141、CS 225，契约 §二）；
// 重复边去重并产生警告；自环作为真实边保留，交环检测统一报告；
// 整份输入没有有效数据时不报错，返回空 Graph，由输入层（B）统一提示
public class DataParser {

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
            String raw = lines[i];
            String trimmed = raw.trim();
            int lineNo = i + 1;
            // 空行与注释行跳过
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            // 全角尖括号与全角逗号统一转为半角后解析
            String normalized = trimmed
                    .replace('\uFF1C', '<')   // ＜
                    .replace('\uFF1E', '>')   // ＞
                    .replace('\uFF0C', ',');  // ，

            int left = normalized.indexOf('<');
            int right = normalized.lastIndexOf('>');
            if (left < 0 || right < 0 || left > right || right <= left + 1) {
                errors.add(new ParseIssue(lineNo, "格式错误：应为 <a,b>", trimmed));
                continue;
            }

            String content = normalized.substring(left + 1, right).trim();
            int comma = content.indexOf(',');
            if (comma <= 0 || comma != content.lastIndexOf(',')) {
                errors.add(new ParseIssue(lineNo, "格式错误：尖括号内应为且仅为一个逗号分隔的两个名称", trimmed));
                continue;
            }

            // trim 首尾空白，保留名称内部空格（契约 §二）
            String from = content.substring(0, comma).trim();
            String to = content.substring(comma + 1).trim();
            if (from.isEmpty() || to.isEmpty()) {
                errors.add(new ParseIssue(lineNo, "顶点名不能为空", trimmed));
                continue;
            }

            String key = from + "|" + to;
            if (seenEdges.contains(key)) {
                warnings.add(new ParseIssue(lineNo, "重复关系已自动去重", trimmed));
                continue;
            }

            try {
                boolean added = graph.addEdge(from, to);
                if (added) {
                    seenEdges.add(key);
                } else {
                    // Graph 端去重命中（理论上 seenEdges 已拦住，双保险）
                    warnings.add(new ParseIssue(lineNo, "重复关系已自动去重", trimmed));
                }
            } catch (IllegalArgumentException ex) {
                // Graph 名称校验拒绝（含分隔符、控制字符等）：转为本行解析错误
                errors.add(new ParseIssue(lineNo, ex.getMessage(), trimmed));
            }
            // 自环 from.equals(to) 作为真实边保留，由 CycleDetector 统一报告
        }

        return new ParseResult(graph, errors, warnings);
    }

    // 将图中全部边回写为 <a,b> 文本（用于表格→文本/保存）
    // Graph 不维护独立 Edge 列表（图设计 §二/§四），按节点名+后继派生全部边
    public static String toText(Graph graph) {
        StringBuilder sb = new StringBuilder();
        for (String from : graph.getVertexNames()) {
            for (String to : graph.getSuccessors(from)) {
                sb.append("<").append(from).append(",").append(to).append(">\n");
            }
        }
        return sb.toString();
    }
}
