package dev.emortal.minestom.core.utils;

import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.StringJoiner;

public final class DurationFormatter {

    public static @NotNull String ofGreatestUnit(@NotNull Duration duration) {
        if (duration.compareTo(TimeUnit.DAY.getDuration()) > -1) return duration.toDays() + "d";
        if (duration.compareTo(TimeUnit.HOUR.getDuration()) > -1) return duration.toHours() + "hr";
        if (duration.compareTo(TimeUnit.MINUTE.getDuration()) > -1) return duration.toMinutes() + "min";
        if (duration.compareTo(TimeUnit.SECOND.getDuration()) > -1) return duration.toSeconds() + "s";
        if (duration.compareTo(TimeUnit.MILLISECOND.getDuration()) > -1) return duration.toMillis() + "ms";
        return duration.toNanosPart() + "ns";
    }

    //https://stackoverflow.com/questions/13627308/add-st-nd-rd-and-th-ordinal-suffix-to-a-number
    public static String withOrdinalSuffix(int num) {
        int j = num % 10;
        int k = num % 100;
        if (j == 1 && k != 11) {
            return num + "st";
        }
        if (j == 2 && k != 12) {
            return num + "nd";
        }
        if (j == 3 && k != 13) {
            return num + "rd";
        }
        return num + "th";
    }

    public static @NotNull String formatDurationMMSS(@NotNull Duration duration) {
        long MM = duration.toMinutes();
        long SS = duration.toSecondsPart();
        return String.format("%02d:%02d", MM, SS);
    }

    public static @NotNull String formatDurationLong(@NotNull Duration duration) {
        StringJoiner joiner = new StringJoiner(", ");
        int unitCount = 0;

        int years = (int) duration.toDays() / 365;
        int months = (int) duration.toDays() / 30;
        int days = (int) duration.toDays() % 30;
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        if (years > 0) {
            joiner.add(years + " years");
            unitCount++;
        }

        if (months > 0) {
            joiner.add(months + " months");
            unitCount++;
        }

        if (days > 0) {
            joiner.add(days + " days");
            if (++unitCount == 3) return joiner.toString();
        }

        if (hours > 0) {
            joiner.add(hours + " hours");
            if (++unitCount == 3) return joiner.toString();
        }

        if (minutes > 0) {
            joiner.add(minutes + " minutes");
            if (++unitCount == 3) return joiner.toString();
        }

        if (seconds > 0) {
            joiner.add(seconds + " seconds");
        }

        return joiner.toString();
    }

    public static @NotNull String formatDuration(@NotNull Duration duration) {
        StringJoiner joiner = new StringJoiner(" ");
        int unitCount = 0;

        int years = (int) duration.toDays() / 365;
        int months = (int) duration.toDays() / 30;
        int days = (int) duration.toDays() % 30;
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        if (years > 0) {
            joiner.add(years + "y");
            unitCount++;
        }

        if (months > 0) {
            joiner.add(months + "m");
            unitCount++;
        }

        if (days > 0) {
            joiner.add(days + "d");
            if (++unitCount == 3) return joiner.toString();
        }

        if (hours > 0) {
            joiner.add(hours + "h");
            if (++unitCount == 3) return joiner.toString();
        }

        if (minutes > 0) {
            joiner.add(minutes + "m");
            if (++unitCount == 3) return joiner.toString();
        }

        if (seconds > 0) {
            joiner.add(seconds + "s");
        }

        return joiner.toString();
    }

    private DurationFormatter() {
        throw new AssertionError("This class cannot be instantiated.");
    }
}