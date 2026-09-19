package algorithm;

import java.util.List;

/**
 * 一次基础拓扑排序的不可变结果，由 TopologicalSolver 创建。
 * 无环时保存完整序列；含环时保存空序列；空图是无环的成功结果。
 *
 * @author A
 */
public final class TopoResult {
    private final List<String> order;
    private final boolean hasCycle;

    TopoResult(List<String> order, boolean hasCycle) {
        this.order = List.copyOf(order);
        this.hasCycle = hasCycle;
    }

    /**
     * @return 不可修改的完整拓扑序列；含环或空图时为空，需结合 hasCycle 判断
     */
    public List<String> getOrder() {
        return order;
    }

    /** @return 输入图含环时为 true，空图和其他无环图为 false */
    public boolean hasCycle() {
        return hasCycle;
    }
}
