package com.saajid.healthmonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Running totals and latency percentiles for a single service. */
public final class ServiceStats {

    private final String service;
    private final List<Long> latencies = new ArrayList<>();
    private int total;
    private int errors;
    private int warnings;

    public ServiceStats(String service) {
        this.service = service;
    }

    public void add(LogEntry entry) {
        total++;
        latencies.add(entry.latencyMs());
        if (entry.isError()) {
            errors++;
        } else if (entry.level() == LogEntry.Level.WARN) {
            warnings++;
        }
    }

    public String service() { return service; }
    public int total() { return total; }
    public int errors() { return errors; }
    public int warnings() { return warnings; }

    public double errorRate() {
        return total == 0 ? 0.0 : (double) errors / total;
    }

    /**
     * Nearest-rank percentile: the smallest value such that at least p% of
     * observations are less than or equal to it. Returns 0 if there is no data.
     */
    public long percentile(double p) {
        if (latencies.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p * sorted.size() / 100.0);
        int index = Math.min(Math.max(rank, 1), sorted.size()) - 1;
        return sorted.get(index);
    }
}
