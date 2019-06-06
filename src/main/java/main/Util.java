package main;

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
}
