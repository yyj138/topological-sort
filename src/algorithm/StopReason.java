package algorithm;

/** 多序列枚举的停止原因，对应接口契约 V1.0。 */
public enum StopReason {
    /** 已穷尽全部选择。 */
    COMPLETED,
    /** 已达到数量上限，尚未确认穷尽。 */
    LIMIT_REACHED,
    /** 调用方请求取消。 */
    CANCELLED,
    /** 已达到运行时限。 */
    TIMEOUT,
    /** 输入图含环，未展开枚举。 */
    CYCLE
}
