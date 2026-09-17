import io.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TestFileManager {
    public static void main(String[] args) {
        int pass = 0;
        int fail = 0;

        // 测试1：保存文件并读回，验证往返读写一致
        System.out.println("=== 测试1：往返读写一致性 ===");
        try {
            String content = "# 测试数据\n<a,b>\n<b,c>\n";
            File testFile = new File("test_output.txt");
            FileManager.saveFile(testFile, content);
            String readBack = FileManager.readFile(testFile);
            if (content.equals(readBack)) {
                System.out.println("通过！写入和读回内容一致");
                pass++;
            } else {
                System.out.println("失败！内容不一致");
                fail++;
            }
            testFile.delete();
        } catch (IOException e) {
            System.out.println("失败！异常: " + e.getMessage());
            fail++;
        }

        // 测试2：导出 txt 格式
        System.out.println("\n=== 测试2：导出txt ===");
        try {
            List<String> results = new ArrayList<>();
            results.add("MA 140 -> MA 141 -> CS 150 -> CS 155");
            results.add("MA 140 -> MA 141 -> CS 225 -> CS 230");

            File txtFile = new File("test_result.txt");
            FileManager.exportTxt(txtFile, results);

            String content = new String(Files.readAllBytes(txtFile.toPath()), StandardCharsets.UTF_8);
            System.out.println("txt内容:\n" + content);

            if (content.contains("共 2 条序列") && content.contains("序列 1:")
                    && content.contains("序列 2:")) {
                System.out.println("通过！");
                pass++;
            } else {
                System.out.println("失败！");
                fail++;
            }
            txtFile.delete();
        } catch (IOException e) {
            System.out.println("失败！异常: " + e.getMessage());
            fail++;
        }

        // 测试3：导出 csv 格式
        System.out.println("\n=== 测试3：导出csv ===");
        try {
            List<String> results = new ArrayList<>();
            results.add("MA 140 -> MA 141 -> CS 150");
            results.add("MA 140 -> MA 141 -> CS 225");

            File csvFile = new File("test_result.csv");
            FileManager.exportCsv(csvFile, results);

            String content = new String(Files.readAllBytes(csvFile.toPath()), StandardCharsets.UTF_8);
            System.out.println("csv内容:\n" + content);

            // 检查是否有 BOM（前3字节）和表头
            byte[] raw = Files.readAllBytes(csvFile.toPath());
            boolean hasBom = (raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF);

            if (hasBom && content.contains("序号") && content.contains("拓扑排序序列")) {
                System.out.println("通过！有 BOM，Excel 可正常打开");
                pass++;
            } else {
                System.out.println("失败！BOM: " + hasBom);
                fail++;
            }
            csvFile.delete();
        } catch (IOException e) {
            System.out.println("失败！异常: " + e.getMessage());
            fail++;
        }

        // 测试4：读取不存在的文件
        System.out.println("\n=== 测试4：读取不存在的文件 ===");
        try {
            FileManager.readFile(new File("不存在的文件.txt"));
            System.out.println("失败！应该抛出异常");
            fail++;
        } catch (IOException e) {
            System.out.println("通过！异常: " + e.getMessage());
            pass++;
        }

        // 测试5：传入 null 文件
        System.out.println("\n=== 测试5：传入null文件 ===");
        try {
            FileManager.readFile(null);
            System.out.println("失败！应该抛出异常");
            fail++;
        } catch (IOException e) {
            System.out.println("通过！异常: " + e.getMessage());
            pass++;
        }

        // 测试6：导出空结果
        System.out.println("\n=== 测试6：导出空结果 ===");
        try {
            FileManager.exportTxt(new File("empty.txt"), new ArrayList<>());
            System.out.println("失败！应该抛出异常");
            fail++;
        } catch (IOException e) {
            System.out.println("通过！异常: " + e.getMessage());
            pass++;
        }

        // 测试7：最近打开路径记录
        System.out.println("\n=== 测试7：最近打开路径记录 ===");
        try {
            File f = new File("test_path.txt");
            FileManager.saveFile(f, "test");
            FileManager.readFile(f);
            String lastPath = FileManager.getLastOpenedPath();
            String lastDir = FileManager.getLastOpenedDirectory();
            System.out.println("最近路径: " + lastPath);
            System.out.println("最近目录: " + lastDir);
            if (lastPath != null && lastDir != null) {
                System.out.println("通过！");
                pass++;
            } else {
                System.out.println("失败！");
                fail++;
            }
            f.delete();
        } catch (IOException e) {
            System.out.println("失败！异常: " + e.getMessage());
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