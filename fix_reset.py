p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

# 把⟲换成↻，这个更常用
s = s.replace('new JButton("\\u21BA")', 'new JButton("\\u21BB")')

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
