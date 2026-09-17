package algorithm;

// 枚举停止原因（契约 §四）
public enum StopReason {
    COMPLETED,       // 已穷尽全部选择
    LIMIT_REACHED,  // 达到数量上限，尚未确认穷尽
    CANCELLED,       // 用户取消
    TIMEOUT,         // 达到运行时限
    CYCLE            // 输入图含环，不进入枚举
}
