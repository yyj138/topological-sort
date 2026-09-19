package test;

import io.DataParser;
import io.ParseResult;
import io.ParseIssue;
import model.Graph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 验证 figure1.txt 和 curriculum.txt 数据文件的正确性。
 * <p>
 * 对每个文件执行以下检查：
 * <ul>
 * <li>DataParser 解析无错误、无警告</li>
 * <li>节点数和边数与文件头声明一致</li>
 * <li>Kahn 环检测确认无环（DAG）</li>
 * </ul>
 * </p>
 */
public class TestCurriculumData {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  数据文件验证");
        System.out.println("========================================\n");

        // ========== 验证 figure1.txt ==========
        verify("data/figure1.txt", 15, 16);

        // ========== 验证 curriculum.txt ==========
        verify("data/curriculum.txt", 43, 85);

        // ========== 结果汇总 ==========
        System.out.println("\n========================================");
        System.out.println("  验证结果汇总：通过 " + passed + "，失败 " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.out.println("存在失败项，请检查！");
        } else {
            System.out.println("全部通过！");
        }
    }

    /**
     * 验证单个数据文件。
     *
     * @param filePath      文件路径
     * @param expectedNodes 期望节点数
     * @param expectedEdges 期望边数
     */
    private static void verify(String filePath, int expectedNodes, int expectedEdges) {
        System.out.println("=== 验证 " + filePath + " ===\n");

        // 1. 读取文件
        String text;
        try {
            text = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("  [FAIL] 文件读取失败：" + e.getMessage());
            failed++;
            return;
        }

        // 2. DataParser 解析
        DataParser parser = new DataParser();
        ParseResult result = parser.parse(text);

        // 3. 检查解析错误
        check("解析无错误", result.getErrors().isEmpty(), filePath);
        if (!result.getErrors().isEmpty()) {
            for (ParseIssue issue : result.getErrors()) {
                System.out.println("         → 行号 " + issue.getLineNumber()
                        + "：" + issue.getMessage());
            }
        }

        // 4. 检查解析警告
        check("解析无警告（无重复边）", result.getWarnings().isEmpty(), filePath);
        if (!result.getWarnings().isEmpty()) {
            for (ParseIssue issue : result.getWarnings()) {
                System.out.println("         → 行号 " + issue.getLineNumber()
                        + "：" + issue.getMessage());
            }
        }

        // 5. 获取图
        Graph graph = result.getGraph();
        int actualNodes = graph.getVertexCount();
        int actualEdges = graph.getEdgeCount();

        System.out.println("         → 节点数：" + actualNodes
                + "（期望 " + expectedNodes + "）");
        System.out.println("         → 边数：" + actualEdges
                + "（期望 " + expectedEdges + "）");

        check("节点数正确", actualNodes == expectedNodes, filePath);
        check("边数正确", actualEdges == expectedEdges, filePath);

        // 6. Kahn 环检测
        boolean hasCycle = kahnCycleDetection(graph);
        check("环检测：无环（DAG）", !hasCycle, filePath);

        System.out.println();
    }

    /**
     * 使用 Kahn 算法检测图中是否有环。
     * <p>
     * 统计入度为 0 的节点入队处理，若出队数 < 总节点数则说明有环。
     * 不修改原图的入度，使用局部入度副本。
     * </p>
     *
     * @param graph 要检测的图
     * @return true 表示有环，false 表示无环
     */
    private static boolean kahnCycleDetection(Graph graph) {
        int vertexCount = graph.getVertexCount();
        if (vertexCount == 0) {
            return false;
        }

        // 建立局部入度表（不修改原图）
        List<String> vertices = graph.getVertexNames();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (String v : vertices) {
            inDegree.put(v, 0);
            adjacency.put(v, new ArrayList<>());
        }

        for (String v : vertices) {
            List<String> successors = graph.getSuccessors(v);
            for (String s : successors) {
                inDegree.put(s, inDegree.get(s) + 1);
                adjacency.get(v).add(s);
            }
        }

        // 入度为 0 的节点入队
        Queue<String> queue = new LinkedList<>();
        for (String v : vertices) {
            if (inDegree.get(v) == 0) {
                queue.add(v);
            }
        }

        // 处理队列
        int processed = 0;
        while (!queue.isEmpty()) {
            String v = queue.poll();
            processed++;
            for (String neighbor : adjacency.get(v)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // 出队数 < 总节点数则有环
        if (processed < vertexCount) {
            System.out.println("         → 环检测：有环！出队 " + processed
                    + " 个，总节点 " + vertexCount + " 个");
            return true;
        }

        System.out.println("         → 环检测：无环（出队 " + processed
                + " = 总节点 " + vertexCount + "）");
        return false;
    }

    /**
     * 检查单个条件。
     */
    private static void check(String name, boolean condition, String context) {
        if (condition) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.out.println("  [FAIL] " + name);
            failed++;
        }
    }
}