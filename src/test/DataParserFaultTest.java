package test;

import io.DataParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * T‑E2 解析器容错测试
 * 对应文档：异常与边界场景清单.md，场景1~场景16，测试编号 T‑E2‑01 ~ T‑E2‑16
 * @author 组员E
 */
public class DataParserFaultTest {
    private static StringBuilder reportSb = new StringBuilder();

    public static void main(String[] args) {
        log("==================== T‑E2 解析器容错测试开始 ====================\n");

        testT_E2_01(); // 场景1：缺少左括号
        testT_E2_02(); // 场景2：缺少右括号
        testT_E2_03(); // 场景3：缺少逗号
        testT_E2_04(); // 场景4：乱码行
        testT_E2_05(); // 场景5：重复边
        testT_E2_06(); // 场景6：自环
        testT_E2_07(); // 场景7：含环图输入；解析器只做解析，环路检测交给CycleDetector
        testT_E2_08(); // 场景8：空数据
        testT_E2_09(); // 场景9：全部孤立节点输入
        testT_E2_10(); // 场景10：大图片段（模拟多行）
        testT_E2_11(); // 场景11：超长结果输入（大量互不相关边）
        testT_E2_12(); // 场景12：注释行和空行混合
        testT_E2_13(); // 场景13：全角尖括号输入
        testT_E2_14(); // 场景14：行首尾多余空格
        testT_E2_15(); // 场景15：文件不存在
        testT_E2_16(); // 场景16：括号内容为空

        log("\n==================== T‑E2 解析器容错测试结束 ====================\n");

        // 输出文件到项目根目录 ../test_output/parser_fault_test_result.txt
        writeReportToFile();
        System.out.println("测试报告已输出至 ../test_output/parser_fault_test_result.txt");
    }

    private static void log(String s) {
        System.out.print(s);
        reportSb.append(s);
    }

    private static void writeReportToFile() {
        // 【方案B修改点】 ../ 向上跳出src目录，输出到项目根目录，与src同级
        File dir = new File("../test_output");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (FileWriter fw = new FileWriter(new File(dir, "parser_fault_test_result.txt"), false)) {
            fw.write(reportSb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** T‑E2‑01 场景1：缺少左括号 a,b> */
    private static void testT_E2_01() {
        log("【T‑E2‑01】场景1：缺少左括号\n");
        String input = "a,b>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑02 场景2：缺少右括号 <a,b */
    private static void testT_E2_02() {
        log("【T‑E2‑02】场景2：缺少右括号\n");
        String input = "<a,b";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑03 场景3：缺少逗号 <a b> */
    private static void testT_E2_03() {
        log("【T‑E2‑03】场景3：缺少逗号\n");
        String input = "<a b>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑04 场景4：乱码行 */
    private static void testT_E2_04() {
        log("【T‑E2‑04】场景4：乱码行\n");
        String input = "@@@乱码内容###";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑05 场景5：重复边 <a,b> 出现两次 */
    private static void testT_E2_05() {
        log("【T‑E2‑05】场景5：重复边\n");
        String input = "<a,b>\n<a,b>\n<c,d>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑06 场景6：自环 <a,a> */
    private static void testT_E2_06() {
        log("【T‑E2‑06】场景6：自环\n");
        String input = "<a,a>\n<a,b>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑07 场景7：含环图输入；解析器只做解析，环路检测交给CycleDetector */
    private static void testT_E2_07() {
        log("【T‑E2‑07】场景7：含环图输入\n");
        String input = "<a,b>\n<b,c>\n<c,a>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("提示：解析器仅输出边集合；环路判断由CycleDetector模块完成\n");
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑08 场景8：空数据 */
    private static void testT_E2_08() {
        log("【T‑E2‑08】场景8：空数据\n");
        String input = "";
        log("输入文本:[空字符串]\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑09 场景9：全部孤立节点（这里解析器需要边，孤立点不会凭空生成；测试无有效边输入） */
    private static void testT_E2_09() {
        log("【T‑E2‑09】场景9：全部孤立节点，无有效边输入\n");
        String input = "#只有注释，没有数据\n#测试\n";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑10 场景10：大图（模拟多条边，不做千节点完整生成，仅解析容错） */
    private static void testT_E2_10() {
        log("【T‑E2‑10】场景10：大图片段，多条边\n");
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<20;i++){
            sb.append("<n").append(i).append(",n").append(i+1).append(">\n");
        }
        String input = sb.toString();
        log("输入文本（前5行）:\n" + input.substring(0, Math.min(200,input.length())) + "...\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑11 场景11：超长结果输入，大量互不依赖边 */
    private static void testT_E2_11() {
        log("【T‑E2‑11】场景11：大量互不相关边输入\n");
        StringBuilder sb = new StringBuilder();
        sb.append("<a1,>\n"); //故意错误
        for(int i=0;i<15;i++){
            sb.append("<x").append(i).append(",y").append(i).append(">\n");
        }
        String input = sb.toString();
        log("输入文本（部分）:\n" + input.substring(0, Math.min(200,input.length())) + "...\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑12 场景12：注释行和空行混合 */
    private static void testT_E2_12() {
        log("【T‑E2‑12】场景12：注释行和空行混合\n");
        String input = "#这是注释\n\n<a,b>\n\n#另一条注释\n<c,d>\n";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑13 场景13：全角尖括号输入 ＜a,b＞ */
    private static void testT_E2_13() {
        log("【T‑E2‑13】场景13：全角尖括号输入\n");
        String input = "＜a,b＞\n＜c,d＞";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑14 场景14：行首尾多余空格 */
    private static void testT_E2_14() {
        log("【T‑E2‑14】场景14：行首尾多余空格\n");
        String input = "  <x,y>  \n   <m,n>   ";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑15 场景15：文件不存在，调用parseFile */
    private static void testT_E2_15() {
        log("【T‑E2‑15】场景15：文件不存在\n");
        String fakePath = "not_exist_123456.txt";
        log("调用parseFile，路径="+fakePath+"\n");
        DataParser.ParseResult res = DataParser.parseFile(fakePath);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** T‑E2‑16 场景16：括号内容为空 <> 、<,> */
    private static void testT_E2_16() {
        log("【T‑E2‑16】场景16：括号内容为空\n");
        String input = "<>\n<,>\n<a,b>";
        log("输入文本:\n" + input + "\n");
        DataParser.ParseResult res = DataParser.parse(input);
        printParseResult(res);
        log("----------------------------------------------------\n");
    }

    /** 打印解析结果辅助函数 */
    private static void printParseResult(DataParser.ParseResult res) {
        log("有效边列表：");
        if(res.getEdges().isEmpty()){
            log("(空)");
        }else{
            for(DataParser.Edge e : res.getEdges()){
                log(e.toString()+" ");
            }
        }
        log("\n");
        log("自环列表：");
        if(res.getSelfLoops().isEmpty()){
            log("(空)");
        }else{
            for(DataParser.Edge e : res.getSelfLoops()){
                log(e.toString()+" ");
            }
        }
        log("\n");
        log("去重计数 duplicateCount="+res.getDuplicateCount()+"\n");
        log("错误列表：\n");
        if(res.getErrors().isEmpty()){
            log("  (无错误)\n");
        }else{
            for(DataParser.ParseError err : res.getErrors()){
                log("  "+err.toString()+"\n");
            }
        }
        log("hasData="+res.hasData()+"  isSuccess="+res.isSuccess()+"\n");
    }
}
