package dev.emortal.minestom.core.command.performance;

import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;
import dev.emortal.minestom.core.utils.DurationFormatter;
import dev.emortal.minestom.core.utils.ProgressBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PerformanceCommand extends Command {
    private static final long SECONDS_IN_NANO = 1_000_000_000L;
    private static final int TPS = ServerFlag.SERVER_TICKS_PER_SECOND;
    private static final BigDecimal TPS_BASE = BigDecimal.valueOf(SECONDS_IN_NANO).multiply(BigDecimal.valueOf(TPS));

    private static final Component VALUE_SEPARATOR = Component.text(", ");
    private static final Set<Pattern> EXCLUDED_MEMORY_SPACES = Stream.of("Metaspace", "Compressed Class Space", "^CodeHeap")
            .map(Pattern::compile)
            .collect(Collectors.toUnmodifiableSet());

    private final TpsRollingAverage tps5s = new TpsRollingAverage(5);
    private final TpsRollingAverage tps15s = new TpsRollingAverage(15);
    private final TpsRollingAverage tps1m = new TpsRollingAverage(60);
    private final TpsRollingAverage tps5m = new TpsRollingAverage(60 * 5);
    private final TpsRollingAverage tps15m = new TpsRollingAverage(60 * 15);
    private final TpsRollingAverage[] tpsAverages = {this.tps5s, this.tps15s, this.tps1m, this.tps5m, this.tps15m};

    private final RollingAverage mspt5s = new RollingAverage(TPS * 5);
    private final RollingAverage mspt15s = new RollingAverage(TPS * 15);
    private final RollingAverage mspt1m = new RollingAverage(TPS * 60);
    private final RollingAverage mspt5m = new RollingAverage(TPS * 60 * 5);
    private final RollingAverage mspt15m = new RollingAverage(TPS * 60 * 15);
    private final RollingAverage[] msptAverages = {this.mspt5s, this.mspt15s, this.mspt1m, this.mspt5m, this.mspt15m};

    private long lastTickTime;

    public PerformanceCommand(@NotNull EventNode<Event> eventNode) {
        super("performance");
        eventNode.addListener(ServerTickMonitorEvent.class, event -> this.onTick(event.getTickMonitor().getTickTime()));
        this.addSyntax(this::onExecute);
    }

    private void onTick(double tickTime) {
        if (this.lastTickTime == 0) {
            this.lastTickTime = System.nanoTime();
            return;
        }

        long now = System.nanoTime();
        long difference = now - this.lastTickTime;
        if (difference <= SECONDS_IN_NANO) return;

        BigDecimal currentTps = TPS_BASE.divide(BigDecimal.valueOf(difference), 30, RoundingMode.HALF_UP);
        BigDecimal total = currentTps.multiply(BigDecimal.valueOf(difference));

        for (TpsRollingAverage average : this.tpsAverages) {
            average.addSample(currentTps, difference, total);
        }
        this.lastTickTime = now;

        BigDecimal duration = BigDecimal.valueOf(tickTime);
        for (RollingAverage average : this.msptAverages) {
            average.addSample(duration);
        }
    }

    private void onExecute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long ramUsage = totalMem - freeMem;
        float ramPercent = (float) ramUsage / (float) totalMem;

        sender.sendMessage(Component.text()
                .append(Component.newline())

                // RAM usage information
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(ProgressBar.create(ramPercent, 30, "┃", NamedTextColor.GREEN, TextColor.color(0, 123, 0)))
                .append(Component.text(String.format(" %sMB / %sMB", ramUsage, totalMem), NamedTextColor.GRAY))
                .append(Component.newline())

                // GC Young/Old generation collection information
                .append(Component.newline())
                .append(this.createGcComponent())
                .append(Component.newline())

                // TPS averages
                .append(Component.text("\nTPS (5s, 15s, 1m, 5m, 15m): ", NamedTextColor.GRAY))
                .append(this.getTpsInfo(this.tpsAverages))
                .append(Component.newline())

                // MSPT averages
                .append(Component.text("MSPT (5s, 15s, 1m, 5m, 15m): ", NamedTextColor.GRAY))
                .append(this.getMsptInfo(this.msptAverages)));
    }

    private @NotNull Component getTpsInfo(@NotNull TpsRollingAverage[] averages) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < averages.length; i++) {
            double average = averages[i].average();
            TextColor color = this.calculateTpsColor(average);

            builder.append(Component.text(String.format("%.2f", average), color));
            if (i < averages.length - 1) {
                builder.append(VALUE_SEPARATOR);
            }
        }

        return builder.build();
    }

    private @NotNull Component getMsptInfo(@NotNull RollingAverage[] averages) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < averages.length; i++) {
            double average = averages[i].mean();
            TextColor color = this.calculateMsptColor(average);

            builder.append(Component.text(String.format("%.2fms", average), color));
            if (i < averages.length - 1) {
                builder.append(VALUE_SEPARATOR);
            }
        }

        return builder.build();
    }

    private @NotNull TextColor calculateTpsColor(double amount) {
        float lerpDiv = (float) (amount / TPS);
        return TextColor.lerp(lerpDiv, NamedTextColor.RED, NamedTextColor.GREEN);
    }

    private @NotNull TextColor calculateMsptColor(double average) {
        float lerpDiv = (float) (average / MinecraftServer.TICK_MS);
        return TextColor.lerp(lerpDiv, NamedTextColor.GREEN, NamedTextColor.RED);
    }

    private @NotNull Component createGcComponent() {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("GC Info:", NamedTextColor.GRAY));

        for (Map.Entry<String, GcInfo> entry : this.getGcInfo().entrySet()) {
            TextComponent.Builder entryBuilder = Component.text();

            String lastRunText;
            if (entry.getValue() == null) {
                lastRunText = "never";
            } else {
                long millisSinceRun = this.getUptime() - entry.getValue().getEndTime();
                lastRunText = entry.getValue() == null ? "never" : DurationFormatter.ofGreatestUnit(Duration.ofMillis(millisSinceRun)) + " ago";
            }

            entryBuilder.append(Component.text("\n  " + entry.getKey() + ":", NamedTextColor.GRAY))
                    .append(Component.text("\n    Last Run: ", NamedTextColor.GRAY))
                    .append(Component.text(lastRunText, NamedTextColor.GOLD));

            if (entry.getValue() != null) {
                entryBuilder.hoverEvent(HoverEvent.showText(this.createGcHover(entry.getKey(), entry.getValue())));
            }

            builder.append(entryBuilder);
        }

        return builder.asComponent();
    }

    private @NotNull Component createGcHover(@NotNull String name, @NotNull GcInfo info) {
        return Component.text()
                .append(Component.text("Name: ", NamedTextColor.GOLD))
                .append(Component.text(name, NamedTextColor.GRAY))
                .append(Component.newline())

                .append(Component.text("Duration: ", NamedTextColor.GOLD))
                .append(Component.text(info.getDuration() + "ms", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text("Memory After:", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(this.createMemoryUsagePeriod(info.getMemoryUsageAfterGc()))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text("Memory Before:", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(this.createMemoryUsagePeriod(info.getMemoryUsageBeforeGc()))

                .build();
    }

    private @NotNull Component createMemoryUsagePeriod(@NotNull Map<String, MemoryUsage> memoryUsageMap) {
        List<Component> lines = new ArrayList<>();

        for (Map.Entry<String, MemoryUsage> entry : memoryUsageMap.entrySet()) {
            if (EXCLUDED_MEMORY_SPACES.stream().anyMatch(pattern -> pattern.matcher(entry.getKey()).find())) {
                continue;
            }

            Component text = Component.text()
                    .append(Component.text("  " + entry.getKey() + ": ", NamedTextColor.GOLD))
                    .append(Component.text(entry.getValue().getUsed() / 1024 / 1024 + "MB", NamedTextColor.GRAY))
                    .build();
            lines.add(text);
        }

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private @NotNull Map<String, GcInfo> getGcInfo() {
        Map<String, GcInfo> gcInfo = new HashMap<>();

        for (java.lang.management.GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            // We need to downcast to the com.sun.management (internal) one because we need the last GC info
            // It is unfortunate that the java.lang.management (API) one isn't very useful in what it actually provides
            GarbageCollectorMXBean bean = (GarbageCollectorMXBean) garbageCollectorMXBean;

            gcInfo.put(bean.getName(), bean.getLastGcInfo());
        }
        return gcInfo;
    }

    private long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }
}