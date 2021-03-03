package org.thesis.dexprocessor.statistics;

public abstract class AbstractTimerStatistics {
    private long start_time = 0;
    public long run_duration = 0;

    public long start() {
        start_time = System.nanoTime();
        return start_time;
    }

    public long stop() {
        long stop_time = System.nanoTime();
        run_duration = stop_time - start_time;
        return stop_time;
    }
}
