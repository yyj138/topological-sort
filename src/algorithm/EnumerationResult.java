package algorithm;

import java.util.Collections;
import java.util.List;

// 多序列枚举结果（契约 §四）
// getGeneratedCount 必须与 getSequences 大小一致
// 仅 isComplete=true 时才作为确切总数显示
public class EnumerationResult {

    private final List<List<String>> sequences;
    private final int generatedCount;
    private final boolean complete;
    private final StopReason stopReason;

    public EnumerationResult(List<List<String>> sequences, boolean complete, StopReason stopReason) {
        this.sequences = sequences == null ? Collections.emptyList() : Collections.unmodifiableList(sequences);
        this.generatedCount = this.sequences.size();
        this.complete = complete;
        this.stopReason = stopReason;
    }

    public List<List<String>> getSequences() { return sequences; }
    public int getGeneratedCount() { return generatedCount; }
    public boolean isComplete() { return complete; }
    public StopReason getStopReason() { return stopReason; }
}
