package Services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateConverter {
    public static String convertISOToFormattedDate(String isoDateString, String outputPattern) {
        try {
            // Parse the ISO 8601 string to Instant
            Instant instant = Instant.parse(isoDateString);

            // Convert to LocalDateTime
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            // Format using the provided pattern
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(outputPattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + e.getMessage());
        }
    }
    public static LocalDateTime getLocalDateTime(String isoDateString) {
        Instant instant = Instant.parse(isoDateString);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public String DateInDays(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - timestamp;

        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;

        StringBuilder timeAFK = new StringBuilder();
        if (days > 0) timeAFK.append(days).append(" days ");
        if (hours > 0) timeAFK.append(hours).append(" hours ");
        if (minutes > 0) timeAFK.append(minutes).append(" minutes");

        return timeAFK.toString().trim();
    }

    // Format as "November 8, 2024"
    public String formatDateOnly(String dateString) {
        Instant instant = Instant.parse(dateString);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        return zonedDateTime.format(formatter);
    }

    // Format as "4:37 AM"
    public String formatTimeOnly(String dateString) {
        Instant instant = Instant.parse(dateString);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        return zonedDateTime.format(formatter);
    }

    // Format as "11/08/2024"
    public String formatShortDate(String dateString) {
        Instant instant = Instant.parse(dateString);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
        return zonedDateTime.format(formatter);
    }

    // Format as "11/08/2024 04:37 AM"
    public String formatShortDateTime(String dateString) {
        Instant instant = Instant.parse(dateString);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.ENGLISH);
        return zonedDateTime.format(formatter);
    }
}
