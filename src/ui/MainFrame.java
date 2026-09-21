package ui;

import util.UIStyle;
import view.GraphPanel;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

// 主窗口（T-B1）：菜单+工具栏+输入区+图形区+结果区+状态栏
public class MainFrame extends JFrame {

    private final InputPanel inputPanel = new InputPanel();
    private final GraphPanel graphPanel = new GraphPanel();
    private final ResultPanel resultPanel = new ResultPanel();
    private final StatusBar statusBar = new StatusBar();

    private JMenuItem miOpen, miSave, miExportPng, miExportResult, miExit;
    private JMenuItem miClear, miSyncToTable, miSyncToText;
    private JMenuItem miCompute, miCancel, miClearResult;
    private JMenuItem miAbout, miHelp;
    private javax.swing.JButton toolOpen, toolSave, toolCompute, toolCancel,
            toolExportPng, toolExportResult,
            toolLayoutLayered, toolLayoutCircular,
            toolZoomIn, toolZoomOut, toolResetView;

    public MainFrame() {
        super("拓扑排序应用软件");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 600));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIStyle.BG_MAIN);
        setContentPane(root);

        root.add(buildMenuBar(), BorderLayout.NORTH);
        root.add(buildToolBar(), BorderLayout.PAGE_START);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        inputPanel.installDefaultSyncActions(this);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        UIStyle.styleMenuBar(bar);

        JMenu mFile = new JMenu("文件(F)");
        mFile.setMnemonic('F');
        UIStyle.styleMenu(mFile);
        miOpen = createMenuItem("打开...", 'O');
        miSave = createMenuItem("保存数据", 'S');
        miExportPng = createMenuItem("导出图片");
        miExportResult = createMenuItem("导出结果...");
        miExit = createMenuItem("退出", 'X');
        mFile.add(miOpen); mFile.add(miSave); mFile.addSeparator();
        mFile.add(miExportPng); mFile.add(miExportResult); mFile.addSeparator();
        mFile.add(miExit);
        bar.add(mFile);

        JMenu mEdit = new JMenu("编辑(E)");
        mEdit.setMnemonic('E');
        UIStyle.styleMenu(mEdit);
        miClear = createMenuItem("清空输入");
        miSyncToTable = createMenuItem("文本 -> 表格");
        miSyncToText = createMenuItem("表格 -> 文本");
        mEdit.add(miSyncToTable); mEdit.add(miSyncToText);
        mEdit.addSeparator(); mEdit.add(miClear);
        bar.add(mEdit);

        JMenu mCalc = new JMenu("计算(C)");
        mCalc.setMnemonic('C');
        UIStyle.styleMenu(mCalc);
        miCompute = createMenuItem("计算拓扑排序", 'C');
        miCancel = createMenuItem("取消计算");
        miCancel.setEnabled(false);
        miClearResult = createMenuItem("清空结果");
        mCalc.add(miCompute); mCalc.add(miCancel); mCalc.addSeparator();
        mCalc.add(miClearResult);
        bar.add(mCalc);

        JMenu mHelp = new JMenu("帮助(H)");
        mHelp.setMnemonic('H');
        UIStyle.styleMenu(mHelp);
        miAbout = createMenuItem("关于");
        miHelp = createMenuItem("使用说明");
        mHelp.add(miHelp); mHelp.addSeparator(); mHelp.add(miAbout);
        bar.add(mHelp);

        return bar;
    }

    private JMenuItem createMenuItem(String text, char mnemonic) {
        JMenuItem item = new JMenuItem(text);
        if (mnemonic != 0) item.setMnemonic(mnemonic);
        UIStyle.styleMenuItem(item);
        return item;
    }

    private JMenuItem createMenuItem(String text) {
        return createMenuItem(text, (char) 0);
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        UIStyle.styleToolBar(bar);

        toolOpen = makeTextButton("打开");
        toolSave = makeTextButton("保存");
        bar.add(toolOpen); bar.add(toolSave); bar.addSeparator();
        toolCompute = makeTextButton("计算");
        toolCancel = makeTextButton("取消计算");
        toolCancel.setEnabled(false);
        bar.add(toolCompute); bar.add(toolCancel); bar.addSeparator();
        toolExportPng = makeTextButton("导出图片");
        toolExportResult = makeTextButton("导出结果");
        bar.add(toolExportPng); bar.add(toolExportResult);
        bar.addSeparator();
        toolLayoutLayered = makeTextButton("分层布局");
        toolLayoutCircular = makeTextButton("环形布局");
        toolZoomIn = makeTextButton("放大");
        toolZoomOut = makeTextButton("缩小");
        toolResetView = makeTextButton("重置视图");
        bar.add(toolLayoutLayered); bar.add(toolLayoutCircular);
        bar.add(toolZoomIn); bar.add(toolZoomOut);
        bar.add(toolResetView);

        return bar;
    }

    private javax.swing.JButton makeTextButton(String text) {
        javax.swing.JButton btn = new javax.swing.JButton(text);
        UIStyle.styleIconButton(btn);
        return btn;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIStyle.BG_MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(
                UIStyle.GAP_SMALL, UIStyle.GAP_SMALL, UIStyle.GAP_SMALL, UIStyle.GAP_SMALL));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrapGraph(graphPanel), resultPanel);
        rightSplit.setResizeWeight(0.65);
        rightSplit.setDividerSize(6);
        rightSplit.setBorder(null);
        rightSplit.setBackground(UIStyle.BG_MAIN);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                inputPanel, rightSplit);
        mainSplit.setResizeWeight(0.32);
        mainSplit.setDividerSize(6);
        mainSplit.setBorder(null);
        mainSplit.setBackground(UIStyle.BG_MAIN);

        content.add(mainSplit, BorderLayout.CENTER);
        return content;
    }

    private JPanel wrapGraph(GraphPanel gp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.BG_PANEL);
        UIStyle.applyTitledPanel(p, "关系图视图");
        p.add(gp, BorderLayout.CENTER);
        return p;
    }

    public InputPanel getInputPanel() { return inputPanel; }
    public GraphPanel getGraphPanel() { return graphPanel; }
    public ResultPanel getResultPanel() { return resultPanel; }
    public StatusBar getStatusBar() { return statusBar; }

    public JMenuItem getMiOpen() { return miOpen; }
    public JMenuItem getMiSave() { return miSave; }
    public JMenuItem getMiExportPng() { return miExportPng; }
    public JMenuItem getMiExportResult() { return miExportResult; }
    public JMenuItem getMiExit() { return miExit; }
    public JMenuItem getMiClear() { return miClear; }
    public JMenuItem getMiSyncToTable() { return miSyncToTable; }
    public JMenuItem getMiSyncToText() { return miSyncToText; }
    public JMenuItem getMiCompute() { return miCompute; }
    public JMenuItem getMiCancel() { return miCancel; }
    public JMenuItem getMiClearResult() { return miClearResult; }
    public JMenuItem getMiAbout() { return miAbout; }
    public JMenuItem getMiHelp() { return miHelp; }

    public javax.swing.JButton getToolOpen() { return toolOpen; }
    public javax.swing.JButton getToolSave() { return toolSave; }
    public javax.swing.JButton getToolCompute() { return toolCompute; }
    public javax.swing.JButton getToolCancel() { return toolCancel; }
    public javax.swing.JButton getToolExportPng() { return toolExportPng; }
    public javax.swing.JButton getToolExportResult() { return toolExportResult; }
    public javax.swing.JButton getToolLayoutLayered() { return toolLayoutLayered; }
    public javax.swing.JButton getToolLayoutCircular() { return toolLayoutCircular; }
    public javax.swing.JButton getToolResetView() { return toolResetView; }
    public javax.swing.JButton getToolZoomIn() { return toolZoomIn; }
    public javax.swing.JButton getToolZoomOut() { return toolZoomOut; }

    // 独立启动入口
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            // 安装控制器，连接菜单/按钮事件与计算流程
            new MainController(frame);
            frame.setVisible(true);
        });
    }
}
