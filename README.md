# 拓扑排序应用软件（Topological Sort Application）

基于 Java Swing 开发的桌面端课程拓扑排序工具，输入课程先修关系，自动枚举所有合法拓扑排序结果并可视化展示。

## 功能特性

- **关系输入**：支持文本框直接输入 `<a,b>` 格式关系、文件导入、表格编辑三种方式
- **自动建图**：解析输入自动构建有向图，支持去重、自环识别、非法行号定位
- **拓扑排序**：集成 Kahn 算法、DFS 回溯多序列枚举、环检测与环路径定位
- **可视化展示**：分层/环形双布局，节点拖动、画布缩放平移、环路径红色高亮
- **结果联动**：点击拓扑序列，图上同步高亮对应路径并标注顺序序号
- **结果导出**：支持 TXT/CSV 排序结果导出、PNG 关系图导出
- **稳定容错**：1000 条结果上限、30 秒超时、可中途取消，大图不卡死

## 技术栈

| 项 | 说明 |
|----|------|
| 开发语言 | Java（JDK 21 开发，兼容 JDK 8+） |
| GUI 框架 | Java Swing（零第三方依赖） |
| 数据存储 | 纯文本文件 `.txt`，无数据库 |
| 版本管理 | Git + GitHub，分支开发 |
| 运行平台 | Windows / Linux / macOS 跨平台 |

## 核心算法

1. **Kahn 算法**：入度队列实现，时间复杂度 O(V+E)，输出单条拓扑序
2. **全拓扑枚举**：DFS + 回溯，每层选择入度为 0 的节点递归，支持结果上限
3. **环检测**：Kahn 计数法判定有环，DFS 三色标记输出具体环路径

## 项目结构

```
src/
├── model/          # 图数据结构（Vertex/Edge/Graph）
├── algorithm/      # 核心算法（Kahn/全枚举/环检测）
├── io/             # 输入输出（解析/文件读写/导出）
├── view/           # 可视化组件（GraphPanel/布局管理器）
├── ui/             # GUI界面（主窗口/输入面板/结果面板/控制器）
├── util/           # 工具类（UI样式/异常处理/输入校验）
└── AlgorithmRunner.java  # 命令行测试入口
data/              # 测试数据（figure1.txt/curriculum.txt）
docs/              # 设计文档/接口契约/测试记录
test/              # 单元测试代码
```

## 成员分工

| 成员 | 学号 | 负责模块 |
|------|------|---------|
| 易雨杰 | 2024611209 | GUI 主框架 + 交互控制 + 项目统筹 |
| 骆深敏 | 2024611026 | 架构设计 + 核心算法 + 文档统筹 |
| 戴燕岚 | 2024611180 | 关系图可视化 + 图片导出 |
| 黄佳慧 | 2024611022 | 数据解析 + 导入导出 + 异常处理 |
| 吴丽梅 | 2024611020 | 测试 + 工程交付 + 汇报材料 |

## 快速开始

### 环境要求
- JDK 8 或以上版本
- Git

### 编译运行（Windows PowerShell）

```powershell
# 克隆仓库
git clone https://github.com/yyj138/topological-sort.git
cd topological-sort

# 编译
$outDir=".\out"
if(Test-Path $outDir){Remove-Item -Recurse -Force $outDir}
New-Item -ItemType Directory -Path $outDir | Out-Null
$src=Get-ChildItem src -Recurse -Filter *.java | Where-Object{$_.Name -ne "package-info.java"}
javac -encoding UTF-8 -d $outDir $src.FullName

# 启动 GUI
java -cp out ui.MainFrame

# 命令行测试
java -cp out AlgorithmRunner data/figure1.txt
```

## 数据格式

每行一条关系，西文尖括号格式：`<先修课程,后续课程>`，表示存在一条从先修到后续的有向边。

```
# 示例
<MA 140, MA 141>
<MA 141, CS 150>
<CS 150, CS 155>
```

- 空行自动忽略
- `#` 开头为注释行
- 重复边自动去重
- 自环单独标记

## 里程碑

| 日期 | 阶段 |
|------|------|
| 09-17 | 接口冻结，模块开发启动 |
| 09-20 | 核心功能跑通，MVP 版本完成 |
| 09-22 | 中期汇报演示 |
| 09-23 | 功能冻结，进入测试阶段 |
| 09-28 | 最终验收交付 |
