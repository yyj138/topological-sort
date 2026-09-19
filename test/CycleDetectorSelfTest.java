import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import algorithm.CycleDetector;
import model.Graph;

/** A4 作者自测及至少三类环的调用示例，不代替 E 的独立测试。 */
public final class CycleDetectorSelfTest {
    private static int passed;

    private CycleDetectorSelfTest() {
    }

    public static void main(String[] args) {
        run("empty graph", CycleDetectorSelfTest::emptyGraph);
        run("isolated vertices", CycleDetectorSelfTest::isolatedVertices);
        run("DAG with converging paths", CycleDetectorSelfTest::dag);
        run("self-loop path", CycleDetectorSelfTest::selfLoop);
        run("two-vertex cycle path", CycleDetectorSelfTest::twoVertexCycle);
        run("longer cycle path", CycleDetectorSelfTest::longerCycle);
        run("exclude the prefix before the cycle", CycleDetectorSelfTest::prefix);
        run("exclude downstream vertices", CycleDetectorSelfTest::downstream);
        run("cycle in a later disconnected component", CycleDetectorSelfTest::disconnected);
        run("edges to black vertices are not back edges", CycleDetectorSelfTest::blackVertices);
        run("multiple cycles return a reproducible valid path", CycleDetectorSelfTest::multipleCycles);
        run("duplicate edges do not duplicate cycle nodes", CycleDetectorSelfTest::duplicateEdges);
        run("Chinese names, internal spaces and case", CycleDetectorSelfTest::names);
        run("repeated calls and reuse after graph edits", CycleDetectorSelfTest::reuse);
        run("read-only results independent of graph edits", CycleDetectorSelfTest::readOnly);
        run("null graph rejected explicitly", CycleDetectorSelfTest::nullGraph);
        run("10000-vertex acyclic chain", CycleDetectorSelfTest::longDag);
        run("10000-vertex path leading to a cycle without recursion", CycleDetectorSelfTest::deepCycle);
        run("all 512 three-vertex graphs checked by transitive closure", CycleDetectorSelfTest::exhaustive);
        System.out.println("PASS: " + passed + "/19 cases");
    }

    private static void emptyGraph() {
        equal(List.of(), detect(new Graph()));
    }

    private static void isolatedVertices() {
        Graph graph = new Graph();
        graph.addVertex("A");
        graph.addVertex("B");
        equal(List.of(), detect(graph));
    }

    private static void dag() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        equal(List.of(), detect(graph));
    }

    private static void selfLoop() {
        Graph graph = new Graph();
        graph.addEdge("A", "A");
        List<String> cycle = detect(graph);
        equal(List.of("A", "A"), cycle);
        System.out.println("Self-loop: edges=[A -> A], cycle=" + cycle);
    }

    private static void twoVertexCycle() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        List<String> cycle = detect(graph);
        equal(List.of("A", "B", "A"), cycle);
        System.out.println("Two-vertex cycle: edges=[A -> B, B -> A], cycle=" + cycle);
    }

    private static void longerCycle() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");
        List<String> cycle = detect(graph);
        equal(List.of("A", "B", "C", "A"), cycle);
        System.out.println("Longer cycle: edges=[A -> B, B -> C, C -> A], cycle=" + cycle);
    }

    private static void prefix() {
        Graph graph = new Graph();
        graph.addEdge("Start", "A");
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "B");
        List<String> cycle = detect(graph);
        equal(List.of("B", "C", "B"), cycle);
        System.out.println("Prefix excluded: edges=[Start -> A, A -> B, B -> C, C -> B], cycle=" + cycle);
    }

    private static void downstream() {
        Graph graph = new Graph();
        // 先探索无环下游，回退后再发现环，检验当前路径是否正确恢复。
        graph.addEdge("A", "Tail");
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        equal(List.of("A", "B", "A"), detect(graph));
    }

    private static void disconnected() {
        Graph graph = new Graph();
        graph.addEdge("Start", "End");
        graph.addVertex("Isolated");
        graph.addEdge("X", "Y");
        graph.addEdge("Y", "X");
        equal(List.of("X", "Y", "X"), detect(graph));
    }

    private static void blackVertices() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "D");
        graph.addEdge("A", "C");
        graph.addEdge("C", "D");
        graph.addEdge("C", "X");
        graph.addEdge("X", "Y");
        graph.addEdge("Y", "X");
        // C -> D 指向已完成节点，不能误判；图中确实有环，确保实际进入 DFS。
        equal(List.of("X", "Y", "X"), detect(graph));
    }

    private static void multipleCycles() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        graph.addEdge("A", "C");
        graph.addEdge("C", "A");
        graph.addEdge("D", "D");
        List<String> first = detect(graph);
        equal(false, first.isEmpty());
        equal(first, detect(graph));
    }

    private static void duplicateEdges() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        equal(false, graph.addEdge("A", "B"));
        equal(false, graph.addEdge("B", "A"));
        equal(List.of("A", "B", "A"), detect(graph));
    }

    private static void names() {
        Graph graph = new Graph();
        graph.addEdge(" 高等数学 ", "CS 225");
        graph.addEdge("CS 225", "A");
        graph.addEdge("A", "a");
        graph.addEdge("a", "高等数学");
        equal(List.of("高等数学", "CS 225", "A", "a", "高等数学"), detect(graph));
    }

    private static void reuse() {
        CycleDetector detector = new CycleDetector();
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        for (int i = 0; i < 3; i++) {
            equal(List.of(), detect(graph, detector));
            graph.addEdge("B", "A");
            equal(List.of("A", "B", "A"), detect(graph, detector));
            equal(List.of(), detect(new Graph(), detector));
            graph.removeEdge("B", "A");
        }
        equal(List.of(), detect(graph, detector));
    }

    private static void readOnly() {
        Graph graph = new Graph();
        graph.addEdge("A", "A");
        CycleDetector detector = new CycleDetector();
        List<String> first = detect(graph, detector);
        expect(UnsupportedOperationException.class, () -> first.add("X"));
        expect(UnsupportedOperationException.class, () -> first.set(0, "X"));
        graph.removeEdge("A", "A");
        List<String> second = detect(graph, detector);
        equal(List.of(), second);
        expect(UnsupportedOperationException.class, () -> second.add("X"));
        equal(List.of("A", "A"), first);
    }

    private static void nullGraph() {
        expect(IllegalArgumentException.class, () -> new CycleDetector().findCycle(null));
    }

    private static Graph createLongChain() {
        Graph graph = new Graph();
        for (int i = 0; i < 9999; i++) {
            graph.addEdge("V" + i, "V" + (i + 1));
        }
        return graph;
    }

    private static void longDag() {
        equal(List.of(), detect(createLongChain()));
    }

    private static void deepCycle() {
        Graph graph = createLongChain();
        graph.addEdge("V9999", "V9000");
        List<String> cycle = detect(graph);
        equal(1001, cycle.size());
        equal("V9000", cycle.get(0));
        equal("V9000", cycle.get(cycle.size() - 1));
        System.out.println("Deep path: vertices=10000, edges=10000, cycle vertices=1000, closed path entries=" + cycle.size());
    }

    private static void exhaustive() {
        CycleDetector detector = new CycleDetector();
        int cyclicCount = 0;
        for (int mask = 0; mask < 512; mask++) {
            Graph graph = new Graph();
            boolean[][] reachable = new boolean[3][3];
            for (int i = 0; i < 3; i++) {
                graph.addVertex("V" + i);
            }
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if ((mask & (1 << (from * 3 + to))) != 0) {
                        graph.addEdge("V" + from, "V" + to);
                        reachable[from][to] = true;
                    }
                }
            }
            // 独立判据：求传递闭包；存在非空路径从节点回到自身则有环。
            // 不预先设置对角线，不把零长度路径误当成环。
            for (int via = 0; via < 3; via++) {
                for (int from = 0; from < 3; from++) {
                    for (int to = 0; to < 3; to++) {
                        reachable[from][to] |= reachable[from][via] && reachable[via][to];
                    }
                }
            }
            boolean hasCycle = reachable[0][0] || reachable[1][1] || reachable[2][2];
            List<String> cycle = detect(graph, detector);
            equal(hasCycle, !cycle.isEmpty());
            if (hasCycle) {
                cyclicCount++;
            }
        }
        System.out.println("Transitive-closure oracle: graphs=512, DAGs=" + (512 - cyclicCount)
                + ", cyclic=" + cyclicCount + ", every returned path verified.");
    }

    private static List<String> detect(Graph graph) {
        return detect(graph, new CycleDetector());
    }

    private static List<String> detect(Graph graph, CycleDetector detector) {
        List<Object> before = snapshot(graph);
        List<String> cycle = detector.findCycle(graph);
        equal(before, snapshot(graph));
        if (!cycle.isEmpty()) {
            if (cycle.size() < 2) {
                throw new AssertionError("A cycle must contain a closing vertex");
            }
            equal(cycle.get(0), cycle.get(cycle.size() - 1));
            Set<String> vertices = new HashSet<>(graph.getVertexNames());
            Set<String> visited = new HashSet<>();
            for (int i = 0; i < cycle.size() - 1; i++) {
                String from = cycle.get(i);
                String to = cycle.get(i + 1);
                equal(true, vertices.contains(from));
                equal(true, visited.add(from));
                equal(true, graph.getSuccessors(from).contains(to));
            }
        }
        return cycle;
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
