package main;

import main.model.Video;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 */
public class Util {
    public static String formatLocalDate(LocalDate date) {
        return date == null ? null : date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static LocalDate parseLocalDate(String s) {
        return s == null ? null : LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static String formatLocalDateTime(LocalDateTime date) {
        return date == null ? null : date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public static LocalDateTime parseLocalDateTime(String s) {
        return s == null ? null : LocalDateTime.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    /**
     * Copied from <a href="https://stackoverflow.com/a/3758880/9492864">How to convert byte size into human readable format in java?</a>.
     *
     * @param bytes bytes to format
     * @param si    if SI should be used (1000) or binary (1024) to format
     * @return human readable form for bytes with Prefix
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
