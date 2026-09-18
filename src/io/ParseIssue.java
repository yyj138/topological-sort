package io;

/**
 * 描述解析过程中发现的单条问题，包含行号（从 1 开始）和原因。
 * <p>
 * 错误（error）表示该行无法解析，有错误时不应启动计算；
 * 警告（warning）表示该行可继续处理但需要提示用户，例如重复关系。
 * </p>
 */
public class ParseIssue {

    private final int lineNumber;
    private final String message;

    /**
     * @param lineNumber 问题所在行号，从 1 开始
     * @param message    中文原因描述
     */
    public ParseIssue(int lineNumber, String message) {
        this.lineNumber = lineNumber;
        this.message = message;
    }

    /**
     * @return 问题所在行号（从 1 开始）
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * @return 原因描述
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "第 " + lineNumber + " 行：" + message;
    }
}