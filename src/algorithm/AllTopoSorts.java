package algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import model.Graph;

/**
 * 以局部入度和显式回溯枚举拓扑序列，不修改输入图，不使用递归调用栈。
 * 每次调用的搜索状态独立，返回顺序按图的节点录入顺序确定。
 *
 * @author A
 */
public final class AllTopoSorts {
    private final LongSupplier nanoTime;

    /** 使用单调时钟计时。 */
    public AllTopoSorts() {
        this(System::nanoTime);
    }

    // 仅供同包自测注入时钟，确保超时测试不依赖机器速度或等待。
    AllTopoSorts(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * 复制图后先执行可检查取消、超时的 Kahn 判环，再枚举无环图的合法序列。
     * 输入需在复制期间保持稳定，按团队约定调用方应在整个计算期间禁止修改。
     * 数量上限由调用方默认传入 1000；达到上限即保守标记未完整。
     * 取消与超时均保留此前生成的完整序列，不保存半条序列。
     *
     * @param graph 待枚举的图
     * @param maxResults 数量上限，0 表示不限，负数非法
     * @param timeoutMillis 包含复制和判环的运行时限，单位毫秒；0 表示不限
     * @param cancelled 取消检查函数，应快速返回且不修改图；异常原样向上传递
     * @return 含数量、完整性及停止原因的结果；空图正常完成时有一条空序列
     * @throws IllegalArgumentException 图或取消函数为 null，或任一限制为负数
     */
    public EnumerationResult enumerate(Graph graph, int maxResults, long timeoutMillis,
            BooleanSupplier cancelled) {
        if (graph == null || cancelled == null || maxResults < 0 || timeoutMillis < 0) {
            throw new IllegalArgumentException("图和取消函数不能为 null，数量及时间限制不能为负数");
        }
        long start = nanoTime.getAsLong();
        // TimeUnit 的饱和转换避免极大的毫秒参数乘法溢出。
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        List<List<String>> results = new ArrayList<>();
        StopReason stopped = interruption(cancelled, start, timeoutNanos);
        if (stopped != null) {
            return new EnumerationResult(results, stopped);
        }

        List<String> names = graph.getVertexNames();
        int size = names.size();
        Map<String, Integer> indices = new HashMap<>();
        int[] inDegrees = new int[size];
        int[][] successors = new int[size][];
        for (int i = 0; i < size; i++) {
            stopped = interruption(cancelled, start, timeoutNanos);
            if (stopped != null) {
                return new EnumerationResult(results, stopped);
            }
            indices.put(names.get(i), i);
            inDegrees[i] = graph.getInDegree(names.get(i));
        }
        for (int i = 0; i < size; i++) {
            stopped = interruption(cancelled, start, timeoutNanos);
            if (stopped != null) {
                return new EnumerationResult(results, stopped);
            }
            List<String> adjacent = graph.getSuccessors(names.get(i));
            successors[i] = new int[adjacent.size()];
            for (int j = 0; j < adjacent.size(); j++) {
                stopped = interruption(cancelled, start, timeoutNanos);
                if (stopped != null) {
                    return new EnumerationResult(results, stopped);
                }
                successors[i][j] = indices.get(adjacent.get(j));
            }
        }

        // Kahn 使用第二份入度，不改变后续回溯的初始入度；此阶段尚不生成序列。
        int[] checkDegrees = inDegrees.clone();
        ArrayDeque<Integer> ready = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            stopped = interruption(cancelled, start, timeoutNanos);
            if (stopped != null) {
                return new EnumerationResult(results, stopped);
            }
            if (checkDegrees[i] == 0) {
                ready.addLast(i);
            }
        }
        int processed = 0;
        while (!ready.isEmpty()) {
            stopped = interruption(cancelled, start, timeoutNanos);
            if (stopped != null) {
                return new EnumerationResult(results, stopped);
            }
            int vertex = ready.removeFirst();
            processed++;
            for (int next : successors[vertex]) {
                stopped = interruption(cancelled, start, timeoutNanos);
                if (stopped != null) {
                    return new EnumerationResult(results, stopped);
                }
                if (--checkDegrees[next] == 0) {
                    ready.addLast(next);
                }
            }
        }
        stopped = interruption(cancelled, start, timeoutNanos);
        if (stopped != null) {
            return new EnumerationResult(results, stopped);
        }
        if (processed != size) {
            return new EnumerationResult(results, StopReason.CYCLE);
        }
        if (size == 0) {
            return new EnumerationResult(List.of(List.of()), StopReason.COMPLETED);
        }

        boolean[] used = new boolean[size];
        int[] order = new int[size];
        // 每一层记住下一个待尝试的节点下标，回退后继续尝试该层的其他选择。
        int[] nextCandidate = new int[size + 1];
        int depth = 0;
        while (depth >= 0) {
            stopped = interruption(cancelled, start, timeoutNanos);
            if (stopped != null) {
                return new EnumerationResult(results, stopped);
            }
            if (depth == size) {
                List<String> sequence = new ArrayList<>(size);
                for (int vertex : order) {
                    stopped = interruption(cancelled, start, timeoutNanos);
                    if (stopped != null) {
                        return new EnumerationResult(results, stopped);
                    }
                    sequence.add(names.get(vertex));
                }
                results.add(List.copyOf(sequence));
                if (maxResults > 0 && results.size() >= maxResults) {
                    return new EnumerationResult(results, StopReason.LIMIT_REACHED);
                }
                depth--;
                undo(order[depth], used, inDegrees, successors);
                continue;
            }

            int candidate = -1;
            while (nextCandidate[depth] < size) {
                stopped = interruption(cancelled, start, timeoutNanos);
                if (stopped != null) {
                    return new EnumerationResult(results, stopped);
                }
                int vertex = nextCandidate[depth]++;
                if (!used[vertex] && inDegrees[vertex] == 0) {
                    candidate = vertex;
                    break;
                }
            }
            if (candidate < 0) {
                depth--;
                if (depth >= 0) {
                    undo(order[depth], used, inDegrees, successors);
                }
            } else {
                used[candidate] = true;
                order[depth] = candidate;
                for (int next : successors[candidate]) {
                    inDegrees[next]--;
                }
                depth++;
                nextCandidate[depth] = 0;
            }
        }
        return new EnumerationResult(results, StopReason.COMPLETED);
    }

    private static void undo(int vertex, boolean[] used, int[] inDegrees, int[][] successors) {
        used[vertex] = false;
        for (int next : successors[vertex]) {
            inDegrees[next]++;
        }
    }

    private StopReason interruption(BooleanSupplier cancelled, long start, long timeoutNanos) {
        if (cancelled.getAsBoolean()) {
            return StopReason.CANCELLED;
        }
        if (timeoutNanos > 0 && nanoTime.getAsLong() - start >= timeoutNanos) {
            return StopReason.TIMEOUT;
        }
        return null;
    }
}
