package topo.parser;

// 解析错误（D 包桩，T-D1/T-D3）
public class ParseError {

    private final int lineNo;
    private final String reason;
    private final String content;

    public ParseError(int lineNo, String reason, String content) {
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
