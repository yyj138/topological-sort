package test;
import algorithm.AllTopoSorts;
import algorithm.CycleDetector;
import algorithm.TopologicalSolver;
import algorithm.TopoResult;
import algorithm.EnumerationResult;
import algorithm.StopReason;
import io.DataParser;
import io.ParseResult;
import model.Graph;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
/**
 * T‑E1：算法四类测试：正确性、边界、异常(环)、性能
 * 遵循接口契约V1.0；项目目标JDK21
 * 输出文件 test/test_E1_result.txt 【维持原有文件名不变】
 * 修复&补强清单：
 * 1. 修复环测试假PASS风险；自环显式断言[A,A]；含环图断言Kahn返回空序列
 * 2. 图1增加AllTopoSorts枚举比对校验；新增枚举停止原因/数量/完整性meta断言
 * 3. 大量同层节点用例补充枚举总数720、isComplete断言
 * 4. figure1.txt双路径兼容：优先data/figure1.txt，不存在回退src/data/figure1.txt
 * 5. 契约增强：null入参校验、maxResults=0语义、子序列不可修改校验
 * 6. 图1增加顶点数、边数、warnings为空断言
 * 7. 性能断言补强：LIMIT_REACHED时生成数量严格等于上限1000
 * 8. 【可选契约补强，非T‑E1强制】算法隔离性：原图入度不被修改、多次调用结果稳定、序列对象独立
 * 9. 不做多线程模拟CANCELLED/TIMEOUT业务场景（不属于T‑E1任务卡）
 * 改动：不再将user.dir调试行写入输出txt文件，控制台仍然打印用于本地调试
 */
public class AlgorithmTest {
    // ============ 路径常量：输出文件名保持不变 ============
    private static final String OUTPUT_FILE = "test/test_E1_result.txt";
    private final PrintWriter out;
    private int passCount = 0;
    private int failCount = 0;
    private static final BooleanSupplier NEVER_CANCEL = () -> false;
    public AlgorithmTest(PrintWriter out) {
        this.out = out;
    }
    public static void main(String[] args) {
        File testDir = new File("test");
        if (!testDir.exists()) {
            boolean mkdirOk = testDir.mkdirs();
            System.out.println("test目录创建：" + (mkdirOk ? "成功" : "已存在/跳过"));
        }
        String userDir = System.getProperty("user.dir");
        String javaVersion = System.getProperty("java.version");
        // 控制台仍然打印调试信息，但是不写入txt文件
        System.out.println("[DEBUG] JVM工作目录 user.dir = " + userDir);
        System.out.println("[DEBUG] JDK版本 = " + javaVersion);
        // figure1 双路径兼容：打包优先data/，开发回退src/data/
        File figureFile;
        File prodPath = new File("data/figure1.txt");
        File devPath = new File("src/data/figure1.txt");
        if (prodPath.exists()) {
            figureFile = prodPath;
        } else if (devPath.exists()) {
            figureFile = devPath;
        } else {
            figureFile = prodPath;
        }
        final String usedFigurePath = figureFile.getPath();
        try (FileOutputStream fos = new FileOutputStream(OUTPUT_FILE, false);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            AlgorithmTest tester = new AlgorithmTest(pw);
            tester.log("==================== T‑E1 算法测试开始 ====================");
            tester.log("测试时间：本地运行；遵循接口契约V1.0；JDK版本:" + javaVersion);
            tester.log("输出文件：" + OUTPUT_FILE);
            tester.log("读取图1数据文件：" + usedFigurePath);
            // 【改动】移除向txt输出user.dir这一行，控制台System.out保留
            tester.log("");
            // ①正确性测试（任务卡：图1 + 3个手算小图）
            tester.log("===== ① 正确性测试 =====");
            tester.testCorrect_ThreeNodeDAG();
            tester.testCorrect_FourNodeBranched();
            tester.testCorrect_ChainDAG();
            tester.testCorrect_FigureOneSample(figureFile);
            // ②边界测试
            tester.log("");
            tester.log("===== ② 边界测试 =====");
            tester.testBoundary_EmptyGraph();
            tester.testBoundary_SingleVertex();
            tester.testBoundary_AllIsolated();
            tester.testBoundary_ManySameLayer();
            // ③异常‑环测试（3种环）
            tester.log("");
            tester.log("===== ③ 异常‑含环图测试 =====");
            tester.testCycle_SelfLoop();
            tester.testCycle_SimpleTwoNodeCycle();
            tester.testCycle_LongCycle();
            // ④接口契约专项校验
            tester.log("");
            tester.log("===== 契约校验：EnumerationResult与入参语义 =====");
            tester.testContract_ResultCompleteFlag();
            tester.testContract_ResultMetaConsistency();
            tester.testContract_IllegalArguments();
            tester.testContract_NullParams();
            tester.testContract_MaxResultZero();
            tester.testContract_AlgorithmIsolation(); // 新增：算法隔离性补强
            // ⑤性能测试：千节点DAG，同时测Kahn、环检测、枚举
            tester.log("");
            tester.log("===== ④ 性能测试：1000节点随机DAG =====");
            tester.testPerformance_1000NodeDAG();
            // 汇总
            tester.log("");
            tester.log("==================== 测试汇总 ====================");
            tester.log(String.format("PASS：%d   FAIL：%d", tester.passCount, tester.failCount));
            tester.log("==================================================");

            // 新增：flush缓冲区，System.exit之前刷入磁盘，防止输出截断
            tester.out.flush();
            if(tester.failCount > 0){
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("无法写入输出文件 " + OUTPUT_FILE);
            e.printStackTrace();
            System.exit(2);
        }
    }
    // ------------------------------工具方法------------------------------
    private void log(String msg) {
        System.out.println(msg);
        out.println(msg);
    }
    private void assertTrue(String caseName, boolean condition) {
        if (condition) {
            log(String.format("[PASS] %s", caseName));
            passCount++;
        } else {
            log(String.format("[FAIL] %s", caseName));
            failCount++;
        }
    }
    /**
     * 【增强版拓扑合法性校验】
     * 1. 全部边from必须在to前面；
     * 2. 序列内部节点无重复；
     * 3. 序列节点集合与原图节点集合完全相等（节点全覆盖，不多不少）。
     */
    private boolean isTopoOrderValid(Graph g, List<String> order) {
        Set<String> graphNodes = new HashSet<>(g.getVertexNames());
        Set<String> orderNodes = new HashSet<>(order);
        if (!graphNodes.equals(orderNodes)) {
            return false;
        }
        if (order.size() != orderNodes.size()) {
            return false;
        }
        for (String from : g.getVertexNames()) {
            for (String to : g.getSuccessors(from)) {
                int idxFrom = order.indexOf(from);
                int idxTo = order.indexOf(to);
                if (idxFrom >= idxTo) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * 校验环路径：闭合，并且每一对相邻节点在原图确实存在边
     * 注意：调用方外层必须先断言 cycle 不为空
     */
    private boolean isCyclePathValid(Graph g, List<String> cycle) {
        if (!cycle.get(0).equals(cycle.get(cycle.size() - 1))) {
            return false;
        }
        for (int i = 0; i < cycle.size() - 1; i++) {
            String u = cycle.get(i);
            String v = cycle.get(i + 1);
            if (!g.getSuccessors(u).contains(v)) {
                return false;
            }
        }
        return true;
    }
    // ------------------------------①正确性测试------------------------------
    /** 手算小图1：3节点DAG A→B，A→C，合法拓扑共2条 */
    void testCorrect_ThreeNodeDAG() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("A", "C");
        TopoResult topoRes = new TopologicalSolver().kahnSort(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean kahnOk = !topoRes.hasCycle() && isTopoOrderValid(g, topoRes.getOrder());
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        boolean enumMetaOk = enumRes.getGeneratedCount() == 2
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED;
        assertTrue("正确性‑3节点DAG", kahnOk && enumAllValid && enumMetaOk);
    }
    /** 手算小图2：4节点分叉DAG A→B,A→C,B→D,C→D；合法拓扑共2条 */
    void testCorrect_FourNodeBranched() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("A", "C");
        g.addEdge("B", "D");
        g.addEdge("C", "D");
        TopoResult topoRes = new TopologicalSolver().kahnSort(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean kahnOk = !topoRes.hasCycle() && isTopoOrderValid(g, topoRes.getOrder());
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        boolean enumMetaOk = enumRes.getGeneratedCount() == 2
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED;
        assertTrue("正确性‑4节点分叉DAG", kahnOk && enumAllValid && enumMetaOk);
    }
    /** 手算小图3：链式A→B→C→D，唯一拓扑 */
    void testCorrect_ChainDAG() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("B", "C");
        g.addEdge("C", "D");
        TopoResult topoRes = new TopologicalSolver().kahnSort(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean kahnOk = !topoRes.hasCycle() && isTopoOrderValid(g, topoRes.getOrder());
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        boolean enumMetaOk = enumRes.getGeneratedCount() == 1
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED
                && enumRes.getSequences().get(0).equals(List.of("A", "B", "C", "D"));
        assertTrue("正确性‑链式DAG(唯一拓扑)", kahnOk && enumAllValid && enumMetaOk);
    }
    /** 图1课程图：增加枚举比对；增加顶点/边/warnings断言；补强枚举meta状态校验 */
    void testCorrect_FigureOneSample(File figureFile) {
        if (!figureFile.exists()) {
            log("    [ERROR] figure1.txt 文件不存在！路径：" + figureFile.getAbsolutePath());
            assertTrue("正确性‑图1课程先修图", false);
            return;
        }
        Graph g;
        try {
            String text = Files.readString(figureFile.toPath(), StandardCharsets.UTF_8);
            DataParser parser = new DataParser();
            ParseResult parseResult = parser.parse(text);
            if (!parseResult.getErrors().isEmpty()) {
                log("    图1文件解析存在错误！" + parseResult.getErrors());
                assertTrue("正确性‑图1课程先修图", false);
                return;
            }
            g = parseResult.getGraph();
            // 新增：图1契约断言：15顶点，16边，无警告
            boolean metaOk = g.getVertexCount() == 15
                    && g.getEdgeCount() == 16
                    && parseResult.getWarnings().isEmpty();
            if (!metaOk) {
                log(String.format("    [ERROR] 图1元数据不匹配: vertex=%d edge=%d warn=%s",
                        g.getVertexCount(), g.getEdgeCount(), parseResult.getWarnings()));
                assertTrue("正确性‑图1课程先修图", false);
                return;
            }
        } catch (Exception e) {
            log("    读取/解析figure1.txt异常：" + e.getMessage());
            e.printStackTrace(out);
            assertTrue("正确性‑图1课程先修图", false);
            return;
        }
        TopoResult topoRes = new TopologicalSolver().kahnSort(g);
        List<String> cyclePath = new CycleDetector().findCycle(g);
        // 任务卡要求：比对 Kahn 与枚举结果，调用enumerate
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean kahnOk = !topoRes.hasCycle() && isTopoOrderValid(g, topoRes.getOrder());
        boolean noCycle = cyclePath.isEmpty();
        boolean enumNoCycle = enumRes.getStopReason() != StopReason.CYCLE;
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        // --------补强图1枚举元数据校验--------
        boolean enumMetaOk;
        if (enumRes.getStopReason() == StopReason.LIMIT_REACHED) {
            enumMetaOk = enumRes.getGeneratedCount() == 1000 && !enumRes.isComplete();
        } else {
            enumMetaOk = enumRes.isComplete();
        }
        log("    图1课程图 Kahn序列长度：" + topoRes.getOrder().size());
        log("    图1课程图环检测返回：" + cyclePath);
        log("    图1枚举：generated=" + enumRes.getGeneratedCount() + " stop=" + enumRes.getStopReason());
        assertTrue("正确性‑图1课程先修图", kahnOk && noCycle && enumNoCycle && enumAllValid && enumMetaOk);
    }
    // ------------------------------②边界测试------------------------------
    void testBoundary_EmptyGraph() {
        Graph g = new Graph();
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cycle = new CycleDetector().findCycle(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean ok = !topo.hasCycle()
                && topo.getOrder().isEmpty()
                && cycle.isEmpty()
                && enumRes.getSequences().size() == 1
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED
                && enumRes.getSequences().get(0).isEmpty();
        assertTrue("边界‑空图", ok);
    }
    void testBoundary_SingleVertex() {
        Graph g = new Graph();
        g.addVertex("X");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cycle = new CycleDetector().findCycle(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean ok = !topo.hasCycle()
                && isTopoOrderValid(g, topo.getOrder())
                && cycle.isEmpty()
                && enumRes.getGeneratedCount() == 1
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED
                && enumRes.getSequences().contains(List.of("X"));
        assertTrue("边界‑单节点无边", ok);
    }
    void testBoundary_AllIsolated() {
        Graph g = new Graph();
        g.addVertex("P");
        g.addVertex("Q");
        g.addVertex("R");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cycle = new CycleDetector().findCycle(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean kahnOk = !topo.hasCycle() && isTopoOrderValid(g, topo.getOrder());
        boolean noCycle = cycle.isEmpty();
        boolean enumMetaOk = enumRes.getGeneratedCount() == 6 && enumRes.isComplete();
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        assertTrue("边界‑全部孤立节点", kahnOk && noCycle && enumMetaOk && enumAllValid);
    }
    /** 大量同层：A/B/C/E/F/G（6个）全部指向D，合法总数 6! =720 */
    void testBoundary_ManySameLayer() {
        Graph g = new Graph();
        g.addEdge("A", "D");
        g.addEdge("B", "D");
        g.addEdge("C", "D");
        g.addEdge("E", "D");
        g.addEdge("F", "D");
        g.addEdge("G", "D");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 2000, 0, NEVER_CANCEL);
        boolean kahnOk = !topo.hasCycle() && isTopoOrderValid(g, topo.getOrder());
        boolean enumAllValid = enumRes.getSequences().stream().allMatch(s -> isTopoOrderValid(g, s));
        // 新增：断言总数720，完整穷尽
        boolean enumMetaOk = enumRes.getGeneratedCount() == 720
                && enumRes.isComplete()
                && enumRes.getStopReason() == StopReason.COMPLETED;
        assertTrue("边界‑大量同层零入度节点", kahnOk && enumAllValid && enumMetaOk);
    }
    // ------------------------------③环测试：修复假PASS漏洞------------------------------
    void testCycle_SelfLoop() {
        Graph g = new Graph();
        g.addEdge("A", "A");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cyclePath = new CycleDetector().findCycle(g);
        boolean hasCycleFlag = topo.hasCycle();
        // 修复：不能允许空列表；自环必须等于[A,A]；Kahn含环返回空order
        boolean pathOk = cyclePath != null
                && !cyclePath.isEmpty()
                && cyclePath.equals(List.of("A", "A"))
                && isCyclePathValid(g, cyclePath);
        boolean kahnOrderEmpty = topo.getOrder().isEmpty();
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean enumRejectCycle = enumRes.getStopReason() == StopReason.CYCLE && enumRes.getGeneratedCount() == 0;
        assertTrue("环‑自环A→A", hasCycleFlag && pathOk && kahnOrderEmpty && enumRejectCycle);
    }
    void testCycle_SimpleTwoNodeCycle() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("B", "A");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cyclePath = new CycleDetector().findCycle(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean hasCycle = topo.hasCycle();
        // 修复：环路径不能为空；Kahn返回空序列
        boolean pathOk = cyclePath != null
                && !cyclePath.isEmpty()
                && cyclePath.size() >= 3
                && isCyclePathValid(g, cyclePath);
        boolean kahnOrderEmpty = topo.getOrder().isEmpty();
        boolean enumStopCycle = enumRes.getStopReason() == StopReason.CYCLE && enumRes.getGeneratedCount() == 0;
        assertTrue("环‑两节点互环", hasCycle && pathOk && kahnOrderEmpty && enumStopCycle);
    }
    void testCycle_LongCycle() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("B", "C");
        g.addEdge("C", "A");
        TopoResult topo = new TopologicalSolver().kahnSort(g);
        List<String> cyclePath = new CycleDetector().findCycle(g);
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean hasCycle = topo.hasCycle();
        boolean pathOk = cyclePath != null
                && !cyclePath.isEmpty()
                && cyclePath.size() >= 4
                && isCyclePathValid(g, cyclePath);
        boolean kahnOrderEmpty = topo.getOrder().isEmpty();
        boolean enumStopCycle = enumRes.getStopReason() == StopReason.CYCLE && enumRes.getGeneratedCount() == 0;
        assertTrue("环‑三节点长环", hasCycle && pathOk && kahnOrderEmpty && enumStopCycle);
    }
    // ------------------------------④契约校验增强------------------------------
    void testContract_ResultCompleteFlag() {
        Graph g = new Graph();
        g.addEdge("A", "C");
        g.addEdge("B", "C");
        EnumerationResult resLimit = new AllTopoSorts().enumerate(g, 1, 0, NEVER_CANCEL);
        boolean caseLimit = resLimit.getStopReason() == StopReason.LIMIT_REACHED && !resLimit.isComplete();
        Graph gCycle = new Graph();
        gCycle.addEdge("X", "Y");
        gCycle.addEdge("Y", "X");
        EnumerationResult resCycle = new AllTopoSorts().enumerate(gCycle, 1000, 0, NEVER_CANCEL);
        boolean caseCycle = resCycle.getStopReason() == StopReason.CYCLE && !resCycle.isComplete();
        assertTrue("契约‑EnumerationResult.isComplete语义", caseLimit && caseCycle);
    }
    void testContract_ResultMetaConsistency() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        EnumerationResult res = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean countMatch = res.getGeneratedCount() == res.getSequences().size();
        boolean outerUnModifiable;
        try {
            res.getSequences().add(List.of());
            outerUnModifiable = false;
        } catch (UnsupportedOperationException e) {
            outerUnModifiable = true;
        }
        // 新增：校验内部子序列也不可修改
        boolean innerSeqUnModifiable;
        try {
            if (!res.getSequences().isEmpty()) {
                res.getSequences().get(0).add("dummy");
            }
            innerSeqUnModifiable = false;
        } catch (UnsupportedOperationException e) {
            innerSeqUnModifiable = true;
        }
        assertTrue("契约‑返回元数据与不可修改集合", countMatch && outerUnModifiable && innerSeqUnModifiable);
    }
    void testContract_IllegalArguments() {
        Graph g = new Graph();
        boolean exMaxNeg;
        boolean exTimeNeg;
        try {
            new AllTopoSorts().enumerate(g, -5, 0, NEVER_CANCEL);
            exMaxNeg = false;
        } catch (IllegalArgumentException ignored) {
            exMaxNeg = true;
        }
        try {
            new AllTopoSorts().enumerate(g, 1000, -10, NEVER_CANCEL);
            exTimeNeg = false;
        } catch (IllegalArgumentException ignored) {
            exTimeNeg = true;
        }
        assertTrue("契约‑非法入参抛出异常", exMaxNeg && exTimeNeg);
    }
    /** 新增契约：null graph / null cancelled 必须抛异常 */
    void testContract_NullParams() {
        boolean exNullGraph;
        try {
            new AllTopoSorts().enumerate(null, 1000, 0, NEVER_CANCEL);
            exNullGraph = false;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            exNullGraph = true;
        }
        boolean exNullCancelled;
        try {
            Graph g = new Graph();
            new AllTopoSorts().enumerate(g, 1000, 0, null);
            exNullCancelled = false;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            exNullCancelled = true;
        }
        assertTrue("契约‑null入参拒绝", exNullGraph && exNullCancelled);
    }
    /** 新增契约：maxResults=0 代表不限制数量 */
    void testContract_MaxResultZero() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        EnumerationResult resZero = new AllTopoSorts().enumerate(g, 0, 0, NEVER_CANCEL);
        boolean ok = resZero.getStopReason() == StopReason.COMPLETED
                && resZero.isComplete()
                && resZero.getGeneratedCount() == 1;
        assertTrue("契约‑maxResults=0不限制结果", ok);
    }
    /**
     * 【可选补强契约，不属于T‑E1任务卡强制验收项】
     * 校验：1.枚举不会修改原图入度；2.同一图两次调用结果一致；3.返回各序列是独立对象引用
     */
    void testContract_AlgorithmIsolation() {
        Graph g = new Graph();
        g.addEdge("A", "B");
        g.addEdge("A", "C");
        Map<String, Integer> beforeInDegree = g.getVertexNames().stream()
                .collect(Collectors.toMap(v -> v, g::getInDegree));
        EnumerationResult res1 = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        // 校验原图入度完全不变
        boolean graphNotModified = true;
        for (String v : g.getVertexNames()) {
            if (!beforeInDegree.get(v).equals(g.getInDegree(v))) {
                graphNotModified = false;
                break;
            }
        }
        // 同一图第二次调用
        EnumerationResult res2 = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        boolean twoCallEqual = res1.getGeneratedCount() == res2.getGeneratedCount()
                && res1.getSequences().equals(res2.getSequences());
        // 序列对象引用独立，不是同一个list
        boolean seqObjectIndependent = true;
        for (int i = 0; i < res1.getSequences().size() - 1; i++) {
            List<String> s1 = res1.getSequences().get(i);
            List<String> s2 = res1.getSequences().get(i + 1);
            if (s1 == s2) {
                seqObjectIndependent = false;
                break;
            }
        }
        assertTrue("契约‑算法隔离性(原图不变/多次调用稳定/序列独立)",
                graphNotModified && twoCallEqual && seqObjectIndependent);
    }
    // ------------------------------⑤性能测试------------------------------
    void testPerformance_1000NodeDAG() {
        Graph g = new Graph();
        int vertexCount = 1000;
        for (int i = 0; i < vertexCount; i++) {
            g.addVertex("V" + i);
        }
        java.util.Random rnd = new java.util.Random(42);
        int edgeCnt = 2500;
        for (int e = 0; e < edgeCnt; e++) {
            int from = rnd.nextInt(vertexCount);
            int to = rnd.nextInt(vertexCount);
            if (from < to) {
                g.addEdge("V" + from, "V" + to);
            }
        }
        long t1 = System.currentTimeMillis();
        new TopologicalSolver().kahnSort(g);
        long costKahn = System.currentTimeMillis() - t1;
        long t2 = System.currentTimeMillis();
        new CycleDetector().findCycle(g);
        long costCycle = System.currentTimeMillis() - t2;
        long t3 = System.currentTimeMillis();
        EnumerationResult enumRes = new AllTopoSorts().enumerate(g, 1000, 0, NEVER_CANCEL);
        long costEnum = System.currentTimeMillis() - t3;
        log(String.format("    1000节点DAG，边数=%d；Kahn耗时=%d ms；环检测耗时=%d ms；枚举耗时=%d ms；已生成序列数量=%d；停止原因=%s",
                g.getEdgeCount(), costKahn, costCycle, costEnum, enumRes.getGeneratedCount(), enumRes.getStopReason()));
        // 补强：LIMIT_REACHED必须正好生成1000条
        boolean ok;
        if (enumRes.getStopReason() == StopReason.LIMIT_REACHED) {
            ok = enumRes.getGeneratedCount() == 1000 && !enumRes.isComplete();
        } else {
            ok = enumRes.getGeneratedCount() <= 1000 && enumRes.isComplete();
        }
        assertTrue("性能‑1000节点DAG(上限1000)", ok);
    }
}
