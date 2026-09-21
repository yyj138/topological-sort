============================================================
                拓扑排序应用软件 topological-sort
                    组别：第6组
============================================================
【项目简介】
本Java程序实现有向图解析、环检测、Kahn算法拓扑排序、枚举全部合法拓扑序列；
支持命令行入口 AlgorithmRunner.java，配套Swing图形界面，附带完整测试用例。

运行环境：
    JDK 11+（推荐JDK21）
    操作系统：Windows / Linux / macOS
    Windows可使用批处理冒烟脚本，Linux/macOS需手动执行编译命令。

============================================================
一、项目目录结构
============================================================
topological-sort/
├─ README.md               # Markdown版本项目说明文档
├─ README.txt              # 本说明文档
├─ .gitignore              # Git忽略文件配置
├─ docs/                   # 项目文档
│  ├─ 接口契约.md          # V0.1 已冻结
│  └─ 代码规范.md
├─ data/                   # 样例输入图文件
│  ├─ figure1.txt          # 15门课程先修样例图
│  └─ curriculum.txt       # 全系课程图，≥30节点
├─ src/                    # 源代码
│  ├─ model/               # 图模型：Vertex / Edge / Graph
│  ├─ algorithm/           # 算法模块：Kahn / 全拓扑枚举 / 环检测
│  ├─ io/                  # IO模块：DataParser / FileManager / ImageExporter
│  ├─ view/                # 绘图面板：GraphPanel / LayoutManager
│  ├─ ui/                  # GUI界面：MainFrame / InputPanel / ResultPanel / MainController
│  ├─ util/                # 工具类：Constants / InputValidator
│  └─ AlgorithmRunner.java # 命令行独立入口（T‑E4）
├─ test/                   # 测试源码：算法测试、解析容错测试（T‑E1、T‑E2）
├─ screenshots/            # 程序运行截图
└─ run_tests.bat           # Windows一键冒烟测试脚本（T‑E3）

说明：bin目录为javac编译class文件自动生成，不需要上传版本库，干净环境编译时自动创建。

============================================================
二、Windows平台使用
============================================================
--------------------------
方式1：一键冒烟测试（Windows专用）
--------------------------
1. 将命令行/资源管理器定位到【项目根目录 topological-sort/】
2. 双击 run_tests.bat
3. 脚本自动完成：清理旧编译产物 → 全部源码编译 → 执行全套测试
4. 👉用途：验收前自检脚本，一键运行全部测试用例，快速判断项目是否正常
5. 测试汇总报告输出至：test\smoke_summary.txt
6. 详细测试输出：
    test\test_E1_result.txt   算法测试详细输出
    test\test_E2_result.txt   解析容错测试详细输出

⚠️重要提示：
run_tests.bat 仅支持Windows系统。
Linux / macOS 无法直接运行bat脚本，请参考下方手动编译运行。

--------------------------
方式2：命令行入口 AlgorithmRunner（无需GUI）
--------------------------
# 第一步编译全部代码（在项目根目录 topological-sort/ 执行）
javac -encoding UTF-8 ^
-sourcepath src;test ^
-d bin ^
src\model\*.java ^
src\algorithm\*.java ^
src\io\*.java ^
src\view\*.java ^
src\ui\*.java ^
src\util\*.java ^
src\AlgorithmRunner.java ^
test\*.java

# 用法1：仅控制台输出
java -Dfile.encoding=UTF-8 -cp bin AlgorithmRunner data/figure1.txt

# 用法2：控制台输出同时写入日志文件
java -Dfile.encoding=UTF-8 -cp bin AlgorithmRunner data/figure1.txt test/out_sample.txt

参数说明：
    第一个参数：输入图文本文件路径（相对路径，基于项目根目录topological-sort/）
    第二个参数(可选)：输出日志txt文件路径

--------------------------
方式3：运行图形界面GUI
--------------------------
编译完成后，在项目根目录执行：
java -Dfile.encoding=UTF-8 -cp bin ui.MainFrame

--------------------------
方式4：打包生成可执行jar包（可选）
--------------------------
可将项目打包为jar，实现一键启动GUI，任务要求两种启动方式：双击bat脚本、java‑jar。
打包后运行示例：
java -jar topological-sort.jar

============================================================
三、Linux / macOS 手动编译&运行
============================================================
# 编译（项目根目录 topological-sort/ 执行）
javac -encoding UTF-8 \
-sourcepath src:test \
-d bin \
src/model/*.java \
src/algorithm/*.java \
src/io/*.java \
src/view/*.java \
src/ui/*.java \
src/util/*.java \
src/AlgorithmRunner.java \
test/*.java

# 命令行入口运行示例
java -Dfile.encoding=UTF-8 -cp bin AlgorithmRunner data/figure1.txt
java -Dfile.encoding=UTF-8 -cp bin AlgorithmRunner data/figure1.txt test/out_sample.txt

# GUI运行
java -Dfile.encoding=UTF-8 -cp bin ui.MainFrame

============================================================
四、数据输入格式规范
============================================================
文本文件每一行代表一条有向边，格式 <起点,终点>
支持特性：
  1. #开头行为注释，程序自动跳过
  2. 忽略空行；容忍行首尾空格；兼容全角尖括号、全角逗号
  3. 自动识别重复边、自环，给出警告或提示

示例内容(figure1.txt片段):
# 课程先修关系样例
<CS 150,CS 200>
<CS 200,CS 230>
<MA 140,MA 141>

============================================================
五、IDE运行注意事项（IDEA/Eclipse）
============================================================
❗关键：IDE运行时，请把【工作目录 Working directory 设置为项目根目录 topological-sort/】，
不要设置为src目录，否则会找不到 data/、test/ 相对路径文件。

JVM运行参数建议添加：
-Dfile.encoding=UTF-8
保证中文、UTF‑8文本读写不出现乱码。

============================================================
六、干净环境解压运行提示【验收重点】
============================================================
从提交压缩包解压之后，**必须进入解压出来的项目根目录 topological-sort/ 执行全部编译/运行命令，不要进入src子文件夹执行**。
本readme文档满足T‑E6任务要求，在未安装IDE的干净环境下，可以按照本文档完成编译、测试、运行GUI与命令行程序。

============================================================
七、测试说明
============================================================
1. AlgorithmTest(test包)：算法全套测试，正确性、边界、含环异常、千节点性能
   输出文件：test/test_E1_result.txt
2. ParserFaultTest(test包)：解析器容错测试，覆盖非法输入、边界场景
   输出文件：test/test_E2_result.txt
3. run_tests.bat：冒烟测试，汇总所有测试PASS/FAIL状态。

============================================================
八、常见问题 FAQ
============================================================
Q1：提示文件找不到 data/figure1.txt
A：必须在【项目根目录 topological-sort/】启动程序，不要进入src子目录运行；IDE检查工作目录配置。

Q2：中文乱码
A：运行时添加JVM参数 -Dfile.encoding=UTF-8；所有输入输出文件使用UTF‑8编码。

Q3：Linux/mac运行bat报错
A：bat是Windows批处理，复制脚本内javac、java命令手动执行。

Q4：输出目录test不存在？
A：程序与脚本会自动创建test文件夹，无需手动新建。

Q5：测试出现FAIL
A：查看test目录下对应的txt日志，定位错误用例，排查代码或输入数据。

============================================================
九、打包提交说明
============================================================
课程任务最终提交压缩包命名为 groupXX.zip（例如group01.zip代表第1组），目录结构与本文件描述一致。
源码提交git仓库不提交bin编译产物。
本readme.txt内容与《用户使用手册》文档内容保持一致，互不冲突。
