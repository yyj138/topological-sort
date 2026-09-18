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
│ [打开] [保存] | [计算] [取消计算] | [导出图片] [导出结果]      │
├──────────────────────────┬──────────────────────────────────┤
│ [载入文件][保存数据]       │  ┌─[关系图视图]──────────────┐    │
│ [文本->表格][表格->文本]   │  │                            │    │
│ [+增行][-删行][清空]       │  │       ●(CS 150)            │    │
│  ┌─[文本编辑区]─────────┐ │  │         ↑                  │    │
│  │ # 请按 <a,b> 格式... │ │  │ ●(MA 140)→●(MA 141)       │    │
│  │ <MA 140,MA 141>      │ │  │ (静态环形布局，环路径标红) │    │
│  │ <MA 141,CS 150>      │ │  └────────────────────────────┘    │
│  ├─[表格编辑视图]───────┤ │  ┌─[拓扑排序结果列表]────────┐    │
│  │ 起点     │ 终点      │ │  │ 1. MA 140 -> MA 141 -> ... │    │
│  │ MA 140   │ MA 141    │ │  │ ...（每页 20 条）          │    │
│  └──────────────────────┘ │  │ 共 N 条 | 1/K 页[首页][上][下] │
│  [InputPanel]            │  │ [复制当前][清空]           │    │
│                          │  │ [ResultPanel]              │    │
│                          │  └────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│ 节点:3 | 边数:2 | 无环 | 序列数:1 | 耗时:6ms    枚举完成...  │
└─────────────────────────────────────────────────────────────┘
```

文本区启动时的默认内容（均为 `#` 注释与示例边）：

```
# 请按 <a,b> 格式输入数据，每行一条关系，# 开头为注释
# a 为前驱，b 为后继，如 <a,b> 表示有向边 a -> b
# 示例：
<MA 140,MA 141>
<MA 141,CS 150>
```

### 2.2 布局组件说明

| 区域 | 组件 | 职责 |
|---|---|---|
| 菜单栏 | JMenuBar | 文件 / 编辑 / 计算 / 帮助 四个菜单，均注册 Alt+字母 助记符 |
| 工具栏 | JToolBar | 打开 / 保存 / 计算 / 取消计算 / 导出图片 / 导出结果 六个文字按钮 |
| 左栏 | InputPanel | 上文本区、下表格视图（垂直分割），载入/保存/双向同步/增删行/清空七个按钮 |
| 右栏上 | GraphPanel（view 包，C 后续演进） | 静态环形布局，绘制有向箭头与自环；环路径红色加粗、选中序列节点蓝色 |
| 右栏下 | ResultPanel | 结果分页列表（每页 20 条），总数/页码、单击高亮、双击复制 |
| 状态栏 | StatusBar | 节点数 / 边数 / 含环 / 序列数 / 耗时 / 右侧动态提示 |

### 2.3 菜单结构

```
文件(F)  Alt+F
  ├─ 打开...          JFileChooser 选择 .txt/.csv，FileManager.readFile 读取并同步表格
  ├─ 保存数据         JFileChooser 选择路径，FileManager.saveFile 写出（自动补 .txt）
  ├─ ─────────
  ├─ 导出图片         GraphPanel.exportPNG 导出当前关系图
  ├─ 导出结果...      按扩展名分流 FileManager.exportTxt / exportCsv
  ├─ ─────────
  └─ 退出

编辑(E)  Alt+E
  ├─ 文本 → 表格      DataParser.parse 后把普通边与自环填入表格
  ├─ 表格 → 文本      表格行写回 <a,b> 文本
  ├─ ─────────
  └─ 清空输入

计算(C)  Alt+C
  ├─ 计算拓扑排序      解析 → 建图 → 判环 → 后台枚举
  ├─ 取消计算          仅枚举进行中可用，请求终止并展示已生成的部分结果
  ├─ ─────────
  └─ 清空结果

帮助(H)  Alt+H
  ├─ 使用说明
  ├─ ─────────
  └─ 关于
```

说明：当前只注册了 Alt+字母 菜单助记符（setMnemonic），未注册 Ctrl+ 加速键；"计算"与"取消计算"按枚举运行状态互斥启用。

### 2.4 状态栏布局

```
[节点: N] | [边数: M] | [无环/含环] | [序列数: K] | [耗时: tms]    [提示文本]
```

- 左侧 5 个统计标签使用 `|` 分隔；
- 右侧提示文本随操作变化（就绪 / 计算中 / 枚举完成共 N 条 / 达到上限 / 超时 / 已取消 / 检测到环）；
- 含环时"含环"标签变红，无环时"无环"标签变绿。

---

## 三、事件处理时序

### 3.1 计算按钮点击后的完整流程

```
EDT(事件分发线程)                    后台线程(SwingWorker)
      │
      │ 点击"计算拓扑排序"（菜单或工具栏）
      ▼
MainController.compute()
      │
      ├─ inputPanel.getInputText()，为空白则警告并中止
      │
      ├─ DataParser.parse(text)                  （D 的正式解析器，静态方法）
      │     └─ ParseResult{ edges, selfLoops, errors, duplicateCount }
      │
      ├─ !isSuccess() ? 组装"第x行：原因"消息 → ExceptionHandler 弹窗 → 中止
      ├─ !hasData()    ? 警告"没有有效的关系数据" → 中止
      │
      ├─ new Graph()，遍历 getEdges() 与 getSelfLoops() 调 graph.addEdge(s,t)
      ├─ graphPanel.setGraph(graph)              （先刷新画布结构）
      │
      ├─ CycleDetector.findCycle(graph)
      │     ├─ 非空（自环 [X,X] / 2 环 [A,B,A] / 更长闭合路径）
      │     │     ├─ graphPanel.setHighlightedCycle(cycle)（环边红色加粗）
      │     │     ├─ resultPanel 清空
      │     │     ├─ statusBar.updateStats(..., hasCycle=true, ...)
      │     │     └─ 警告弹窗显示环路径，流程结束
      │     └─ 空列表（无环）→ 进入枚举
      │
      ├─ setBusy(true)：禁用"计算"，启用"取消计算"
      │
      ├─ EnumerationWorker.execute() ──────────────▶ AllTopoSorts.enumerate(
      │                                                   graph,
      │                                                   maxResults = 10000,
      │                                                   timeoutMillis = 30000,
      │                                                   cancelled = () -> 取消标记 || isCancelled())
      │   用户可随时点"取消计算"：cancelRequested = true，
      │   枚举在下一个回溯检查点退出，保留已生成序列
      │                                                     │
      │ ◀──────────── done()：EnumerationResult ────────────┘
      ├─ setBusy(false)
      ├─ resultPanel.setResults(sequences)
      ├─ graphPanel.setSelectedOrder(sequences.get(0))
      ├─ statusBar.updateStats(节点, 边, false, 条数, 耗时ms)
      └─ statusBar.setTip(按 StopReason 生成的中文提示)
```

### 3.2 关键事件序列（文字版）

1. 用户点击"计算拓扑排序"（菜单项或工具栏按钮触发同一入口）；
2. MainController.compute() 取输入文本，空白（仅空白字符）直接警告并中止；
3. 调用静态方法 `DataParser.parse(text)`，不再先做 InputValidator 预校验（原因见 7.3）；
4. `ParseResult.isSuccess()` 为 false（errors 非空）时，把每条 ParseError 格式化为"第x行：原因"（lineNumber 为 0 的全局性错误只显示原因），弹窗最多展示 20 条，**中止流程**（V1.0 设计中的"有错仍继续解析"已废弃，避免脏数据建图）；
5. `hasData()` 为 false（边与自环均为空，如全文只有注释）时警告并中止；
6. 用边列表建图：先遍历 `getEdges()`，再遍历 `getSelfLoops()`，逐条 `graph.addEdge(source,target)`；解析器已对普通边去重，重复数可经 `getDuplicateCount()` 获取；
7. `graphPanel.setGraph(graph)` 先把图交给画布布局；
8. `CycleDetector.findCycle(graph)` 返回闭合路径列表；空列表表示无环；自环返回 `[X,X]`，两节点互指返回 `[A,B,A]`；
9. 有环：画布 `setHighlightedCycle` 标红、结果列表清空、状态栏按含环更新，弹窗显示环路径后结束；
10. 无环：setBusy 切换按钮状态，启动内部类 EnumerationWorker（继承 SwingWorker）；
11. 后台执行 `AllTopoSorts.enumerate(graph, 10000, 30000, 取消谓词)`，结果上限 10000 条、超时 30 秒；
12. done() 回到 EDT：填充 ResultPanel、画布高亮首条序列、状态栏更新统计；
13. 状态栏提示按 StopReason 区分：COMPLETED"枚举完成，共 N 条"、LIMIT_REACHED"达到结果上限"、TIMEOUT"超时停止"、CANCELLED"已取消，已显示部分序列"。

### 3.3 用户选中序列时的事件流

```
ResultPanel：用户单击列表项（单选模式）
   │
   ▼
SelectionListener.onSequenceSelected(seq)
   │
   ▼
MainController 在 bindActions() 中注册的 lambda
   │
   └── graphPanel.setSelectedOrder(seq)   // 序列中的节点在画布上蓝色高亮
```

双击列表项或点击"复制当前"把 `a -> b -> c` 形式的序列写入系统剪贴板。

### 3.4 文件操作事件流

文件对话框（JFileChooser）由 UI 层负责弹出，D 的 FileManager 只接收 File 参数并抛 IOException：

| 操作 | 触发位置 | 实际调用链 |
|---|---|---|
| 载入文件 | InputPanel"载入文件"按钮 / 菜单"打开" / 工具栏 | JFileChooser（初始目录取 FileManager.getLastOpenedDirectory）→ FileManager.readFile(File) → textArea.setText → syncTextToTable |
| 保存数据 | InputPanel"保存数据"按钮 / 菜单 / 工具栏 | JFileChooser（无扩展名自动补 .txt）→ FileManager.saveFile(File, text) |
| 导出图片 | 菜单 / 工具栏"导出图片" | 无图时先警告；JFileChooser → GraphPanel.exportPNG(File) |
| 导出结果 | 菜单 / 工具栏"导出结果" | 无结果时先警告；JFileChooser，.csv 走 FileManager.exportCsv（UTF-8 BOM），其余走 exportTxt（自动补 .txt） |

---

## 四、GUI 类设计

### 4.1 类结构总览

```
ui 包（组员 B）                       view 包（C 演进，B 维护当前静态版）
┌──────────────────────┐             ┌──────────────────────┐
│ MainFrame (JFrame)   │  持有        │ GraphPanel (JPanel)  │
│  T-B1 主窗口/入口     │────────────▶│  setGraph            │
├──────────────────────┤             │  setHighlightedCycle │
│ InputPanel (JPanel)  │             │  setSelectedOrder    │
│  T-B2 文本+表格输入   │             │  exportPNG/clear     │
├──────────────────────┤             └──────────────────────┘
│ ResultPanel (JPanel) │
│  T-B3 分页结果列表    │
├──────────────────────┤
│ StatusBar (JPanel)   │      util 包（组员 B）
│  T-B5 状态栏          │      ┌──────────────────────┐
└──────────┬───────────┘      │ UIStyle              │
           │ 被持有            │  T-B8 配色/字体/间距  │
           ▼                  ├──────────────────────┤
┌──────────────────────┐      │ ExceptionHandler     │
│ MainController       │      │  T-B5 中文弹窗        │
│  T-B4 流程编排        │      └──────────────────────┘
│  内部类 Enumeration-  │
│  Worker(SwingWorker) │      io 包（组员 D 正式实现，直接复用）
└──────────┬───────────┘      ┌──────────────────────┐
           │ 调用             │ DataParser（静态）   │
           ▼                  │ FileManager（静态）  │
model 包（A）  algorithm 包（A）│ InputValidator       │
Graph         TopologicalSolver └──────────────────────┘
              AllTopoSorts / EnumerationResult / StopReason
              CycleDetector / TopoResult
```

### 4.2 类职责说明

| 类 | 包 | 职责 | 任务编号 |
|---|---|---|---|
| MainFrame | ui | 主窗口，组装菜单/工具栏/分割面板/状态栏，main 入口内自行装配 MainController | T-B1 |
| InputPanel | ui | 文本区 + JTable 双视图，载入/保存/双向同步/增删行/清空，默认中文格式提示 | T-B2 |
| ResultPanel | ui | 结果分页列表（每页 20 条），总数与页码、翻页、单击回调、双击/按钮复制 | T-B3 |
| MainController | ui | 菜单与工具栏绑定、计算流程编排、SwingWorker 后台枚举与取消、导出 | T-B4 |
| StatusBar | ui | 节点/边/含环/序列数/耗时统计与右侧动态提示 | T-B5 |
| ExceptionHandler | util | 统一中文弹窗（错误/警告/信息/确认/问题清单/异常兜底） | T-B5 |
| UIStyle | util | 配色、字体（含中文兼容的逻辑等宽字体）、间距与组件样式 | T-B8 |
| GraphPanel | view | 静态环形画布：有向箭头/自环、环标红、选中序列高亮、PNG 导出 | C（B 维护当前版本） |

### 4.3 关键方法签名（均与源码一致）

```java
// ui.MainFrame
public MainFrame();
public InputPanel getInputPanel();
public GraphPanel getGraphPanel();
public ResultPanel getResultPanel();
public StatusBar getStatusBar();
public JMenuItem getMiCompute();   public JMenuItem getMiCancel();
public JButton getToolCompute();   public JButton getToolCancel();
// 其余 getMiOpen/getMiSave/... 与 getToolOpen/... 为同名 getter
public static void main(String[] args);   // 入口：new MainController(frame) 后 setVisible

// ui.InputPanel
public String getInputText();
public void setInputText(String text);
public void requestLoadFile();              // 供菜单/工具栏复用
public void requestSaveFile();
public void clearAll();
public void installDefaultSyncActions(Component dialogParent);
public void syncTextToTable();              // DataParser.parse 后填充普通边+自环
public void syncTableToText();

// ui.ResultPanel
public void setResults(List<List<String>> results);
public List<List<String>> getResults();
public void setSelectionListener(SelectionListener listener);
interface SelectionListener { void onSequenceSelected(List<String> sequence); }

// ui.StatusBar
public void updateStats(int nodeCount, int edgeCount, boolean hasCycle,
                        int totalSorts, long costMs);
public void setTip(String text);
public void reset();

// ui.MainController
public MainController(MainFrame frame);
// 常量：MAX_RESULTS = 10000，TIMEOUT_MILLIS = 30000
private void compute();            // 解析→建图→判环→（环：结束 / 无环：后台枚举）
private void cancelCompute();      // cancelRequested=true 并 worker.cancel(true)
private void clearResults();
private void exportResult();       // txt/csv
private void exportPNG();
// 内部类：private class EnumerationWorker extends SwingWorker<EnumerationResult, Void>

// view.GraphPanel
public void setGraph(Graph graph);
public void setHighlightedCycle(List<String> closedCycle); // 闭合序列 [A,...,A]
public void setSelectedOrder(List<String> order);
public void clear();
public void exportPNG(File file) throws IOException;

// util.ExceptionHandler
public static void showError(Component parent, String message);
public static void showIssues(Component parent, List<String> messages, String title);
public static void showParseErrors(Component parent, List<String> messages);
public static void showWarning(Component parent, String message);
public static void showInfo(Component parent, String message);
public static boolean showConfirm(Component parent, String message);
public static void handle(Component parent, Throwable t);
```

---

## 五、与算法、解析模块的调用关系

### 5.1 调用契约清单（实际使用的签名）

| 调用方 | 被调方 | 方法 | 返回 | 何时调用 |
|---|---|---|---|---|
| MainController / InputPanel | io.DataParser | static parse(String) | ParseResult | 计算前解析、文本→表格同步 |
| MainController | ParseResult | isSuccess() / hasData() / getEdges() / getSelfLoops() / getErrors() | boolean / List<Edge> / List<ParseError> | 校验与建图 |
| MainController | model.Graph | addEdge(String,String) | boolean | 边列表逐条建图（含自环） |
| MainController | algorithm.CycleDetector | static findCycle(Graph) | List<String>（空=无环，闭合=有环） | 建图后判环 |
| EnumerationWorker | algorithm.AllTopoSorts | static enumerate(Graph,int,long,BooleanSupplier) | EnumerationResult | 无环时后台枚举 |
| MainController | EnumerationResult | getSequences() / getGeneratedCount() / isComplete() / getStopReason() | — | 回填 UI 与提示 |
| MainController / InputPanel | io.FileManager | readFile / saveFile / exportTxt / exportCsv（均 static，File 参数，抛 IOException） | — | 文件读写与结果导出 |
| MainController | view.GraphPanel | setGraph / setHighlightedCycle / setSelectedOrder / exportPNG | void | 画布刷新与导出 |
| MainController | ResultPanel / StatusBar | setResults / updateStats / setTip | void | 结果与状态刷新 |

补充类型：

- `DataParser.Edge`：getSource()、getTarget()、isSelfLoop()；getEdges() 返回已去重、不含自环的边，自环在 getSelfLoops() 中；
- `DataParser.ParseError`：getLineNumber()（0 表示全局性错误，如"没有有效数据行"）、getMessage()；
- `algorithm.TopoResult`：getOrder()、hasCycle()；`StopReason`：COMPLETED / LIMIT_REACHED / CANCELLED / TIMEOUT / CYCLE；
- 空图枚举按契约返回 `[[]]`（生成数 1、完整），含环图返回 CYCLE、序列为空。

### 5.2 Graph 公开能力（A 图设计，算法/视图层只允许使用这些方法）

`boolean addVertex(String)`、`boolean addEdge(String,String)`、`boolean removeEdge(String,String)`、`List<String> getVertexNames()`、`List<String> getSuccessors(String)`、`int getInDegree(String)`、`int getOutDegree(String)`、`int getVertexCount()`、`int getEdgeCount()`。Graph 不暴露 Vertex/Edge 内部结构，也不维护可供外部遍历的独立 Edge 列表；视图层通过 getVertexNames + getSuccessors 派生全部边。

### 5.3 数据流图

```
[文本区 <a,b> 输入 / data 目录数据文件]
                 │ String
                 ▼
          io.DataParser.parse（D 的标准解析器）
                 │ ParseResult
                 ▼
   errors 非空？──是──▶ 中文错误弹窗（带行号），中止
   edges+selfLoops 为空？──是──▶ 警告，中止
                 │ 否
                 ▼
        new Graph() + addEdge 逐条建图
                 │ Graph
                 ▼
       CycleDetector.findCycle
                 │
      ┌──────────┴───────────┐
      ▼ 非空                  ▼ 空
 环路径红色高亮+警告      EnumerationWorker（后台线程）
                          AllTopoSorts.enumerate
                          （上限 10000 / 超时 30s / 可取消）
                                │ EnumerationResult
                                ▼
        ResultPanel.setResults + GraphPanel 首条序列高亮
        StatusBar.updateStats + 按 StopReason 提示
                                │
                                ▼
                  可导出 PNG / TXT / CSV
```

---

## 六、异常处理设计

### 6.1 异常场景与处理方式（当前实现）

| 场景 | 判定位置 | 处理方式与用户反馈 |
|---|---|---|
| 输入为空白 | compute() 开头 | 警告"请输入关系数据后再计算"，中止 |
| 语法/格式错误（如缺少尖括号、括号不匹配） | ParseResult.getErrors() | 错误弹窗，逐行"第 x 行：原因"，最多 20 条，中止建图 |
| 只有注释/空行，无有效边 | ParseResult.hasData()=false | 警告"没有有效的关系数据"，中止 |
| 重复边 | 解析器自动去重 | 不报错；重复计数保留在 getDuplicateCount() |
| 自环 `<x,x>` | 进入 getSelfLoops()，照常建图 | findCycle 返回 [x,x]，画布标红 + 环警告 |
| 两节点互指/更长环 | findCycle | 闭合路径标红，结果列表清空，状态栏"含环" |
| 枚举达到上限 / 超时 / 被取消 | StopReason | 展示已生成序列，状态栏分别提示上限/超时/已取消 |
| 载入、保存 IO 失败 | InputPanel 内 try-catch | JOptionPane 错误弹窗（含异常消息） |
| 导出结果 / PNG 失败 | MainController try-catch | ExceptionHandler.handle 统一弹窗 |
| 导出时无结果/无图 | 导出方法开头 | 警告"请先计算"，不弹文件对话框 |

### 6.2 错误消息格式

解析错误在 MainController 中统一格式化后交给 ExceptionHandler：

```
第3行：无法识别的数据格式
第7行：括号不匹配
（lineNumber == 0 时省略"第x行："前缀，直接显示全局原因）
```

---

## 七、设计评审与遗留问题

### 7.1 已实现项与验证证据

| 任务 | 状态 | 验证 |
|---|---|---|
| T-B1 MainFrame | 完成 | 全量 `-encoding UTF-8` 编译通过；GUI 启动截图，菜单/工具栏/分割布局正常 |
| T-B2 InputPanel | 完成 | 默认中文提示正常显示；载入/保存/双向同步/增删行可用；解析器接入 |
| T-B3 ResultPanel | 完成 | 每页 20 条分页、单击高亮回调、双击复制均已验证 |
| T-B4 MainController | 完成 | 示例数据计算得 `MA 140 -> MA 141 -> CS 150`；后台枚举与取消按钮互斥启用 |
| T-B5 StatusBar + ExceptionHandler | 完成 | 含环红色/无环绿色；错误弹窗带行号 |
| T-B6/T-B7 文档 | 完成 | 需求分析报告、本报告随代码同步更新至 V1.1 |
| T-B8 UIStyle | 完成 | 全局样式统一；修复中文方块问题（Consolas → Font.MONOSPACED） |

流水线验证（dev-b）：data/figure1.txt（15 节点/16 边）、data/curriculum.txt（43 节点/85 边）解析建图成功，Kahn 序列长度等于节点数，枚举达上限正确停止；互指环返回 `a -> b -> a`、自环返回 `x -> x`、纯注释输入与非法行被正确拒绝。

### 7.2 中文显示问题的排查结论

- 源码为 UTF-8 无 BOM，编译带 `-encoding UTF-8`，class 文件中字符串经 Unicode 转义探针核对完全正确（早期 javap 看到的乱码只是 PowerShell 控制台 GBK 代码页的显示问题）；
- 真正的显示故障是字体：文本区使用的物理字体 Consolas 不含中文字形，JTextArea/JList 中中文被画成方块（JLabel 使用微软雅黑因此正常）；
- 修复：UIStyle.FONT_MONO 改为逻辑字体 `new Font(Font.MONOSPACED, Font.PLAIN, 13)`，ASCII 仍等宽，中文自动回退，GUI 截图确认正常。

### 7.3 已知差异与遗留项

1. **InputValidator 规则不一致**：D 的 `InputValidator.validate` 正则不允许节点名含内部空格，会把契约与解析器都接受的 `MA 140` 判为非法。因此计算流程以 DataParser 为唯一权威，未接入预校验；需例会上请 D 统一校验规则后再决定是否启用；
2. **A 新版契约未合入 main**：当前算法层按冻结契约 V1.0 编译运行；A 适配边列表的新契约合入后，预计只需调整 MainController 的建图适配段；
3. **画布为占位实现**：当前环形静态布局由 B 维护，分层布局、缩放拖拽、悬停高亮、点击反馈按会议决议等待 C 迭代；GraphPanel 的三个 set/export 方法签名已按对接需要固定；
4. **节点两行展示**："编码 + 课程名"依赖课程名数据，curriculum.txt 目前以中文实践课名作为节点名的一部分存在，独立课程名映射尚未提供；
5. **快捷键与国际化**：仅 Alt 菜单助记符，无 Ctrl 加速键；界面文案中文硬编码，ResourceBundle 国际化后续迭代。

---

## 八、附录：包结构与构建运行

### 8.1 实际目录结构（dev-b）

```
e:\tp
├── src\                          源码根（编译输出 src\out，已被 .gitignore 忽略）
│   ├── model\                    A：Graph / Vertex / Edge
│   ├── algorithm\                A：TopologicalSolver / TopoResult
│   │                             │   AllTopoSorts / EnumerationResult / StopReason
│   │                             └── CycleDetector
│   ├── io\                       D：DataParser / FileManager（直接复用正式实现）
│   ├── util\                     D：InputValidator；B：UIStyle / ExceptionHandler
│   ├── ui\                       B：MainFrame(T-B1) / InputPanel(T-B2)
│   │                             │   ResultPanel(T-B3) / MainController(T-B4)
│   │                             └── StatusBar(T-B5)
│   └── view\                     GraphPanel（静态画布，C 演进）
├── data\
│   ├── figure1.txt               15 节点 / 16 边示例
│   └── curriculum.txt            43 节点 / 85 边培养方案数据
├── docs\                         需求分析、接口契约、代码规范、本设计报告
├── screenshots\                  验收截图目录
├── test\                         测试目录
└── README.md
```

### 8.2 编译与运行（PowerShell，必须显式指定 UTF-8）

```powershell
Set-Location "e:\tp\src"
$files = (Get-ChildItem -Recurse -Filter "*.java" |
          Where-Object { $_.Name -ne "package-info.java" }).FullName
$argList = @('-encoding','UTF-8','-d','out') + $files
& javac @argList

Set-Location out
java ui.MainFrame
```

注意：在 Windows PowerShell 中通过数组 splatting（`@argList`）传参，确保 `-encoding UTF-8` 真正传给 javac；直接写 `javac -encoding UTF-8 ...` 在部分调用方式下参数可能被吞掉。
