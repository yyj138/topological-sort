p = 'src/view/GraphPanel.java'
with open(p, 'r', encoding='utf-8') as f:
    s = f.read()

old = '''        int size = 26;
        int margin = 12;
        int gap = 4;
        int y = h - size - margin;
        btnResetView.setBounds(w - margin - size, y, size, size);
        btnZoomOut.setBounds(w - margin - size * 2 - gap, y, size, size);
        btnZoomIn.setBounds(w - margin - size * 3 - gap * 2, y, size, size);
        if (!isPopoutInstance) {
            btnPopout.setBounds(15, 15, 84, 26);
        }'''
new = '''        int size = 30;
        int margin = 12;
        int gap = 6;
        int y = h - size - margin;
        // 从右往左摆
        int x = w - margin;
        btnResetView.setBounds(x - 56, y, 56, size); x -= 56 + gap;
        btnZoomOut.setBounds(x - size, y, size, size); x -= size + gap;
        btnZoomIn.setBounds(x - size, y, size, size);
        if (!isPopoutInstance) {
            btnPopout.setBounds(15, 15, 50, 30);
        }'''
s = s.replace(old, new)

with open(p, 'w', encoding='utf-8') as f:
    f.write(s)
print('done')
