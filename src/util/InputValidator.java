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
 * <li>名称不允许包含 {@code <}、{@code >}、{@code ,} 或换行符</li>
 * <li>重复关系作为警告提示，自环保留为合法关系</li>
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

        if (input == null || input.trim().isEmpty()) {
            return new ValidationResult(true, errors, warnings);
        }

        Set<String> seenEdges = new HashSet<>();
        String[] lines = input.split("\\r?\\n");

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

            // 校验单行的 <from,to> 格式
            String[] pair = extractPair(line, lineNumber, errors);
            if (pair == null) {
                continue;
            }

            String from = pair[0];
            String to = pair[1];

            // 检查重复关系
            String edgeKey = from + "\t" + to;
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
     *
     * @param line       已去除首尾空白并完成全角转换的行
     * @param lineNumber 行号（从 1 开始）
     * @param errors     错误列表
     * @return 成功时返回 [from, to]，失败返回 null
     */
    private static String[] extractPair(String line, int lineNumber,
            List<io.ParseIssue> errors) {
        int left = line.indexOf('<');
        int right = line.lastIndexOf('>');

        if (left == -1 || right == -1 || left >= right) {
            errors.add(new io.ParseIssue(lineNumber,
                    "格式错误：缺少尖括号，无法识别为关系"));
            return null;
        }

        String content = line.substring(left + 1, right).trim();

        if (content.isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber,
                    "格式错误：括号内为空"));
            return null;
        }

        int comma = content.indexOf(',');
        if (comma == -1) {
            errors.add(new io.ParseIssue(lineNumber,
                    "格式错误：缺少逗号分隔符"));
            return null;
        }

        String from = content.substring(0, comma).trim();
        String to = content.substring(comma + 1).trim();

        if (from.isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber,
                    "格式错误：起点名称为空"));
            return null;
        }
        if (containsFormatDelimiter(from)) {
            errors.add(new io.ParseIssue(lineNumber,
                    "起点名称包含非法字符：" + from));
            return null;
        }

        if (to.isEmpty()) {
            errors.add(new io.ParseIssue(lineNumber,
                    "格式错误：终点名称为空"));
            return null;
        }
        if (containsFormatDelimiter(to)) {
            errors.add(new io.ParseIssue(lineNumber,
                    "终点名称包含非法字符：" + to));
            return null;
        }

        return new String[] { from, to };
    }

    /**
     * 检查名称中是否包含格式分隔符（{@code <}、{@code >}、{@code ,}）或换行符。
     * <p>
     * 与 DataParser.containsFormatDelimiter 规则完全一致。
     * </p>
     */
    private static boolean containsFormatDelimiter(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '<' || c == '>' || c == ',' || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }
}