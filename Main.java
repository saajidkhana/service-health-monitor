package com.saajid.healthmonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Usage: java -cp out com.saajid.healthmonitor.Main LOG_FILE [windowSeconds] [threshold]
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: Main LOG_FILE [windowSeconds=60] [threshold=5]");
            System.exit(2);
        }
        Path file = Path.of(args[0]);
        long windowSeconds = args.length > 1 ? Long.parseLong(args[1]) : 60;
        int threshold = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        List<String> lines = Files.readAllLines(file);
        LogParser.ParseResult parsed = LogParser.parseAll(lines);
        HealthReport report = new HealthReport(parsed.entries());

        System.out.printf("Parsed %d entries (%d malformed lines skipped)%n%n",
                parsed.entries().size(), parsed.malformedCount());

        System.out.printf("%-12s %7s %7s %9s %8s %8s%n",
                "SERVICE", "TOTAL", "ERRORS", "ERR RATE", "P50 ms", "P95 ms");
        for (ServiceStats s : report.all()) {
            System.out.printf("%-12s %7d %7d %8.1f%% %8d %8d%n",
                    s.service(), s.total(), s.errors(), s.errorRate() * 100,
                    s.percentile(50), s.percentile(95));
        }

        System.out.println("\nTop services by error rate:");
        for (ServiceStats s : report.topByErrorRate(3)) {
            System.out.printf("  %-12s %.1f%%%n", s.service(), s.errorRate() * 100);
        }

        SpikeDetector detector = new SpikeDetector(Duration.ofSeconds(windowSeconds), threshold);
        List<SpikeDetector.Spike> spikes = detector.detect(parsed.entries());
        System.out.printf("%nError spikes (>= %d errors within %ds): %d%n",
                threshold, windowSeconds, spikes.size());
        for (SpikeDetector.Spike spike : spikes) {
            System.out.printf("  %s at %s (%d errors)%n",
                    spike.service(), spike.detectedAt(), spike.errorCount());
        }
    }
}
