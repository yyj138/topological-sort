package io;

import java.io.*;
import java.util.*;

/**
 * <a,b> 格式文本解析器。
 * 负责解析用户输入或文件中的关系数据，具备容错能力。
 *
 * <p>
 * 功能：
 * </p>
 * <ul>
 * <li>跳过空行和注释行（# 开头）</li>
 * <li>容忍行首尾空格和全角尖括号</li>
 * <li>自动去除重复边</li>
 * <li>识别自环（如 &lt;a,a&gt;）</li>
 * <li>解析失败记录行号和原因，返回结构化错误列表</li>
 * </ul>
 *
 * @author huxixi19
 */
public class DataParser {

    /**
     * 解析得到的单条边。
     */
    public static class Edge {
        private final String source;
        private final String target;

        public Edge(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        /** 判断是否为自环 */
        public boolean isSelfLoop() {
            return source.equals(target);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Edge edge = (Edge) o;
            return source.equals(edge.source) && target.equals(edge.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }

        @Override
        public String toString() {
            return "<" + source + "," + target + ">";
        }
    }

    /**
     * 单条解析错误记录。
     */
    public static class ParseError {
        private final int lineNumber;
        private final String message;

        public ParseError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "第" + lineNumber + "行：" + message;
        }
    }

    /**
     * 解析结果，包含解析成功的边列表和错误列表。
     */
    public static class ParseResult {
        private final List<Edge> edges;
        private final List<Edge> selfLoops;
        private final List<ParseError> errors;
        private final int duplicateCount;

        public ParseResult(List<Edge> edges, List<Edge> selfLoops,
                List<ParseError> errors, int duplicateCount) {
            this.edges = edges;
            this.selfLoops = selfLoops;
            this.errors = errors;
            this.duplicateCount = duplicateCount;
        }

        /** 获取所有有效边（不含自环和重复边） */
        public List<Edge> getEdges() {
            return edges;
        }

        /** 获取所有自环边 */
        public List<Edge> getSelfLoops() {
            return selfLoops;
        }

        /** 获取所有错误信息 */
        public List<ParseError> getErrors() {
            return errors;
        }

        /** 获取去重数量 */
        public int getDuplicateCount() {
            return duplicateCount;
        }

        /** 是否解析完全成功（无错误） */
        public boolean isSuccess() {
            return errors.isEmpty();
        }

        /** 是否有有效数据（至少有一条边或一条自环） */
        public boolean hasData() {
            return !edges.isEmpty() || !selfLoops.isEmpty();
        }

        /** 获取所有顶点名称（去重） */
        public Set<String> getAllVertices() {
            Set<String> vertices = new LinkedHashSet<>();
            for (Edge edge : edges) {
                vertices.add(edge.getSource());
                vertices.add(edge.getTarget());
            }
            for (Edge edge : selfLoops) {
                vertices.add(edge.getSource());
            }
            return vertices;
        }
    }

    /**
     * 从字符串文本解析关系数据。
     *
     * @param text 包含 <a,b> 格式关系的多行文本
     * @return ParseResult 解析结果
     */
    public static ParseResult parse(String text) {
        List<Edge> edges = new ArrayList<>();
        List<Edge> selfLoops = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        Set<Edge> seen = new LinkedHashSet<>();
        int duplicateCount = 0;

        if (text == null) {
            errors.add(new ParseError(0, "输入文本为null"));
            return new ParseResult(edges, selfLoops, errors, 0);
        }

        String[] lines = text.split("\\r?\\n");

        boolean hasValidLine = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i].trim();

            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }

            // 跳过注释行
            if (line.startsWith("#")) {
                continue;
            }

            hasValidLine = true;

            // 全角尖括号转半角
            line = line.replace('\uff1c', '<')
                    .replace('\uff1e', '>');

            // 去除尖括号外侧的空格，保留括号内顶点名称中的空格
            line = line.replaceAll("\\s*<", "<");
            line = line.replaceAll(">\\s*", ">");

            // 检查尖括号配对
            int firstBracket = line.indexOf('<');
            int lastBracket = line.lastIndexOf('>');

            if (firstBracket == -1) {
                errors.add(new ParseError(lineNum, "缺少左括号 '<'，原始内容: " + lines[i].trim()));
                continue;
            }
            if (lastBracket == -1) {
                errors.add(new ParseError(lineNum, "缺少右括号 '>'，原始内容: " + lines[i].trim()));
                continue;
            }
            if (firstBracket > lastBracket) {
                errors.add(new ParseError(lineNum, "括号顺序错误，原始内容: " + lines[i].trim()));
                continue;
            }

            // 提取括号内的内容
            String content = line.substring(firstBracket + 1, lastBracket).trim();

            if (content.isEmpty()) {
                errors.add(new ParseError(lineNum, "括号内为空，原始内容: " + lines[i].trim()));
                continue;
            }

            // 检查逗号
            if (!content.contains(",")) {
                errors.add(new ParseError(lineNum, "缺少逗号 ','，原始内容: " + lines[i].trim()));
                continue;
            }

            String[] parts = content.split(",", -1);
            if (parts.length != 2) {
                errors.add(new ParseError(lineNum, "逗号数量不正确，原始内容: " + lines[i].trim()));
                continue;
            }

            String source = parts[0].trim();
            String target = parts[1].trim();

            // 检查顶点名称是否为空
            if (source.isEmpty()) {
                errors.add(new ParseError(lineNum, "逗号前的顶点名称为空，原始内容: " + lines[i].trim()));
                continue;
            }
            if (target.isEmpty()) {
                errors.add(new ParseError(lineNum, "逗号后的顶点名称为空，原始内容: " + lines[i].trim()));
                continue;
            }

            // 构造边
            Edge edge = new Edge(source, target);

            // 检查是否为自环
            if (edge.isSelfLoop()) {
                if (!seen.contains(edge)) {
                    selfLoops.add(edge);
                    seen.add(edge);
                } else {
                    duplicateCount++;
                }
                continue;
            }

            // 检查重复边
            if (seen.contains(edge)) {
                duplicateCount++;
                continue;
            }

            seen.add(edge);
            edges.add(edge);
        }

        // 检查是否全部为空行或注释行
        if (!hasValidLine && errors.isEmpty()) {
            errors.add(new ParseError(0, "没有有效的数据行"));
        }

        return new ParseResult(edges, selfLoops, errors, duplicateCount);
    }

    /**
     * 从文件解析关系数据。
     *
     * @param filePath 文件路径
     * @return ParseResult 解析结果
     */
    public static ParseResult parseFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            List<ParseError> errors = new ArrayList<>();
            errors.add(new ParseError(0, "文件路径为空"));
            return new ParseResult(new ArrayList<>(), new ArrayList<>(), errors, 0);
        }

        File file = new File(filePath);
        if (!file.exists()) {
            List<ParseError> errors = new ArrayList<>();
            errors.add(new ParseError(0, "文件不存在: " + filePath));
            return new ParseResult(new ArrayList<>(), new ArrayList<>(), errors, 0);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            List<ParseError> errors = new ArrayList<>();
            errors.add(new ParseError(0, "读取文件失败: " + e.getMessage()));
            return new ParseResult(new ArrayList<>(), new ArrayList<>(), errors, 0);
        }

        return parse(sb.toString());
    }
}