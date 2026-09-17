package topo.algorithm;

import java.util.Collections;
import java.util.List;

// 环检测结果（A 包桩，T-A4）
public class CycleResult {

    private final boolean hasCycle;
    private final List<String> cyclePath;

    public CycleResult(boolean hasCycle, List<String> cyclePath) {
        this.hasCycle = hasCycle;
        this.cyclePath = cyclePath == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(cyclePath);
    }

    public static CycleResult noCycle() {
        return new CycleResult(false, Collections.emptyList());
    }

    public boolean hasCycle() { return hasCycle; }
    public List<String> getCyclePath() { return cyclePath; }

    // 格式化为 A -> B -> C -> A
    public String formatPath() {
        return cyclePath.isEmpty() ? "" : String.join(" -> ", cyclePath);
    }

    @Override
    public String toString() {
        return hasCycle ? "含环: " + formatPath() : "无环";
    }
}
