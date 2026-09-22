p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 替换按钮文字
s = s.replace('new JButton("重置")', 'new JButton("\\u21BA")')
s = s.replace('new JButton("<<")', 'new JButton("<>")')

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
