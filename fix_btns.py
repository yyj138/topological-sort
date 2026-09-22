p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 1. 改按钮文字，用Unicode图标
s = s.replace('new JButton("+")', 'new JButton("\\u2295")')
s = s.replace('new JButton("-")', 'new JButton("\\u2296")')
s = s.replace('new JButton("放大查看")', 'new JButton("\\u2197")')

# 2. 改样式方法
old_style = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font(util.UIStyle.FONT_FAMILY, Font.BOLD, fontSize));
        util.UIStyle.styleIconButton(b);
        b.setPreferredSize(util.UIStyle.BTN_SMALL_ICON_SIZE);
        b.setSize(util.UIStyle.BTN_SMALL_ICON_SIZE);
    }'''
new_style = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font(util.UIStyle.FONT_FAMILY, Font.PLAIN, 18));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(util.UIStyle.BTN_FLOAT_SIZE);
        b.setSize(util.UIStyle.BTN_FLOAT_SIZE);
    }'''
s = s.replace(old_style, new_style)

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
