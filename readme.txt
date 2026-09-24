============================================================
                拓扑排序应用软件 topological-sort
                    组别：第6组
============================================================
【项目简介】
本Java程序实现有向图解析、环检测、Kahn算法拓扑排序、枚举全部合法拓扑序列；
支持命令行入口 AlgorithmRunner.java，配套Swing图形界面，附带完整测试用例。

运行环境：
1. 操作系统：Windows 10/11（推荐），兼容 Linux / macOS
2. Java 环境：**使用 JDK 21 编译和运行，本机已验证 JDK 21.0.6**
3. 依赖说明：程序只使用 JDK 标准库，无需另装 Swing、数据库、服务器或第三方绘图库。
4. 编码与字体：源码、输入输出文本全部按 UTF‑8 处理，编译必须携带 `-encoding UTF‑8` 参数；界面、画布使用 Microsoft YaHei，文本区域使用逻辑字体，系统需要具备可用中文字体保证中文符号正常显示。
5. 运行前置条件：GUI图形界面需要桌面图形环境；输入文件需要具备可读权限，导出保存的目录需要具备可写权限。
6. 注意：直接运行编译好的class，**不需要安装Git、不需要IDE，不需要手动配置CLASSPATH，不需要额外设置系统中文区域**。

============================================================
一、项目目录结构
============================================================
topological-sort/
├── readme.txt                 
├── .gitignore
├── TopoSortApp.jar           # 可运行Jar包(T‑D8交付产物)
├── run.bat                   # Windows双击启动脚本(T‑D8交付产物)
├── run_tests.bat              # 冒烟自动化测试脚本
├── docs/                      # docs目录下存放Java+Swing课程拓扑排序桌面软件全套设计、接口、测试与协作文档，会议记录
├── data/                      # figure1.txt（15 门课程）、curriculum.txt（全系 ≥30 节点）
├── src/
│   ├── model/                 # Vertex / Edge / Graph
│   ├── algorithm/             # Kahn / 枚举 / 环检测
│   ├── io/                    # DataParser / FileManager / ParseIssue / ParseResult / ImageExporter
│   ├── view/                  # GraphPanel（静态环形画布），LayoutManager（分层布局）
│   ├── ui/                    # MainFrame / InputPanel / ResultPanel / MainController / StatusBar
│   ├── util/                  # UIStyle / ExceptionHandler / InputValidator
│   └── AlgorithmRunner.java   # 无 GUI 的命令行独立测试入口
├── test/                      # 算法测试（四类用例）代码 / 解析器测试代码 / 自测代码 / txt测试报告
└── screenshots/               # 运行截图
说明：bin和out目录为javac编译class文件自动生成，不需要上传版本库，干净环境编译时自动创建。

============================================================
二、Windows平台使用
============================================================
--------------------------
方式0：直接运行打包完成的GUI程序（T‑D8，推荐，无需编译）
--------------------------
> 前提：本机安装JDK21并配置环境变量；TopoSortApp.jar、run.bat必须与data文件夹处于同一个根目录。

方式0‑1：Jar命令行启动
1. 打开cmd控制台，进入项目根目录 topological‑sort
2. 执行命令：
java -jar "TopoSortApp.jar"
3. 执行后直接弹出拓扑排序应用软件图形界面。

方式0‑2：双击bat脚本启动（Windows推荐）
1. 在项目根目录找到 run.bat
2. 直接双击 run.bat 即可启动GUI程序。
> 说明：该方式附带控制台窗口，程序发生异常不会闪退，可以查看报错堆栈用于调试。
> 重要提醒：data文件夹必须与TopoSortApp.jar、run.bat放在同一目录，否则示例数据无法加载。

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
java -Dfile.encoding=UTF-8 -cp bin AlgorithmRunner data/figure1.txt test/algorithm_runner_sample_output.txt

参数说明：
    第一个参数：输入图文本文件路径（相对路径，基于项目根目录topological-sort/）
    第二个参数(可选)：输出日志txt文件路径

--------------------------
方式3：运行图形界面GUI
--------------------------
# 说明：该方式直接从源码编译运行，不需要Jar包；需要 src、test 源码完整存在。
# 在项目根目录 topological-sort/ 下执行以下CMD命令：

清理旧编译输出目录 out
递归收集src、test下全部Java源文件，排除 package‑info.java
UTF‑8编译，class输出到 out
删除临时源文件列表，存在才删除，避免文件不存在时报错
启动图形界面GUI
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java | findstr /v "package-info.java" > sources_temp.txt
dir /s /b test\*.java | findstr /v "package-info.java" >> sources_temp.txt
javac -encoding UTF-8 -d out @sources_temp.txt
if exist sources_temp.txt del sources_temp.txt
java -cp out ui.MainFrame

⚠️注意：
1. 必须在项目根目录执行，不要进入src子文件夹；
2. sources_temp.txt 为运行时临时文件，脚本执行完毕自动删除，不需要随项目提交；
3. 此方式用于开发调试；验收直接优先使用【方式0】运行 TopoSortApp.jar。

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

> 注：Linux/macOS没有bat脚本，无法直接使用run.bat；可使用jar命令运行GUI：java -jar TopoSortApp.jar

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
本readme文档满足T‑E6任务要求，在未安装IDE的干净环境下，可以按照本文档完成：直接运行jar/bat、编译源码、冒烟测试、运行GUI与命令行程序。

============================================================
七、测试说明
============================================================
1. AlgorithmTest(test包)：算法全套测试，正确性、边界、含环异常、千节点性能
   输出文件：test/test_E1_result.txt
2. ParserFaultTest(test包)：解析器容错测试，覆盖非法输入、边界场景
   输出文件：test/test_E2_result.txt
3. run_tests.bat：冒烟测试，汇总所有测试PASS/FAIL状态。
4. 运行GUI: 软件可直接粘贴`<a,b>`格式数据或导入TXT文件，点击计算即可完成解析、建图、环检测与拓扑枚举，支持中途取消，默认1000条结果上限、30秒超时停止；画布支持缩放平移，可切换布局，点击结果可高亮对应拓扑序，支持将结果导出为TXT/CSV、关系图导出为PNG。

============================================================
八、常见问题 FAQ
============================================================
Q1：提示文件找不到 data/figure1.txt
A：必须在【项目根目录 topological-sort/】启动程序，不要进入src子目录运行；IDE检查工作目录配置；使用jar/bat启动时确认data文件夹与TopoSortApp.jar同级。

Q2：中文乱码
A：运行时添加JVM参数 -Dfile.encoding=UTF-8；所有输入输出文件使用UTF‑8编码；bat脚本保存编码建议为ANSI。

Q3：双击run.bat提示找不到TopoSortApp.jar
A：run.bat必须与TopoSortApp.jar放在同一个文件夹，不能分开存放。

Q4：提示“jar中没有主清单属性”
A：打包时使用jar cfe命令指定主类ui.MainFrame，避免manifest.mf换行缺失问题。

Q5：Linux/mac运行bat报错
A：bat是Windows批处理，复制脚本内javac、java命令手动执行；GUI直接使用java‑jar运行jar包。

Q6：输出目录test不存在？
A：程序与脚本会自动创建test文件夹，无需手动新建。

Q7：测试出现FAIL
A：查看test目录下对应的txt日志，定位错误用例，排查代码或输入数据。

============================================================
九、打包提交说明
============================================================
课程任务最终提交压缩包命名为 groupXX.zip（例如group06.zip代表第6组）。
✅提交包02‑项目源程序必须包含交付产物：
src/（业务源码）、test/（测试源码）、data/、docs/、screenshots/、
TopoSortApp.jar、run.bat、run_tests.bat、README.txt

> 注意：bin、out、sources.txt为本地编译临时产物，**不要放进提交zip压缩包**。
> jar包内仅包含src编译后的业务class；test仅上交源代码，不打入jar包。
本readme.txt内容与《用户使用手册(T‑D8)》文档内容保持一致，互不冲突。
