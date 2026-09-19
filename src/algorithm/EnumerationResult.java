package algorithm;

import java.util.List;
import java.util.Objects;

/**
 * 不可变的多序列枚举结果，只有 COMPLETED 才能将生成数量作为确切总数。
 *
 * @author A
 */
public final class EnumerationResult {
    private final List<List<String>> sequences;
    private final StopReason stopReason;

    EnumerationResult(List<List<String>> sequences, StopReason stopReason) {
        this.sequences = sequences.stream().map(List::copyOf).toList();
        this.stopReason = Objects.requireNonNull(stopReason);
    }

    /** @return 外层和每条序列都不可修改的结果快照 */
    public List<List<String>> getSequences() {
        return sequences;
    }

    /** @return 当前实际保存的序列数量，不一定等于合法序列总数 */
    public int getGeneratedCount() {
        return sequences.size();
    }

    /** @return 仅在已穷尽所有选择时为 true；含环、限量、取消、超时均为 false */
    public boolean isComplete() {
        return stopReason == StopReason.COMPLETED;
    }

    /** @return 本次计算停止的原因 */
    public StopReason getStopReason() {
        return stopReason;
    }
}
