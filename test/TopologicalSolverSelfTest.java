import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import algorithm.TopoResult;
import algorithm.TopologicalSolver;
import model.Graph;

/** A2 作者自测及调用示例；可选参数为 D 提供的图 1 文件，不代替 E 的独立测试。 */
public final class TopologicalSolverSelfTest {
    private static int passed;

    private TopologicalSolverSelfTest() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("用法：TopologicalSolverSelfTest [图1文件路径]");
        }
        run("empty graph succeeds with empty order", TopologicalSolverSelfTest::emptyGraph);
        run("single vertex", TopologicalSolverSelfTest::singleVertex);
        run("chain with reversed vertex insertion", TopologicalSolverSelfTest::chain);
        run("diamond dependencies", TopologicalSolverSelfTest::diamond);
        run("design example includes isolated vertex", TopologicalSolverSelfTest::designExample);
        run("all isolated vertices keep insertion order", TopologicalSolverSelfTest::isolatedVertices);
        run("disconnected DAG components", TopologicalSolverSelfTest::disconnectedDag);
        run("duplicate edge is counted once", TopologicalSolverSelfTest::duplicateEdge);
        run("self-loop returns cycle and empty order", TopologicalSolverSelfTest::selfLoop);
        run("two-vertex cycle", TopologicalSolverSelfTest::twoVertexCycle);
        run("longer cycle with downstream vertex", TopologicalSolverSelfTest::longerCycle);
        run("cycle discards an already processed prefix", TopologicalSolverSelfTest::partialOrder);
        run("Chinese names, internal spaces and case", TopologicalSolverSelfTest::names);
        run("repeated calls and solver reuse preserve graphs", TopologicalSolverSelfTest::reuse);
        run("result is immutable and independent of later calls", TopologicalSolverSelfTest::resultSnapshot);
        run("null graph is rejected explicitly", TopologicalSolverSelfTest::nullGraph);
        run("all 512 three-vertex directed graphs checked by permutations", TopologicalSolverSelfTest::exhaustiveSmallGraphs);
        run("1000-vertex chain correctness", TopologicalSolverSelfTest::longChain);
        if (args.length == 1) {
            Graph figure1 = readFigure1(Path.of(args[0]));
            run("Figure 1 from D: all 15 vertices and 16 edge constraints", () -> figure1(figure1));
        } else {
            System.out.println("Figure 1 verification: NOT RUN (pass the D5 file path to include it).");
        }
        System.out.println("PASS: " + passed + "/" + (args.length == 1 ? 19 : 18) + " cases");
    }

    private static void emptyGraph() {
        TopoResult result = solve(new Graph());
        equal(false, result.hasCycle());
        equal(List.of(), result.getOrder());
        System.out.println("Empty graph: order=" + result.getOrder() + ", hasCycle=" + result.hasCycle());
    }

    private static void singleVertex() {
        Graph graph = new Graph();
        graph.addVertex("A");
        equal(List.of("A"), solve(graph).getOrder());
    }

    private static void chain() {
        Graph graph = new Graph();
        for (String name : List.of("C", "B", "A")) {
            graph.addVertex(name);
        }
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        equal(List.of("A", "B", "C"), solve(graph).getOrder());
    }

    private static void diamond() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        // 通用检查验证全部边，而非只与一条人工序列比较。
        equal(false, solve(graph).hasCycle());
    }

    private static void designExample() {
        Graph graph = new Graph();
        for (String name : List.of("A", "B", "C", "D", "E")) {
            graph.addVertex(name);
        }
        graph.addEdge("A", "C");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
        TopoResult result = solve(graph);
        equal(List.of("A", "B", "E", "C", "D"), result.getOrder());
        System.out.println("Hand-built DAG (not Figure 1): order=" + result.getOrder()
                + ", hasCycle=" + result.hasCycle());
    }

    private static void isolatedVertices() {
        Graph graph = new Graph();
        for (String name : List.of("Z", "A", "M")) {
            graph.addVertex(name);
        }
        equal(List.of("Z", "A", "M"), solve(graph).getOrder());
    }

    private static void disconnectedDag() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("C", "D");
        graph.addVertex("E");
        equal(false, solve(graph).hasCycle());
    }

    private static void duplicateEdge() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        equal(false, graph.addEdge("A", "B"));
        equal(List.of("A", "B"), solve(graph).getOrder());
    }

    private static void selfLoop() {
        Graph graph = new Graph();
        graph.addEdge("A", "A");
        assertCycle(solve(graph));
    }

    private static void twoVertexCycle() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        assertCycle(solve(graph));
    }

    private static void longerCycle() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");
        graph.addEdge("C", "D");
        assertCycle(solve(graph));
    }

    private static void partialOrder() {
        Graph graph = new Graph();
        graph.addEdge("Start", "Finish");
        graph.addEdge("X", "Y");
        graph.addEdge("Y", "X");
        graph.addVertex("Isolated");
        TopoResult result = solve(graph);
        assertCycle(result);
        System.out.println("DAG plus cyclic component: order=" + result.getOrder()
                + ", hasCycle=" + result.hasCycle());
    }

    private static void names() {
        Graph graph = new Graph();
        graph.addEdge(" 高等数学 ", "CS 225");
        graph.addEdge("CS 225", "a");
        graph.addVertex("A");
        TopoResult result = solve(graph);
        equal(4, result.getOrder().size());
        equal(true, result.getOrder().contains("高等数学"));
        equal(true, result.getOrder().contains("CS 225"));
    }

    private static void reuse() {
        TopologicalSolver solver = new TopologicalSolver();
        Graph dag = new Graph();
        dag.addEdge("A", "B");
        dag.addVertex("C");
        Graph cycle = new Graph();
        cycle.addEdge("X", "X");
        List<String> expected = solve(dag, solver).getOrder();
        for (int i = 0; i < 3; i++) {
            assertCycle(solve(cycle, solver));
            equal(List.of(), solve(new Graph(), solver).getOrder());
            equal(expected, solve(dag, solver).getOrder());
        }
        dag.removeEdge("A", "B");
        dag.addEdge("B", "A");
        equal(List.of("B", "C", "A"), solve(dag, solver).getOrder());
    }

    private static void resultSnapshot() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        TopologicalSolver solver = new TopologicalSolver();
        TopoResult first = solve(graph, solver);
        expect(UnsupportedOperationException.class, () -> first.getOrder().add("X"));
        expect(UnsupportedOperationException.class, () -> first.getOrder().set(0, "X"));
        graph.addEdge("B", "A");
        TopoResult second = solve(graph, solver);
        assertCycle(second);
        expect(UnsupportedOperationException.class, () -> second.getOrder().add("X"));
        equal(List.of("A", "B"), first.getOrder());
        equal(false, first.hasCycle());
    }

    private static void nullGraph() {
        expect(IllegalArgumentException.class, () -> new TopologicalSolver().kahnSort(null));
    }

    private static void exhaustiveSmallGraphs() {
        // 枚举三个固定节点之间全部 2^9 张有向图（包含自环）。
        // 独立判据是逐一尝试全部六种排列，不复用 Kahn 的入度逻辑。
        int[][] permutations = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2},
                {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        TopologicalSolver solver = new TopologicalSolver();
        int dagCount = 0;
        for (int mask = 0; mask < 512; mask++) {
            Graph graph = new Graph();
            for (int i = 0; i < 3; i++) {
                graph.addVertex("V" + i);
            }
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if ((mask & (1 << (from * 3 + to))) != 0) {
                        graph.addEdge("V" + from, "V" + to);
                    }
                }
            }
            boolean hasValidOrder = false;
            for (int[] permutation : permutations) {
                int[] positions = new int[3];
                for (int i = 0; i < 3; i++) {
                    positions[permutation[i]] = i;
                }
                boolean valid = true;
                for (int from = 0; from < 3; from++) {
                    for (int to = 0; to < 3; to++) {
                        if ((mask & (1 << (from * 3 + to))) != 0
                                && positions[from] >= positions[to]) {
                            valid = false;
                        }
                    }
                }
                hasValidOrder |= valid;
            }
            TopoResult result = solve(graph, solver);
            equal(!hasValidOrder, result.hasCycle());
            if (hasValidOrder) {
                dagCount++;
            }
        }
        System.out.println("Exhaustive oracle: graphs=512, DAGs=" + dagCount
                + ", cyclic=" + (512 - dagCount));
    }

    private static void longChain() {
        Graph graph = new Graph();
        for (int i = 999; i >= 0; i--) {
            graph.addVertex("V" + i);
        }
        for (int i = 0; i < 999; i++) {
            graph.addEdge("V" + i, "V" + (i + 1));
        }
        List<String> order = solve(graph).getOrder();
        equal(1000, order.size());
        for (int i = 0; i < 1000; i++) {
            equal("V" + i, order.get(i));
        }
    }

    // 仅用于读取本次自测的标准关系对文件，不替代 D 的 DataParser 或输入校验模块。
    private static Graph readFigure1(Path path) throws IOException {
        Graph graph = new Graph();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!line.startsWith("<") || !line.endsWith(">")) {
                throw new IllegalArgumentException("图1自测文件第 " + (i + 1) + " 行不是标准关系对");
            }
            String[] endpoints = line.substring(1, line.length() - 1).split(",", -1);
            if (endpoints.length != 2) {
                throw new IllegalArgumentException("图1自测文件第 " + (i + 1) + " 行必须包含两个端点");
            }
            graph.addEdge(endpoints[0], endpoints[1]);
        }
        return graph;
    }

    private static void figure1(Graph graph) {
        equal(15, graph.getVertexCount());
        equal(16, graph.getEdgeCount());
        TopoResult result = solve(graph);
        equal(false, result.hasCycle());
        System.out.println("Figure 1 input (D): vertices=" + graph.getVertexCount()
                + ", edges=" + graph.getEdgeCount());
        System.out.println("Figure 1 order: " + result.getOrder());
        System.out.println("Figure 1 hasCycle: " + result.hasCycle());
        System.out.println("Figure 1 verification: all vertices exactly once, all 16 edges respected, input unchanged.");
    }

    private static TopoResult solve(Graph graph) {
        return solve(graph, new TopologicalSolver());
    }

    private static TopoResult solve(Graph graph, TopologicalSolver solver) {
        List<Object> before = snapshot(graph);
        TopoResult result = solver.kahnSort(graph);
        equal(before, snapshot(graph));
        if (result.hasCycle()) {
            equal(List.of(), result.getOrder());
        } else {
            assertValidOrder(graph, result.getOrder());
        }
        return result;
    }

    private static List<Object> snapshot(Graph graph) {
        List<Object> snapshot = new ArrayList<>();
        snapshot.add(graph.getVertexNames());
        snapshot.add(graph.getVertexCount());
        snapshot.add(graph.getEdgeCount());
        for (String name : graph.getVertexNames()) {
            snapshot.add(List.of(name, graph.getSuccessors(name),
                    graph.getInDegree(name), graph.getOutDegree(name)));
        }
        return snapshot;
    }

    private static void assertValidOrder(Graph graph, List<String> order) {
        equal(graph.getVertexCount(), order.size());
        equal(new HashSet<>(graph.getVertexNames()), new HashSet<>(order));
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            positions.put(order.get(i), i);
        }
        for (String from : graph.getVertexNames()) {
            for (String to : graph.getSuccessors(from)) {
                if (positions.get(from) >= positions.get(to)) {
                    throw new AssertionError("Edge constraint violated: " + from + " -> " + to);
                }
            }
        }
    }

    private static void assertCycle(TopoResult result) {
        equal(true, result.hasCycle());
        equal(List.of(), result.getOrder());
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + passed + ": " + name);
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            if (!type.isInstance(ex) || (type == IllegalArgumentException.class && ex.getMessage() == null)) {
                throw new AssertionError("Expected " + type.getSimpleName() + " with a reason", ex);
            }
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
