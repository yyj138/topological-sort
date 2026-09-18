package util;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入合法性校验工具类。
 * 用于在解析之前预检用户输入，检查括号配对、逗号存在、字符合法性等，
 * 错误信息可定位到具体行号，供 UI 层实时反馈使用。
 *
 * @author huxixi19
 */
public class InputValidator {

    /**
     * 校验结果内部类，包含是否通过和错误信息列表。
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        /** 校验是否通过 */
        public boolean isValid() {
            return valid;
        }

        /** 获取所有错误信息（中文描述 + 行号） */
        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * 对用户输入的文本进行合法性校验。
     *
     * @param input 用户输入的原始文本（多行，每行一条关系）
     * @return ValidationResult 校验结果，包含是否通过和错误信息
     */
    public static ValidationResult validate(String input) {
        List<String> errors = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            errors.add("输入为空，请输入数据");
            return new ValidationResult(false, errors);
        }

        String[] lines = input.split("\\r?\\n");

        boolean hasValidLine = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i].trim();

            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }

            // 跳过注释行（# 开头）
            if (line.startsWith("#")) {
                continue;
            }

            hasValidLine = true;

            // 全角尖括号转半角
            String normalized = line
                    .replace('\uff1c', '<') // ＜ → <
                    .replace('\uff1e', '>'); // ＞ → >

            // 检查尖括号是否配对
            int firstBracket = normalized.indexOf('<');
            int lastBracket = normalized.lastIndexOf('>');

            if (firstBracket == -1) {
                errors.add("第" + lineNum + "行：缺少左括号 '<'，当前内容为: " + line);
                continue;
            }
            if (lastBracket == -1) {
                errors.add("第" + lineNum + "行：缺少右括号 '>'，当前内容为: " + line);
                continue;
            }
            if (firstBracket > lastBracket) {
                errors.add("第" + lineNum + "行：括号顺序错误（'>' 出现在 '<' 之前），当前内容为: " + line);
                continue;
            }
            if (firstBracket != 0) {
                errors.add("第" + lineNum + "行：左括号 '<' 不在行首，当前内容为: " + line);
            }

            // 提取括号内容
            String content = normalized.substring(firstBracket + 1, lastBracket).trim();

            // 检查括号内是否为空
            if (content.isEmpty()) {
                errors.add("第" + lineNum + "行：括号内为空，当前内容为: " + line);
                continue;
            }

            // 检查逗号是否存在
            if (!content.contains(",")) {
                errors.add("第" + lineNum + "行：缺少逗号 ','，当前内容为: " + line);
                continue;
            }

            // 检查逗号分割后的两部分
            String[] parts = content.split(",", -1);
            if (parts.length != 2) {
                errors.add("第" + lineNum + "行：逗号数量不正确，应只有一个逗号，当前内容为: " + line);
                continue;
            }

            String source = parts[0].trim();
            String target = parts[1].trim();

            // 检查顶点名称是否为空
            if (source.isEmpty()) {
                errors.add("第" + lineNum + "行：逗号前的顶点名称为空，当前内容为: " + line);
            }
            if (target.isEmpty()) {
                errors.add("第" + lineNum + "行：逗号后的顶点名称为空，当前内容为: " + line);
            }

            // 检查顶点名称是否包含非法字符
            if (!source.isEmpty() && !isValidVertexName(source)) {
                errors.add("第" + lineNum + "行：顶点名称 '" + source + "' 包含非法字符，当前内容为: " + line);
            }
            if (!target.isEmpty() && !isValidVertexName(target)) {
                errors.add("第" + lineNum + "行：顶点名称 '" + target + "' 包含非法字符，当前内容为: " + line);
            }
        }

        // 检查是否全部为空行或注释行
        if (!hasValidLine && errors.isEmpty()) {
            errors.add("没有有效的数据行，请输入至少一条关系数据");
            return new ValidationResult(false, errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 判断顶点名称是否合法。
     * 允许字母、数字、下划线、短横线、中文。
     *
     * @param name 顶点名称
     * @return 合法返回 true，否则返回 false
     */
    private static boolean isValidVertexName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // 允许：字母、数字、下划线、短横线、中文字符
        return name.matches("[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+");
    }
}