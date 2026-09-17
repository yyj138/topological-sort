package topo.util;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

// 界面美化工具类（T-B8）：集中配色/字体/间距
public final class UIStyle {

    // 配色
    public static final Color BG_MAIN       = new Color(248, 249, 252);
    public static final Color BG_PANEL      = new Color(255, 255, 255);
    public static final Color BG_TITLE      = new Color(45, 55, 72);
    public static final Color BG_TOOLBAR    = new Color(238, 240, 245);
    public static final Color BG_STATUSBAR   = new Color(50, 55, 65);
    public static final Color FG_PRIMARY     = new Color(33, 37, 41);
    public static final Color FG_SECONDARY   = new Color(108, 117, 125);
    public static final Color FG_ON_DARK     = new Color(236, 240, 245);
    public static final Color ACCENT          = new Color(50, 120, 200);
    public static final Color ACCENT_HOVER   = new Color(35, 95, 175);
    public static final Color SUCCESS        = new Color(40, 167, 69);
    public static final Color WARNING        = new Color(255, 159, 0);
    public static final Color DANGER         = new Color(220, 53, 69);
    public static final Color BORDER_LIGHT    = new Color(220, 223, 228);

    // 字体
    public static final String FONT_FAMILY   = "Microsoft YaHei";
    public static final Font  FONT_TITLE     = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font  FONT_SUBTITLE  = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font  FONT_BODY      = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font  FONT_SMALL     = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font  FONT_MONO      = new Font("Consolas", Font.PLAIN, 13);
    public static final Font  FONT_STATUS    = new Font(FONT_FAMILY, Font.PLAIN, 12);

    // 间距
    public static final int GAP_TINY     = 4;
    public static final int GAP_SMALL    = 8;
    public static final int GAP_MEDIUM   = 12;
    public static final int GAP_LARGE   = 16;
    public static final int PADDING_BTN  = 6;

    // 按钮尺寸
    public static final Dimension BTN_SIZE      = new Dimension(96, 32);
    public static final Dimension BTN_SIZE_SM   = new Dimension(76, 28);
    public static final Dimension BTN_ICON_SIZE = new Dimension(36, 36);

    private UIStyle() {}

    public static void applyMainBackground(JComponent c) {
        c.setBackground(BG_MAIN);
        c.setFont(FONT_BODY);
    }

    public static void applyPanelStyle(JPanel p) {
        p.setBackground(BG_PANEL);
        p.setFont(FONT_BODY);
    }

    public static void applyTitledPanel(JPanel p, String title) {
        p.setBackground(BG_PANEL);
        p.setFont(FONT_BODY);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                        title,
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        FONT_SUBTITLE, FG_PRIMARY),
                BorderFactory.createEmptyBorder(GAP_SMALL, GAP_SMALL, GAP_SMALL, GAP_SMALL)));
    }

    public static void styleButton(JButton btn) {
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_PANEL);
        btn.setForeground(FG_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(PADDING_BTN, GAP_MEDIUM, PADDING_BTN, GAP_MEDIUM)));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(BG_TOOLBAR); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(BG_PANEL); }
        });
    }

    public static void stylePrimaryButton(JButton btn) {
        styleButton(btn);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(PADDING_BTN, GAP_MEDIUM, PADDING_BTN, GAP_MEDIUM));
        btn.removeMouseListener(btn.getMouseListeners()[0]);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(ACCENT); }
        });
    }

    public static void styleTitleLabel(JLabel lbl) {
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(BG_TITLE);
    }

    public static void styleSubtitleLabel(JLabel lbl) {
        lbl.setFont(FONT_SUBTITLE);
        lbl.setForeground(FG_PRIMARY);
    }

    public static void styleStatusBar(JPanel bar) {
        bar.setBackground(BG_STATUSBAR);
        bar.setFont(FONT_STATUS);
        bar.setForeground(FG_ON_DARK);
        bar.setBorder(BorderFactory.createEmptyBorder(GAP_TINY, GAP_MEDIUM, GAP_TINY, GAP_MEDIUM));
    }

    public static void styleToolBar(JToolBar bar) {
        bar.setBackground(BG_TOOLBAR);
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));
        bar.setMargin(new Insets(GAP_TINY, GAP_SMALL, GAP_TINY, GAP_SMALL));
    }

    public static void styleMenuBar(JMenuBar bar) {
        bar.setBackground(BG_PANEL);
        bar.setForeground(FG_PRIMARY);
        bar.setFont(FONT_BODY);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));
    }

    public static void styleMenu(JMenu menu) {
        menu.setFont(FONT_BODY);
        menu.setForeground(FG_PRIMARY);
    }

    public static void styleMenuItem(JMenuItem item) {
        item.setFont(FONT_BODY);
        item.setForeground(FG_PRIMARY);
        item.setBackground(BG_PANEL);
    }

    public static void styleIconButton(AbstractButton btn) {
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_TOOLBAR);
        btn.setForeground(FG_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setMargin(new Insets(PADDING_BTN, PADDING_BTN, PADDING_BTN, PADDING_BTN));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setPreferredSize(BTN_ICON_SIZE);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_PANEL);
                btn.setBorderPainted(true);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_TOOLBAR);
                btn.setBorderPainted(false);
            }
        });
    }

    public static Border emptyBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
