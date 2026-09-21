package ui;

import algorithm.AllTopoSorts;
import algorithm.CycleDetector;
import algorithm.EnumerationResult;
import algorithm.StopReason;
import io.DataParser;
import io.FileManager;
import io.ParseIssue;
import io.ParseResult;
import model.Graph;
import util.ExceptionHandler;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 主控制器（T-B4/T-B7）：连接菜单/工具栏事件与解析、取图、计算、导出流程
// 解析复用 D 的 DataParser（契约 §6：parse 返回 ParseResult，B 直接取 getGraph()）
// 全拓扑枚举在 SwingWorker 后台执行，可随时取消
public class MainController {

    // 枚举上限与超时：防止超大图长时间阻塞
    private static final int MAX_RESULTS = 1000;
    private static final long TIMEOUT_MILLIS = 30_000L;

    private final MainFrame frame;
    // 契约 §6：DataParser 为实例方法，B 持有解析器实例
    private final DataParser dataParser = new DataParser();

    private Graph currentGraph;
    private volatile boolean cancelRequested;
    private EnumerationWorker currentWorker;
    // worker 被取消时 get() 抛异常，用字段保留其已生成的部分结果
    private volatile EnumerationResult lastResult;

    private long costMillis;

    public MainController(MainFrame frame) {
        this.frame = frame;
        bindActions();
    }

    private void bindActions() {
        // 文件
        frame.getMiOpen().addActionListener(e -> frame.getInputPanel().requestLoadFile());
        frame.getToolOpen().addActionListener(e -> frame.getInputPanel().requestLoadFile());
        frame.getMiSave().addActionListener(e -> frame.getInputPanel().requestSaveFile());
        frame.getToolSave().addActionListener(e -> frame.getInputPanel().requestSaveFile());
        frame.getMiExportPng().addActionListener(e -> exportPNG());
        frame.getToolExportPng().addActionListener(e -> exportPNG());
        frame.getMiExportResult().addActionListener(e -> exportResult());
        frame.getToolExportResult().addActionListener(e -> exportResult());
        frame.getMiExit().addActionListener(e -> frame.dispose());

        // 编辑
        frame.getMiClear().addActionListener(e -> frame.getInputPanel().clearAll());
        frame.getMiSyncToTable().addActionListener(e -> frame.getInputPanel().syncTextToTable());
        frame.getMiSyncToText().addActionListener(e -> frame.getInputPanel().syncTableToText());

        // 计算
        frame.getMiCompute().addActionListener(e -> compute());
        frame.getToolCompute().addActionListener(e -> compute());
        frame.getMiCancel().addActionListener(e -> cancelCompute());
        frame.getToolCancel().addActionListener(e -> cancelCompute());
        frame.getMiClearResult().addActionListener(e -> clearResults());

        // 帮助
        frame.getMiHelp().addActionListener(e -> ExceptionHandler.showInfo(frame,
                "1. 在文本区按 <a,b> 格式输入关系，每行一条\n"
                + "2. # 开头为注释，可载入/保存数据文件\n"
                + "3. 点击计算：有环将标红环路径，无环则列出多种拓扑序列（默认最多1000条）\n"
                + "4. 单击结果可在图中高亮，双击可复制"));
        frame.getMiAbout().addActionListener(e -> ExceptionHandler.showInfo(frame,
                "拓扑排序应用软件\nCST4823A 高级算法原理实践\n开发分支 dev-b"));

        // 布局切换和视图控制
        frame.getToolLayoutLayered().addActionListener(e -> frame.getGraphPanel().switchLayout(0));
        frame.getToolLayoutCircular().addActionListener(e -> frame.getGraphPanel().switchLayout(1));
        frame.getToolResetView().addActionListener(e -> frame.getGraphPanel().resetView());

        // 结果选中 -> 图上高亮该序列；颜色随结果序号轮换色板，点不同行颜色不同
        frame.getResultPanel().setSelectionListener(seq -> {
            int idx = frame.getResultPanel().getSelectedResultIndex();
            frame.getGraphPanel().setSelectedOrder(seq, Math.max(idx, 0));
        });
    }

    // 核心流程：解析 -> 校验 -> 取图 -> 判环 -> 后台枚举
    // 契约 §6：D 的 DataParser.parse 返回 ParseResult，通过 getGraph() 直接交付 model.Graph
    // B 不再手动建图；问题类型为 io.ParseIssue
    private void compute() {
        String text = frame.getInputPanel().getInputText();
        if (text == null || text.trim().isEmpty()) {
            ExceptionHandler.showWarning(frame, "请输入关系数据后再计算");
            return;
        }

        long start = System.currentTimeMillis();

        // D 的 DataParser 是唯一解析入口（契约 §6 + 9.17 会议决议）
        ParseResult parsed = dataParser.parse(text);

        // 解析错误：直接中止，不进入取图与计算
        List<ParseIssue> errors = parsed.getErrors();
        if (!errors.isEmpty()) {
            List<String> msgs = new ArrayList<>();
            for (ParseIssue err : errors) {
                int line = err.getLineNumber();
                if (line > 0) {
                    msgs.add("第" + line + "行：" + err.getMessage());
                } else {
                    msgs.add(err.getMessage());
                }
            }
            ExceptionHandler.showParseErrors(frame, msgs);
            return;
        }

        // 警告（如重复关系）：不中止，提示用户
        List<ParseIssue> warnings = parsed.getWarnings();
        if (!warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder("解析提示：\n");
            int show = Math.min(warnings.size(), 10);
            for (int i = 0; i < show; i++) {
                ParseIssue w = warnings.get(i);
                int line = w.getLineNumber();
                String reason = w.getMessage();
                sb.append(line > 0 ? "第" + line + "行：" + reason : reason);
                sb.append("\n");
            }
            if (warnings.size() > show) {
                sb.append("... 共 ").append(warnings.size()).append(" 条提示");
            }
            ExceptionHandler.showInfo(frame, sb.toString());
        }

        // 契约 §6：Graph 直接来自 ParseResult.getGraph()，B 不再手动建图
        Graph graph = parsed.getGraph();
        if (graph.getVertexCount() == 0 && graph.getEdgeCount() == 0) {
            ExceptionHandler.showWarning(frame, "没有有效的关系数据，请检查输入");
            return;
        }

        this.currentGraph = graph;
        this.costMillis = System.currentTimeMillis() - start;

        frame.getGraphPanel().setGraph(graph);

        List<String> cycle = new CycleDetector().findCycle(graph);
        if (!cycle.isEmpty()) {
            onCycleFound(graph, cycle);
            return;
        }
        startEnumeration(graph);
    }

    private void onCycleFound(Graph graph, List<String> cycle) {
        frame.getGraphPanel().setHighlightedCycle(cycle);
        frame.getResultPanel().setResults(new ArrayList<>());
        frame.getStatusBar().updateStats(
                graph.getVertexCount(), graph.getEdgeCount(), true, 0, costMillis);
        frame.getStatusBar().setTip("检测到环：" + String.join(" -> ", cycle));
        ExceptionHandler.showWarning(frame,
                "图中存在环，无法进行拓扑排序：\n" + String.join(" -> ", cycle));
    }

    // 后台枚举，UI 不冻结
    private void startEnumeration(Graph graph) {
        cancelRequested = false;
        lastResult = null;
        setBusy(true);
        frame.getStatusBar().setTip("计算中，正在枚举拓扑序列...");

        currentWorker = new EnumerationWorker(graph);
        currentWorker.execute();
    }

    private class EnumerationWorker extends SwingWorker<EnumerationResult, Void> {
        private final Graph graph;

        EnumerationWorker(Graph graph) { this.graph = graph; }

        @Override
        protected EnumerationResult doInBackground() {
            EnumerationResult result = new AllTopoSorts().enumerate(
                    graph, MAX_RESULTS, TIMEOUT_MILLIS,
                    () -> cancelRequested || isCancelled());
            lastResult = result;
            return result;
        }

        @Override
        protected void done() {
            setBusy(false);
            EnumerationResult result = lastResult;
            if (result == null) {
                frame.getStatusBar().setTip("计算中断");
                return;
            }
            applyEnumerationResult(graph, result);
        }
    }

    private void applyEnumerationResult(Graph graph, EnumerationResult result) {
        List<List<String>> sequences = result.getSequences();
        frame.getResultPanel().setResults(sequences);

        // 不自动高亮第一条，等用户单击结果时再高亮
        frame.getStatusBar().updateStats(
                graph.getVertexCount(), graph.getEdgeCount(),
                false, sequences.size(), costMillis);
        frame.getStatusBar().setTip(tipFor(result));
    }

    private String tipFor(EnumerationResult result) {
        StopReason reason = result.getStopReason();
        switch (reason) {
            case COMPLETED:
                return "枚举完成，共 " + result.getGeneratedCount() + " 条序列";
            case LIMIT_REACHED:
                return "达到结果上限 " + MAX_RESULTS + " 条，仅显示部分序列";
            case TIMEOUT:
                return "超时停止，已显示 " + result.getGeneratedCount() + " 条序列";
            case CANCELLED:
                return "已取消，已显示 " + result.getGeneratedCount() + " 条部分序列";
            case CYCLE:
                return "图中存在环";
            default:
                return "就绪";
        }
    }

    private void cancelCompute() {
        cancelRequested = true;
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }
    }

    private void clearResults() {
        frame.getResultPanel().setResults(new ArrayList<>());
        frame.getGraphPanel().setSelectedOrder(new ArrayList<>());
        frame.getStatusBar().reset();
        frame.getStatusBar().setTip("结果已清空");
    }

    private void setBusy(boolean busy) {
        frame.getMiCompute().setEnabled(!busy);
        frame.getToolCompute().setEnabled(!busy);
        frame.getMiCancel().setEnabled(busy);
        frame.getToolCancel().setEnabled(busy);
    }

    // 导出全部枚举序列：按扩展名选择 txt / csv，复用 D 的 FileManager
    private void exportResult() {
        List<List<String>> results = frame.getResultPanel().getResults();
        if (results == null || results.isEmpty()) {
            ExceptionHandler.showWarning(frame, "没有可导出的结果，请先计算");
            return;
        }

        JFileChooser chooser = new JFileChooser(FileManager.getLastOpenedDirectory());
        chooser.setSelectedFile(new File("topo_result.txt"));
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("文本文件", "txt"));
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("CSV 文件", "csv"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        boolean csv = file.getName().toLowerCase().endsWith(".csv");
        if (!csv && !file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }

        List<String> lines = new ArrayList<>();
        for (List<String> seq : results) {
            lines.add(String.join(" -> ", seq));
        }
        // 契约 §七：导出必须写明完整性与停止原因（B 作中间人，从 A 的 EnumerationResult 转成中文传给 D）
        boolean isComplete = lastResult != null && lastResult.isComplete();
        String stopReasonText = stopReasonText(lastResult);
        try {
            if (csv) FileManager.exportCsv(file, lines, isComplete, stopReasonText);
            else FileManager.exportTxt(file, lines, isComplete, stopReasonText);
            ExceptionHandler.showInfo(frame, "导出成功：" + file.getName());
        } catch (Exception ex) {
            ExceptionHandler.handle(frame, ex);
        }
    }

    // 把 A 的 StopReason 枚举转成导出文件头用的简短中文文案（与 D 约定一致）
    private String stopReasonText(EnumerationResult result) {
        if (result == null || result.getStopReason() == null) {
            return "未知";
        }
        switch (result.getStopReason()) {
            case COMPLETED:
                return "已生成全部结果";
            case LIMIT_REACHED:
                return "达到输出上限，仅显示前 " + result.getGeneratedCount() + " 条";
            case CANCELLED:
                return "用户取消";
            case TIMEOUT:
                return "计算超时";
            case CYCLE:
                return "输入图含环，无合法拓扑排序";
            default:
                return "未知";
        }
    }

    private void exportPNG() {
        if (currentGraph == null) {
            ExceptionHandler.showWarning(frame, "没有可导出的关系图，请先计算");
            return;
        }
        JFileChooser chooser = new JFileChooser(FileManager.getLastOpenedDirectory());
        chooser.setSelectedFile(new File("graph.png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 图片", "png"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            frame.getGraphPanel().exportPNG(file);
            ExceptionHandler.showInfo(frame, "图片已导出：" + file.getName());
        } catch (Exception ex) {
            ExceptionHandler.handle(frame, ex);
        }
    }
}
