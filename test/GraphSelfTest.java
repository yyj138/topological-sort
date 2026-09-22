import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import model.Edge;
import model.Graph;
import model.Vertex;

/** A1 作者自测及可运行调用示例，不代替 E 的独立测试。 */
public final class GraphSelfTest {
    private static int passed;

    private GraphSelfTest() {
    }

    public static void main(String[] args) {
        run("empty graph", GraphSelfTest::emptyGraph);
        run("isolated vertex", GraphSelfTest::isolatedVertex);
        run("automatic endpoints", GraphSelfTest::automaticEndpoints);
        run("duplicate vertex preserves edges", GraphSelfTest::duplicateVertex);
        run("duplicate edge preserves degrees", GraphSelfTest::duplicateEdge);
        run("self-loop add, deduplicate and remove", GraphSelfTest::selfLoop);
        run("opposite directed edges", GraphSelfTest::oppositeEdges);
        run("design example and deletion", GraphSelfTest::designExample);
        run("missing edge and repeated deletion", GraphSelfTest::missingEdge);
        run("names: whitespace, Chinese, internal spaces and case", GraphSelfTest::names);
        run("invalid names and atomic rejection", GraphSelfTest::invalidNames);
        run("unknown node queries and deletion", GraphSelfTest::unknownNodes);
        run("read-only snapshots", GraphSelfTest::snapshots);
        run("stable insertion order", GraphSelfTest::insertionOrder);
        run("Vertex and directed Edge value semantics", GraphSelfTest::valueObjects);
        run("500 seeded mutations against an adjacency matrix", GraphSelfTest::mutationSequence);
        System.out.println("PASS: " + passed + "/16 cases");
    }

    private static void emptyGraph() {
        Graph graph = new Graph();
        equal(0, graph.getVertexCount());
        equal(0, graph.getEdgeCount());
        equal(List.of(), graph.getVertexNames());
    }

    private static void isolatedVertex() {
        Graph graph = new Graph();
        equal(true, graph.addVertex("E"));
        equal(List.of("E"), graph.getVertexNames());
        equal(1, graph.getVertexCount());
        equal(0, graph.getEdgeCount());
        equal(0, graph.getInDegree("E"));
        equal(0, graph.getOutDegree("E"));
        equal(List.of(), graph.getSuccessors("E"));
    }

    private static void automaticEndpoints() {
        Graph graph = new Graph();
        equal(true, graph.addEdge("A", "B"));
        equal(List.of("A", "B"), graph.getVertexNames());
        equal(List.of("B"), graph.getSuccessors("A"));
        equal(1, graph.getEdgeCount());
        equal(1, graph.getOutDegree("A"));
        equal(1, graph.getInDegree("B"));
        equal(0, graph.getInDegree("A"));
        equal(0, graph.getOutDegree("B"));
    }

    private static void duplicateVertex() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        equal(false, graph.addVertex(" A "));
        equal(2, graph.getVertexCount());
        equal(List.of("B"), graph.getSuccessors("A"));
        equal(1, graph.getInDegree("B"));
    }

    private static void duplicateEdge() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        equal(false, graph.addEdge(" A ", " B "));
        equal(1, graph.getEdgeCount());
        equal(1, graph.getOutDegree("A"));
        equal(1, graph.getInDegree("B"));
    }

    private static void selfLoop() {
        Graph graph = new Graph();
        equal(true, graph.addEdge(" A ", "A"));
        equal(false, graph.addEdge("A", "A"));
        equal(1, graph.getVertexCount());
        equal(1, graph.getEdgeCount());
        equal(List.of("A"), graph.getSuccessors("A"));
        equal(1, graph.getInDegree("A"));
        equal(1, graph.getOutDegree("A"));
        equal(true, graph.removeEdge("A", " A "));
        equal(1, graph.getVertexCount());
        equal(0, graph.getEdgeCount());
        equal(0, graph.getInDegree("A"));
        equal(0, graph.getOutDegree("A"));
    }

    private static void oppositeEdges() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        equal(2, graph.getEdgeCount());
        for (String name : graph.getVertexNames()) {
            equal(1, graph.getInDegree(name));
            equal(1, graph.getOutDegree(name));
        }
        graph.removeEdge("A", "B");
        equal(List.of("A"), graph.getSuccessors("B"));
        equal(1, graph.getInDegree("A"));
        equal(0, graph.getInDegree("B"));
    }

    private static void designExample() {
        Graph graph = new Graph();
        for (String name : List.of("A", "B", "C", "D", "E")) {
            graph.addVertex(name);
        }
        graph.addEdge("A", "C");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
        equal(5, graph.getVertexCount());
        equal(3, graph.getEdgeCount());
        equal(List.of("C"), graph.getSuccessors("A"));
        equal(List.of("D"), graph.getSuccessors("C"));
        equal(2, graph.getInDegree("C"));
        equal(0, graph.getInDegree("E"));
        System.out.println("Example: vertices=" + graph.getVertexNames()
                + ", edges=" + graph.getEdgeCount() + ", inDegree(C)=" + graph.getInDegree("C"));
        for (String name : graph.getVertexNames()) {
            System.out.println("  " + name + " -> " + graph.getSuccessors(name));
        }
        equal(true, graph.removeEdge("A", "C"));
        equal(5, graph.getVertexCount());
        equal(2, graph.getEdgeCount());
        equal(1, graph.getInDegree("C"));
        equal(0, graph.getOutDegree("A"));
        System.out.println("After removing A -> C: vertices=" + graph.getVertexCount()
                + ", edges=" + graph.getEdgeCount() + ", inDegree(C)=" + graph.getInDegree("C"));
    }

    private static void missingEdge() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        equal(false, graph.removeEdge("B", "A"));
        equal(true, graph.removeEdge("A", "B"));
        equal(false, graph.removeEdge("A", "B"));
        equal(0, graph.getEdgeCount());
        equal(List.of("A", "B"), graph.getVertexNames());
        equal(0, graph.getInDegree("B"));
        equal(0, graph.getOutDegree("A"));
    }

    private static void names() {
        Graph graph = new Graph();
        graph.addEdge("\t 高等数学 \u3000", " CS 225 ");
        equal(List.of("高等数学", "CS 225"), graph.getVertexNames());
        equal(List.of("CS 225"), graph.getSuccessors(" 高等数学 "));
        equal(1, graph.getInDegree(" CS 225 "));
        equal(1, graph.getOutDegree(" 高等数学 "));
        equal(true, graph.removeEdge(" 高等数学 ", " CS 225 "));
        equal(true, graph.addVertex("A"));
        equal(true, graph.addVertex("a"));
        equal(false, graph.addVertex("\u3000A\t"));
        graph.addVertex("CS  225");
        equal(true, graph.getVertexNames().contains("CS  225"));
        equal(5, graph.getVertexCount());
    }

    private static void invalidNames() {
        String[] invalid = {null, "", " \t\u3000", "A,B", "<A", "A>", "＜A", "A＞",
                "\nA", "A\r", "A\r\nB", "A\u0085B", "A\u2028B", "A\u2029B"};
        for (String name : invalid) {
            Graph graph = new Graph();
            graph.addEdge("A", "B");
            expect(IllegalArgumentException.class, () -> graph.addVertex(name));
            expect(IllegalArgumentException.class, () -> graph.addEdge("NEW", name));
            expect(IllegalArgumentException.class, () -> graph.addEdge(name, "NEW"));
            expect(IllegalArgumentException.class, () -> graph.getSuccessors(name));
            expect(IllegalArgumentException.class, () -> graph.getInDegree(name));
            expect(IllegalArgumentException.class, () -> graph.getOutDegree(name));
            expect(IllegalArgumentException.class, () -> graph.removeEdge("A", name));
            expect(IllegalArgumentException.class, () -> graph.removeEdge(name, "B"));
            expect(IllegalArgumentException.class, () -> new Vertex(name));
            expect(IllegalArgumentException.class, () -> new Edge("A", name));
            expect(IllegalArgumentException.class, () -> new Edge(name, "B"));
            equal(List.of("A", "B"), graph.getVertexNames());
            equal(List.of("B"), graph.getSuccessors("A"));
            equal(1, graph.getEdgeCount());
            equal(1, graph.getInDegree("B"));
            equal(1, graph.getOutDegree("A"));
        }
    }

    private static void unknownNodes() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        expect(IllegalArgumentException.class, () -> graph.getSuccessors("missing"));
        expect(IllegalArgumentException.class, () -> graph.getInDegree("missing"));
        expect(IllegalArgumentException.class, () -> graph.getOutDegree("missing"));
        expect(IllegalArgumentException.class, () -> graph.removeEdge("A", "missing"));
        expect(IllegalArgumentException.class, () -> graph.removeEdge("missing", "B"));
        equal(List.of("A", "B"), graph.getVertexNames());
        equal(List.of("B"), graph.getSuccessors("A"));
        equal(1, graph.getEdgeCount());
        equal(1, graph.getInDegree("B"));
    }

    private static void snapshots() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        List<String> vertices = graph.getVertexNames();
        List<String> successors = graph.getSuccessors("A");
        expect(UnsupportedOperationException.class, () -> vertices.add("X"));
        expect(UnsupportedOperationException.class, () -> vertices.set(0, "X"));
        expect(UnsupportedOperationException.class, () -> successors.remove("B"));
        graph.addEdge("A", "C");
        graph.removeEdge("A", "B");
        equal(List.of("A", "B"), vertices);
        equal(List.of("B"), successors);
        equal(List.of("C"), graph.getSuccessors("A"));
        equal(0, graph.getInDegree("B"));
        equal(1, graph.getEdgeCount());
    }

    private static void insertionOrder() {
        for (int repeat = 0; repeat < 2; repeat++) {
            Graph graph = new Graph();
            graph.addVertex("Z");
            graph.addEdge("B", "C");
            graph.addEdge("B", "A");
            graph.addEdge("B", "C");
            equal(List.of("Z", "B", "C", "A"), graph.getVertexNames());
            equal(List.of("C", "A"), graph.getSuccessors("B"));
            graph.removeEdge("B", "C");
            graph.addEdge("B", "C");
            equal(List.of("A", "C"), graph.getSuccessors("B"));
            equal(List.of("Z", "B", "C", "A"), graph.getVertexNames());
        }
    }

    private static void valueObjects() {
        Vertex vertex = new Vertex(" A ");
        equal("A", vertex.getName());
        equal(List.of(), vertex.getSuccessors());
        equal(0, vertex.getInDegree());
        equal(0, vertex.getOutDegree());
        expect(UnsupportedOperationException.class, () -> vertex.getSuccessors().add("B"));
        Edge edge = new Edge(" A ", "B");
        Edge same = new Edge("A", " B ");
        equal("A", edge.getFrom());
        equal("B", edge.getTo());
        equal(true, edge.equals(edge));
        equal(true, edge.equals(same));
        equal(true, same.equals(edge));
        equal(edge.hashCode(), same.hashCode());
        equal(false, edge.equals(new Edge("B", "A")));
        equal(false, edge.equals(null));
        equal(false, edge.equals("A -> B"));
        equal(2, new HashSet<>(List.of(edge, same, new Edge("B", "A"))).size());
    }

    private static void mutationSequence() {
        Graph graph = new Graph();
        boolean[][] expected = new boolean[8][8];
        for (int i = 0; i < expected.length; i++) {
            graph.addVertex("V" + i);
        }
        Random random = new Random(20260918L);
        for (int step = 0; step < 500; step++) {
            int from = random.nextInt(8);
            int to = random.nextInt(8);
            if (random.nextBoolean()) {
                equal(!expected[from][to], graph.addEdge("V" + from, "V" + to));
                expected[from][to] = true;
            } else {
                equal(expected[from][to], graph.removeEdge("V" + from, "V" + to));
                expected[from][to] = false;
            }
            int edges = 0;
            int inSum = 0;
            int outSum = 0;
            equal(8, graph.getVertexCount());
            for (int i = 0; i < 8; i++) {
                int in = 0;
                int out = 0;
                List<String> successors = graph.getSuccessors("V" + i);
                for (int j = 0; j < 8; j++) {
                    equal(expected[i][j], successors.contains("V" + j));
                    if (expected[i][j]) {
                        out++;
                    }
                    if (expected[j][i]) {
                        in++;
                    }
                }
                equal(out, successors.size());
                equal(out, graph.getOutDegree("V" + i));
                equal(in, graph.getInDegree("V" + i));
                edges += out;
                inSum += graph.getInDegree("V" + i);
                outSum += graph.getOutDegree("V" + i);
            }
            equal(edges, graph.getEdgeCount());
            equal(edges, inSum);
            equal(edges, outSum);
        }
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
            if (!type.isInstance(ex) || ex.getMessage() == null && type == IllegalArgumentException.class) {
                throw new AssertionError("Expected " + type.getSimpleName() + " with a reason", ex);
            }
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
