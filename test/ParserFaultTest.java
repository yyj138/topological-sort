package test;
import io.DataParser;
import io.ParseIssue;
import io.ParseResult;
import util.InputValidator;
import util.InputValidator.ValidationResult;
import java.io.FileWriter;
import java.io.IOException;
/**
 * T‑E2：解析器容错测试【E组员独立交付】
 * 文件名：ParserFaultTest.java
 * 输出记录：test/test_E2_result.txt
 * 依据：接口契约V1.0、docs/异常与边界场景清单.md V1.8
 * 说明：
 * 本测试只做内存字符串文本解析测试；
 * ⚠️文件IO(T‑E2‑15)、环检测逻辑(T‑E2‑07环判定)、大图性能、拓扑枚举不属于本类；
 * 文件IO异常放到 FileManagerTest.java；环检测、算法性能属于T‑E1。
 *
 * T‑E2‑09【全部孤立节点】说明：
 * 当前<from,to>文本语法无法只生成孤立顶点而不生成边；
 * 孤立节点只能通过Graph.addVertex() API手动构造，不属于文本解析测试，
 * 该场景的验证放在T‑E1算法测试中，本类不提供对应文本用例。
 *
 * 更新记录：
 * 2026‑09‑19：补充【首尾Unicode行分隔符】用例；一致性校验增加U+2003相关输入；优化用例命名，修复日志标题Unicode控制字符输出问题。
 * 2026‑09‑19‑rev：修复一致性校验打印未转义Unicode控制字符；移除开发调试输出文本，不改变业务逻辑。
 */
public class ParserFaultTest {
    private static int passed = 0;
    private static int failed = 0;
    private static int skipped = 0;
    // 新增：一致性校验单独统计
    private static int consPassed = 0;
    private static int consFailed = 0;
    private static final String OUTPUT_FILE = "test/test_E2_result.txt";
    private static FileWriter fileWriter;
    @FunctionalInterface
    interface ParseTestCase {
        void check(ParseResult parseResult);
    }
    public static void main(String[] args) {
        try (FileWriter fw = new FileWriter(OUTPUT_FILE, false)) {
            fileWriter = fw;
            printlnConsoleAndFile("========================================");
            printlnConsoleAndFile("      T‑E2 解析器容错测试 E组员交付");
            printlnConsoleAndFile("依据：接口契约V1.0、docs/异常与边界场景清单.md V1.8");
            printlnConsoleAndFile("========================================\n");
            // ========== T‑E2‑01 ~ T‑E2‑23 原有解析用例 ==========
            runParseCase("T‑E2‑01 缺少左括号", "a,b>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "缺少左括号 '<'");
            });
            runParseCase("T‑E2‑02 缺少右括号", "<A,B", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "缺少右括号 '>'");
            });
            runParseCase("T‑E2‑03 缺少逗号分隔符", "<AB>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "缺少逗号分隔符");
            });
            runParseCase("T‑E2‑04 乱码行无任何尖括号", "qwertyuiop", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "缺少左括号");
            });
            // T‑E2‑07：含环图【仅验证解析层正常建图，环检测逻辑归T‑E1 CycleDetector】
            runParseCase("T‑E2‑07 含环图解析层不报错", "<A,B>\n<B,C>\n<C,A>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("警告数", r.getWarnings().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 3);
                assertEqual("边数", r.getGraph().getEdgeCount(), 3);
            });
            runParseCase("T‑E2‑05 重复边 <A,B>出现两次", "<A,B>\n<A,B>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("警告数", r.getWarnings().size(), 1);
                assertEqual("边数量", r.getGraph().getEdgeCount(), 1);
                ParseIssue warn = r.getWarnings().get(0);
                assertEqual("警告行号", warn.getLineNumber(), 2);
                assertContains("警告消息", warn.getMessage(), "重复的关系");
            });
            runParseCase("T‑E2‑06 自环 <A,A>（解析层不报错，交给环检测）", "<A,A>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("警告数", r.getWarnings().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 1);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑08 null输入（空图无解析错误）", null, r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 0);
                assertEqual("边数", r.getGraph().getEdgeCount(), 0);
            });
            runParseCase("T‑E2‑12 注释+空行混合输入", "#注释\n\n<A,B>\n#注释", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑13 全角尖括号全角逗号输入", "＜A，B＞", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑14 行首尾多余空格", "  <A,B>  ", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑16 括号内为空 <>", "<>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "括号内为空");
            });
            runParseCase("T‑E2‑17 尖括号顺序错误 >A,B<", ">A,B<", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "尖括号顺序错误");
            });
            runParseCase("T‑E2‑18 起点名称为空 <,B>", "<,B>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称为空");
            });
            runParseCase("T‑E2‑19 终点名称为空 <A,>", "<A,>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "终点名称为空");
            });
            runParseCase("T‑E2‑20 名称包含非法分隔符 <A<,B>", "<A<,B>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            // T‑E2‑21：Unicode行分隔符出现在【名称中间】
            runParseCase("T‑E2‑21 名称中间含LINE SEPARATOR U+2028", "<A\u2028B,C>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            runParseCase("T‑E2‑21b 名称中间含NEL U+0085", "<A\u0085B,C>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            runParseCase("T‑E2‑21c 名称中间含PARAGRAPH SEPARATOR U+2029", "<A\u2029B,C>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            // ========= 新增：D修复后，测试【首尾位置Unicode行分隔符】（原来会漏检，现在必须报错） =========
            runParseCase("T‑E2‑21d 起点名称首部含U+2028 <U+2028A,B>", "<\u2028A,B>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            runParseCase("T‑E2‑21e 终点名称尾部含U+2028 <A,BU+2028>", "<A,B\u2028>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "终点名称包含非法字符");
            });
            runParseCase("T‑E2‑21f 起点首部含U+0085 <U+0085A,B>", "<\u0085A,B>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称包含非法字符");
            });
            runParseCase("T‑E2‑21g 终点尾部含U+2029 <A,BU+2029>", "<A,B\u2029>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "终点名称包含非法字符");
            });
            /*
             * T‑E2‑22：Graph名称校验兜底异常【条件性用例】
             * 业务含义：模拟containsFormatDelimiter漏检，addEdge抛IAE被DataParser捕获转为ParseIssue
             * 现实：D解析层提前拦截非法字符，很难黑盒触发该catch分支；
             * 场景清单备注：containsFormatDelimiter优先拦截，try‑catch仅作为防护兜底。
             * 行为：
             *   输出消息="名称不符合图结构规则" → PASS（真正触发兜底catch）
             *   输出消息="起点名称包含非法字符" → SKIP：解析提前拦截，未触达兜底，不属于bug
             */
            runConditionalCase("T‑E2‑22 Graph名称校验兜底异常（防护兜底）", "<\u0085OK,XYZ>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                String msg = iss.getMessage();
                if(msg.contains("名称不符合图结构规则")){
                    //成功命中兜底catch
                    return CaseResult.PASS;
                }else if(msg.contains("起点名称包含非法字符")){
                    //解析层提前拦截，没有走到addEdge的catch，预期SKIP
                    return CaseResult.SKIP;
                }else{
                    throw new AssertionError(String.format("消息不符合预期：text=[%s]",msg));
                }
            });
            // ========== T‑E2‑23 U+2003 EM‑SPACE Unicode空白（合法，strip清除，不报错） ==========
            runParseCase("T‑E2‑23 起点前带U+2003空白 <U+2003A,B>", "<\u2003A,B>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑23b 终点后带U+2003空白 <A,U+2003B>", "<A,\u2003B>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            runParseCase("T‑E2‑23c strip后起点为空 <U+2003,B>", "<\u2003,B>", r -> {
                assertEqual("错误数量", r.getErrors().size(), 1);
                ParseIssue iss = r.getErrors().get(0);
                assertEqual("行号", iss.getLineNumber(), 1);
                assertContains("错误消息", iss.getMessage(), "起点名称为空");
            });
            // 正向契约用例：节点名称带内部空格（课程名 MA 141、CS 225）
            runParseCase("合法：节点名称带内部空格 <MA 141,CS 225>", "<MA 141,CS 225>", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 2);
                assertEqual("边数", r.getGraph().getEdgeCount(), 1);
            });
            // 混合输入：合法行+错误行，验收强制要求行号定位
            runParseCase("混合输入：合法、错误行，校验行号定位", "<A,B>\n乱码内容\n<C,D>", r -> {
                assertEqual("错误数", r.getErrors().size(), 1);
                ParseIssue err = r.getErrors().get(0);
                assertEqual("错误行号必须等于2", err.getLineNumber(), 2);
                assertEqual("节点数", r.getGraph().getVertexCount(), 4);
                assertEqual("边数", r.getGraph().getEdgeCount(), 2);
            });
            runParseCase("空字符串输入", "", r -> {
                assertEqual("错误数", r.getErrors().size(), 0);
                assertEqual("节点数", r.getGraph().getVertexCount(), 0);
            });
            // =========【全部T‑E2‑xx跑完之后，再执行一致性校验】=========
            printlnConsoleAndFile("\n---------- 一致性校验：DataParser vs InputValidator ----------");
            // 扩充一致性校验数组，增加U+2003相关用例
            String[] consistencyInputs = {
                    "<A,B>",
                    "<,B>",
                    ">A,B<",
                    "a,b>",
                    "<A,A>",
                    "<A,B>\n<A,B>",
                    "<A,A>\n<A,A>",
                    "＜A，B＞",
                    "#abc\n  <X,Y>  ",
                    // 新增U+2003 Unicode空白
                    "<\u2003A,B>",
                    "<A,\u2003B>",
                    "<\u2003,B>",
                    "<A,\u2003>",
                    // 新增首尾Unicode行分隔符一致性校验
                    "<\u2028A,B>",
                    "<A,B\u2029>"
            };
            for (String inText : consistencyInputs) {
                checkParserValidatorConsistency(inText);
            }
            // ====== 分开输出两套统计：主解析用例、一致性校验 ======
            printlnConsoleAndFile("\n========================================");
            printlnConsoleAndFile("【主解析用例统计】");
            printlnConsoleAndFile(String.format("PASS = %d ，SKIP(兜底未触发)=%d，FAIL = %d", passed, skipped, failed));
            printlnConsoleAndFile("【DataParser‑InputValidator一致性校验统计】");
            printlnConsoleAndFile(String.format("PASS = %d，FAIL = %d", consPassed, consFailed));
            printlnConsoleAndFile("========================================");
            if (failed > 0 || consFailed >0) {
                printlnConsoleAndFile("⚠️存在失败用例，请D检查DataParser/InputValidator！");
            } else {
                printlnConsoleAndFile("✅全部业务解析容错用例通过；SKIP代表兜底防护未触发（黑盒无法复现，非缺陷）");
            }
            printlnConsoleAndFile("输出记录已保存到 test/test_E2_result.txt，可以提交D用于T‑D7测试报告");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    enum CaseResult {
        PASS,SKIP
    }
    /**条件用例，支持SKIP状态，专门用于T‑E2‑22兜底场景*/
    private static void runConditionalCase(String caseName, String inputText, java.util.function.Function<ParseResult,CaseResult> tc){
        DataParser parser = new DataParser();
        try {
            ParseResult res = parser.parse(inputText);
            CaseResult cr = tc.apply(res);
            if(cr == CaseResult.PASS){
                printlnConsoleAndFile(String.format("[PASS] %s", caseName));
                passed++;
            }else if(cr == CaseResult.SKIP){
                printlnConsoleAndFile(String.format("[SKIP‑兜底未触发] %s", caseName));
                skipped++;
            }
            //打印错误、警告详情
            for (ParseIssue e : res.getErrors()) {
                printlnConsoleAndFile(String.format("      ERR: %s", escapeUnicode(e.toString())));
            }
            for (ParseIssue w : res.getWarnings()) {
                printlnConsoleAndFile(String.format("      WARN: %s", escapeUnicode(w.toString())));
            }
            if (res.getGraph().getVertexCount() > 0) {
                printlnConsoleAndFile(String.format("      Graph info: vertex=%d edge=%d",
                        res.getGraph().getVertexCount(), res.getGraph().getEdgeCount()));
            }
        } catch (AssertionError ae) {
            printlnConsoleAndFile(String.format("[FAIL] %s —— %s", caseName, ae.getMessage()));
            failed++;
        } catch (Exception ex) {
            printlnConsoleAndFile(String.format("[ERROR] %s ——程序抛出异常 %s", caseName, ex.getMessage()));
            failed++;
        }
    }
    /** 运行普通解析用例，同时输出控制台+写入文件 */
    private static void runParseCase(String caseName, String inputText, ParseTestCase tc) {
        DataParser parser = new DataParser();
        try {
            ParseResult res = parser.parse(inputText);
            tc.check(res);
            printlnConsoleAndFile(String.format("[PASS] %s", caseName));
            //打印错误、警告详情
            for (ParseIssue e : res.getErrors()) {
                printlnConsoleAndFile(String.format("      ERR: %s", escapeUnicode(e.toString())));
            }
            for (ParseIssue w : res.getWarnings()) {
                printlnConsoleAndFile(String.format("      WARN: %s", escapeUnicode(w.toString())));
            }
            if (res.getGraph().getVertexCount() > 0) {
                printlnConsoleAndFile(String.format("      Graph info: vertex=%d edge=%d",
                        res.getGraph().getVertexCount(), res.getGraph().getEdgeCount()));
            }
            passed++;
        } catch (AssertionError ae) {
            printlnConsoleAndFile(String.format("[FAIL] %s —— %s", caseName, ae.getMessage()));
            failed++;
        } catch (Exception ex) {
            printlnConsoleAndFile(String.format("[ERROR] %s ——程序抛出异常 %s", caseName, ex.getMessage()));
            failed++;
        }
    }
    /**
     * 校验同一个输入：DataParser与InputValidator，同时比对【行号+消息】，不再只比对消息文本
     */
    private static void checkParserValidatorConsistency(String input) {
        DataParser dp = new DataParser();
        ParseResult pr = dp.parse(input);
        ValidationResult vr = InputValidator.validate(input);
        // key格式：行号:消息，同时校验行号+消息
        var dpErrKeyList = pr.getErrors().stream()
                .map(e -> e.getLineNumber() + ":" + e.getMessage()).toList();
        var ivErrKeyList = vr.getErrors().stream()
                .map(e -> e.getLineNumber() + ":" + e.getMessage()).toList();
        var dpWarnKeyList = pr.getWarnings().stream()
                .map(w -> w.getLineNumber() + ":" + w.getMessage()).toList();
        var ivWarnKeyList = vr.getWarnings().stream()
                .map(w -> w.getLineNumber() + ":" + w.getMessage()).toList();
        if (dpErrKeyList.equals(ivErrKeyList) && dpWarnKeyList.equals(ivWarnKeyList)) {
            // =========【修改点】对输入字符串执行escapeUnicode，防止控制字符撕裂日志行 =========
            printlnConsoleAndFile(String.format("[PASS]一致性校验输入=\"%s\"", escapeUnicode(shortStr(input))));
            consPassed++;
        } else {
            printlnConsoleAndFile(String.format("[FAIL]一致性校验输入=\"%s\"", escapeUnicode(shortStr(input))));
            printlnConsoleAndFile("      DataParser ERR(key):" + dpErrKeyList);
            printlnConsoleAndFile("      InputValidator ERR(key):" + ivErrKeyList);
            printlnConsoleAndFile("      DataParser WARN(key):" + dpWarnKeyList);
            printlnConsoleAndFile("      InputValidator WARN(key):" + ivWarnKeyList);
            consFailed++;
        }
    }
    private static String shortStr(String s) {
        if (s == null) return "null";
        String t = s.replace("\n", "\\n");
        if (t.length() > 80) return t.substring(0, 80) + "...";
        return t;
    }
    /**
     * 转义Unicode行分隔符，避免输出txt文件发生异常换行
     * \u0085 NEL  / \u2028 LINE SEPARATOR / \u2029 PARAGRAPH SEPARATOR / \u2003 EM‑SPACE
     */
    private static String escapeUnicode(String raw){
        if(raw == null) return null;
        return raw
                .replace("\u0085","\\u0085")
                .replace("\u2028","\\u2028")
                .replace("\u2029","\\u2029")
                .replace("\u2003","\\u2003");
    }
    //断言工具
    private static void assertEqual(String label, Object actual, Object expect) {
        if (!actual.equals(expect)) {
            throw new AssertionError(String.format("%s : expect=[%s] actual=[%s]", label, expect, actual));
        }
    }
    private static void assertContains(String label, String text, String keyword) {
        if (text == null || !text.contains(keyword)) {
            throw new AssertionError(String.format("%s : text=[%s] must contains=[%s]", label, text, keyword));
        }
    }
    /**
     * 内部捕获IOException，包装运行时异常，消除lambda受检异常编译报错
     */
    private static void printlnConsoleAndFile(String line) {
        System.out.println(line);
        if (fileWriter != null) {
            try {
                fileWriter.write(line);
                fileWriter.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException("写入测试输出文件test/test_E2_result.txt发生IO异常", e);
            }
        }
    }
}
