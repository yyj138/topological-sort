p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 直接用"重置"两个字，绝对不会乱码
s = s.replace('new JButton("\\u21BB")', 'new JButton("重置")')
# 字体也改回普通的
old = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(util.UIStyle.BTN_FLOAT_SIZE);
        b.setSize(util.UIStyle.BTN_FLOAT_SIZE);
    }'''
new = '''    private void styleSmallButton(JButton b, int fontSize) {
        b.setFont(new Font(util.UIStyle.FONT_FAMILY, Font.PLAIN, 12));
        util.UIStyle.styleFloatButton(b);
        b.setPreferredSize(new Dimension(50, 30));
        b.setSize(new Dimension(50, 30));
    }'''
s = s.replace(old, new)

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
