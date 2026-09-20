package ui;

import util.UIStyle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

// 结果面板（T-B3）：分页(每页20条) + 单击高亮 + 双击复制
public class ResultPanel extends JPanel {

    public interface SelectionListener {
        void onSequenceSelected(List<String> sequence);
    }

    private static final int PAGE_SIZE = 20;

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> resultList = new JList<>(listModel);
    private final JLabel lblTotal = new JLabel("共 0 条结果");
    private final JLabel lblPage  = new JLabel("0 / 0 页");

    private final JButton btnFirst = new JButton("首页");
    private final JButton btnPrev  = new JButton("上一页");
    private final JButton btnNext  = new JButton("下一页");
    private final JButton btnLast  = new JButton("末页");
    private final JButton btnCopy  = new JButton("复制当前");
    private final JButton btnClear = new JButton("清空");

    private List<List<String>> allResults = Collections.emptyList();
    private int currentPage = 0;
    private int totalPages = 0;
    private SelectionListener selectionListener;

    public ResultPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(
                UIStyle.GAP_SMALL, UIStyle.GAP_SMALL, UIStyle.GAP_SMALL, UIStyle.GAP_SMALL));
        setBackground(UIStyle.BG_MAIN);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        add(buildPager(), BorderLayout.SOUTH);

        installListeners();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIStyle.BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(
                        UIStyle.GAP_TINY, UIStyle.GAP_SMALL, UIStyle.GAP_TINY, UIStyle.GAP_SMALL)));
        JLabel title = new JLabel("拓扑排序结果列表");
        UIStyle.styleSubtitleLabel(title);
        p.add(title, BorderLayout.WEST);
        p.add(lblTotal, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildList() {
        resultList.setFont(UIStyle.FONT_MONO);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setFixedCellHeight(26);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(UIStyle.ACCENT);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(index % 2 == 0
                            ? UIStyle.BG_PANEL
                            : new Color(244, 246, 250));
                    c.setForeground(UIStyle.FG_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(
                        UIStyle.GAP_TINY, UIStyle.GAP_SMALL, UIStyle.GAP_TINY, UIStyle.GAP_SMALL));
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(resultList);
        scroll.setBorder(new LineBorder(UIStyle.BORDER_LIGHT, 1));
        return scroll;
    }

    private JPanel buildPager() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBackground(UIStyle.BG_MAIN);
        p.setBorder(BorderFactory.createEmptyBorder(UIStyle.GAP_TINY, 0, 0, 0));

        UIStyle.styleButton(btnFirst);
        UIStyle.styleButton(btnPrev);
        UIStyle.styleButton(btnNext);
        UIStyle.styleButton(btnLast);
        UIStyle.styleButton(btnCopy);
        UIStyle.styleButton(btnClear);

        p.add(lblPage);  p.add(Box.createHorizontalStrut(UIStyle.GAP_SMALL));
        p.add(btnFirst); p.add(Box.createHorizontalStrut(UIStyle.GAP_TINY));
        p.add(btnPrev);  p.add(Box.createHorizontalStrut(UIStyle.GAP_TINY));
        p.add(btnNext);  p.add(Box.createHorizontalStrut(UIStyle.GAP_TINY));
        p.add(btnLast);  p.add(Box.createHorizontalGlue());
        p.add(btnCopy);  p.add(Box.createHorizontalStrut(UIStyle.GAP_TINY));
        p.add(btnClear);
        return p;
    }

    private void installListeners() {
        resultList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = resultList.getSelectedIndex();
            if (idx < 0) return;
            int globalIdx = currentPage * PAGE_SIZE + idx;
            if (globalIdx >= allResults.size()) return;
            if (selectionListener != null) {
                selectionListener.onSequenceSelected(allResults.get(globalIdx));
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) copyCurrent();
            }
        });
        btnFirst.addActionListener(e -> goToPage(0));
        btnPrev.addActionListener(e -> goToPage(currentPage - 1));
        btnNext.addActionListener(e -> goToPage(currentPage + 1));
        btnLast.addActionListener(e -> goToPage(totalPages - 1));
        btnCopy.addActionListener(e -> copyCurrent());
        btnClear.addActionListener(e -> setResults(Collections.emptyList()));
    }

    public void setResults(List<List<String>> results) {
        if (results == null) results = Collections.emptyList();
        this.allResults = results;
        this.totalPages = (int) Math.ceil((double) results.size() / PAGE_SIZE);
        this.currentPage = 0;
        refreshPage();
    }

    private void refreshPage() {
        listModel.clear();
        if (allResults.isEmpty()) {
            lblTotal.setText("共 0 条结果");
            lblPage.setText("0 / 0 页");
            updatePagerState();
            return;
        }
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allResults.size());
        for (int i = start; i < end; i++) {
            listModel.addElement(String.format("%3d. %s", i + 1,
                    String.join(" -> ", allResults.get(i))));
        }
        lblTotal.setText("共 " + allResults.size() + " 条结果");
        lblPage.setText((currentPage + 1) + " / " + totalPages + " 页");
        updatePagerState();
    }

    private void goToPage(int page) {
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        if (page == currentPage) return;
        currentPage = page;
        refreshPage();
    }

    private void updatePagerState() {
        boolean hasResults = !allResults.isEmpty();
        btnFirst.setEnabled(hasResults && currentPage > 0);
        btnPrev.setEnabled(hasResults && currentPage > 0);
        btnNext.setEnabled(hasResults && currentPage < totalPages - 1);
        btnLast.setEnabled(hasResults && currentPage < totalPages - 1);
        btnCopy.setEnabled(hasResults);
    }

    private void copyCurrent() {
        int idx = resultList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条结果", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int globalIdx = currentPage * PAGE_SIZE + idx;
        if (globalIdx >= allResults.size()) return;
        String text = String.join(" -> ", allResults.get(globalIdx));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this, "已复制：" + text,
                "复制成功", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // 当前选中结果的全局序号（未选中返回 -1），供控制器决定图上高亮的轮换颜色
    public int getSelectedResultIndex() {
        int idx = resultList.getSelectedIndex();
        if (idx < 0) return -1;
        int globalIdx = currentPage * PAGE_SIZE + idx;
        return globalIdx < allResults.size() ? globalIdx : -1;
    }

    public List<List<String>> getResults() { return allResults; }
}
