package test;

import io.ParseIssue;
import util.InputValidator;
import util.InputValidator.ValidationResult;

/**
 * InputValidator 校验测试，覆盖合法、非法、混合三类输入场景。
 * 校验规则与 DataParser 完全一致，对应 T-D3 交付物。
 */
public class TestInputValidator {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  InputValidator 校验测试");
        System.out.println("========================================\n");

        // ========== 一、合法输入 ==========
        System.out.println("【一、合法输入】");

        test("合法基本关系",
                "<A,B>\n<C,D>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("带注释行",
                "# 这是注释\n<A,B>\n# 另一行注释\n<C,D>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("带空行",
                "\n<A,B>\n\n<C,D>\n",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("行首尾有多余空格",
                "  <A,B>  \n   <C,D>   ",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("全角尖括号和逗号",
                "＜A，B＞\n＜C，D＞",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("名称含内部空格",
                "<MA 141,CS 225>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("自环保留为合法关系",
                "<A,A>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("区分大小写",
                "<a,B>\n<A,b>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("中文名称",
                "<高等数学,线性代数>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        // ========== 二、非法输入 ==========
        System.out.println("\n【二、非法输入】");

        test("缺少左括号",
                "A,B",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "缺少左括号");
                });

        test("缺少右括号",
                "<A,B",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "缺少右括号");
                });

        test("尖括号顺序错误",
                ">A,B<",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "尖括号顺序错误");
                });

        test("缺少逗号",
                "<AB>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "逗号");
                });

        test("起点为空",
                "<,B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "起点");
                });

        test("终点为空",
                "<A,>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "终点");
                });

        test("完全乱码行",
                "qwertyuiop",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "缺少左括号");
                });

        test("名称含非法字符（尖括号）",
                "<A<,B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "非法字符");
                });

        test("名称含非法字符（逗号）",
                "<A,,B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(), "非法字符");
                });

        test("名称含非法字符（换行符）",
                "<A\nB,C>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 2);
                    assertEqual("第1个错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("第2个错误行号", result.getErrors().get(1).getLineNumber(), 2);
                });

        test("名称中间含 Unicode 行分隔符 \\u2028",
                "<A\u2028B,C>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(),
                            "非法字符");
                });

        test("名称中间含 Unicode 行分隔符 \\u0085",
                "<A\u0085B,C>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(),
                            "非法字符");
                });

        test("名称首含 Unicode 行分隔符 \\u2028",
                "<\u2028A,B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(),
                            "非法字符");
                });

        test("名称尾含 Unicode 行分隔符 \\u2028",
                "<A,\u2028B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertContains("错误信息含关键字", result.getErrors().get(0).getMessage(),
                            "非法字符");
                });

        test("特殊空白 U+2003 起点的重复边",
                "<\u2003A,B>\n<A,B>",
                result -> {
                    assertTrue("格式合法应通过", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("警告行号", result.getWarnings().get(0).getLineNumber(), 2);
                    assertEqual("警告消息", result.getWarnings().get(0).getMessage(),
                            "重复的关系 <A,B>，已忽略");
                });

        test("特殊空白 U+2003 起点为空名称",
                "<\u2003,B>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：起点名称为空");
                });

        test("制表符不同端点不误判重复",
                "<A\tB,C>\n<A,B\tC>",
                result -> {
                    assertTrue("应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                    assertEqual("警告数", result.getWarnings().size(), 0);
                });

        test("括号内为空",
                "<>",
                result -> {
                    assertFalse("不应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误消息", result.getErrors().get(0).getMessage(),
                            "格式错误：括号内为空");
                });

        test("null 输入不崩溃",
                null,
                result -> {
                    assertTrue("null 应返回合法", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("空字符串输入不崩溃",
                "",
                result -> {
                    assertTrue("空字符串应返回合法", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("只有空格输入不崩溃",
                "   ",
                result -> {
                    assertTrue("纯空格应返回合法", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        // ========== 三、混合输入 ==========
        System.out.println("\n【三、混合输入】");

        test("重复关系产生警告",
                "<A,B>\n<A,B>",
                result -> {
                    assertTrue("格式合法应通过", result.isValid());
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("警告行号", result.getWarnings().get(0).getLineNumber(), 2);
                    assertContains("警告信息含关键字", result.getWarnings().get(0).getMessage(), "重复");
                    assertEqual("警告消息", result.getWarnings().get(0).getMessage(),
                            "重复的关系 <A,B>，已忽略");
                });

        test("合法+非法混合，错误行不影响合法行",
                "<A,B>\n乱码行\n<C,D>",
                result -> {
                    assertFalse("有错误不应通过", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 2);
                });

        test("注释+空格+合法+重复+非法混合",
                "# 注释\n  <A,B>  \n\n＜C，D＞\n<A,B>\n乱码",
                result -> {
                    assertFalse("有错误不应通过", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 1);
                    assertEqual("错误行号", result.getErrors().get(0).getLineNumber(), 6);
                    assertEqual("警告数", result.getWarnings().size(), 1);
                    assertEqual("警告行号", result.getWarnings().get(0).getLineNumber(), 5);
                });

        test("多条错误各有正确行号",
                "<A,B>\n乱码1\n<C,D>\n乱码2\n<E,F>",
                result -> {
                    assertFalse("有错误不应通过", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 2);
                    assertEqual("第1个错误行号", result.getErrors().get(0).getLineNumber(), 2);
                    assertEqual("第2个错误行号", result.getErrors().get(1).getLineNumber(), 4);
                });

        test("空文件（只有注释和空行）",
                "# 只有注释\n\n# 再一行",
                result -> {
                    assertTrue("注释和空行应返回合法", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                });

        test("自环+重复自环",
                "<A,A>\n<A,A>",
                result -> {
                    assertTrue("自环保留应通过", result.isValid());
                    assertEqual("警告数", result.getWarnings().size(), 1);
                });

        test("图1数据（15门课程）",
                "# 任务书图1\n" +
                        "<MA 140,MA 141>\n<MA 141,CS 150>\n<MA 141,CS 225>\n" +
                        "<CS 150,CS 155>\n<CS 155,CS 200>\n<CS 155,CS 225>\n" +
                        "<CS 225,CS 230>\n<CS 225,CS 300>\n<CS 225,CS 250>\n" +
                        "<CS 300,CS 301>\n<CS 300,CS 340>\n<CS 340,CS 345>\n" +
                        "<CS 340,CS 360>\n<CS 250,CS 350>\n<CS 250,CS 360>\n" +
                        "<CS 360,CS 390>",
                result -> {
                    assertTrue("图1数据应通过校验", result.isValid());
                    assertEqual("错误数", result.getErrors().size(), 0);
                    assertEqual("警告数", result.getWarnings().size(), 0);
                });

        // ========== 四、与 DataParser 一致性验证 ==========
        System.out.println("\n【四、与 DataParser 一致性验证】");

        test("内部空格名称：<MA 141,CS 225>",
                "<MA 141,CS 225>",
                result -> {
                    assertTrue("应通过校验（内部空格合法）", result.isValid());
                });

        test("前有空格：<  A,B>",
                "  <A,B>",
                result -> {
                    assertTrue("行首空格应容忍", result.isValid());
                });

        // 注：尖括号前允许有前缀字符，InputValidator 与 DataParser 行为一致，
        // 契约未要求尖括号必须在行首，故不设计“尖括号外侧非法字符”用例。

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
        void check(ValidationResult result);
    }

    private static void test(String name, String input, TestCase testCase) {
        try {
            ValidationResult result = InputValidator.validate(input);
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

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label + "：期望 true，实际 false");
        }
    }

    private static void assertFalse(String label, boolean value) {
        if (value) {
            throw new AssertionError(label + "：期望 false，实际 true");
        }
    }

    private static void assertContains(String label, String text, String keyword) {
        if (text == null || !text.contains(keyword)) {
            throw new AssertionError(label + "：期望包含 \"" + keyword + "\"，实际 \"" + text + "\"");
        }
    }
}