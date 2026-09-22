p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 把乱码的Unicode换回常用字符
s = s.replace('new JButton("\\u2295")', 'new JButton("+")')
s = s.replace('new JButton("\\u2296")', 'new JButton("-")')
s = s.replace('new JButton("\\u2197")', 'new JButton("新窗口")')

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
