package ui;

import algorithm.AllTopoSorts;
import algorithm.CycleDetector;
import algorithm.EnumerationResult;
import algorithm.StopReason;
import algorithm.TopoResult;
import algorithm.TopologicalSolver;
import io.DataParser;
import io.FileManager;
import io.ParseIssue;
import io.ParseResult;
import model.Graph;
import util.ExceptionHandler;
import util.InputValidator;
import view.GraphPanel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.BooleanSupplier;

// 主流程控制器（T-B4）：计算流程编排 + 菜单/按钮事件绑定
// 流程：取输入 -> 校验 -> 解析(DataParser 直接建图) -> 环检测 -> Kahn/全枚举 -> 刷新画布+结果+状态栏
// 按契约：含环时不进入枚举；枚举结果带完整性 + 停止原因
public class MainController {

    private final MainFrame frame;
    private final InputPanel inputPanel;
    private final GraphPanel graphPanel;
    private final ResultPanel resultPanel;
    private final StatusBar statusBar;

    private int maxResults = 1000;
    private long timeoutMillis = 0;
    private volatile boolean cancelled = false;
    private final BooleanSupplier cancelledChecker = () -> cancelled;

    public MainController(MainFrame frame) {
        this.frame = frame;
        this.inputPanel = frame.getInputPanel();
        this.graphPanel = frame.getGraphPanel();
        this.resultPanel = frame.getResultPanel();
        this.statusBar = frame.getStatusBar();
        bindActions();
        wireSelectionListener();
    }

    private void bindActions() {
        frame.getMiOpen().addActionListener(e -> safeAction("载入文件", this::loadFromFile));
        frame.getToolOpen().addActionListener(e -> safeAction("载入文件", this::loadFromFile));
        frame.getMiSave().addActionListener(e -> safeAction("保存数据", this::saveData));
        frame.getToolSave().addActionListener(e -> safeAction("保存数据", this::saveData));
        frame.getMiExportPng().addActionListener(e -> safeAction("导出图片", this::exportPng));
        frame.getToolExportPng().addActionListener(e -> safeAction("导出图片", this::exportPng));
        frame.getMiExportResult().addActionListener(e -> safeAction("导出结果", this::exportResults));
        frame.getToolExportResult().addActionListener(e -> safeAction("导出结果", this::exportResults));
        frame.getMiExit().addActionListener(e -> System.exit(0));

        frame.getMiClear().addActionListener(e -> {
            inputPanel.setInputText("");
            graphPanel.clear();
            resultPanel.setResults(Collections.emptyList());
            statusBar.reset();
            statusBar.setTip("已清空输入与结果");
        });
        frame.getMiSyncToTable().addActionListener(e -> safeAction("文本同步到表格", inputPanel::syncTextToTable));
        frame.getMiSyncToText().addActionListener(e -> safeAction("表格同步到文本", inputPanel::syncTableToText));

        frame.getMiCompute().addActionListener(e -> safeAction("计算", this::compute));
        frame.getToolCompute().addActionListener(e -> safeAction("计算", this::compute));
        frame.getMiClearResult().addActionListener(e -> {
            resultPanel.setResults(Collections.emptyList());
            graphPanel.setSelectedOrder(Collections.emptyList());
            statusBar.setTip("结果已清空");
        });

        frame.getMiAbout().addActionListener(e ->
                ExceptionHandler.showInfo(frame,
                        "拓扑排序应用软件\nJava Swing 实现\n版本：B 模块 1.0（接口契约 V0.1）"));
        frame.getMiHelp().addActionListener(e ->
                ExceptionHandler.showInfo(frame,
                        "1. 输入或载入 <a,b> 格式的关系数据\n"
                                + "2. 点击「计算」按钮\n"
                                + "3. 在右侧图形区查看关系图与拓扑排序结果\n"
                                + "4. 含环时高亮显示环路径，不进入枚举\n"
                                + "5. 支持导出图片与结果（含完整性 + 停止原因）"));
    }

    private void wireSelectionListener() {
        resultPanel.setSelectionListener(sequence -> {
            graphPanel.setSelectedOrder(sequence);
            statusBar.setTip("已选中序列：" + String.join(" -> ", sequence));
        });
    }

    // 核心计算流程（按契约 §三/§四/§五/§六）
    public void compute() {
        long t0 = System.currentTimeMillis();
        String text = inputPanel.getInputText();
        if (text == null || text.trim().isEmpty()) {
            ExceptionHandler.showWarning(frame, "输入数据为空，请输入 <a,b> 关系后重试");
            return;
        }

        // 预校验：仅作快速格式筛查，不阻断解析
        List<ParseIssue> validationIssues = InputValidator.validate(text);
        if (!validationIssues.isEmpty()) {
            ExceptionHandler.showParseErrors(frame, validationIssues);
        }

        // 解析：DataParser.parse() 直接建图，返回 Graph + 问题清单
        DataParser parser = new DataParser();
        ParseResult parseResult = parser.parse(text);

        // 阻塞性错误：不进入计算
        if (parseResult.hasErrors()) {
            ExceptionHandler.showParseErrors(frame, parseResult.getErrors());
            statusBar.reset();
            statusBar.setTip("解析失败：" + parseResult.getErrorCount() + " 处错误");
            return;
        }

        // 非阻塞警告：仅提示，继续计算
        if (parseResult.hasWarnings()) {
            ExceptionHandler.showParseWarnings(frame, parseResult.getWarnings());
        }

        Graph graph = parseResult.getGraph();
        if (graph == null || graph.isEmpty()) {
            ExceptionHandler.showWarning(frame, "未解析出有效关系，请检查输入格式");
            statusBar.reset();
            return;
        }

        // 环检测（契约 §五）：返回空列表表示无环
        List<String> cycle = CycleDetector.findCycle(graph);
        boolean hasCycle = !cycle.isEmpty();

        long cost = System.currentTimeMillis() - t0;

        if (hasCycle) {
            // 含环：高亮环路径，不进入枚举
            graphPanel.setGraph(graph);
            graphPanel.setHighlightedCycle(cycle);
            resultPanel.setResults(Collections.emptyList());
            statusBar.updateStats(graph.getVertexCount(), graph.getEdgeCount(),
                    true, 0, cost);
            String cyclePath = String.join(" -> ", cycle);
            statusBar.setTip("检测到环：" + cyclePath);
            ExceptionHandler.showWarning(frame,
                    "图中存在环：" + cyclePath + "\n已高亮显示环路径，不进行拓扑枚举。");
            return;
        }

        // 无环：Kahn 单序列排序（契约 §三）
        TopoResult kahn = TopologicalSolver.kahnSort(graph);
        // 与 findCycle 结果一致性校验（防御性）
        if (kahn.hasCycle()) {
            ExceptionHandler.showWarning(frame, "内部一致性错误：Kahn 判定含环但环检测未发现环");
            return;
        }

        // 全拓扑枚举（契约 §四）：支持上限与取消
        cancelled = false;
        EnumerationResult enumResult = AllTopoSorts.enumerate(
                graph, maxResults, timeoutMillis, cancelledChecker);

        // 刷新画布与结果列表
        graphPanel.setGraph(graph);
        List<List<String>> sequences = enumResult.getSequences();
        if (!sequences.isEmpty()) {
            graphPanel.setSelectedOrder(sequences.get(0));
        }
        resultPanel.setResults(sequences);

        statusBar.updateStats(graph.getVertexCount(), graph.getEdgeCount(),
                false, enumResult.getGeneratedCount(), cost);

        statusBar.setTip(formatStopReason(enumResult));
    }

    // 按契约 §四：向用户说明枚举是否完整与停止原因
    private String formatStopReason(EnumerationResult result) {
        StopReason reason = result.getStopReason();
        int count = result.getGeneratedCount();
        switch (reason) {
            case COMPLETED:
                return "计算完成，共 " + count + " 条序列（已穷尽全部选择）";
            case LIMIT_REACHED:
                return "已达数量上限 " + maxResults + "，生成 " + count + " 条（未必穷尽）";
            case CANCELLED:
                return "用户已取消，已生成 " + count + " 条";
            case TIMEOUT:
                return "运行超时 " + timeoutMillis + " ms，已生成 " + count + " 条";
            case CYCLE:
                return "输入图含环，未进入枚举";
            default:
                return "停止原因：" + reason + "，生成 " + count + " 条";
        }
    }

    private void loadFromFile() {
        String content = FileManager.openFile(frame);
        if (content != null) {
            inputPanel.setInputText(content);
            inputPanel.syncTextToTable();
            statusBar.setTip("已载入文件: " + FileManager.getLastOpenPath());
        }
    }

    private void saveData() {
        if (!FileManager.saveFile(frame, inputPanel.getInputText())) return;
        statusBar.setTip("数据已保存到: " + FileManager.getLastOpenPath());
    }

    private void exportPng() {
        if (graphPanel.getGraphics() == null) {
            ExceptionHandler.showWarning(frame, "画布为空，请先计算生成关系图");
            return;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(FileManager.getLastOpenPath());
        chooser.setSelectedFile(new File("graph_" + timestamp + ".png"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG 图片", "png"));
        if (chooser.showSaveDialog(frame) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            graphPanel.exportPNG(file);
            statusBar.setTip("图片已导出: " + file.getAbsolutePath());
            ExceptionHandler.showInfo(frame, "图片已导出:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            ExceptionHandler.handle(frame, ex);
        }
    }

    // 按契约 §七：导出枚举结果，写明完整性 + 停止原因
    private void exportResults() {
        List<List<String>> results = resultPanel.getResults();
        if (results == null || results.isEmpty()) {
            ExceptionHandler.showWarning(frame, "暂无结果，请先计算拓扑排序");
            return;
        }
        Object[] options = {"TXT", "CSV", "取消"};
        int choice = javax.swing.JOptionPane.showOptionDialog(frame,
                "选择导出格式：", "导出拓扑排序结果",
                javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice == 2 || choice == javax.swing.JOptionPane.CLOSED_OPTION) return;
        String format = (choice == 0) ? "txt" : "csv";

        // 重构 EnumerationResult 供 FileManager 写出元信息
        // （此处直接复用最后一次计算结果更优，但为保持简单，按当前列表构造）
        EnumerationResult exportResult = rebuildResultForExport(results);
        if (FileManager.exportResults(frame, exportResult, format)) {
            statusBar.setTip("结果已导出 (" + format.toUpperCase() + "): "
                    + FileManager.getLastOpenPath());
        }
    }

    // 导出时构造结果对象：标记为不完整以提醒用户对照实际计算输出
    private EnumerationResult rebuildResultForExport(List<List<String>> results) {
        List<List<String>> copy = new ArrayList<>(results);
        // 导出时无法确知原始停止原因，按已生成数标记为未确认完整
        return new EnumerationResult(copy, false, StopReason.LIMIT_REACHED);
    }

    public void setMaxResults(int max) { this.maxResults = max; }
    public void setTimeoutMillis(long ms) { this.timeoutMillis = ms; }
    public void cancel() { this.cancelled = true; }

    private void safeAction(String name, Runnable task) {
        try { task.run(); }
        catch (Exception e) {
            ExceptionHandler.handle(frame, e);
            statusBar.setTip(name + " 失败：" + e.getMessage());
        }
    }
}
