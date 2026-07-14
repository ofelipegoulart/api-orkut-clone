package com.orkutclone.api.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formats the "last post" column of a community list: a post made today shows only its
 * time ("14:32"), anything older shows its date ("09/07/2026").
 *
 * <p>Rendered in UTC, the timezone the app persists timestamps in.</p>
 */
public final class LastPostFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LastPostFormatter() {}

    public static String format(Instant instant, Instant now) {
        if (instant == null) {
            return null;
        }
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate today = now.atOffset(ZoneOffset.UTC).toLocalDate();
        DateTimeFormatter formatter = day.equals(today) ? TIME : DATE;
        return formatter.format(instant.atOffset(ZoneOffset.UTC));
    }
}
