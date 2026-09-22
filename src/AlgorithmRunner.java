import algorithm.AllTopoSorts;
import algorithm.CycleDetector;
import algorithm.EnumerationResult;
import algorithm.StopReason;
import algorithm.TopologicalSolver;
import algorithm.TopoResult;
import io.DataParser;
import io.ParseIssue;
import io.ParseResult;
import model.Graph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * T‑E4 命令行测试入口 AlgorithmRunner
 * 任务卡：E组员，截止9.20；src根目录，无package
 * <p>
 * 【运行说明（复制到readme.txt）】
 * 编译：javac -encoding UTF-8 src/*.java src/model/*.java src/algorithm/*.java src/io/*.java src/util/*.java src/test/*.java
 * 用法：
 *    java -cp src AlgorithmRunner <输入数据文件路径>
 *    示例：java -cp src AlgorithmRunner data/figure1.txt
 *    带输出日志：java -cp src AlgorithmRunner data/figure1.txt test/algorithm_runner_sample_output.txt
 * </p>
 * 功能：
 * 1.接收命令行第一个参数作为输入txt路径；无参数打印帮助提示并退出
 * 2.可选传入第二个参数输出文件路径，则同时将日志写入指定UTF‑8文本文件
 * 3.读取UTF‑8文本，调用DataParser解析得到Graph、错误、警告
 * 4.打印解析错误/警告；存在解析错误直接终止，不执行算法；错误同时输出控制台+日志文件
 * 5.依次执行：Kahn单条拓扑排序、环路径检测、AllTopoSorts全部拓扑枚举
 * 6.输出：节点数、边数、环路径(如有)、Kahn序列、枚举统计、各阶段耗时(ms)
 * 7.不依赖Swing GUI，供冒烟脚本run_tests.bat调用
 *
 * @author E
 */
public class AlgorithmRunner {
    private static final BooleanSupplier NEVER_CANCEL = () -> false;
    // 枚举上限，与接口契约保持默认1000条
    private static final int MAX_ENUM_RESULTS = 1000;
    private static final long TIMEOUT_MS = 0; // 0代表不限制超时

    public static void main(String[] args) {
        // ==========1 参数校验 ==========
        if (args.length < 1) {
            System.err.println("=== AlgorithmRunner 使用帮助 ===");
            System.err.println("用法：java -cp src AlgorithmRunner <输入数据文件路径> [输出txt文件路径]");
            System.err.println("示例：java -cp src AlgorithmRunner data/figure1.txt");
            System.exit(1);
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.printf("[错误] 文件不存在：%s%n", inputFile.getAbsolutePath());
            System.exit(2);
        }

        File outFile = null;
        // 【修复】先只保存输出文件对象，暂时不打开输出流，避免提前截断文件
        if (args.length >= 2) {
            outFile = new File(args[1]);
            // 高危校验：禁止输入文件和输出文件为同一个文件，防止源文件被清空
            if (inputFile.equals(outFile)) {
                System.err.printf("[错误]禁止输入文件与输出文件为同一个文件：%s%n", args[0]);
                System.exit(5);
            }
        }

        // ==========2 先完整读取输入文件（全部读完之后，再打开输出文件） ==========
        String fileText = null;
        try {
            fileText = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.printf("[错误]读取文件失败：%s%n", e.getMessage());
            System.exit(3);
        }

        // 输入读取成功，此时才去创建输出文件
        PrintWriter fileWriter = null;
        if (outFile != null) {
            try {
                // 父目录不存在自动创建
                if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                    outFile.getParentFile().mkdirs();
                }
                fileWriter = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream(outFile, false), StandardCharsets.UTF_8)
                );
            } catch (Exception e) {
                System.err.printf("[警告]无法打开输出文件，不会写入txt：%s%n", e.getMessage());
            }
        }

        // 辅助打印函数：同时输出控制台+文件（如果打开成功）
        final PrintWriter fw = fileWriter;
        Consumer<String> printLine = (String text) -> {
            System.out.println(text);
            if (fw != null) {
                fw.println(text);
            }
        };

        printLine.accept("==============================================");
        printLine.accept("输入文件：" + args[0]);
        printLine.accept("==============================================");

        DataParser parser = new DataParser();
        ParseResult parseResult = parser.parse(fileText);
        List<ParseIssue> parseErrors = parseResult.getErrors();
        List<ParseIssue> parseWarnings = parseResult.getWarnings();
        Graph graph = parseResult.getGraph();

        // 打印解析警告
        if (!parseWarnings.isEmpty()) {
            printLine.accept("【解析警告】");
            for (ParseIssue w : parseWarnings) {
                printLine.accept(String.format("  行%d : %s", w.getLineNumber(), w.getMessage()));
            }
        }

        // 【修复】解析错误改用printLine，错误写入日志文件，不再只用System.err
        if (!parseErrors.isEmpty()) {
            printLine.accept("【解析错误，终止计算】");
            for (ParseIssue err : parseErrors) {
                printLine.accept(String.format("  行%d : %s", err.getLineNumber(), err.getMessage()));
            }
            closeWriterAndCheckError(fw);
            System.exit(4);
        }

        // ==========3 图基础信息 ==========
        int vertexCnt = graph.getVertexCount();
        int edgeCnt = graph.getEdgeCount();
        printLine.accept(String.format("%n【图基础信息】节点数=%d ，边数=%d", vertexCnt, edgeCnt));

        // ==========4 Kahn算法计时运行 ==========
        long kahnStart = System.currentTimeMillis();
        TopologicalSolver solver = new TopologicalSolver();
        TopoResult kahnRes = solver.kahnSort(graph);
        long kahnCost = System.currentTimeMillis() - kahnStart;

        printLine.accept("\n【Kahn算法结果】");
        if (kahnRes.hasCycle()) {
            printLine.accept("  检测到图存在环，无Kahn完整拓扑序列");
        } else {
            printLine.accept("  Kahn单条拓扑序列：" + kahnRes.getOrder());
        }
        printLine.accept(String.format("  Kahn计算耗时：%d ms", kahnCost));

        // ==========5 环路径检测 ==========
        long cycleStart = System.currentTimeMillis();
        CycleDetector cycleDetector = new CycleDetector();
        List<String> cyclePath = cycleDetector.findCycle(graph);
        long cycleCost = System.currentTimeMillis() - cycleStart;

        printLine.accept("\n【环检测结果】");
        if (cyclePath.isEmpty()) {
            printLine.accept("  未检测到环（DAG有向无环图）");
        } else {
            printLine.accept("  检测到环路径：" + String.join("→", cyclePath));
        }
        printLine.accept(String.format("  环检测耗时：%d ms", cycleCost));

        // ==========6 AllTopoSorts 全部拓扑枚举 ==========
        long enumStart = System.currentTimeMillis();
        EnumerationResult enumRes = new AllTopoSorts()
                .enumerate(graph, MAX_ENUM_RESULTS, TIMEOUT_MS, NEVER_CANCEL);
        long enumCost = System.currentTimeMillis() - enumStart;

        printLine.accept("\n【全部拓扑枚举结果】");
        printLine.accept("  生成序列总数：" + enumRes.getGeneratedCount());
        printLine.accept("  停止原因：" + enumRes.getStopReason());
        printLine.accept("  是否完整穷尽全部解：" + enumRes.isComplete());
        if (enumRes.getStopReason() == StopReason.COMPLETED || enumRes.getStopReason() == StopReason.LIMIT_REACHED) {
            List<List<String>> seqList = enumRes.getSequences();
            int showMax = Math.min(5, seqList.size());
            printLine.accept("  输出前" + showMax + "条序列样例：");
            for (int i = 0; i < showMax; i++) {
                printLine.accept("    [" + (i + 1) + "] " + seqList.get(i));
            }
        }
        printLine.accept(String.format("  枚举计算耗时：%d ms", enumCost));

        printLine.accept("\n==============================================");
        printLine.accept("AlgorithmRunner 执行完毕");
        printLine.accept("==============================================");

        // 【修复】写入文件成功提示，同时写入txt日志文件
        if (fw != null) {
            printLine.accept("\n✅输出已写入文件：" + args[1]);
        }

        // 关闭PrintWriter并且checkError捕获底层IO写入异常
        closeWriterAndCheckError(fw);
    }

    /**
     * 工具：关闭PrintWriter，调用checkError检测PrintWriter吞掉的IO异常，控制台输出警告
     */
    private static void closeWriterAndCheckError(PrintWriter pw) {
        if (pw == null) {
            return;
        }
        pw.flush();
        boolean hasIoError = pw.checkError();
        pw.close();
        if (hasIoError) {
            System.err.println("[警告]输出文件写入过程发生IO错误！部分内容可能未保存。");
        }
    }
}
