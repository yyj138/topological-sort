package test;
import io.DataParser;
import io.ParseIssue;
import io.ParseResult;
import model.Graph;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
/**
 * T‑E2：解析器容错测试
 * 用例编号 T‑E2‑01 ~ T‑E2‑16，对应异常与边界场景清单16个场景
 * 依据：接口契约.md、异常与边界场景清单.md
 * 输出：控制台同时输出，并且持久化输出到 test/test_E2_result.txt
 */
public class ParserFaultTest {
    private static final DataParser parser = new DataParser();
    private static int pass = 0;
    private static int fail = 0;
    // 双输出流：控制台 + 文件
    private static PrintStream out;
    public static void main(String[] args) {
        try {
            FileOutputStream fos = new FileOutputStream("test/test_E2_result.txt");
            out = new PrintStream(fos, true, "UTF-8");
        } catch (IOException e) {
            System.err.println("无法创建输出文件 test/test_E2_result.txt：" + e.getMessage());
            System.err.println("仅使用控制台输出！");
            out = System.out;
        }
        try {
            println("==== T‑E2 解析器容错测试开始 ====\n");
            // T‑E2‑01 场景1：缺少左括号，预期err=1 warn=0
            runCase("T‑E2‑01 缺少左括号", "a,b>", 1, 0);
            // T‑E2‑02 场景2：缺少右括号，预期err=1 warn=0
            runCase("T‑E2‑02 缺少右括号", "<a,b", 1, 0);
            // T‑E2‑03 场景3：缺少逗号，预期err=1 warn=0
            runCase("T‑E2‑03 缺少逗号", "<a b>", 1, 0);
            // T‑E2‑04 场景4：乱码行（注意：不能#开头，#是注释！预期err=1 warn=0）
            runCase("T‑E2‑04 乱码行", "@@@乱码内容$$$", 1, 0);
            // T‑E2‑05 场景5：重复边，预期err=0 warn=1
            runCase("T‑E2‑05 重复边", "<a,b>\n<a,b>", 0, 1);
            // T‑E2‑06 场景6：自环 <a,a>，预期err=0 warn=0；环检测交给CycleDetector(T‑E1)
            runCase("T‑E2‑06 自环", "<a,a>", 0, 0);
            // T‑E2‑07 场景7：含环图循环依赖 a→b b→c c→a
            // 仅做解析测试，环路检测属于算法模块T‑E1，解析无错误警告
            runCase("T‑E2‑07 含环图循环依赖", "<a,b>\n<b,c>\n<c,a>", 0, 0);
            // T‑E2‑08 场景8：空数据（完全空白输入），预期err=0 warn=0
            runCase("T‑E2‑08 空数据", "", 0, 0);
            // T‑E2‑09 场景9：全部孤立节点(无有效边)
            // 注意：解析器语法只有边，不能凭空生成孤立节点，孤立节点需要外部调用 graph.addVertex()
            runCase("T‑E2‑09 全部孤立节点(无有效边)", "#只有注释，没有任何关系\n#结束", 0, 0);
            // T‑E2‑10 场景10：大图解析(1000条边)
            // 仅验证解析大文本不崩溃；maxResults、超时、序列爆炸属于AllTopoSorts(T‑E1)
            StringBuilder bigInput = new StringBuilder();
            for (int i = 1; i <= 1000; i++) {
                bigInput.append("<n").append(i).append(",n").append(i + 1).append(">\n");
            }
            runCase("T‑E2‑10 大图解析(1000条边)", bigInput.toString(), 0, 0);
            // T‑E2‑11 场景11：超长结果(解析层)
            // 仅验证解析，枚举数量爆炸属于算法模块
            StringBuilder manyIsolated = new StringBuilder();
            for (int i = 1; i <= 12; i++) {
                manyIsolated.append("<x").append(i).append(",y").append(i).append(">\n");
            }
            runCase("T‑E2‑11 大量输入(解析层)", manyIsolated.toString(), 0, 0);
            // T‑E2‑12 场景12：注释+空行混合
            runCase("T‑E2‑12 注释空行混合", "#这是注释\n\n<a,b>\n\n#另一条注释\n<c,d>", 0, 0);
            // T‑E2‑13 场景13：全角尖括号输入
            runCase("T‑E2‑13 全角尖括号输入", "＜a,b＞", 0, 0);
            // T‑E2‑14 场景14：行首尾多余空格
            runCase("T‑E2‑14 行首尾空格", "  <a,b>  \n   <c , d>   ", 0, 0);
            // T‑E2‑15 场景15 null文本输入；文件IO异常属于FileManager，不在DataParser职责
            runCase("T‑E2‑15 null文本输入", null, 0, 0);
            // T‑E2‑16 场景16：括号内容为空 <> 和 <,>，两行均解析错误
            runCase("T‑E2‑16 括号内为空", "<>\n<,>", 2, 0);
            println("\n==== T‑E2 测试汇总 ====");
            println("通过：" + pass);
            println("失败：" + fail);
            println("总用例：" + (pass + fail));
            println("\n测试输出文件已生成：test/test_E2_result.txt");
        } finally {
            // 无论是否异常，都关闭输出流
            if (out != System.out) {
                out.close();
            }
        }
    }
    /**
     * 同时打印控制台和txt文件
     */
    private static void println(String msg) {
        System.out.println(msg);
        out.println(msg);
    }
    /**
     * 执行单条测试用例：调用parser.parse，打印errors、warnings、图统计；增加断言校验预期错误/警告数量
     *
     * @param caseName   用例名称（T‑E2‑xx）
     * @param text       待解析文本
     * @param expectErr  预期错误数量
     * @param expectWarn 预期警告数量
     */
    private static void runCase(String caseName, String text, int expectErr, int expectWarn) {
        println("【" + caseName + "】");
        try {
            ParseResult res = parser.parse(text);
            List<ParseIssue> errors = res.getErrors();
            List<ParseIssue> warnings = res.getWarnings();
            Graph g = res.getGraph();
            println("  错误数量：" + errors.size());
            for (ParseIssue e : errors) {
                String lineStr = String.format("    [行%d] ERROR：%s", e.getLineNumber(), e.getMessage());
                println(lineStr);
            }
            println("  警告数量：" + warnings.size());
            for (ParseIssue w : warnings) {
                String warnStr = String.format("    [行%d] WARN：%s", w.getLineNumber(), w.getMessage());
                println(warnStr);
            }
            String graphInfo = String.format("  Graph：节点数=%d，边数=%d", g.getVertexCount(), g.getEdgeCount());
            println(graphInfo);
            // 自动化断言比对预期值
            if (errors.size() == expectErr && warnings.size() == expectWarn) {
                pass++;
                println("  ✅用例执行完成，结果符合预期\n");
            } else {
                fail++;
                String tip = String.format(
                        "  ❌结果不匹配！预期err=%d warn=%d，实际err=%d warn=%d%n",
                        expectErr, expectWarn, errors.size(), warnings.size()
                );
                println(tip);
            }
        } catch (Exception ex) {
            println("  ❌用例崩溃！");
            ex.printStackTrace(out);
            fail++;
            println("");
        }
    }
}
