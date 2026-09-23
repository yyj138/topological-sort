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
├── README.md                 
├── .gitignore
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
 |   └── AlgorithmRunner.java   # 无 GUI 的命令行独立测试入口
├── test/                      # 算法测试（四类用例）代码 / 解析器测试代码 / 自测代码 / txt测试报告
└── screenshots/               # 运行截图

说明：bin和out目录为javac编译class文件自动生成，不需要上传版本库，干净环境编译时自动创建。

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

# 编译src和test到out文件夹中
$outDir=".\out"
if(Test-Path $outDir){Remove-Item -Recurse -Force $outDir}
New-Item -ItemType Directory -Path $outDir | Out-Null
$src=Get-ChildItem src -Recurse -Filter *.java|Where-Object{$_.Name -ne "package-info.java"}
$test=Get-ChildItem test -Recurse -Filter *.java|Where-Object{$_.Name -ne "package-info.java"}
javac -encoding UTF-8 -d $outDir @($src.FullName+$test.FullName)

# 运行GUI
java -cp out ui.MainFrame

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
4. 运行GUI: 软件可直接粘贴`<a,b>`格式数据或导入TXT文件，点击计算即可完成解析、建图、环检测与拓扑枚举，支持中途取消，默认1000条结果上限、30秒超时停止；画布支持缩放平移，可切换布局，点击结果可高亮对应拓扑序，支持将结果导出为TXT/CSV、关系图导出为PNG。

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
源码提交git仓库不提交bin和out编译产物。
本readme.txt内容与《用户使用手册》文档内容保持一致，互不冲突。
