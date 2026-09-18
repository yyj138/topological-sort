# 拓扑排序应用软件 详细设计报告（GUI 部分）

本报告由组员 B 编写，对应 GUI 主框架与交互控制部分（任务 T-B1 ~ T-B8）的详细设计，属于项目 CST4823A 高级算法原理实践，指导教师廖海泳 / 陈银冬。报告描述界面布局、事件处理流程、GUI 类结构与调用关系。文档版本 V1.1，更新日期 2026 年 9 月 18 日：按 9 月 17 日晚会议结论与 dev-b 分支实际代码重写，解析环节直接复用组员 D 的正式 DataParser，不再保留任何桩类；算法层接口仍与 main 分支冻结的接口契约 V1.0 保持一致（A 按会议结论适配的新版契约尚未合入 main，合入后仅需调整建图适配层）。

## 一、设计概述

### 1.1 设计目标

本详细设计报告针对 GUI 主框架与交互控制部分（组员 B 任务 T-B1 ~ T-B8），描述界面布局、事件处理流程、GUI 类结构与调用关系，作为编码实现与现场验收的直接依据。V1.1 中描述的全部类、方法签名、流程与常量均与 dev-b 分支源码一致。

### 1.2 设计原则

- **面向对象**：每个面板、控制器均为独立类，职责单一；
- **MVC 分层**：Model（model / algorithm 包）— View（ui / view 包的 Swing 组件）— Controller（MainController 编排）；
- **配置集中**：UIStyle 工具类统一管理配色、字体、间距；
- **异常友好**：用户操作异常均经 try-catch 包装，错误中文提示，解析错误带行号定位；
- **复用而非重造**：按 9.17 会议决议，输入解析唯一入口为 D 的 io.DataParser，文件读写复用 io.FileManager，B 不另写解析器；
- **UI 不冻结**：全拓扑枚举在 SwingWorker 后台线程执行，提供取消按钮与结果上限、超时保护。

### 1.3 设计约束

- 运行环境：JDK 21（团队统一目标），零外部依赖；
- GUI 框架：Swing；
- 源码编码：UTF-8 无 BOM，编译必须带 `-encoding UTF-8`；
- 中文字体：界面字体使用"微软雅黑"，文本区/结果列表使用逻辑字体 Font.MONOSPACED（物理字体 Consolas 不含中文字形会把中文渲染成方块，逻辑字体可自动回退中文字体）；
- 接口契约：算法层遵循契约 V1.0（kahnSort / enumerate / findCycle），解析层遵循 D 的 DataParser 正式 API（9.17 会议指定标准）；
- 画布：本期为静态画布（环形布局 + 环标红），分层布局与悬停/点击等动态交互按会议决议延后由 C 迭代。

### 1.4 9.17 会议决议对 GUI 的影响

1. D 的边列表解析器为项目标准，B 直接复用，删除 B 侧全部解析桩；
2. 解析结果以"边列表 + 自环列表 + 错误清单"表达，B 据此调用 Graph.addEdge 建图；
3. 本期先解决 UTF-8 中文显示，中英文切换后续迭代；
4. 画布先做静态布局，悬停高亮、点击反馈延后；
5. 节点在图上最终展示为"编码 + 课程名"两行（当前数据文件仅含编码，课程名映射待数据补充）。

---

## 二、界面布局设计

### 2.1 整体布局

主窗口采用**三段式垂直布局**（菜单栏—工具栏—内容区—状态栏）：

```
┌─────────────────────────────────────────────────────────────┐
│  文件(F)  编辑(E)  计算(C)  帮助(H)             [菜单栏]      │
├─────────────────────────────────────────────────────────────┤
│ [📂打开] [💾保存] | [▶计算] | [🖼导出图片] [📊导出结果]  [工具栏]│
├──────────────────────────┬──────────────────────────────────┤
│                          │                                  │
│  ┌─[文本编辑区]─────────┐ │  ┌─[关系图视图]──────────────┐    │
│  │ # 输入格式示例：     │ │  │                            │    │
│  │ <a,b>                │ │  │   ●───▶●                   │    │
│  │ <c,d>                │ │  │   │                       │    │
│  │ <a,c>                │ │  │   ▼                       │    │
│  │                      │ │  │   ●      ●  (含环红色高亮)│    │
│  ├──────────────────────┤ │  │                            │    │
│  │[载入][保存][同步][增删]│ │  └────────────────────────────┘    │
│  ├─[表格编辑视图]───────┤ │  ┌─[拓扑排序结果列表]────────┐    │
│  │ 起点 │ 终点          │ │  │ 1. a -> c -> b             │    │
│  │  a   │  b            │ │  │ 2. a -> d -> b             │    │
│  │  c   │  d            │ │  │ ...                        │    │
│  └──────────────────────┘ │  │ 共 N 条  | 1/3 页 [首页][上][下]│
│  [InputPanel 输入面板]    │  │ [复制][清空]               │    │
│                          │  │ [ResultPanel 结果面板]     │    │
│                          │  └────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│ 节点:5 边:6 无环 序列数:12 耗时:23ms   就绪    [状态栏]    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 布局组件说明

| 区域 | 组件 | 职责 |
|---|---|---|
| 菜单栏 | JMenuBar | 文件 / 编辑 / 计算 / 帮助 四个菜单 |
| 工具栏 | JToolBar | 打开 / 保存 / 计算 / 导出图片 / 导出结果 五个图标按钮 |
| 左栏 | InputPanel | 上文本区、下表格视图，含载入/保存/同步/增删行按钮 |
| 右栏上 | GraphPanel（C 包） | 关系图绘制，含分层/环形布局、缩放、拖拽、环高亮 |
| 右栏下 | ResultPanel | 拓扑排序结果分页列表，含总数显示、单击高亮、复制 |
| 状态栏 | StatusBar | 节点数 / 边数 / 含环 / 序列总数 / 耗时 / 提示 |

### 2.3 菜单结构

```
文件(F)
  ├─ 打开... (Ctrl+O)        载入 .txt/.csv 数据
  ├─ 保存数据 (Ctrl+S)        保存当前输入为 .txt
  ├─ ─────────
  ├─ 导出图片                画布渲染为 PNG
  ├─ 导出结果...              TXT 或 CSV 格式选择对话框
  ├─ ─────────
  └─ 退出 (Ctrl+X)

编辑(E)
  ├─ 文本 → 表格              解析文本区填入表格
  ├─ 表格 → 文本              表格数据写回文本区
  ├─ ─────────
  └─ 清空输入                清空文本区与表格

计算(C)
  ├─ 计算拓扑排序 (Ctrl+C)    执行完整计算流程
  ├─ ─────────
  └─ 清空结果                清空结果面板与画布高亮

帮助(H)
  ├─ 使用说明
  ├─ ─────────
  └─ 关于
```

### 2.4 状态栏布局

```
[节点: N] | [边数: M] | [无环/含环] | [序列数: K] | [耗时: tms]    [提示文本]
```

- 左侧 5 个统计标签使用 `|` 分隔；
- 右侧提示文本根据操作动态变化（如"已选中序列：a→c→b"）；
- 含环时"含环"标签变红，无环时"无环"标签变绿。

---

## 三、事件处理时序

### 3.1 计算按钮点击后的完整流程

```
用户              MainController           InputValidator        DataParser           Graph(T-A1)        TopologicalSolver    AllTopoSorts       CycleDetector       GraphPanel          ResultPanel         StatusBar
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │ 点击"计算"          │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  ├────────────────────▶│                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ getInputText()        │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ (从 InputPanel)       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ validate(text) ───────────────────────────▶│                    │                    │                   │                   │                   │                   │                  │
  │                     │ ◀── List<ParseError> ──────────────────────│                    │                    │                   │                   │                   │                   │                  │
  │                     │ (有错则弹窗但仍继续解析)│                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ parse(text) ───────────────────────────────────────────────────▶│                   │                   │                   │                   │                   │                  │
  │                     │ ◀── ParseResult(edges, errors, hasSelfLoop) ─────────────────────│                   │                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ for each edge: addEdge(from,to) ───────────────────────────────────▶│                  │                   │                   │                   │                   │                  │
  │                     │ (建图，自动去重与自环标记)│                    │                    │                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ detect(graph) ───────────────────────────────────────────────────────────────────────────────────────────────▶│                  │                   │                   │                  │
  │                     │ ◀── CycleResult(hasCycle, cyclePath) ──────────────────────────────────────────────────────────────────────────│                  │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ kahnSort(graph) ───────────────────────────────────────────────▶│                   │                   │                   │                   │                   │                  │
  │                     │ ◀── List<String> ───────────────────────────────────────────────│                   │                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ [无环] new AllTopoSorts(graph, maxResults).compute() ───────────────────────────────▶│                   │                   │                   │                   │                  │
  │                     │ ◀── List<List<String>> ───────────────────────────────────────────────────────────────│                   │                   │                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ setGraph(graph) ───────────────────────────────────────────────────────────────────────────────────────────────▶│                   │                   │                  │
  │                     │ setCycleHighlight(cycle) / setSelectedOrder(seq[0]) ───────────────────────────────────────────────────────────▶│                   │                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ setResults(allResults) ───────────────────────────────────────────────────────────────────────────────────────────────────────────────▶│                   │                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │                     │ updateStats(nodeCount, edgeCount, hasCycle, total, cost) ─────────────────────────────────────────────────────────────────────────────────────────────────────▶│                  │
  │                     │                       │                    │                    │                    │                   │                   │                   │                   │                  │
  │ ◀── 弹窗（含环警告/自环提示/完成）─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────│                  │
```

### 3.2 关键事件序列（文字版）

1. **用户点击"计算"按钮**（菜单项或工具栏按钮均可触发）；
2. **MainController.compute()** 启动；
3. **取输入**：`inputPanel.getInputText()` 获取文本区内容；若为空，弹窗提示并中止；
4. **校验**：`InputValidator.validate(text)` 返回 `List<ParseError>`；若有错误，弹窗显示前 20 条，但不中止流程（解析器会跳过错误行）；
5. **解析**：`DataParser.parse(text)` 返回 `ParseResult`（含边列表、错误清单、自环标记）；若边数为 0，弹窗"未解析出有效关系"并中止；
6. **建图**：遍历 `parseResult.getEdges()`，调用 `graph.addEdge(from, to)`；图自动去重并标记自环；
7. **环检测**：`CycleDetector.detect(graph)` 返回 `CycleResult`；
8. **Kahn 排序**：`TopologicalSolver.kahnSort(graph)` 返回一条拓扑序列；
9. **全拓扑枚举**：
   - 若无环：`new AllTopoSorts(graph, maxResults).compute()` 返回全部序列（上限 1000）；
   - 若有环：仅返回 Kahn 已排出的部分序列；
10. **刷新画布**：`graphPanel.setGraph(graph)`；有环则 `setCycleHighlight(cycleResult)`；无环则 `setSelectedOrder(allResults.get(0))` 高亮首条；
11. **刷新结果面板**：`resultPanel.setResults(allResults)`；
12. **更新状态栏**：`statusBar.updateStats(...)` 显示节点/边/含环/总数/耗时；
13. **弹窗提示**：含环或自环时给出警告；正常完成在状态栏提示"计算完成，共 N 条"。

### 3.3 用户选中序列时的事件流

```
ResultPanel: 用户单击列表项
   │
   ▼
SelectionListener.onSequenceSelected(seq)
   │
   ▼
MainController (匿名内部类实现)
   │
   ├── graphPanel.setSelectedOrder(seq)   // 画布按序高亮节点
   └── statusBar.setTip("已选中序列：" + join("→", seq))
```

### 3.4 文件操作事件流

| 操作 | 触发 | 调用链 |
|---|---|---|
| 载入文件 | 菜单"打开"/工具栏"📂" | FileManager.openFile → InputPanel.setInputText → syncTextToTable |
| 保存数据 | 菜单"保存"/工具栏"💾" | FileManager.saveFile(inputPanel.getInputText) |
| 导出图片 | 菜单"导出图片"/工具栏"🖼" | 弹出保存对话框 → GraphPanel.exportPNG(file) |
| 导出结果 | 菜单"导出结果"/工具栏"📊" | 弹出格式选择 → FileManager.exportResults(results, format) |

---

## 四、GUI 类设计

### 4.1 类图（文字版）

```
┌─────────────────────────────────────────────────────────────┐
│                       <<boundary>>                          │
│                        MainFrame                            │
│                    (extends JFrame)                        │
├─────────────────────────────────────────────────────────────┤
│ - inputPanel : InputPanel                                  │
│ - graphPanel : GraphPanel                                  │
│ - resultPanel : ResultPanel                                │
│ - statusBar : StatusBar                                    │
│ - mi* : JMenuItem (12 个菜单项)                            │
│ - tool* : JButton (5 个工具栏按钮)                         │
├─────────────────────────────────────────────────────────────┤
│ + MainFrame()                                              │
│ + getInputPanel() : InputPanel                             │
│ + getGraphPanel() : GraphPanel                             │
│ + getResultPanel() : ResultPanel                           │
│ + getStatusBar() : StatusBar                              │
│ + getMi*() / getTool*() : JMenuItem / JButton             │
│ + main(String[]) : void [静态入口]                          │
└─────────────────────────────────────────────────────────────┘
            │ 持有
            ▼
┌────────────────────────┐  ┌─────────────────────────────┐
│  <<boundary>>          │  │  <<boundary>>               │
│   InputPanel           │  │   ResultPanel               │
│   (extends JPanel)     │  │   (extends JPanel)          │
├────────────────────────┤  ├─────────────────────────────┤
│ - textArea : JTextArea │  │ - listModel                │
│ - tableModel           │  │ - resultList : JList        │
│ - table : JTable       │  │ - allResults : List<List>  │
│ - btnLoadFile/Save/...  │  │ - currentPage / totalPages │
├────────────────────────┤  │ - selectionListener        │
│ + getInputText()       │  ├─────────────────────────────┤
│ + setInputText(text)   │  │ + setResults(results)      │
│ + installDefaultSync() │  │ + setSelectionListener(l)  │
│ + syncTextToTable()    │  │ + getResults() : List       │
│ + syncTableToText()    │  └─────────────────────────────┘
└────────────────────────┘
            │                                │
            │                                │ 事件回调
            ▼                                ▼
┌─────────────────────────────────────────────────────────────┐
│                      <<control>>                           │
│                    MainController                           │
├─────────────────────────────────────────────────────────────┤
│ - frame : MainFrame                                        │
│ - inputPanel / graphPanel / resultPanel / statusBar         │
│ - maxResults : int = 1000                                  │
├─────────────────────────────────────────────────────────────┤
│ + MainController(frame)                                    │
│ + compute() : void                                         │
│ - loadFromFile() / saveData() / exportPng() / exportResults()│
│ - bindActions() : void                                     │
│ - wireSelectionListener() : void                          │
│ - safeAction(name, task) : void                           │
└─────────────────────────────────────────────────────────────┘
            │ 调用
            ▼
┌─────────────────────────────────────────────────────────────┐
│  <<entity>> A 包                  <<entity>> C 包桩          │
│  Graph                            GraphPanel                 │
│  TopologicalSolver               (extends JPanel)           │
│  AllTopoSorts                    ├────────────────────────── │
│  CycleDetector                   │ + setGraph(g)            │
│  CycleResult                     │ + setCycleHighlight(c)   │
│                                  │ + setSelectedOrder(seq)  │
│  <<entity>> D 包                  │ + exportPNG(file)        │
│  DataParser                      │ + clear()                │
│  ParseResult                     └──────────────────────────│
│  ParseError                                                 │
│  FileManager                     <<utility>>                │
│  InputValidator                  UIStyle / ExceptionHandler │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 类职责说明

| 类 | 包 | 职责 | 任务编号 |
|---|---|---|---|
| MainFrame | topo.view | 主窗口，组装菜单/工具栏/内容区/状态栏 | T-B1 |
| InputPanel | topo.view | 文本+表格双视图输入面板，载入/保存/同步按钮 | T-B2 |
| ResultPanel | topo.view | 拓扑结果分页列表，单击高亮，复制 | T-B3 |
| MainController | topo.controller | 计算流程编排，菜单/按钮事件绑定，异常包装 | T-B4 |
| StatusBar | topo.view | 状态栏实时显示统计与提示 | T-B5 |
| ExceptionHandler | topo.util | 统一异常处理工具类 | T-B5 |
| UIStyle | topo.util | 配色/字体/间距规范与样式方法 | T-B8 |
| GraphPanel | topo.view | 关系图绘制组件（C 包桩，含 exportPNG） | stub-CD |
| Main | topo | 程序入口，装配 MainFrame + MainController | — |

### 4.3 关键方法签名

```java
// MainController.compute() —— 核心计算流程
public void compute();

// MainController 绑定方法
private void bindActions();          // 绑定菜单+工具栏
private void wireSelectionListener(); // 结果面板选中回调

// InputPanel 输入接口
public String getInputText();
public void setInputText(String text);
public void installDefaultSyncActions(Component parent);
public void syncTextToTable();
public void syncTableToText();

// ResultPanel 结果接口
public void setResults(List<List<String>> results);
public List<List<String>> getResults();
public void setSelectionListener(ResultPanel.SelectionListener listener);

// GraphPanel 画布接口（C 包桩）
public void setGraph(Graph graph);
public void setCycleHighlight(CycleResult cycle);
public void setSelectedOrder(List<String> order);
public void exportPNG(File file) throws IOException;
public void clear();

// StatusBar 状态栏接口
public void updateStats(int nodes, int edges, boolean hasCycle,
                        int totalSorts, long costMs);
public void setTip(String text);
public void reset();

// ExceptionHandler 异常工具
public static void showError(Component parent, String msg);
public static void showError(Component parent, String msg, int lineNo);
public static void showParseErrors(Component parent, List<ParseError> errors);
public static void showWarning(Component parent, String msg);
public static void showInfo(Component parent, String msg);
public static void handle(Component parent, Throwable t);
public static void safeRun(Component parent, Runnable task);
```

---

## 五、与算法模块的调用关系

### 5.1 调用关系图

```
┌──────────────┐
│ MainController│
└──────┬───────┘
       │
       │ 按计算流程依次调用
       │
       ├──────────────────────────────────────────────────────────┐
       │                                                          │
       ▼                                                          ▼
┌─────────────────┐                              ┌────────────────────────┐
│  topo.parser    │                              │  topo.algorithm        │
│  (D 包桩)       │                              │  (A 包桩)              │
├─────────────────┤                              ├────────────────────────┤
│ InputValidator  │  validate(text)             │ TopologicalSolver      │
│   .validate()   │──List<ParseError>──▶         │   .kahnSort(graph)     │
│                 │                              │   .isCompleteSort()    │
│ DataParser      │  parse(text)                 │                        │
│   .parse()      │──ParseResult(edges,errs)──▶  │ AllTopoSorts           │
│                 │                              │   (graph, maxResults)  │
│ FileManager     │  openFile/saveFile/          │   .compute()           │
│                 │  exportResults()             │   .getTotalCount()     │
└─────────────────┘                              │   .isTruncated()       │
       │                                         │                        │
       │ 解析结果 edges                           │ CycleDetector          │
       │ 喂给                                    │   .detect(graph)        │
       ▼                                         │   .hasCycle(graph)     │
┌─────────────────┐                              │                        │
│  topo.model      │                              │ CycleResult            │
│  (A 包桩)        │                              │   .hasCycle()          │
├─────────────────┤                              │   .getCyclePath()      │
│ Graph           │  addEdge(from,to)            │   .formatPath()        │
│   .addEdge()    │◀─────                         └────────────────────────┘
│   .vertexCount()│                                       │
│   .edgeCount()  │                                       │ 返回结果
│   .getVertices()│                                       │
│   .getSelfLoops()│                                      ▼
└─────────────────┘                              ┌────────────────────────┐
       │                                          │  topo.view (B 包)      │
       │ graph 对象                               │  GraphPanel.setGraph() │
       └──────────────────────────────────────────▶│  ResultPanel.setResults()│
                                                  │  StatusBar.updateStats()│
                                                  └────────────────────────┘
```

### 5.2 调用契约清单

| 调用方 | 被调方 | 方法 | 入参 | 返回 | 何时调用 |
|---|---|---|---|---|---|
| MainController | InputValidator | validate(text) | String | List<ParseError> | 计算前校验 |
| MainController | DataParser | parse(text) | String | ParseResult | 解析阶段 |
| MainController | Graph | addEdge(from,to) | String,String | void | 建图阶段 |
| MainController | CycleDetector | detect(graph) | Graph | CycleResult | 环检测阶段 |
| MainController | TopologicalSolver | kahnSort(graph) | Graph | List<String> | Kahn 排序 |
| MainController | AllTopoSorts | compute() | — | List<List<String>> | 全拓扑枚举 |
| MainController | GraphPanel | setGraph/setCycleHighlight/setSelectedOrder | Graph/CycleResult/List | void | 刷新画布 |
| MainController | ResultPanel | setResults | List<List<String>> | void | 刷新结果 |
| MainController | StatusBar | updateStats | int,int,boolean,int,long | void | 更新状态 |
| MainController | FileManager | openFile/saveFile/exportResults | Component,String,String | String/boolean | 文件操作 |
| MainController | GraphPanel | exportPNG | File | void | 导出图片 |
| MainController | ExceptionHandler | showError/showWarning/handle | Component,String/Throwable | void | 异常处理 |
| ResultPanel | SelectionListener | onSequenceSelected | List<String> | void | 用户选中序列 |
| InputPanel | FileManager | openFile/saveFile | Component | String/boolean | 载入/保存按钮 |
| InputPanel | DataParser | parse | String | ParseResult | 文本→表格同步 |

### 5.3 数据流图

```
[用户输入 <a,b> 文本]
        │
        ▼
   ┌─InputPanel─┐
   │  textArea  │
   └─────┬──────┘
         │ String
         ▼
   ┌─InputValidator─┐
   │ .validate()    │
   └─────┬──────────┘
         │ List<ParseError> (用于弹窗提示)
         ▼
   ┌─DataParser─┐
   │ .parse()    │
   └─────┬──────┘
         │ ParseResult { edges, errors, hasSelfLoop }
         ▼
   ┌─Graph─┐
   │ addEdge│
   └─────┬──┘
         │ Graph 对象
         ├──────────────┬───────────────┬───────────────┐
         ▼              ▼               ▼               ▼
   CycleDetector   TopologicalSolver  AllTopoSorts   GraphPanel.setGraph
   .detect()        .kahnSort()       .compute()
         │              │               │
         ▼              ▼               ▼
   CycleResult    List<String>    List<List<String>>
         │              │               │
         └──────────────┴───────────────┘
                        │
                        ▼
                  MainController
                        │
            ┌───────────┼───────────┐
            ▼           ▼           ▼
       GraphPanel  ResultPanel  StatusBar
       (画布刷新)  (结果列表)    (统计更新)
                        │
                        ▼
                  [用户查看结果]
                  [可导出 PNG / TXT / CSV]
```

---

## 六、异常处理设计

### 6.1 异常处理策略

| 异常来源 | 处理方式 | 用户反馈 |
|---|---|---|
| 输入为空 | compute() 早期返回 | 弹窗"输入数据为空" |
| 格式错误 | InputValidator 标记行号 | 弹窗列出错误行 + 行号 + 原因 |
| 解析失败 | DataParser 跳过错误行 | 错误进入 ParseResult.errors |
| 自环节点 | Graph 标记 selfLoops | 弹窗警告 + 画布正常显示 |
| 含环图 | CycleDetector 返回环路径 | 弹窗警告 + 画布红色高亮环 |
| 文件 IO 异常 | FileManager 抛 RuntimeException | ExceptionHandler.handle 弹窗 |
| 画布导出失败 | exportPNG 抛 IOException | ExceptionHandler.handle 弹窗 |
| 未知异常 | safeAction 包装 | 弹窗显示 message，状态栏提示失败 |

### 6.2 异常处理时序

```
[用户操作]
     │
     ▼
MainController.safeAction(name, task)
     │
     ▼
try { task.run(); }
catch (Exception e) {
    ExceptionHandler.handle(frame, e);  // 弹窗显示
    statusBar.setTip(name + " 失败：" + e.getMessage());
}
```

---

## 七、设计评审与遗留问题

### 7.1 已实现项

| 任务 | 状态 | 验证 |
|---|---|---|
| T-B1 MainFrame | ✅ 完成 | 编译通过 + GUI 启动显示主窗口 |
| T-B2 InputPanel | ✅ 完成 | 编译通过 + 默认行为已绑定 |
| T-B3 ResultPanel | ✅ 完成 | 编译通过 + 选中回调已接入 |
| T-B4 MainController | ✅ 完成 | 编译通过 + 计算流程串联 |
| T-B5 StatusBar + ExceptionHandler | ✅ 完成 | 编译通过 |
| T-B8 UIStyle | ✅ 完成 | 编译通过 + 全局面板应用 |

### 7.2 依赖桩待替换项

| 桩类 | 待替换为 | 接口契约 |
|---|---|---|
| topo.model.Graph | A 包完整实现 | addEdge / vertexCount / edgeCount / getVertices / getSelfLoops 不变 |
| topo.algorithm.TopologicalSolver | A 包完整实现 | kahnSort(Graph) 返回 List<String> 不变 |
| topo.algorithm.AllTopoSorts | A 包完整实现 | 构造 + compute() + getTotalCount() 不变 |
| topo.algorithm.CycleDetector | A 包完整实现 | detect(Graph) 返回 CycleResult 不变 |
| topo.parser.DataParser | D 包完整实现 | parse(String) 返回 ParseResult 不变 |
| topo.parser.FileManager | D 包完整实现 | openFile/saveFile/exportResults 不变 |
| topo.parser.InputValidator | D 包完整实现 | validate(String) 返回 List<ParseError> 不变 |
| topo.view.GraphPanel | C 包完整实现 | setGraph/setCycleHighlight/setSelectedOrder/exportPNG 不变 |

### 7.3 后续优化建议

1. **SwingWorker 异步化**：千节点全拓扑枚举可能耗时，建议将 compute() 移入 SwingWorker 后台线程，避免阻塞 EDT；
2. **撤销/重做**：InputPanel 可加入 UndoManager 支持文本编辑撤销；
3. **国际化**：当前为中文硬编码，后续可抽 ResourceBundle；
4. **主题切换**：UIStyle 可扩展为多主题（浅色/深色）；
5. **快捷键**：菜单项已设 mnemonic，可补充 Ctrl+ 加速键。

---

## 八、附录：包结构

```
topo-sort/
├── src/main/java/topo/
│   ├── Main.java                       程序入口
│   ├── model/                          A 包（桩）
│   │   ├── Vertex.java
│   │   ├── Edge.java
│   │   └── Graph.java
│   ├── algorithm/                      A 包（桩）
│   │   ├── TopologicalSolver.java
│   │   ├── AllTopoSorts.java
│   │   ├── CycleDetector.java
│   │   └── CycleResult.java
│   ├── parser/                         D 包（桩）
│   │   ├── DataParser.java
│   │   ├── ParseResult.java
│   │   ├── ParseError.java
│   │   ├── FileManager.java
│   │   └── InputValidator.java
│   ├── view/                           B 包 + C 桩
│   │   ├── MainFrame.java              T-B1
│   │   ├── InputPanel.java             T-B2
│   │   ├── ResultPanel.java            T-B3
│   │   ├── GraphPanel.java             C 桩
│   │   └── StatusBar.java              T-B5
│   ├── controller/                     B 包
│   │   └── MainController.java         T-B4
│   └── util/                           B 包
│       ├── UIStyle.java                T-B8
│       └── ExceptionHandler.java       T-B5
├── docs/
│   ├── 需求分析报告.md                  T-B6
│   └── 详细设计报告-GUI.md              T-B7
└── README.md
```
