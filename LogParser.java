package com.saajid.healthmonitor;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses log lines of the form:
 * <pre>2026-10-01T12:00:05Z payments ERROR 230</pre>
 * (ISO-8601 timestamp, service name, level, latency in milliseconds).
 * Malformed lines are skipped and counted rather than crashing the run.
 */
public final class LogParser {

    public record ParseResult(List<LogEntry> entries, int malformedCount) {}

    private LogParser() {}

    public static Optional<LogEntry> parseLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            Instant timestamp = Instant.parse(parts[0]);
            LogEntry.Level level = LogEntry.Level.valueOf(parts[2].toUpperCase());
            long latencyMs = Long.parseLong(parts[3]);
            if (latencyMs < 0) {
                return Optional.empty();
            }
            return Optional.of(new LogEntry(timestamp, parts[1], level, latencyMs));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static ParseResult parseAll(List<String> lines) {
        List<LogEntry> entries = new ArrayList<>();
        int malformed = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue; // blank lines are not errors
            }
            Optional<LogEntry> entry = parseLine(line);
            if (entry.isPresent()) {
                entries.add(entry.get());
            } else {
                malformed++;
            }
        }
        return new ParseResult(entries, malformed);
    }
}
