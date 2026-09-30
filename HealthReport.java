package com.saajid.healthmonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/** Groups log entries by service and answers simple questions about them. */
public final class HealthReport {

    private static final Comparator<ServiceStats> WORST_FIRST =
            Comparator.comparingDouble(ServiceStats::errorRate)
                    .thenComparingInt(ServiceStats::errors)
                    .reversed();

    private final Map<String, ServiceStats> statsByService = new TreeMap<>();

    public HealthReport(List<LogEntry> entries) {
        for (LogEntry entry : entries) {
            statsByService
                    .computeIfAbsent(entry.service(), ServiceStats::new)
                    .add(entry);
        }
    }

    public Collection<ServiceStats> all() {
        return statsByService.values();
    }

    /**
     * Returns the n services with the highest error rate (ties broken by error
     * count), worst first. Keeps a min-heap of size n, so this is O(S log n)
     * for S services rather than sorting all of them.
     */
    public List<ServiceStats> topByErrorRate(int n) {
        if (n <= 0) {
            return List.of();
        }
        Comparator<ServiceStats> weakestFirst = WORST_FIRST.reversed();
        PriorityQueue<ServiceStats> heap = new PriorityQueue<>(weakestFirst); // min-heap: weakest at top
        for (ServiceStats stats : statsByService.values()) {
            heap.offer(stats);
            if (heap.size() > n) {
                heap.poll(); // drop the weakest of the current top n+1
            }
        }
        List<ServiceStats> result = new ArrayList<>(heap);
        result.sort(WORST_FIRST);
        return result;
    }
}
