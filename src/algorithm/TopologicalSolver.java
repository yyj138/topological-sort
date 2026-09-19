package algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Graph;

/**
 * 使用 Kahn 算法求出一种合法拓扑顺序，不修改输入图。
 * 每次调用只使用局部状态，同一个排序器可用于多次计算。
 *
 * @author A
 */
public final class TopologicalSolver {
    /** 创建无持久计算状态的排序器。 */
    public TopologicalSolver() {
    }

    /**
     * 将全部零入度节点按录入顺序加入先进先出队列，逐个处理节点及其后继。
     * 同一图的节点和后继遍历顺序不变时，结果可复现；不保证字典序最小。
     * 忽略名称长度、按哈希表平均复杂度计，时间 O(V+E)，辅助空间 O(V)。
     *
     * @param graph 待排序的图；调用方须保证计算期间不修改它
     * @return 无环时的完整序列，或含环状态与空序列；空图返回空序列且无环
     * @throws IllegalArgumentException graph 为 null
     */
    public TopoResult kahnSort(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待排序的图不能为 null");
        }

        List<String> names = graph.getVertexNames();
        Map<String, Integer> remainingInDegrees = new HashMap<>();
        ArrayDeque<String> ready = new ArrayDeque<>();
        for (String name : names) {
            int inDegree = graph.getInDegree(name);
            remainingInDegrees.put(name, inDegree);
            if (inDegree == 0) {
                ready.addLast(name);
            }
        }

        List<String> order = new ArrayList<>(names.size());
        while (!ready.isEmpty()) {
            String name = ready.removeFirst();
            order.add(name);
            for (String successor : graph.getSuccessors(name)) {
                // 只减少计算副本中的入度，保留 Graph 的原始边和前驱关系。
                int remaining = remainingInDegrees.get(successor) - 1;
                remainingInDegrees.put(successor, remaining);
                if (remaining == 0) {
                    ready.addLast(successor);
                }
            }
        }

        // 无法处理全部节点说明含环；丢弃部分顺序，避免被误认为完整答案。
        if (order.size() != names.size()) {
            return new TopoResult(List.of(), true);
        }
        return new TopoResult(order, false);
    }
}
