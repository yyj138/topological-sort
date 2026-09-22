p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 改样式方法里的字体，用Segoe UI Symbol，支持特殊符号
old = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font(util.UIStyle.FONT_FAMILY, Font.PLAIN, 18));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(util.UIStyle.BTN_FLOAT_SIZE);
        b.setSize(util.UIStyle.BTN_FLOAT_SIZE);
    }'''
new = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(util.UIStyle.BTN_FLOAT_SIZE);
        b.setSize(util.UIStyle.BTN_FLOAT_SIZE);
    }'''
s = s.replace(old, new)

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
