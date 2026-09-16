# 拓扑排序应用软件

本仓库用于 CST4823A 高级算法原理实践的完整开发过程，统一使用 [GitHub 仓库](https://github.com/yyj138/topological-sort)共享源码、设计文档和验证材料。项目目标是制作带图形界面的桌面应用，接收先后关系、显示有向图、生成多种拓扑排序结果并支持导入导出。当前处于基础工程与设计阶段，环境检查程序不代表拓扑排序功能已经完成。

## 当前状态

开发路线为 Java + Swing，当前工程以 JDK 21 为编译基线，供 9 月 17 日团队会议确认。Swing 随 JDK 提供，无需额外安装图形框架；当前没有 Maven、Gradle 或第三方运行依赖。A 的工作发布在 `dev-a`，其他成员的已有分支保持原样，经过审查的共享版本再按团队约定合入 `main`。

## 在 Windows 上检查基础工程

先安装 JDK 21，并确认终端的 `java -version` 和 `javac -version` 都指向兼容版本。使用 VS Code 时安装或启用 Extension Pack for Java，并通过 `Java: Configure Java Runtime` 选择项目 JDK。在仓库根目录运行以下命令，脚本会编译源码并检查 Java 与 Swing 类是否可用，不会打开软件窗口。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-environment.ps1
```

成功时会输出编译成功信息、Java 版本、`javax.swing.JFrame` 类名和环境检查通过提示。编译产物放入 `out/`，该目录不提交 Git；单独编译可运行下方命令。最终桌面软件的启动入口将在 B 的主窗口接入后说明，交付版 `readme.txt` 由 E 按团队分工补齐。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

## 目录与责任

| 路径 | 内容 | 主要负责人 |
| --- | --- | --- |
| `src/model/` | 顶点、边、图结构 | A |
| `src/algorithm/` | Kahn 排序、多结果枚举、环检测 | A |
| `src/ui/` | 主窗口、输入、结果展示和流程控制 | B |
| `src/view/` | 关系图布局、绘图和交互 | C |
| `src/io/` | 数据解析、文件读写；图片导出由 C 对接 | D、C |
| `src/util/` | 共享工具和输入校验 | 按接口责任分配 |
| `test/` | 测试源码、用例和说明 | E 主导，各模块自测 |
| `data/` | 图 1 和真实课程关系数据 | D |
| `docs/` | 接口、规范、设计和阶段安排 | A 统筹，全员提供各自内容 |
| `screenshots/` | 实际软件截图及图注 | C |
| `scripts/` | 编译与基础环境检查 | A 初始化，后续共同维护 |

## 输入和课程要求

基本输入是一行一个关系，英文半角尖括号和逗号表示 `<a,b>`，约定 a 必须先于 b。最终程序需要支持图形界面、文本数据存储、多种排序结果、关系图和结果导出，并用任务书图 1 及学业指南中的课程关系验证。空输入、自环、含环和结果数量过多等行为须先通过接口契约明确，不能把部分结果误报为全部结果。

```text
<A,C>
<B,C>
```

## 协作与记录

具体 Git 操作和提交规则见 [Git 协作说明](docs/Git协作说明.md)。每天实际完成的代码、设计和验证成果通过 Git 提交并上传个人分支，提交说明带任务编号，便于追踪阶段工作。个人日志仅在本人明确要求整理时生成，保存在仓库之外，不作为仓库提交材料。
