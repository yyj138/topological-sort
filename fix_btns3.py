p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 替换所有按钮文字
s = s.replace('new JButton("\\u21BA")', 'new JButton("重置")')
s = s.replace('new JButton("新窗口")', 'new JButton("<<")')

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
