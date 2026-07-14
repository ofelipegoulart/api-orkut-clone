package com.orkutclone.api.support;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Formats an instant as a human-friendly label used in the topic message list:
 * up to 5 elapsed days it shows a relative label ("hoje", "1 dia atrás", "N dias atrás");
 * beyond that it falls back to an absolute date ("14 jul 2026").
 *
 * <p>Elapsed days are whole 24h periods (via {@link ChronoUnit#DAYS}); the absolute date is
 * rendered in UTC (the timezone the app persists timestamps in), pt-BR locale.
 */
public final class RelativeTimeFormatter {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter ABSOLUTE_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", PT_BR);
    private static final long RELATIVE_THRESHOLD_DAYS = 5;

    private RelativeTimeFormatter() {}

    public static String format(Instant instant) {
        return format(instant, Instant.now());
    }

    public static String format(Instant instant, Instant now) {
        if (instant == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(instant, now);
        if (days <= 0) {
            return "hoje";
        }
        if (days == 1) {
            return "1 dia atrás";
        }
        if (days <= RELATIVE_THRESHOLD_DAYS) {
            return days + " dias atrás";
        }
        return ABSOLUTE_DATE.format(instant.atOffset(ZoneOffset.UTC));
    }
}
