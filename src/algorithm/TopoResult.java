package algorithm;

import java.util.Collections;
import java.util.List;

// 单序列排序结果（契约 §三）：无环时 getOrder 含全部节点；含环时返回空列表
public class TopoResult {

    private final List<String> order;
    private final boolean hasCycle;

    public TopoResult(List<String> order, boolean hasCycle) {
        this.order = order == null ? Collections.emptyList() : Collections.unmodifiableList(order);
        this.hasCycle = hasCycle;
    }

    public List<String> getOrder() { return order; }
    public boolean hasCycle() { return hasCycle; }
}
