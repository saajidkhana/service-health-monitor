package com.saajid.healthmonitor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flags error spikes: a service that logs at least {@code threshold} errors
 * within any {@code window} of time.
 *
 * Uses one sliding window (a deque of error timestamps) per service, so the
 * work is O(n log n) for the initial sort plus O(n) for the scan, since each
 * timestamp is added and removed at most once.
 */
public final class SpikeDetector {

    public record Spike(String service, Instant detectedAt, int errorCount) {}

    private final Duration window;
    private final int threshold;

    public SpikeDetector(Duration window, int threshold) {
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be at least 1");
        }
        this.window = window;
        this.threshold = threshold;
    }

    public List<Spike> detect(List<LogEntry> entries) {
        List<LogEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(LogEntry::timestamp));

        Map<String, Deque<Instant>> windows = new HashMap<>();
        List<Spike> spikes = new ArrayList<>();

        for (LogEntry entry : sorted) {
            if (!entry.isError()) {
                continue;
            }
            Deque<Instant> errorTimes =
                    windows.computeIfAbsent(entry.service(), k -> new ArrayDeque<>());
            errorTimes.addLast(entry.timestamp());

            Instant cutoff = entry.timestamp().minus(window);
            while (!errorTimes.isEmpty() && errorTimes.peekFirst().isBefore(cutoff)) {
                errorTimes.removeFirst();
            }

            if (errorTimes.size() >= threshold) {
                spikes.add(new Spike(entry.service(), entry.timestamp(), errorTimes.size()));
                // Clear so one burst is reported once, not on every later error.
                errorTimes.clear();
            }
        }
        return spikes;
    }
}
