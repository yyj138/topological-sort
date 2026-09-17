package topo.parser;

import java.util.ArrayList;
import java.util.List;

// 输入预校验器（D 包桩，T-D3）：解析前即时反馈中文提示与行号
public class InputValidator {

    public static List<ParseError> validate(String text) {
        List<ParseError> errors = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return errors;

        String[] lines = text.split("\\r\\n|\\n|\\r");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNo = i + 1;
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (!line.startsWith("<") && !line.startsWith("＜")) {
                errors.add(new ParseError(lineNo, "缺少起始尖括号 <", line));
                continue;
            }
            if (!line.endsWith(">") && !line.endsWith("＞")) {
                errors.add(new ParseError(lineNo, "缺少结束尖括号 >", line));
                continue;
            }
            if (!line.contains(",") && !line.contains("，")) {
                errors.add(new ParseError(lineNo, "缺少逗号分隔符", line));
                continue;
            }
            for (char ch : line.toCharArray()) {
                if (ch == '<' || ch == '>' || ch == '＜' || ch == '＞'
                        || ch == ',' || ch == '，' || ch == ' '
                        || ch == '\t' || Character.isLetterOrDigit(ch)) {
                    continue;
                }
                errors.add(new ParseError(lineNo, "包含非法字符: '" + ch + "'", line));
                break;
            }
        }
        return errors;
    }

    public static boolean isParsable(String text) {
        return validate(text).isEmpty();
    }
}
