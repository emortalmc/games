package dev.emortal.minestom.core.command.performance;

import net.minestom.server.ServerFlag;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This code is taken from Paper, licensed under the MIT license.
 *
 * <p>
 * Original source: https://github.com/PaperMC/Paper/blob/master/patches/server/0031-Further-improve-server-tick-loop.patch
 * </p>
 */
public final class TpsRollingAverage {
    private static final long SECONDS_IN_NANO = 1_000_000_000L;
    private static final BigDecimal TPS = BigDecimal.valueOf(ServerFlag.SERVER_TICKS_PER_SECOND);

    private final int size;
    private long time;
    private @NotNull BigDecimal total;
    private int index;
    private final @NotNull BigDecimal[] samples;
    private final long[] times;

    public TpsRollingAverage(int size) {
        this.size = size;
        this.time = size * SECONDS_IN_NANO;
        this.total = TPS.multiply(BigDecimal.valueOf(SECONDS_IN_NANO)).multiply(BigDecimal.valueOf(size));

        this.samples = new BigDecimal[size];
        this.times = new long[size];

        for (int i = 0; i < size; i++) {
            this.samples[i] = TPS;
            this.times[i] = SECONDS_IN_NANO;
        }
    }

    public void addSample(@NotNull BigDecimal sample, long time, @NotNull BigDecimal total) {
        this.time -= this.times[this.index];
        this.total = this.total.subtract(this.samples[this.index].multiply(BigDecimal.valueOf(this.times[this.index])));

        this.samples[this.index] = sample;
        this.times[this.index] = time;

        this.time += time;
        this.total = this.total.add(total);

        if (++this.index == this.size) {
            this.index = 0;
        }
    }

    public double average() {
        return this.total.divide(BigDecimal.valueOf(this.time), 30, RoundingMode.HALF_UP).doubleValue();
    }
}
