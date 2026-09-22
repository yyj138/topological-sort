import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import algorithm.AllTopoSortsSelfTest;

/**
 * A1 至 A4 作者自测统一入口，保留各测试类的独立运行方式。
 * 在仓库根目录运行；本次生成的全量结果写入 out，不覆盖历史交付记录。
 *
 * @author A
 */
public final class AlgorithmSelfTestSuite {
    private AlgorithmSelfTestSuite() {
    }

    /**
     * 依次执行图结构、Kahn、环检测和全枚举自测，失败时直接抛出异常。
     * @param args 可选的图 1 输入路径，默认 data/figure1.txt
     * @throws IOException 输入读取或复跑结果写入失败
     * @throws NoSuchAlgorithmException 当前运行环境不支持 SHA-256
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length > 1) {
            throw new IllegalArgumentException("用法：AlgorithmSelfTestSuite [图1输入文件]");
        }
        Path input = Path.of(args.length == 0 ? "data/figure1.txt" : args[0]);
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("图1输入不存在或不是普通文件：" + input);
        }
        Path output = Path.of("out", "a-self-test", "all_results.txt");

        System.out.println("A1 GraphSelfTest");
        GraphSelfTest.main(new String[0]);
        System.out.println("A2 TopologicalSolverSelfTest");
        TopologicalSolverSelfTest.main(new String[] {input.toString()});
        System.out.println("A4 CycleDetectorSelfTest");
        CycleDetectorSelfTest.main(new String[0]);
        System.out.println("A3 AllTopoSortsSelfTest");
        AllTopoSortsSelfTest.main(new String[] {input.toString(), output.toString()});
        System.out.println("A author self-test suite: 4/4 suites passed.");
    }
}
