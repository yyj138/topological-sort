package algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.Graph;

/**
 * 先以 Kahn 判环，再以 DFS 三色标记定位一条真实闭合环路径。
 * 只读取输入图，每次调用的访问标记和路径互不共享。
 *
 * @author A
 */
public final class CycleDetector {
    // 未进入颜色表的节点为白色；灰色在当前 DFS 路径中，黑色已遍历完成。
    private enum Color {
        GRAY, BLACK
    }

    /** 创建无持久计算状态的环检测器。 */
    public CycleDetector() {
    }

    /**
     * 按图的录入顺序搜索，返回遇到的第一条环，而非最短环或全部环。
     * 使用显式栈模拟 DFS，避免长路径导致递归调用栈溢出。
     * 忽略名称长度、按哈希表平均复杂度计，时间 O(V+E)；由于栈中保留
     * Graph 返回的后继快照，辅助空间上界为 O(V+E)。
     *
     * @param graph 待检测的图；调用方须保证整个计算期间不修改它
     * @return 不可修改的闭合节点序列，如 [A, B, C, A]；自环为 [A, A]；无环为空
     * @throws IllegalArgumentException graph 为 null
     */
    public List<String> findCycle(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待检测的图不能为 null");
        }
        if (!new TopologicalSolver().kahnSort(graph).hasCycle()) {
            return List.of();
        }

        Map<String, Color> colors = new HashMap<>();
        List<String> path = new ArrayList<>();
        ArrayDeque<Iterator<String>> stack = new ArrayDeque<>();
        for (String root : graph.getVertexNames()) {
            if (colors.containsKey(root)) {
                continue;
            }
            colors.put(root, Color.GRAY);
            path.add(root);
            stack.push(graph.getSuccessors(root).iterator());

            while (!stack.isEmpty()) {
                Iterator<String> successors = stack.peek();
                if (!successors.hasNext()) {
                    // 当前节点的所有后继已检查，完成回退；黑色节点不再属于当前路径。
                    stack.pop();
                    colors.put(path.remove(path.size() - 1), Color.BLACK);
                    continue;
                }

                String next = successors.next();
                Color color = colors.get(next);
                if (color == Color.GRAY) {
                    // 指回灰色祖先形成环，只截取环本身，不包含进入环前的路径。
                    int start = path.indexOf(next);
                    List<String> cycle = new ArrayList<>(path.subList(start, path.size()));
                    cycle.add(next);
                    return List.copyOf(cycle);
                }
                if (color == null) {
                    colors.put(next, Color.GRAY);
                    path.add(next);
                    stack.push(graph.getSuccessors(next).iterator());
                }
                // 指向黑色节点是已完成分支上的关系，不是当前路径的回边。
            }
        }

        throw new IllegalStateException("Kahn 判环与 DFS 路径定位结果不一致，请检查计算期间图是否被修改");
    }
}
