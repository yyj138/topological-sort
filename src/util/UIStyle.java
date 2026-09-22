package util;

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
    public static final Color BG_MAIN       = new Color(243, 244, 246);
    public static final Color BG_PANEL      = new Color(255, 255, 255);
    public static final Color BG_TITLE      = new Color(17, 24, 39);
    public static final Color BG_TOOLBAR    = new Color(249, 250, 251);
    public static final Color BG_STATUSBAR   = new Color(31, 41, 55);
    public static final Color FG_PRIMARY     = new Color(17, 24, 39);
    public static final Color FG_SECONDARY   = new Color(107, 114, 128);
    public static final Color FG_ON_DARK     = new Color(243, 244, 246);
    public static final Color ACCENT          = new Color(59, 130, 246);
    public static final Color ACCENT_HOVER   = new Color(37, 99, 235);
    public static final Color SUCCESS        = new Color(16, 185, 129);
    public static final Color DANGER         = new Color(239, 68, 68);
    public static final Color BORDER_DEFAULT = new Color(229, 231, 235);
    public static final Color BORDER_HOVER   = new Color(209, 213, 219);
    public static final Color HOVER_BG       = new Color(243, 244, 246);
    public static final Color ACTIVE_BG       = new Color(229, 231, 235);

    // 字体
    public static final String FONT_FAMILY   = "Microsoft YaHei";
    public static final Font  FONT_TITLE     = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font  FONT_SUBTITLE  = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font  FONT_BODY      = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font  FONT_MONO      = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final Font  FONT_STATUS    = new Font(FONT_FAMILY, Font.PLAIN, 12);

    // 间距
    public static final int GAP_TINY     = 4;
    public static final int GAP_SMALL    = 8;
    public static final int GAP_MEDIUM   = 12;
    public static final int PADDING_BTN  = 6;

    // 按钮尺寸
    public static final Dimension BTN_ICON_SIZE = new Dimension(36, 36);
    public static final Dimension BTN_FLOAT_SIZE = new Dimension(30, 30);

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
                        BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
                        title,
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        FONT_SUBTITLE, FG_SECONDARY),
                BorderFactory.createEmptyBorder(GAP_SMALL, GAP_SMALL, GAP_SMALL, GAP_SMALL)));
    }

    // 普通按钮：扁平风格
    public static void styleButton(JButton btn) {
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_PANEL);
        btn.setForeground(FG_PRIMARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(6, BORDER_DEFAULT),
                BorderFactory.createEmptyBorder(PADDING_BTN, GAP_MEDIUM, PADDING_BTN, GAP_MEDIUM)));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(HOVER_BG);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  {
                btn.setBackground(BG_PANEL);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(ACTIVE_BG);
            }
        });
    }

    // 主要按钮
    public static void stylePrimaryButton(JButton btn) {
        btn.setFont(FONT_BODY);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(PADDING_BTN, GAP_MEDIUM, PADDING_BTN, GAP_MEDIUM));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(ACCENT_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  {
                btn.setBackground(ACCENT);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(29, 78, 216));
            }
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
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_DEFAULT));
        bar.setMargin(new Insets(GAP_TINY, GAP_SMALL, GAP_TINY, GAP_SMALL));
    }

    public static void styleMenuBar(JMenuBar bar) {
        bar.setBackground(BG_PANEL);
        bar.setForeground(FG_PRIMARY);
        bar.setFont(FONT_BODY);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_DEFAULT));
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

    // 工具栏图标按钮：无框，浅灰背景，hover变深（上一版风格）
    public static void styleIconButton(AbstractButton btn) {
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_TOOLBAR);
        btn.setForeground(FG_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setMargin(new Insets(PADDING_BTN, PADDING_BTN, PADDING_BTN, PADDING_BTN));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setPreferredSize(BTN_ICON_SIZE);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(HOVER_BG);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_TOOLBAR);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(ACTIVE_BG);
            }
        });
    }

    // 悬浮小按钮：GitHub风格，白底+浅灰边框+圆角，用于图上的缩放按钮
    public static void styleFloatButton(AbstractButton btn) {
        btn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 16));
        btn.setBackground(BG_PANEL);
        btn.setForeground(FG_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setPreferredSize(BTN_FLOAT_SIZE);
        btn.setBorder(new RoundedBorder(4, BORDER_DEFAULT));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(248, 249, 250));
                btn.setBorder(new RoundedBorder(4, BORDER_HOVER));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_PANEL);
                btn.setBorder(new RoundedBorder(4, BORDER_DEFAULT));
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(ACTIVE_BG);
            }
        });
    }

    public static Border emptyBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    // 自定义圆角边框
    static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private int radius;
        private Color color;
        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        @Override
        public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            ((java.awt.Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
        @Override
        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(1, 1, 1, 1);
        }
    }
}
