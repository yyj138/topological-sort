package io;

// 解析问题（契约 §六）：错误或警告，含行号与原因
public class ParseIssue {

    private final int lineNo;
    private final String reason;
    private final String content;

    public ParseIssue(int lineNo, String reason, String content) {
        this.lineNo = lineNo;
        this.reason = reason;
        this.content = content == null ? "" : content;
    }

    public int getLineNo() { return lineNo; }
    public String getReason() { return reason; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "第 " + lineNo + " 行: " + reason + " (内容: \"" + content + "\")";
    }
}
