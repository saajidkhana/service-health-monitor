# Service Health Monitor

A command-line Java tool that reads service logs and reports per-service error rates, latency percentiles, the worst-performing services, and error spikes. It is a small-scale version of the monitoring work that operations teams do to catch problems in production.

Built with plain Java (JDK 17+), with no external dependencies.

## Features
- **Log parsing:** Reads lines like `2026-10-01T12:00:05Z payments ERROR 230`. Malformed lines are skipped and counted instead of crashing the run.
- **Per-service stats:** Total requests, errors, warnings, error rate, and p50/p95 latency (nearest-rank percentile).
- **Top-N worst services:** Uses a min-heap of size N, so it runs in O(S log N) for S services instead of sorting everything.
- **Spike detection:** Flags any service with at least K errors inside a sliding time window, using one deque of error timestamps per service. After the O(n log n) sort, the scan is O(n) because each timestamp is added and removed at most once.

## Project structure
```
src/main/java/com/saajid/healthmonitor/
  LogEntry.java       immutable record for one log line
  LogParser.java      validation and parsing
  ServiceStats.java   per-service aggregation and percentiles
  HealthReport.java   groups by service, top-N by error rate
  SpikeDetector.java  sliding-window spike detection
  Main.java           CLI entry point
src/test/java/com/saajid/healthmonitor/
  TestRunner.java     dependency-free tests (18 checks)
data/sample_logs.txt  sample data with a planted error burst
```

## Build and run
```bash
# compile (from project root)
javac -d out $(find src -name "*.java")

# run tests
java -cp out com.saajid.healthmonitor.TestRunner

# run the tool: LOG_FILE [windowSeconds] [threshold]
java -cp out com.saajid.healthmonitor.Main data/sample_logs.txt 10 4
```

### Example output
```
Parsed 482 entries (2 malformed lines skipped)

SERVICE        TOTAL  ERRORS  ERR RATE   P50 ms   P95 ms
auth             123       4      3.3%       42       71
checkout         117       9      7.7%      195      637
payments         123       9      7.3%      162      440
search           119       3      2.5%      113      193

Top services by error rate:
  checkout     7.7%
  payments     7.3%
  auth         3.3%

Error spikes (>= 4 errors within 10s): 1
  payments at 2026-10-01T12:05:05Z (4 errors)
```

## Design decisions
- **Skip bad input instead of failing.** Real logs contain garbage, so the parser counts and reports malformed lines.
- **One burst is reported once.** After a spike fires, that service's window is cleared so later errors in the same burst don't re-alert.
- **Sorting before detection.** Logs can arrive out of order, so entries are sorted by timestamp first.
- **No test framework.** A small runner keeps the project buildable with only a JDK. Moving to JUnit 5 is a natural next step.

## Possible next steps
- Convert the tests to JUnit 5 and build with Maven or Gradle.
- Stream large files instead of loading all lines into memory.
- Add JSON or CSV output.
- Add a rolling latency-based alert (for example, p95 above a threshold).
