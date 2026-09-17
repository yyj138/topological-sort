import util.InputValidator;
import util.InputValidator.ValidationResult;

public class TestInputValidator {
    public static void main(String[] args) {
        // 测试1：正常输入
        System.out.println("=== 测试1：正常输入 ===");
        test("<a,b>\n<c,d>");

        // 测试2：缺少左括号
        System.out.println("\n=== 测试2：缺少左括号 ===");
        test("a,b>");

        // 测试3：缺少逗号
        System.out.println("\n=== 测试3：缺少逗号 ===");
        test("<a b>");

        // 测试4：空输入
        System.out.println("\n=== 测试4：空输入 ===");
        test("");

        // 测试5：混合输入（含注释和空行）
        System.out.println("\n=== 测试5：混合输入 ===");
        test("# 这是注释\n<a,b>\n\n<c,d>");

        // 测试6：全角尖括号
        System.out.println("\n=== 测试6：全角尖括号 ===");
        test("＜a,b＞");

        // 测试7：乱码行
        System.out.println("\n=== 测试7: 乱码行 ===");
        test("###乱码内容@@@");
    }

    private static void test(String input) {
        ValidationResult result = InputValidator.validate(input);
        if (result.isValid()) {
            System.out.println("通过！");
        } else {
            for (String error : result.getErrors()) {
                System.out.println("错误: " + error);
            }
        }
    }
}