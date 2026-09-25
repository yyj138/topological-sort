# C 与 B 集成对接与可视化优化记录

**参与人**：B（易雨杰，GUI 与交互控制）、C（戴燕岚，可视化与图片导出）
**记录日期**：2026-09-22
**涉及模块**：ui（B）↔ view（C）
**用途**：备案 B-C 跨模块集成对接、可视化优化及修复结论

---

## 一、可视化优化需求对接（B 提出 → C 实现）

B 在集成测试后向 C 提出了《可视化优化需求清单》，C 按清单完成以下修改：

| 序号 | B 的需求 | C 的实现 |
| :--- | :--- | :--- |
| 1 | 图例精简，不要占太多空间 | 去掉“普通节点”和“环路径”，只保留“当前拓扑序”和“选中节点”；尺寸由 160x118 缩到 105x68，字体 10f |
| 2 | 节点右上角加序号徽章 | 新增 `orderPositionMap`，在节点右上角绘制带数字的小圆圈，显示该节点在当前拓扑序中的位置 |
| 3 | 属于当前拓扑序的边高亮 | `drawEdges` 增加判断，属于当前拓扑序的边颜色跟随节点边框色，粗细 2.2f |
| 4 | 放大缩小按钮位置调整 | 在 `GraphPanel` 右下角新增 `+` / `-` / `重置` 悬浮按钮 |
| 5 | 左上角加“放大查看”按钮 | 新增 `btnPopout`，点击弹出独立窗口显示大图 |
| 6 | 计算后关系图自动自适应 | 由 B 在 `MainController` 的 `setGraph` 之后调用 C 的 `resetView()` 实现，C 配合提供接口 |

以上优化均在 `GraphPanel.java` 内完成，接口 `setGraph` / `setHighlightedCycle` / `setSelectedOrder` / `exportPNG` 未变。

---

## 二、集成问题修复记录

### 1. 滚轮缩放集成后失效（B 发现 → C 配合修复）
- **问题**：C 单测时滚轮缩放正常；集成进 B 的 `MainFrame` 后，滚轮事件被外层容器拦截，缩放失效。
- **解决**：B 在工具栏新增了放大/缩小按钮，C 在 `GraphPanel` 中新增 `zoomIn()` / `zoomOut()` 公开方法供 B 调用。
- **结果**：按钮缩放正常，滚轮缩放保留（作为备用）。

### 2. 放大查看窗口不显示环形布局（B 发现 → C 修复）
- **问题**：主窗口切换为环形布局后，点击“放大查看”弹出的窗口仍然是分层布局。
- **原因**：popout 创建的新 `GraphPanel` 使用默认 `LayoutManager`（分层），未复制主面板的布局模式。
- **解决**：C 新增 `getLayoutMode()` 方法；`showPopoutView()` 中调用 `popout.switchLayout(this.getLayoutMode())` 同步布局。
- **结果**：无论主窗口是分层还是环形，弹窗均一致显示。

### 3. 按钮样式统一（B 协助）
- B 修改了 `GraphPanel` 的悬浮按钮样式，使用 `util.UIStyle.styleFloatButton()`，按钮文字保持 `+`、`-`、`重置`、`<>`。
- C 保留 B 的修改，未再调整按钮图标。

---

## 三、接口调用核对

| B 调用的方法 | C 提供的方法 | 状态 |
| :--- | :--- | :--- |
| `graphPanel.setGraph(graph)` | `public void setGraph(Graph)` | ✅ 对齐 |
| `graphPanel.setHighlightedCycle(cycle)` | `public void setHighlightedCycle(List<String>)` | ✅ 对齐 |
| `graphPanel.setSelectedOrder(seq, idx)` | `public void setSelectedOrder(List<String>, int)` | ✅ 对齐 |
| `graphPanel.exportPNG(file)` | `public void exportPNG(File)` | ✅ 对齐 |
| `graphPanel.switchLayout(mode)` | `public void switchLayout(int)` | ✅ 对齐 |
| `graphPanel.zoomIn()` / `zoomOut()` | `public void zoomIn()` / `zoomOut()` | ✅ 对齐 |
| `graphPanel.getScale()` | `public double getScale()` | ✅ 对齐 |

---

## 四、结论

- B 与 C 在集成阶段进行了充分对接，可视化优化需求和集成 Bug 均已修复。
- 双方接口对齐，签名一致，无遗漏。
- 2026-09-22 最终确认，模块集成无遗留问题。