package topo.controller;

import topo.algorithm.AllTopoSorts;
import topo.algorithm.CycleDetector;
import topo.algorithm.CycleResult;
import topo.algorithm.TopologicalSolver;
import topo.model.Edge;
import topo.model.Graph;
import topo.parser.DataParser;
import topo.parser.FileManager;
import topo.parser.InputValidator;
import topo.parser.ParseError;
import topo.parser.ParseResult;
import topo.util.ExceptionHandler;
import topo.view.GraphPanel;
import topo.view.InputPanel;
import topo.view.MainFrame;
import topo.view.ResultPanel;
import topo.view.StatusBar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// 主流程控制器（T-B4）：计算流程编排 + 菜单/按钮事件绑定
// 取输入 -> 校验 -> 解析 -> 建图 -> Kahn/全枚举/环检测 -> 刷新画布+结果+状态栏
public class MainController {

    private final MainFrame frame;
    private final InputPanel inputPanel;
    private final GraphPanel graphPanel;
    private final ResultPanel resultPanel;
    private final StatusBar statusBar;

    private int maxResults = 1000;

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
                        "拓扑排序应用软件\nJava Swing 实现\n版本：B 模块 1.0"));
        frame.getMiHelp().addActionListener(e ->
                ExceptionHandler.showInfo(frame,
                        "1. 输入或载入 <a,b> 格式的关系数据\n"
                                + "2. 点击「计算」按钮\n"
                                + "3. 在右侧图形区查看关系图与拓扑排序结果\n"
                                + "4. 支持导出图片与结果"));
    }

    private void wireSelectionListener() {
        resultPanel.setSelectionListener(sequence -> {
            graphPanel.setSelectedOrder(sequence);
            statusBar.setTip("已选中序列：" + String.join(" -> ", sequence));
        });
    }

    // 核心计算流程
    public void compute() {
        long t0 = System.currentTimeMillis();
        String text = inputPanel.getInputText();
        if (text == null || text.trim().isEmpty()) {
            ExceptionHandler.showWarning(frame, "输入数据为空，请输入 <a,b> 关系后重试");
            return;
        }
        List<ParseError> validationErrors = InputValidator.validate(text);
        if (!validationErrors.isEmpty()) {
            ExceptionHandler.showParseErrors(frame, validationErrors);
        }
        ParseResult parseResult = DataParser.parse(text);
        if (parseResult.getEdgeCount() == 0) {
            ExceptionHandler.showWarning(frame, "未解析出有效关系，请检查输入格式");
            return;
        }
        Graph graph = new Graph();
        for (Edge e : parseResult.getEdges()) {
            graph.addEdge(e.getFrom(), e.getTo());
        }
        CycleResult cycleResult = CycleDetector.detect(graph);
        List<String> kahnResult = TopologicalSolver.kahnSort(graph);
        List<List<String>> allResults;
        boolean truncated = false;
        if (cycleResult.hasCycle()) {
            allResults = new ArrayList<>();
            allResults.add(kahnResult);
        } else {
            AllTopoSorts solver = new AllTopoSorts(graph, maxResults);
            allResults = solver.compute();
            truncated = solver.isTruncated();
        }
        long cost = System.currentTimeMillis() - t0;

        graphPanel.setGraph(graph);
        if (cycleResult.hasCycle()) {
            graphPanel.setCycleHighlight(cycleResult);
        } else if (!allResults.isEmpty()) {
            graphPanel.setSelectedOrder(allResults.get(0));
        }
        resultPanel.setResults(allResults);
        statusBar.updateStats(graph.vertexCount(), graph.edgeCount(),
                cycleResult.hasCycle(), allResults.size(), cost);

        String tip;
        if (cycleResult.hasCycle()) {
            tip = "检测到环：" + cycleResult.formatPath();
            ExceptionHandler.showWarning(frame,
                    "图中存在环：" + cycleResult.formatPath()
                            + "\n仅显示已成功排序的部分序列。");
        } else if (parseResult.hasSelfLoop()) {
            tip = "图中存在自环节点：" + graph.getSelfLoops();
            ExceptionHandler.showWarning(frame, "检测到自环节点：" + graph.getSelfLoops());
        } else {
            tip = "计算完成，共 " + allResults.size()
                    + (truncated ? " 条（已截断到上限 " + maxResults + "）" : " 条");
        }
        statusBar.setTip(tip);
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
        if (FileManager.exportResults(frame, results, format)) {
            statusBar.setTip("结果已导出 (" + format.toUpperCase() + "): "
                    + FileManager.getLastOpenPath());
        }
    }

    public void setMaxResults(int max) { this.maxResults = max; }

    private void safeAction(String name, Runnable task) {
        try { task.run(); }
        catch (Exception e) {
            ExceptionHandler.handle(frame, e);
            statusBar.setTip(name + " 失败：" + e.getMessage());
        }
    }
}
