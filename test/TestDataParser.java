import io.DataParser;
import io.DataParser.ParseResult;
import io.DataParser.Edge;

public class TestDataParser {
    public static void main(String[] args) {
        int pass = 0;
        int fail = 0;

        // 测试1：正常输入
        System.out.println("=== 测试1：正常输入 ===");
        ParseResult r1 = DataParser.parse("<a,b>\n<b,c>\n<c,d>");
        if (r1.isSuccess() && r1.getEdges().size() == 3) {
            System.out.println("通过！边数: " + r1.getEdges().size());
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试2：重复边自动去重
        System.out.println("\n=== 测试2：重复边去重 ===");
        ParseResult r2 = DataParser.parse("<a,b>\n<a,b>\n<b,c>\n<b,c>");
        if (r2.isSuccess() && r2.getEdges().size() == 2 && r2.getDuplicateCount() == 2) {
            System.out.println("通过！有效边: " + r2.getEdges().size()
                    + "，去重数: " + r2.getDuplicateCount());
            pass++;
        } else {
            System.out.println("失败！边数: " + r2.getEdges().size()
                    + "，去重数: " + r2.getDuplicateCount());
            fail++;
        }

        // 测试3：自环识别
        System.out.println("\n=== 测试3：自环识别 ===");
        ParseResult r3 = DataParser.parse("<a,b>\n<a,a>\n<b,c>");
        if (r3.getSelfLoops().size() == 1 && r3.getSelfLoops().get(0).getSource().equals("a")) {
            System.out.println("通过！自环: " + r3.getSelfLoops().get(0));
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试4：空输入
        System.out.println("\n=== 测试4：空输入 ===");
        ParseResult r4 = DataParser.parse("");
        if (!r4.isSuccess() && !r4.getErrors().isEmpty()) {
            System.out.println("通过！错误: " + r4.getErrors().get(0));
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试5：注释和空行混合
        System.out.println("\n=== 测试5：注释和空行混合 ===");
        ParseResult r5 = DataParser.parse("# 这是注释\n\n<a,b>\n\n# 另一行注释\n<b,c>");
        if (r5.isSuccess() && r5.getEdges().size() == 2) {
            System.out.println("通过！边数: " + r5.getEdges().size());
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试6：全角尖括号
        System.out.println("\n=== 测试6：全角尖括号 ===");
        ParseResult r6 = DataParser.parse("＜a,b＞\n＜b,c＞");
        if (r6.isSuccess() && r6.getEdges().size() == 2) {
            System.out.println("通过！边数: " + r6.getEdges().size());
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试7：行首尾空格容忍
        System.out.println("\n=== 测试7：行首尾空格容忍 ===");
        ParseResult r7 = DataParser.parse("  <a,b>  \n   <b,c>   ");
        if (r7.isSuccess() && r7.getEdges().size() == 2) {
            System.out.println("通过！边数: " + r7.getEdges().size());
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试8：非法输入（缺括号）
        System.out.println("\n=== 测试8：非法输入（缺括号）===");
        ParseResult r8 = DataParser.parse("a,b>");
        if (!r8.isSuccess() && !r8.getErrors().isEmpty()) {
            System.out.println("通过！错误: " + r8.getErrors().get(0));
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试9：非法输入（缺逗号）
        System.out.println("\n=== 测试9：非法输入（缺逗号）===");
        ParseResult r9 = DataParser.parse("<a b>");
        if (!r9.isSuccess() && !r9.getErrors().isEmpty()) {
            System.out.println("通过！错误: " + r9.getErrors().get(0));
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试10：混合输入（合法+非法+注释+空行）
        System.out.println("\n=== 测试10：混合输入 ===");
        ParseResult r10 = DataParser.parse("# 注释\n<a,b>\n\nb,c>\n<b,c>\n<a,b>");
        System.out.println("有效边: " + r10.getEdges().size()
                + "，错误数: " + r10.getErrors().size()
                + "，去重数: " + r10.getDuplicateCount());
        if (r10.getEdges().size() == 2 && r10.getErrors().size() == 1
                && r10.getDuplicateCount() == 1) {
            System.out.println("通过！");
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 测试11：使用 figure1.txt 文件解析
        System.out.println("\n=== 测试11：figure1.txt 文件解析 ===");
        ParseResult r11 = DataParser.parseFile("data/figure1.txt");
        if (r11.isSuccess() && r11.getEdges().size() == 16) {
            System.out.println("通过！边数: " + r11.getEdges().size()
                    + "，顶点数: " + r11.getAllVertices().size());
            System.out.println("所有顶点: " + r11.getAllVertices());
            pass++;
        } else {
            System.out.println("失败！边数: " + r11.getEdges().size());
            if (!r11.isSuccess()) {
                for (var e : r11.getErrors()) {
                    System.out.println("  " + e);
                }
            }
            fail++;
        }

        // 测试12：null 输入
        System.out.println("\n=== 测试12：null输入 ===");
        ParseResult r12 = DataParser.parse(null);
        if (!r12.isSuccess() && !r12.getErrors().isEmpty()) {
            System.out.println("通过！错误: " + r12.getErrors().get(0));
            pass++;
        } else {
            System.out.println("失败！");
            fail++;
        }

        // 汇总
        System.out.println("\n========== 测试汇总 ==========");
        System.out.println("通过: " + pass + "，失败: " + fail);
        if (fail == 0) {
            System.out.println("全部通过！");
        }
    }
}