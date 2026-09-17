import io.DataParser;
import io.DataParser.ParseResult;

public class VerifyCurriculum {
    public static void main(String[] args) {
        System.out.println("=== 验证 figure1.txt ===");
        verify("data/figure1.txt");

        System.out.println("\n=== 验证 curriculum.txt ===");
        verify("data/curriculum.txt");
    }

    private static void verify(String filePath) {
        ParseResult result = DataParser.parseFile(filePath);

        if (!result.isSuccess()) {
            System.out.println("解析错误：");
            for (var err : result.getErrors()) {
                System.out.println("  " + err);
            }
            return;
        }

        System.out.println("解析成功！");
        System.out.println("  有效边数：" + result.getEdges().size());
        System.out.println("  自环数：" + result.getSelfLoops().size());
        System.out.println("  重复边数：" + result.getDuplicateCount());
        System.out.println("  顶点数：" + result.getAllVertices().size());
        System.out.println("  所有顶点：" + result.getAllVertices());

        // 简单检测环：Kahn 算法
        // 统计每个顶点的入度
        java.util.Map<String, Integer> inDegree = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<String>> adj = new java.util.HashMap<>();

        for (String v : result.getAllVertices()) {
            inDegree.put(v, 0);
            adj.put(v, new java.util.ArrayList<>());
        }

        for (var edge : result.getEdges()) {
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            adj.get(edge.getSource()).add(edge.getTarget());
        }

        java.util.Queue<String> queue = new java.util.LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int count = 0;
        while (!queue.isEmpty()) {
            String v = queue.poll();
            count++;
            for (String neighbor : adj.get(v)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (count == result.getAllVertices().size()) {
            System.out.println("  环检测：无环（DAG）");
        } else {
            System.out.println("  环检测：有环！出队顶点数=" + count + "，总顶点数=" + result.getAllVertices().size());
        }
    }
}