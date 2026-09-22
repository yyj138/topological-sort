@echo off
chcp 65001 >nul
echo ==============================================
echo          T‑E3 冒烟测试脚本 run_tests.bat
echo ==============================================
setlocal enabledelayedexpansion
:: 路径配置
set SRC=src
set BIN=bin
set TEST_OUT=test\smoke_summary.txt

:: =========【新增：自动创建test目录，防止全新环境不存在目录重定向报错】========
if not exist test mkdir test
:: ==============================================================================

:: 清理旧输出与旧class
del /Q %TEST_OUT% 2>nul
if exist %BIN% rmdir /S /Q %BIN%
mkdir %BIN%
echo.
echo [1/4] 编译全部Java源码到 %BIN%
javac -encoding UTF-8 ^
-sourcepath %SRC%;test ^
-d %BIN% ^
%SRC%\model\*.java ^
%SRC%\algorithm\*.java ^
%SRC%\io\*.java ^
%SRC%\util\*.java ^
%SRC%\AlgorithmRunner.java ^
test\AlgorithmTest.java ^
test\ParserFaultTest.java
if !errorlevel! neq 0 (
    echo ❌ Java编译失败！终止冒烟测试
    echo ❌ Java编译失败！ >> %TEST_OUT%
    goto END
)
echo ✅ 编译完成
echo.
echo [2/4] 运行 AlgorithmRunner 校验标准样例 figure1.txt
echo -------- AlgorithmRunner -------- >> %TEST_OUT%
:: -Dfile.encoding=UTF‑8 强制Java控制台输出UTF‑8
java -Dfile.encoding=UTF-8 -cp %BIN% AlgorithmRunner data\figure1.txt >nul 2>&1
if !errorlevel! equ 0 (echo ✅ AlgorithmRunner:PASS >>%TEST_OUT% && echo ✅ AlgorithmRunner:PASS) else (echo ❌ AlgorithmRunner:FAIL >>%TEST_OUT% && echo ❌ AlgorithmRunner:FAIL)
echo.
echo [3/4] 运行 T‑E1 AlgorithmTest 算法全套测试
echo -------- AlgorithmTest(T‑E1) -------- >> %TEST_OUT%
:: 重点：不把完整日志重定向进smoke_summary！完整报告由Java内部输出test_E1_result.txt
java -Dfile.encoding=UTF-8 -cp %BIN% test.AlgorithmTest >nul 2>&1
if !errorlevel! equ 0 (echo ✅ AlgorithmTest:PASS >>%TEST_OUT% && echo ✅ AlgorithmTest:PASS) else (echo ❌ AlgorithmTest:FAIL >>%TEST_OUT% && echo ❌ AlgorithmTest:FAIL)
echo.
echo [4/4] 运行 T‑E2 ParserFaultTest 解析容错全套测试
echo -------- ParserFaultTest(T‑E2) -------- >> %TEST_OUT%
:: 完整业务日志Java内部输出test_E2_result.txt，bat只记录执行结果标记
java -Dfile.encoding=UTF-8 -cp %BIN% test.ParserFaultTest >nul 2>&1
if !errorlevel! equ 0 (echo ✅ ParserFaultTest:PASS >>%TEST_OUT% && echo ✅ ParserFaultTest:PASS) else (echo ❌ ParserFaultTest:FAIL >>%TEST_OUT% && echo ❌ ParserFaultTest:FAIL)
echo.
echo ==============================================
echo ✔ 冒烟测试全部执行完毕
echo 📋冒烟汇总报告：%TEST_OUT%
echo 📋独立详细报告：
echo    test\test_E1_result.txt （算法完整输出）
echo    test\test_E2_result.txt （解析容错完整输出）
echo ==============================================
:END
pause
endlocal
