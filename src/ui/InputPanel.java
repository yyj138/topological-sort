package ui;

import io.DataParser;
import io.FileManager;
import util.UIStyle;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

// 输入面板（T-B2）：文本区 + 表格视图，载入/保存/同步/增删行
// 直接复用 D 的 DataParser 与 FileManager（9.17 会议决议）
public class InputPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"起点", "终点"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return true; }
    };
    private final JTable table = new JTable(tableModel);

    private final JButton btnLoadFile = new JButton("载入文件");
    private final JButton btnSaveData = new JButton("保存数据");
    private final JButton btnToTable  = new JButton("文本 -> 表格");
    private final JButton btnToText   = new JButton("表格 -> 文本");
    private final JButton btnAddRow   = new JButton("+ 增行");
    private final JButton btnDelRow   = new JButton("- 删行");
    private final JButton btnClear    = new JButton("清空");

    public InputPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(
                UIStyle.GAP_SMALL, UIStyle.GAP_SMALL,
                UIStyle.GAP_SMALL, UIStyle.GAP_SMALL));
        setBackground(UIStyle.BG_MAIN);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildSplitContent(), BorderLayout.CENTER);
        initTableEditor();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setBackground(UIStyle.BG_MAIN);
        bar.setBorder(BorderFactory.createEmptyBorder(UIStyle.GAP_TINY, 0, UIStyle.GAP_SMALL, 0));

        UIStyle.styleButton(btnLoadFile);
        UIStyle.styleButton(btnSaveData);
        UIStyle.styleButton(btnToTable);
        UIStyle.styleButton(btnToText);
        UIStyle.styleButton(btnAddRow);
        UIStyle.styleButton(btnDelRow);
        UIStyle.styleButton(btnClear);

        bar.add(btnLoadFile); bar.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        bar.add(btnSaveData); bar.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        bar.add(btnToTable);  bar.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        bar.add(btnToText);   bar.add(Box.createHorizontalGlue());
        bar.add(btnAddRow);   bar.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        bar.add(btnDelRow);   bar.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        bar.add(btnClear);
        return bar;
    }

    private JSplitPane buildSplitContent() {
        textArea.setFont(UIStyle.FONT_MONO);
        textArea.setBackground(UIStyle.BG_PANEL);
        textArea.setForeground(UIStyle.FG_PRIMARY);
        textArea.setLineWrap(false);
        textArea.setText(defaultHint());
        JScrollPane textScroll = new JScrollPane(textArea);
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(UIStyle.BG_PANEL);
        JLabel textTitle = new JLabel("  文本编辑区");
        UIStyle.styleSubtitleLabel(textTitle);
        textTitle.setBorder(BorderFactory.createEmptyBorder(
                UIStyle.GAP_TINY, UIStyle.GAP_TINY,
                UIStyle.GAP_TINY, UIStyle.GAP_TINY));
        textPanel.add(textTitle, BorderLayout.NORTH);
        textPanel.add(textScroll, BorderLayout.CENTER);

        table.setFont(UIStyle.FONT_BODY);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(table);
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(UIStyle.BG_PANEL);
        JLabel tableTitle = new JLabel("  表格编辑视图");
        UIStyle.styleSubtitleLabel(tableTitle);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(
                UIStyle.GAP_TINY, UIStyle.GAP_TINY,
                UIStyle.GAP_TINY, UIStyle.GAP_TINY));
        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textPanel, tablePanel);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        split.setBackground(UIStyle.BG_MAIN);
        split.setBorder(null);
        return split;
    }

    private void initTableEditor() {
        JTextField cellField = new JTextField();
        cellField.setFont(UIStyle.FONT_BODY);
        table.setDefaultEditor(Object.class, new DefaultCellEditor(cellField));
    }

    // 预设中文提示：第一行即格式说明，全部以 # 注释
    private static String defaultHint() {
        return "# 请按 <a,b> 格式输入数据，每行一条关系，# 开头为注释\n"
                + "# a 为前驱，b 为后继，如 <a,b> 表示有向边 a -> b\n"
                + "# 示例：\n"
                + "<MA 140,MA 141>\n"
                + "<MA 141,CS 150>\n";
    }

    public String getInputText() { return textArea.getText(); }
    public void setInputText(String text) { textArea.setText(text); }

    // 供主菜单/工具栏复用
    public void requestLoadFile() { loadFile(this); }
    public void requestSaveFile() { saveFile(this); }
    public void clearAll() {
        textArea.setText("");
        tableModel.setRowCount(0);
    }

    // 默认行为：载入/保存/同步/增删行/清空
    public void installDefaultSyncActions(Component dialogParent) {
        btnToTable.setAction(new AbstractAction("文本 -> 表格") {
            @Override public void actionPerformed(ActionEvent e) { syncTextToTable(); }
        });
        UIStyle.styleButton(btnToTable);

        btnToText.setAction(new AbstractAction("表格 -> 文本") {
            @Override public void actionPerformed(ActionEvent e) { syncTableToText(); }
        });
        UIStyle.styleButton(btnToText);

        btnAddRow.setAction(new AbstractAction("+ 增行") {
            @Override public void actionPerformed(ActionEvent e) {
                tableModel.addRow(new Object[]{"", ""});
            }
        });
        UIStyle.styleButton(btnAddRow);

        btnDelRow.setAction(new AbstractAction("- 删行") {
            @Override public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0 && table.getRowCount() > 0) row = table.getRowCount() - 1;
                if (row >= 0) tableModel.removeRow(row);
            }
        });
        UIStyle.styleButton(btnDelRow);

        btnClear.setAction(new AbstractAction("清空") {
            @Override public void actionPerformed(ActionEvent e) {
                textArea.setText("");
                tableModel.setRowCount(0);
            }
        });
        UIStyle.styleButton(btnClear);

        btnLoadFile.setAction(new AbstractAction("载入文件") {
            @Override public void actionPerformed(ActionEvent e) { loadFile(dialogParent); }
        });
        UIStyle.styleButton(btnLoadFile);

        btnSaveData.setAction(new AbstractAction("保存数据") {
            @Override public void actionPerformed(ActionEvent e) { saveFile(dialogParent); }
        });
        UIStyle.styleButton(btnSaveData);
    }

    private void loadFile(Component parent) {
        JFileChooser chooser = new JFileChooser(FileManager.getLastOpenedDirectory());
        chooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt", "csv"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            String content = FileManager.readFile(file);
            textArea.setText(content);
            syncTextToTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "载入文件失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile(Component parent) {
        JFileChooser chooser = new JFileChooser(FileManager.getLastOpenedDirectory());
        chooser.setSelectedFile(new File("data.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        try {
            FileManager.saveFile(file, getInputText());
            JOptionPane.showMessageDialog(parent, "保存成功",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "保存失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 用 D 解析器把文本解析为边（含自环），填入表格
    public void syncTextToTable() {
        DataParser.ParseResult result = DataParser.parse(textArea.getText());
        tableModel.setRowCount(0);
        for (DataParser.Edge edge : result.getEdges()) {
            tableModel.addRow(new Object[]{edge.getSource(), edge.getTarget()});
        }
        for (DataParser.Edge selfLoop : result.getSelfLoops()) {
            tableModel.addRow(new Object[]{selfLoop.getSource(), selfLoop.getTarget()});
        }
    }

    public void syncTableToText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 表格编辑生成的关系数据\n");
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object fromObj = tableModel.getValueAt(i, 0);
            Object toObj = tableModel.getValueAt(i, 1);
            if (fromObj == null || toObj == null) continue;
            String from = fromObj.toString().trim();
            String to = toObj.toString().trim();
            if (from.isEmpty() || to.isEmpty()) continue;
            sb.append("<").append(from).append(",").append(to).append(">\n");
        }
        textArea.setText(sb.toString());
    }
}
