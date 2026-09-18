package algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import model.Graph;

/** A3 作者自测；可注入时钟验证超时，文件读取和结果保存仅用于本次验证。 */
public final class AllTopoSortsSelfTest {
    private static int passed;

    private AllTopoSortsSelfTest() {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("用法：AllTopoSortsSelfTest [图1文件 结果输出文件]");
        }
        System.out.println("Environment: OS=" + System.getProperty("os.name")
                + ", arch=" + System.getProperty("os.arch") + ", Java=" + System.getProperty("java.version")
                + ", logical processors=" + Runtime.getRuntime().availableProcessors());
        run("empty graph has one empty sequence", AllTopoSortsSelfTest::empty);
        run("single vertex and a chain", AllTopoSortsSelfTest::chain);
        run("two hand-enumerated orders", AllTopoSortsSelfTest::twoOrders);
        run("diamond has two orders", AllTopoSortsSelfTest::diamond);
        run("disconnected graph includes isolated vertex", AllTopoSortsSelfTest::disconnected);
        run("three isolated vertices have all six permutations", AllTopoSortsSelfTest::isolated);
        run("names and duplicate edges", AllTopoSortsSelfTest::names);
        run("self-loop, two-vertex and longer cycles", AllTopoSortsSelfTest::cycles);
        run("cycle plus many isolated vertices is rejected before search", AllTopoSortsSelfTest::mixedCycle);
        run("quantity limit retains only complete sequences", AllTopoSortsSelfTest::limited);
        run("exact limit is conservatively incomplete", AllTopoSortsSelfTest::exactLimit);
        run("limit above total allows complete enumeration", AllTopoSortsSelfTest::aboveLimit);
        run("caller default limit of 1000", AllTopoSortsSelfTest::defaultLimit);
        run("cancel before work", AllTopoSortsSelfTest::cancelBefore);
        run("cancel during search retains valid partial results", AllTopoSortsSelfTest::cancelDuring);
        run("deterministic timeout before work", AllTopoSortsSelfTest::timeoutBefore);
        run("deterministic timeout during search", AllTopoSortsSelfTest::timeoutDuring);
        run("large timeout does not overflow", AllTopoSortsSelfTest::largeTimeout);
        run("invalid arguments", AllTopoSortsSelfTest::invalidArguments);
        run("deeply immutable and independent results", AllTopoSortsSelfTest::immutable);
        run("reuse after cancellation, limits, cycles and callback failure", AllTopoSortsSelfTest::reuse);
        run("all 512 three-vertex graphs checked against permutations", AllTopoSortsSelfTest::exhaustive);
        run("1000-vertex chain", AllTopoSortsSelfTest::thousandChain);
        run("1000 isolated vertices with limit 1000", AllTopoSortsSelfTest::thousandIsolated);
        if (args.length == 2) {
            figure1(Path.of(args[0]), Path.of(args[1]));
            passed++;
            System.out.println("PASS " + passed + ": Figure 1 checked against independent subset counting");
        } else {
            System.out.println("Figure 1: NOT RUN (supply input and output paths).");
        }
        System.out.println("PASS: " + passed + "/" + (args.length == 2 ? 25 : 24) + " cases");
    }

    private static void empty() {
        for (int limit : new int[] {0, 1, 1000}) {
            EnumerationResult result = enumerate(new Graph(), limit);
            status(result, StopReason.COMPLETED, 1);
            equal(List.of(List.of()), result.getSequences());
        }
    }

    private static void chain() {
        Graph graph = new Graph();
        graph.addVertex("A");
        equal(List.of(List.of("A")), enumerate(graph, 0).getSequences());
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        EnumerationResult result = enumerate(graph, 0);
        status(result, StopReason.COMPLETED, 1);
        equal(List.of(List.of("A", "B", "C")), result.getSequences());
    }

    private static Graph twoChoices() {
        Graph graph = new Graph();
        graph.addEdge("A", "C");
        graph.addEdge("B", "C");
        return graph;
    }

    private static void twoOrders() {
        EnumerationResult result = enumerate(twoChoices(), 0);
        status(result, StopReason.COMPLETED, 2);
        equal(Set.of(List.of("A", "B", "C"), List.of("B", "A", "C")), new HashSet<>(result.getSequences()));
        System.out.println("Hand example: " + result.getSequences() + ", complete=" + result.isComplete());
    }

    private static void diamond() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        EnumerationResult result = enumerate(graph, 0);
        status(result, StopReason.COMPLETED, 2);
        equal(Set.of(List.of("A", "B", "C", "D"), List.of("A", "C", "B", "D")), new HashSet<>(result.getSequences()));
    }

    private static void disconnected() {
        Graph graph = new Graph();
        graph.addEdge("A", "B");
        graph.addVertex("C");
        EnumerationResult result = enumerate(graph, 0);
        status(result, StopReason.COMPLETED, 3);
        equal(Set.of(List.of("A", "B", "C"), List.of("A", "C", "B"), List.of("C", "A", "B")),
                new HashSet<>(result.getSequences()));
    }

    private static Graph independent(int size) {
        Graph graph = new Graph();
        for (int i = 0; i < size; i++) {
            graph.addVertex("V" + i);
        }
        return graph;
    }

    private static void isolated() {
        status(enumerate(independent(3), 0), StopReason.COMPLETED, 6);
    }

    private static void names() {
        Graph graph = new Graph();
        graph.addEdge(" 高等数学 ", "CS 225");
        graph.addEdge("CS 225", "A");
        graph.addEdge("A", "a");
        graph.addEdge("高等数学", "CS 225");
        equal(List.of(List.of("高等数学", "CS 225", "A", "a")), enumerate(graph, 0).getSequences());
    }

    private static void cycles() {
        for (int size = 1; size <= 3; size++) {
            Graph graph = independent(size);
            for (int i = 0; i < size; i++) {
                graph.addEdge("V" + i, "V" + ((i + 1) % size));
            }
            status(enumerate(graph, 0), StopReason.CYCLE, 0);
        }
    }

    private static void mixedCycle() {
        Graph graph = independent(1000);
        graph.addEdge("V998", "V999");
        graph.addEdge("V999", "V998");
        status(enumerate(graph, 0), StopReason.CYCLE, 0);
    }

    private static void limited() {
        status(enumerate(twoChoices(), 1), StopReason.LIMIT_REACHED, 1);
    }

    private static void exactLimit() {
        status(enumerate(twoChoices(), 2), StopReason.LIMIT_REACHED, 2);
    }

    private static void aboveLimit() {
        status(enumerate(twoChoices(), 3), StopReason.COMPLETED, 2);
    }

    private static void defaultLimit() {
        status(enumerate(independent(7), 1000), StopReason.LIMIT_REACHED, 1000);
    }

    private static void cancelBefore() {
        status(checked(twoChoices(), new AllTopoSorts(), 0, 0, () -> true), StopReason.CANCELLED, 0);
    }

    private static void cancelDuring() {
        AtomicInteger checks = new AtomicInteger();
        EnumerationResult result = checked(independent(6), new AllTopoSorts(), 0, 0,
                () -> checks.incrementAndGet() >= 1000);
        partial(result, StopReason.CANCELLED, 720);
        System.out.println("Cancellation during search: generated=" + result.getGeneratedCount() + ", complete=false");
    }

    private static void timeoutBefore() {
        AtomicLong clock = new AtomicLong();
        AllTopoSorts solver = new AllTopoSorts(() -> clock.getAndAdd(1_000_000));
        status(checked(twoChoices(), solver, 0, 1, () -> false), StopReason.TIMEOUT, 0);
    }

    private static void timeoutDuring() {
        AtomicLong clock = new AtomicLong();
        AllTopoSorts solver = new AllTopoSorts(() -> clock.getAndAdd(1000));
        EnumerationResult result = checked(independent(6), solver, 0, 1, () -> false);
        partial(result, StopReason.TIMEOUT, 720);
        System.out.println("Simulated timeout during search: generated=" + result.getGeneratedCount() + ", complete=false");
        status(checked(twoChoices(), solver, 0, 0, () -> false), StopReason.COMPLETED, 2);
    }

    private static void largeTimeout() {
        status(checked(twoChoices(), new AllTopoSorts(), 0, Long.MAX_VALUE, () -> false), StopReason.COMPLETED, 2);
    }

    private static void invalidArguments() {
        AllTopoSorts solver = new AllTopoSorts();
        Graph graph = twoChoices();
        List<Object> before = snapshot(graph);
        expect(IllegalArgumentException.class, () -> solver.enumerate(null, 0, 0, () -> false));
        expect(IllegalArgumentException.class, () -> solver.enumerate(graph, -1, 0, () -> false));
        expect(IllegalArgumentException.class, () -> solver.enumerate(graph, 0, -1, () -> false));
        expect(IllegalArgumentException.class, () -> solver.enumerate(graph, 0, 0, null));
        equal(before, snapshot(graph));
    }

    private static void immutable() {
        Graph graph = twoChoices();
        EnumerationResult first = enumerate(graph, 0);
        expect(UnsupportedOperationException.class, () -> first.getSequences().add(List.of()));
        expect(UnsupportedOperationException.class, () -> first.getSequences().get(0).set(0, "X"));
        List<List<String>> saved = first.getSequences();
        graph.addEdge("C", "A");
        status(enumerate(graph, 0), StopReason.CYCLE, 0);
        equal(saved, first.getSequences());
        List<String> row = new ArrayList<>(List.of("A"));
        List<List<String>> rows = new ArrayList<>();
        rows.add(row);
        EnumerationResult copy = new EnumerationResult(rows, StopReason.COMPLETED);
        row.set(0, "B");
        rows.clear();
        equal(List.of(List.of("A")), copy.getSequences());
    }

    private static void reuse() {
        AllTopoSorts solver = new AllTopoSorts();
        Graph graph = twoChoices();
        List<List<String>> expected = checked(graph, solver, 0, 0, () -> false).getSequences();
        status(checked(graph, solver, 1, 0, () -> false), StopReason.LIMIT_REACHED, 1);
        status(checked(graph, solver, 0, 0, () -> true), StopReason.CANCELLED, 0);
        List<Object> before = snapshot(graph);
        expect(IllegalStateException.class, () -> solver.enumerate(graph, 0, 0,
                () -> { throw new IllegalStateException("test callback"); }));
        equal(before, snapshot(graph));
        graph.addEdge("C", "A");
        status(checked(graph, solver, 0, 0, () -> false), StopReason.CYCLE, 0);
        graph.removeEdge("C", "A");
        equal(expected, checked(graph, solver, 0, 0, () -> false).getSequences());
    }

    private static void exhaustive() {
        int[][] permutations = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        for (int mask = 0; mask < 512; mask++) {
            Graph graph = independent(3);
            for (int from = 0; from < 3; from++) {
                for (int to = 0; to < 3; to++) {
                    if ((mask & (1 << (from * 3 + to))) != 0) {
                        graph.addEdge("V" + from, "V" + to);
                    }
                }
            }
            Set<List<String>> expected = new HashSet<>();
            for (int[] order : permutations) {
                int[] positions = new int[3];
                for (int i = 0; i < 3; i++) {
                    positions[order[i]] = i;
                }
                boolean valid = true;
                for (int from = 0; from < 3; from++) {
                    for (int to = 0; to < 3; to++) {
                        if ((mask & (1 << (from * 3 + to))) != 0 && positions[from] >= positions[to]) {
                            valid = false;
                        }
                    }
                }
                if (valid) {
                    expected.add(List.of("V" + order[0], "V" + order[1], "V" + order[2]));
                }
            }
            EnumerationResult result = enumerate(graph, 0);
            equal(expected, new HashSet<>(result.getSequences()));
            equal(expected.isEmpty() ? StopReason.CYCLE : StopReason.COMPLETED, result.getStopReason());
        }
    }

    private static void thousandChain() {
        Graph graph = independent(1000);
        for (int i = 0; i < 999; i++) {
            graph.addEdge("V" + i, "V" + (i + 1));
        }
        List<Object> before = snapshot(graph);
        long start = System.nanoTime();
        EnumerationResult result = new AllTopoSorts().enumerate(graph, 0, 10000, () -> false);
        double elapsed = (System.nanoTime() - start) / 1_000_000.0;
        validate(graph, result);
        equal(before, snapshot(graph));
        status(result, StopReason.COMPLETED, 1);
        System.out.println("BENCH chain: V=1000, E=999, maxResults=0, timeoutMillis=10000, generated=1, complete=true, elapsedMillis=" + elapsed);
    }

    private static void thousandIsolated() {
        Graph graph = independent(1000);
        List<Object> before = snapshot(graph);
        long start = System.nanoTime();
        EnumerationResult result = new AllTopoSorts().enumerate(graph, 1000, 10000, () -> false);
        double elapsed = (System.nanoTime() - start) / 1_000_000.0;
        validate(graph, result);
        equal(before, snapshot(graph));
        status(result, StopReason.LIMIT_REACHED, 1000);
        System.out.println("BENCH isolated: V=1000, E=0, maxResults=1000, timeoutMillis=10000, generated=1000, complete=false, elapsedMillis=" + elapsed);
    }

    private static void figure1(Path input, Path output) throws IOException, NoSuchAlgorithmException {
        Graph graph = new Graph();
        for (String raw : Files.readAllLines(input, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!line.startsWith("<") || !line.endsWith(">")) {
                throw new IllegalArgumentException("图1样例不是标准关系对：" + line);
            }
            String[] endpoints = line.substring(1, line.length() - 1).split(",", -1);
            if (endpoints.length != 2) {
                throw new IllegalArgumentException("图1样例端点数量错误：" + line);
            }
            graph.addEdge(endpoints[0], endpoints[1]);
        }
        equal(15, graph.getVertexCount());
        equal(16, graph.getEdgeCount());
        long expectedCount = countBySubsets(graph);
        int limit = expectedCount <= 100000 ? 0 : 1000;
        List<Object> before = snapshot(graph);
        long start = System.nanoTime();
        EnumerationResult result = new AllTopoSorts().enumerate(graph, limit, 30000, () -> false);
        double elapsed = (System.nanoTime() - start) / 1_000_000.0;
        validate(graph, result);
        equal(before, snapshot(graph));
        if (limit == 0) {
            equal(StopReason.COMPLETED, result.getStopReason());
            equal(expectedCount, (long) result.getGeneratedCount());
        } else {
            status(result, StopReason.LIMIT_REACHED, 1000);
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input)));
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("# A3 Figure 1 enumeration output\n# Input: " + input + "\n# SHA-256: " + hash
                    + "\n# Vertices: 15\n# Edges: 16\n# maxResults: " + limit
                    + "\n# timeoutMillis: 30000\n# Generated: " + result.getGeneratedCount()
                    + "\n# Complete: " + result.isComplete() + "\n# StopReason: " + result.getStopReason()
                    + "\n# Independent subset-DP total: " + expectedCount + "\n# elapsedMillis: " + elapsed
                    + "\n# Every sequence checked for vertex coverage, edge constraints and uniqueness.\n");
            for (int i = 0; i < result.getGeneratedCount(); i++) {
                writer.write((i + 1) + "\t" + String.join(" -> ", result.getSequences().get(i)) + "\n");
            }
        }
        System.out.println("Figure 1: V=15, E=16, independentTotal=" + expectedCount + ", generated="
                + result.getGeneratedCount() + ", complete=" + result.isComplete() + ", reason=" + result.getStopReason()
                + ", maxResults=" + limit + ", timeoutMillis=30000, elapsedMillis=" + elapsed);
        System.out.println("Figure 1 saved to: " + output);
    }

    // 仅针对本次 15 节点图1，用子集动态规划独立计数，不复用回溯算法。
    private static long countBySubsets(Graph graph) {
        List<String> names = graph.getVertexNames();
        int[] prerequisites = new int[names.size()];
        for (int from = 0; from < names.size(); from++) {
            for (String to : graph.getSuccessors(names.get(from))) {
                prerequisites[names.indexOf(to)] |= 1 << from;
            }
        }
        long[] counts = new long[1 << names.size()];
        counts[0] = 1;
        for (int mask = 0; mask < counts.length; mask++) {
            for (int next = 0; next < names.size(); next++) {
                int bit = 1 << next;
                if ((mask & bit) == 0 && (mask & prerequisites[next]) == prerequisites[next]) {
                    counts[mask | bit] += counts[mask];
                }
            }
        }
        return counts[counts.length - 1];
    }

    private static EnumerationResult enumerate(Graph graph, int limit) {
        return checked(graph, new AllTopoSorts(), limit, 0, () -> false);
    }

    private static EnumerationResult checked(Graph graph, AllTopoSorts solver, int limit,
            long timeout, BooleanSupplier cancelled) {
        List<Object> before = snapshot(graph);
        EnumerationResult result = solver.enumerate(graph, limit, timeout, cancelled);
        equal(before, snapshot(graph));
        validate(graph, result);
        return result;
    }

    private static void validate(Graph graph, EnumerationResult result) {
        equal(result.getSequences().size(), result.getGeneratedCount());
        equal(result.getStopReason() == StopReason.COMPLETED, result.isComplete());
        equal(result.getGeneratedCount(), new HashSet<>(result.getSequences()).size());
        Set<String> names = new HashSet<>(graph.getVertexNames());
        for (List<String> order : result.getSequences()) {
            equal(graph.getVertexCount(), order.size());
            equal(names, new HashSet<>(order));
            Map<String, Integer> positions = new HashMap<>();
            for (int i = 0; i < order.size(); i++) {
                positions.put(order.get(i), i);
            }
            for (String from : graph.getVertexNames()) {
                for (String to : graph.getSuccessors(from)) {
                    equal(true, positions.get(from) < positions.get(to));
                }
            }
        }
    }

    private static List<Object> snapshot(Graph graph) {
        List<Object> snapshot = new ArrayList<>();
        snapshot.add(graph.getVertexNames());
        snapshot.add(graph.getVertexCount());
        snapshot.add(graph.getEdgeCount());
        for (String name : graph.getVertexNames()) {
            snapshot.add(List.of(name, graph.getSuccessors(name), graph.getInDegree(name), graph.getOutDegree(name)));
        }
        return snapshot;
    }

    private static void status(EnumerationResult result, StopReason reason, int count) {
        equal(reason, result.getStopReason());
        equal(count, result.getGeneratedCount());
        equal(reason == StopReason.COMPLETED, result.isComplete());
    }

    private static void partial(EnumerationResult result, StopReason reason, int total) {
        equal(reason, result.getStopReason());
        equal(false, result.isComplete());
        equal(true, result.getGeneratedCount() > 0 && result.getGeneratedCount() < total);
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
            if (!type.isInstance(ex)) {
                throw new AssertionError("Expected " + type.getSimpleName(), ex);
            }
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
