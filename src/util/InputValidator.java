package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 输入合法性校验工具类。
 * <p>
 * 用于在解析之前预检用户输入，与 {@link io.DataParser} 复用同一套格式规则。
 * 校验结果包含结构化的错误列表（{@link io.ParseIssue}），错误定位到具体行号，
 * 供 UI 层实时反馈使用。
 * </p>
 *
 * <p>
 * 校验规则与 DataParser 保持一致：
 * </p>
 * <ul>
 * <li>跳过空行和以 {@code #} 开头的注释行</li>
 * <li>容忍行首尾空格，兼容全角尖括号和全角逗号</li>
 * <li>名称允许内部空格（如 {@code MA 141}），区分大小写</li>
 * <li>名称首尾空白使用 {@code String.strip()} 清理，与 A 的 Vertex.normalizeName 保持一致</li>
 * <li>名称不允许包含 {@code <}、{@code >}、{@code ,}、换行符或 Unicode 行分隔符</li>
 * <li>重复关系作为警告提示，自环保留为合法关系</li>
 * <li>重复判断使用 (from, to) 二元组作为键，避免名称内部含 {@code \t} 时误判</li>
 * </ul>
 */
public class InputValidator {

    /**
     * 校验结果，包含是否通过、错误列表和警告列表。
     * <p>
     * 错误表示该行格式不合法，有错误时不应启动计算；
     * 警告表示格式合法但需要提示用户（如重复关系）。
     * </p>
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<io.ParseIssue> errors;
        private final List<io.ParseIssue> warnings;

        public ValidationResult(boolean valid,
                List<io.ParseIssue> errors,
                List<io.ParseIssue> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        /** 校验是否通过（无错误） */
        public boolean isValid() {
            return valid;
        }

        /** 获取错误列表 */
        public List<io.ParseIssue> getErrors() {
            return errors;
        }

        /** 获取警告列表 */
        public List<io.ParseIssue> getWarnings() {
            return warnings;
        }
    }

    /**
     * 对用户输入的文本进行合法性校验。
     * <p>
     * 规则与 {@link io.DataParser#parse(String)} 完全一致：
     * 跳过空行和注释行、全角转半角、校验尖括号和逗号、
     * 校验名称合法性、检测重复关系。
     * </p>
     *
     * @param input 用户输入的原始文本（多行，每行一条关系）
     * @return 校验结果，包含是否通过、错误列表和警告列表
     */
    public static ValidationResult validate(String input) {
        List<io.ParseIssue> errors = new ArrayList<>();
        List<io.ParseIssue> warnings = new ArrayList<>();

        if (input == null || input.strip().isEmpty()) {
            return new ValidationResult(true, errors, warnings);
        }

        // 使用 (from, to) 二元组作为重复键，避免 from/to 内部含 \t 时拼接碰撞
        Set<List<String>> seenEdges = new HashSet<>();
        String[] lines = input.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].strip();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                continue;
            }

            line = normalizeFullWidth(line);

            String[] pair = extractPair(line, lineNumber, errors);
            if (pair == null) {
                continue;
            }

            String from = pair[0];
            String to = pair[1];

            // 使用不可变二元组作为重复键，基于元素内容比较，不会因名称内部含 \t 而碰撞
            List<String> edgeKey = List.of(from, to);
            if (seenEdges.contains(edgeKey)) {
                warnings.add(new io.ParseIssue(lineNumber,
                        "重复的关系 <" + from + "," + to + ">，已忽略"));
                continue;
            }
            seenEdges.add(edgeKey);
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * 将全角格式符号转为半角（与 DataParser 一致）。
     */
    private static String normalizeFullWidth(String line) {
        return line.replace('＜', '<')
                .replace('＞', '>')
                .replace('，', ',');
    }

    /**
     * 从一行中提取并校验 {@code <from,to>} 的两个名称。
     * <p>
     * 错误消息与 DataParser.extractPair 完全一致。
     * </p>
     * <p>
     * 名称处理顺序（与 A 的 Vertex.normalizeName 保持一致）：
     * <ol>
     * <li>不提前 strip 括号内容，保留原始字符；</li>
     * <li>用 {@code content.strip().isEmpty()} 判断括号内是否为空，
     * 不改变 content 本身；</li>
     * <li>提取 fromRaw / toRaw 时不做 strip；</li>
     * <li>先对原始名称检查非法字符（{@code <}、{@code >}、{@code ,}、
     * 换行符、Unicode 行分隔符）；</li>
     * <li>再对原始名称使用 {@code String.strip()} 去除首尾空白
     * （含 U+2003 等 Unicode 空白）；</li>
     * <li>最后判断清理后的名称是否为空。</li>
     * </ol>
     * </p>
     *
     * @param line       已去除首尾空白并完成全角转换的行
     * @param lineNumber 行号（从 1 开始）
     * @param errors     错误列表
     * @return 成功时返回 [from, to]，失败返回 null
     */
    // ⚠️【重要】本方法逻辑必须与 io.DataParser.extractPair 完全一致！
    // 修改此处，必须同步修改 DataParser 的同名方法；
    // 修改完成运行 TestDataParser + TestInputValidator。
    private static String[] extractPair(String line, int lineNumber,
            List<io.ParseIssue> errors) {
        int left = line.indexOf('<');
        int right = line.lastIndexOf('>');

        if (left == -1) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：缺少左括号 '<'"));
            return null;
        }
        if (right == -1) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：缺少右括号 '>'"));
            return null;
        }
        if (left >= right) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：尖括号顺序错误"));
            return null;
        }

        // 不提前 strip 括号内容，保留原始字符，避免首尾 Unicode 行分隔符被清掉
        String content = line.substring(left + 1, right);

        // 用 content.strip() 判断是否为空，但不改变 content 本身
        if (content.strip().isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：括号内为空"));
            return null;
        }

        int comma = content.indexOf(',');
        if (comma == -1) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：缺少逗号分隔符"));
            return null;
        }

        // 提取原始名称，不 strip
        String fromRaw = content.substring(0, comma);
        String toRaw = content.substring(comma + 1);

        // 先检查非法字符（含 Unicode 行分隔符），不能先 strip
        if (containsFormatDelimiter(fromRaw)) {
            errors.add(new io.ParseIssue(lineNumber, "起点名称包含非法字符：" + fromRaw));
            return null;
        }
        if (containsFormatDelimiter(toRaw)) {
            errors.add(new io.ParseIssue(lineNumber, "终点名称包含非法字符：" + toRaw));
            return null;
        }

        // 再 strip，去除首尾空白（含 U+2003 等 Unicode 空白）
        String from = fromRaw.strip();
        String to = toRaw.strip();

        if (from.isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：起点名称为空"));
            return null;
        }
        if (to.isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber, "格式错误：终点名称为空"));
            return null;
        }

        return new String[] { from, to };
    }

    /**
     * 检查名称中是否包含格式分隔符（{@code <}、{@code >}、{@code ,}）、
     * 换行符（{@code \n}、{@code \r}）或 Unicode 行分隔符
     * （{@code \u0085}、{@code \u2028}、{@code \u2029}）。
     * <p>
     * 与 DataParser.containsFormatDelimiter 规则完全一致。
     * 规则与 {@link model.Vertex#normalizeName(String)} 保持一致。
     * </p>
     */
    private static boolean containsFormatDelimiter(String name) {
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