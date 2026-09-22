# 拓扑排序应用软件（Topological Sort Application）

> 高级算法原理实践课程项目 · 基于 Java Swing 的有向图拓扑排序枚举与可视化工具

---

## 项目简介

本项目是高级算法原理实践课程的小组作业，实现了一个基于图形界面的拓扑排序工具。用户输入课程先修关系后，系统自动解析构建有向无环图，枚举所有合法的拓扑排序结果，并以可视化方式展示图结构、高亮环路径、联动查看每条拓扑序的执行顺序。

核心目标：不仅输出单条拓扑排序，而是**枚举所有可能的拓扑排序结果**，并支持大图下的上限、超时、取消保护机制。

---

## 核心功能

| 功能模块 | 说明 |
|---------|------|
| 关系输入 | 支持文本框直接输入、TXT文件导入、表格编辑三种方式 |
| 自动建图 | 解析 `<a,b>` 格式关系，自动去重、识别自环、定位非法行号 |
| 拓扑排序枚举 | 集成 Kahn 算法 + DFS 回溯全枚举，默认上限1000条 |
| 环检测 | 自动检测环并红色高亮环路径，状态栏提示环信息 |
| 可视化画布 | 分层/环形双布局，支持缩放、平移、节点拖动 |
| 结果联动 | 点击结果列表，画布同步高亮对应拓扑序路径与顺序序号 |
| 多格式导出 | 支持 TXT/CSV 结果导出、PNG 关系图导出 |
| 稳定保护 | 30秒超时、可中途取消，大图运算不卡死 |

---

## 技术栈

```mermaid
graph LR
    A[开发语言<br/>Java 21 兼容JDK8+] --> B[GUI框架<br/>Java Swing 零依赖]
    B --> C[数据格式<br/>纯文本.txt 无数据库]
    C --> D[版本管理<br/>Git + GitHub]
    D --> E[运行平台<br/>Windows/Linux/macOS]
```

---

## 系统架构

```mermaid
flowchart TB
    subgraph UI层
        A1[MainFrame 主窗口]
        A2[InputPanel 输入面板]
        A3[ResultPanel 结果面板]
        A4[StatusBar 状态栏]
    end
    subgraph 控制层
        B1[MainController 主控制器<br/>任务调度/线程管理/模块对接]
    end
    subgraph 功能模块层
        C1[算法模块<br/>Kahn/全枚举/环检测]
        C2[可视化模块<br/>GraphPanel/布局/缩放]
        C3[IO模块<br/>解析/文件读写/导出]
        C4[模型模块<br/>Graph/Vertex/Edge]
    end
    UI层 --> 控制层
    控制层 --> 功能模块层
```

---

## 核心算法说明

### 1. Kahn 拓扑排序
基于入度队列的经典算法，时间复杂度 O(V+E)，每次选取入度为 0 的节点加入结果，更新后继入度，直到队列为空。

### 2. 全拓扑排序枚举
基于 DFS + 回溯：
```mermaid
flowchart LR
    S[开始] --> A[找出所有入度为0的节点]
    A --> B{还有候选节点?}
    B -->|是| C[选一个节点加入结果]
    C --> D[递归深入]
    D --> E[回溯 恢复入度]
    E --> B
    B -->|否| F[记录一条完整拓扑序]
    F --> G{达到上限?}
    G -->|否| A
    G -->|是| H[停止返回]
```

### 3. 环检测与环路径定位
- Kahn 计数法判定：出队节点数 < 总节点数 则存在环
- DFS 三色标记法定位具体环路径

---

## 项目结构

```mermaid
flowchart TB
    Root[topological-sort]
    Root --> Src[src/ 源码]
    Root --> Data[data/ 测试数据]
    Root --> Docs[docs/ 设计文档]
    Root --> Test[test/ 单元测试]
    Src --> Model[model/ 图结构]
    Src --> Algo[algorithm/ 核心算法]
    Src --> IO[io/ 输入输出]
    Src --> View[view/ 可视化组件]
    Src --> UI[ui/ GUI界面]
    Src --> Util[util/ 工具类]
    Data --> F1[figure1.txt 15节点示例]
    Data --> F2[curriculum.txt 43节点课程]
```

---

## 成员分工

| 成员 | 学号 | 负责模块 |
|------|------|---------|
| 易雨杰 | 2024611209 | GUI 主框架 + 交互控制 + 项目统筹 |
| 骆深敏 | 2024611026 | 架构设计 + 核心算法 + 文档统筹 |
| 戴燕岚 | 2024611180 | 关系图可视化 + 图片导出 |
| 黄佳慧 | 2024611022 | 数据解析 + 导入导出 + 异常处理 |
| 吴丽梅 | 2024611020 | 测试 + 工程交付 + 汇报材料 |

---

## 快速开始

### 环境要求
- JDK 8 或以上版本
- Git

### 编译运行

```powershell
# 克隆仓库
git clone https://github.com/yyj138/topological-sort.git
cd topological-sort

# 编译源码
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | where name -ne package-info).FullName

# 启动图形界面
java -cp out ui.MainFrame

# 命令行测试（无需GUI）
java -cp out AlgorithmRunner data/figure1.txt
```

---

## 数据格式说明

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

---

## 项目里程碑

```mermaid
gantt
    title 项目进度时间线
    dateFormat  YYYY-MM-DD
    section 开发阶段
    接口冻结           :done, a1, 2026-09-17, 1d
    核心功能MVP        :done, a2, 2026-09-18, 3d
    中期汇报演示       :active, a3, 2026-09-22, 2d
    功能冻结测试       :a4, after a3, 3d
    最终验收交付       :a5, 2026-09-28, 1d
```
