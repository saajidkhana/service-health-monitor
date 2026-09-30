package com.saajid.healthmonitor;

import java.time.Instant;

/** One parsed line from a service log. */
public record LogEntry(Instant timestamp, String service, Level level, long latencyMs) {

    public enum Level { INFO, WARN, ERROR }

    public boolean isError() {
        return level == Level.ERROR;
    }
}
