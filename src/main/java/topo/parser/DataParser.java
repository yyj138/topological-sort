package topo.parser;

import topo.model.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 数据解析器（D 包桩，T-D1）：容忍全角尖括号、跳过空行与 # 注释、自动去重
public class DataParser {

    // 匹配 <起,终>，兼容全角 <> 与空格
    private static final Pattern RELATION_PATTERN =
            Pattern.compile("[<＜]\\s*([^,，<>＞\\s]+)\\s*[,，]\\s*([^,，<>＞\\s]+)\\s*[>＞]");

    public static ParseResult parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ParseResult(new ArrayList<>(), new ArrayList<>(), false, 0);
        }
        String[] lines = text.split("\\r\\n|\\n|\\r");
        return parseLines(lines);
    }

    public static ParseResult parseLines(String[] lines) {
        List<Edge> edges = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();
        boolean hasSelfLoop = false;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            int lineNo = i + 1;
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            Matcher m = RELATION_PATTERN.matcher(trimmed);
            if (!m.find()) {
                errors.add(new ParseError(lineNo, "格式错误：应为 <a,b>", trimmed));
                continue;
            }
            String from = m.group(1).trim();
            String to = m.group(2).trim();
            if (from.isEmpty() || to.isEmpty()) {
                errors.add(new ParseError(lineNo, "顶点名不能为空", trimmed));
                continue;
            }
            String key = from + "|" + to;
            if (seenEdges.contains(key)) {
                errors.add(new ParseError(lineNo, "重复边已自动跳过", trimmed));
                continue;
            }
            seenEdges.add(key);
            Edge edge = new Edge(from, to);
            if (edge.isSelfLoop()) hasSelfLoop = true;
            edges.add(edge);
        }
        return new ParseResult(edges, errors, hasSelfLoop, lines.length);
    }

    public static String toText(ParseResult result) {
        StringBuilder sb = new StringBuilder();
        for (Edge e : result.getEdges()) {
            sb.append("<").append(e.getFrom()).append(",")
              .append(e.getTo()).append(">\n");
        }
        return sb.toString();
    }
}
