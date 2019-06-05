package main;

import java.time.LocalDate;
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
}
