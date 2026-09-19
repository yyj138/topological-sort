package test;

import io.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager 文件读写测试，覆盖读写一致性、导出格式、异常处理等场景。
 * 对应 T-D2 交付物验证。
 */
public class TestFileManager {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  FileManager 文件读写测试");
        System.out.println("========================================\n");

        // 测试1：半角输入保存并读回，验证往返读写一致
        test("往返读写一致性（半角输入）", () -> {
            String content = "# 测试数据\n<a,b>\n<b,c>\n";
            File testFile = new File("test_output.txt");
            FileManager.saveFile(testFile, content);
            String readBack = FileManager.readFile(testFile);
            assertEqual("内容一致", readBack, content);
            testFile.delete();
        });

        // 测试2：导出 txt 格式
        test("导出txt格式", () -> {
            List<String> results = new ArrayList<>();
            results.add("MA 140 -> MA 141 -> CS 150 -> CS 155");
            results.add("MA 140 -> MA 141 -> CS 225 -> CS 230");

            File txtFile = new File("test_result.txt");
            FileManager.exportTxt(txtFile, results);

            String content = new String(Files.readAllBytes(txtFile.toPath()), StandardCharsets.UTF_8);
            System.out.println("         → txt内容:");
            for (String line : content.split("\n")) {
                System.out.println("           " + line);
            }
            assertTrue("含序列数", content.contains("共 2 条序列"));
            assertTrue("含序列1", content.contains("序列 1:"));
            assertTrue("含序列2", content.contains("序列 2:"));
            txtFile.delete();
        });

        // 测试3：导出 csv 格式
        test("导出csv格式（含BOM）", () -> {
            List<String> results = new ArrayList<>();
            results.add("MA 140 -> MA 141 -> CS 150");
            results.add("MA 140 -> MA 141 -> CS 225");

            File csvFile = new File("test_result.csv");
            FileManager.exportCsv(csvFile, results);

            byte[] raw = Files.readAllBytes(csvFile.toPath());
            boolean hasBom = (raw[0] == (byte) 0xEF
                    && raw[1] == (byte) 0xBB
                    && raw[2] == (byte) 0xBF);
            assertTrue("有UTF-8 BOM", hasBom);

            String content = new String(raw, StandardCharsets.UTF_8);
            System.out.println("         → csv内容:");
            for (String line : content.split("\n")) {
                System.out.println("           " + line);
            }
            assertTrue("含表头序号", content.contains("序号"));
            assertTrue("含表头序列", content.contains("拓扑排序序列"));
            csvFile.delete();
        });

        // 测试4：读取不存在的文件
        test("读取不存在的文件抛异常", () -> {
            try {
                FileManager.readFile(new File("不存在的文件.txt"));
                throw new AssertionError("应该抛出IOException");
            } catch (IOException e) {
                assertContains("异常信息", e.getMessage(), "不存在");
            }
        });

        // 测试5：传入null文件
        test("传入null文件抛异常", () -> {
            try {
                FileManager.readFile(null);
                throw new AssertionError("应该抛出IOException");
            } catch (IOException e) {
                assertContains("异常信息", e.getMessage(), "未选择");
            }
        });

        // 测试6：保存到null路径
        test("保存到null路径抛异常", () -> {
            try {
                FileManager.saveFile(null, "test");
                throw new AssertionError("应该抛出IOException");
            } catch (IOException e) {
                assertContains("异常信息", e.getMessage(), "未指定");
            }
        });

        // 测试7：导出空结果
        test("导出空结果抛异常", () -> {
            try {
                FileManager.exportTxt(new File("empty.txt"), new ArrayList<>());
                throw new AssertionError("应该抛出IOException");
            } catch (IOException e) {
                assertContains("异常信息", e.getMessage(), "没有");
            }
        });

        // 测试8：最近打开路径记录
        test("最近打开路径记录", () -> {
            File f = new File("test_path.txt");
            FileManager.saveFile(f, "test");
            FileManager.readFile(f);
            String lastPath = FileManager.getLastOpenedPath();
            String lastDir = FileManager.getLastOpenedDirectory();
            System.out.println("         → 最近路径: " + lastPath);
            System.out.println("         → 最近目录: " + lastDir);
            assertTrue("路径不为空", lastPath != null);
            assertTrue("目录不为空", lastDir != null);
            f.delete();
        });

        // 测试9：全角输入保存，读回得到半角
        test("全角输入保存后读回为半角", () -> {
            String fullWidthContent = "＜A，B＞\n＜C，D＞\n";
            File testFile = new File("test_fullwidth.txt");
            FileManager.saveFile(testFile, fullWidthContent);
            String readBack = FileManager.readFile(testFile);
            assertEqual("读回内容为半角", readBack, "<A,B>\n<C,D>\n");
            testFile.delete();
        });

        // 测试10：保存时换行统一为 \n
        test("保存时换行统一为\\n", () -> {
            String contentWithCrlf = "<a,b>\r\n<c,d>\r\n";
            File testFile = new File("test_crlf.txt");
            FileManager.saveFile(testFile, contentWithCrlf);
            String readBack = FileManager.readFile(testFile);
            assertEqual("读回内容换行为\\n", readBack, "<a,b>\n<c,d>\n");
            testFile.delete();
        });

        // 结果汇总
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
    interface TestAction {
        void run() throws Exception;
    }

    private static void test(String name, TestAction action) {
        // 每个测试前重置静态变量，避免测试用例之间互相污染
        FileManager.resetLastOpenedPathForTest();
        try {
            action.run();
            System.out.println("  [PASS] " + name);
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

    private static void assertContains(String label, String text, String keyword) {
        if (text == null || !text.contains(keyword)) {
            throw new AssertionError(label + "：期望包含 \"" + keyword + "\"，实际 \"" + text + "\"");
        }
    }
}