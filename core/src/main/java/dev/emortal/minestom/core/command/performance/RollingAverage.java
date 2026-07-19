package dev.emortal.minestom.core.command.performance;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public final class RollingAverage {

    private final @NotNull Queue<BigDecimal> samples;
    private final int size;

    private @NotNull BigDecimal sum = BigDecimal.ZERO;

    public RollingAverage(int size) {
        this.samples = new ArrayDeque<>(size + 1);
        this.size = size;
    }

    public int sampleCount() {
        synchronized (this) {
            return this.samples.size();
        }
    }

    public void addSample(@NotNull BigDecimal sample) {
        synchronized (this) {
            this.sum = this.sum.add(sample);
            this.samples.add(sample);
            if (this.samples.size() > this.size) {
                // Remove old samples from the queue and sum so we only keep max. size samples
                this.sum = this.sum.subtract(this.samples.remove());
            }
        }
    }

    public double mean() {
        int sampleCount;
        synchronized (this) {
            if (this.samples.isEmpty()) return 0;
            sampleCount = this.samples.size();
        }

        // mean = sum / count
        return this.sum.divide(BigDecimal.valueOf(sampleCount), 30, RoundingMode.HALF_UP).doubleValue();
    }

    public double min() {
        BigDecimal min = null;
        synchronized (this) {
            for (BigDecimal sample : this.samples) {
                if (min == null || sample.compareTo(min) < 0) min = sample;
            }
        }
        return min == null ? 0 : min.doubleValue();
    }

    public double max() {
        BigDecimal max = null;
        synchronized (this) {
            for (BigDecimal sample : this.samples) {
                if (max == null || sample.compareTo(max) > 0) max = sample;
            }
        }
        return max == null ? 0 : max.doubleValue();
    }

    /**
     * Returns the value at the given percentile.
     *
     * @param percentile The percentile to get the value for, between 0 and 1.
     * @return The value at the given percentile.
     */
    public double percentile(double percentile) {
        if (percentile < 0 || percentile > 1) throw new IllegalArgumentException("Percentile must be between 0 and 1!");

        BigDecimal[] sortedSamples;
        synchronized (this) {
            if (this.samples.isEmpty()) return 0;
            sortedSamples = this.samples.toArray(new BigDecimal[0]);
        }
        Arrays.sort(sortedSamples);

        int rank = (int) Math.ceil(percentile * (sortedSamples.length - 1));
        return sortedSamples[rank].doubleValue();
    }
}
