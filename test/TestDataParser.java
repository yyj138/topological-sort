package test;

import io.DataParser;
import io.ParseIssue;
import io.ParseResult;
import model.Graph;

/**
 * DataParser 容错测试，覆盖合法、非法、混合三类输入场景。
 * 对应 T-D1 交付物"容错用例运行结果"，同时供 T-E2 参考。
 */
public class TestDataParser {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  DataParser 容错测试");
        System.out.println("========================================\n");

        // ========== 一、合法输入 ==========
        System.out.println("【一、合法输入】");

        test("合法基本关系",
                "<A,B>\n<C,D>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("带注释行",
                "# 这是注释\n<A,B>\n# 另一行注释\n<C,D>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("带空行",
                "\n<A,B>\n\n<C,D>\n",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("行首尾有多余空格",
                "  <A,B>  \n   <C,D>   ",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("全角尖括号和逗号",
                "＜A，B＞\n＜C，D＞",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("名称含内部空格",
                "<MA 141,CS 225>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 2);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 1);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("自环保留为正常边",
                "<A,A>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 1);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 1);
                    assertEqual("A入度", result.getGraph().getInDegree("A"), 1);
                    assertEqual("A出度", result.getGraph().getOutDegree("A"), 1);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("区分大小写",
                "<a,B>\n<A,b>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        // ========== 二、非法输入（细分报错消息） ==========
        System.out.println("\n【二、非法输入】");

        test("缺少左括号",
                "A,B",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：缺少左括号 '<'");
                });

        test("缺少右括号",
                "<A,B",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：缺少右括号 '>'");
                });

        test("尖括号顺序错误",
                ">A,B<",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：尖括号顺序错误");
                });

        test("括号内为空",
                "<>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：括号内为空");
                });

        test("缺少逗号",
                "<AB>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：缺少逗号分隔符");
                });

        test("起点为空",
                "<,B>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：起点名称为空");
                });

        test("终点为空",
                "<A,>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：终点名称为空");
                });

        test("名称含非法字符（尖括号）",
                "<A<,B>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误消息", result.getErrors().get(0).getMessage(),
                            "起点名称包含非法字符");
                });

        test("名称含非法字符（逗号）",
                "<A,,B>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误消息", result.getErrors().get(0).getMessage(),
                            "终点名称包含非法字符");
                });

        test("名称含 Unicode 行分隔符 \\u2028",
                "<A\u2028B,C>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误消息", result.getErrors().get(0).getMessage(),
                            "起点名称包含非法字符");
                });

        test("名称含 Unicode 行分隔符 \\u0085",
                "<A\u0085B,C>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误消息", result.getErrors().get(0).getMessage(),
                            "起点名称包含非法字符");
                });

        test("特殊空白 U+2003 起点的重复边",
                "<\u2003A,B>\n<A,B>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 2);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 1);
                    assertEqual("错误数", result.getErrors().size(), 0);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("警告行号", result.getWarnings().get(0).getLineNumber(), 2);
                    assertEqual("警告消息", result.getWarnings().get(0).getMessage(),
                            "重复的关系 <A,B>，已忽略");
                });

        test("特殊空白 U+2003 起点为空名称",
                "<\u2003,B>",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：起点名称为空");
                });

        test("完全乱码行",
                "qwertyuiop",
                result -> {
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：缺少左括号 '<'");
                });

        test("null 输入不崩溃",
                null,
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 0);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 0);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("空字符串输入不崩溃",
                "",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 0);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 0);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("只有空格输入不崩溃",
                "   ",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 0);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 0);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        // ========== 三、混合输入 ==========
        System.out.println("\n【三、混合输入】");

        test("重复边自动去重并产生警告",
                "<A,B>\n<A,B>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 2);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 1);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("错误数", result.getErrors().size(), 0);
                    assertEqual("警告消息", result.getWarnings().get(0).getMessage(),
                            "重复的关系 <A,B>，已忽略");
                });

        test("合法+非法混合，错误行不影响合法行",
                "<A,B>\n乱码行\n<C,D>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 2);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：缺少左括号 '<'");
                });

        test("注释+空格+合法+重复+非法混合",
                "# 注释\n  <A,B>  \n\n＜C，D＞\n<A,B>\n乱码",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 4);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 2);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 6);
                });

        test("多条错误各有正确行号",
                "<A,B>\n乱码1\n<C,D>\n乱码2\n<E,F>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 6);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 3);
                    assertEqual("错误数", result.getErrors().size(), 2);
                    assertEqual("第1个错误行号", result.getErrors().get(0).getLineNumber(), 2);
                    assertEqual("第2个错误行号", result.getErrors().get(1).getLineNumber(), 4);
                });

        test("空文件（只有注释和空行）",
                "# 只有注释\n\n# 再一行",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 0);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 0);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("自环+重复自环",
                "<A,A>\n<A,A>",
                result -> {
                    assertEqual("节点数", result.getGraph().getVertexCount(), 1);
                    assertEqual("边数", result.getGraph().getEdgeCount(), 1);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                });

        test("图1数据模拟（15门课程核心关系）",
                "# 任务书图1：学生选课先修后修关系图\n" +
                        "# 共15门课程，16条先修关系\n" +
                        "<MA 140,MA 141>\n" +
                        "<MA 141,CS 150>\n" +
                        "<MA 141,CS 225>\n" +
                        "<CS 150,CS 155>\n" +
                        "<CS 155,CS 200>\n" +
                        "<CS 155,CS 225>\n" +
                        "<CS 225,CS 230>\n" +
                        "<CS 225,CS 300>\n" +
                        "<CS 225,CS 250>\n" +
                        "<CS 300,CS 301>\n" +
                        "<CS 300,CS 340>\n" +
                        "<CS 340,CS 345>\n" +
                        "<CS 340,CS 360>\n" +
                        "<CS 250,CS 350>\n" +
                        "<CS 250,CS 360>\n" +
                        "<CS 360,CS 390>",
                result -> {
                    Graph g = result.getGraph();
                    assertEqual("节点数", g.getVertexCount(), 15);
                    assertEqual("边数", g.getEdgeCount(), 16);
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        // ========== 结果汇总 ==========
        System.out.println("\n========================================");
        System.out.println("  测试结果汇总：通过 " + passed + "，失败 " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.out.println("存在失败用例，请检查！");
        } else {
            System.out.println("全部通过！");
        }
    }

    // ========== 测试工具方法 ==========

    @FunctionalInterface
    interface TestCase {
        void check(ParseResult result);
    }

    private static void test(String name, String input, TestCase testCase) {
        DataParser parser = new DataParser();
        try {
            ParseResult result = parser.parse(input);
            testCase.check(result);
            System.out.println("  [PASS] " + name);
            if (!result.getErrors().isEmpty()) {
                for (ParseIssue issue : result.getErrors()) {
                    System.out.println("         → 行号 " + issue.getLineNumber()
                            + "：" + issue.getMessage());
                }
            }
            if (!result.getWarnings().isEmpty()) {
                for (ParseIssue issue : result.getWarnings()) {
                    System.out.println("         → 行号 " + issue.getLineNumber()
                            + "：" + issue.getMessage());
                }
            }
            Graph g = result.getGraph();
            if (g.getVertexCount() > 0) {
                System.out.println("         → 图信息：节点 " + g.getVertexCount()
                        + "，边 " + g.getEdgeCount());
            }
            passed++;
        } catch (AssertionError e) {
            System.out.println("  [FAIL] " + name + " — " + e.getMessage());
            failed++;
        } catch (Exception e) {
            System.out.println("  [ERROR] " + name + " — " + e.getMessage());
            failed++;
        }
    }

    private static void assertEqual(String label, Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError(label + "：期望 " + expected + "，实际 " + actual);
        }
    }

    private static void assertContains(String label, String text, String keyword) {
        if (text == null || !text.contains(keyword)) {
            throw new AssertionError(label + "：期望包含 \"" + keyword + "\"，实际 \"" + text + "\"");
        }
    }
}