package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiException;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Labels, colours, date formatting and dialogs shared by the screens. */
public final class Ui {
    private Ui() { }

    /** A status code with its human-friendly label; used in combo boxes. */
    public record StatusItem(String code, String label) {
        @Override public String toString() { return label; }
    }

    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();
    static {
        STATUS_LABELS.put("REPORTED", "Reported");
        STATUS_LABELS.put("UNDER_REVIEW", "Under review");
        STATUS_LABELS.put("IN_PROGRESS", "In progress");
        STATUS_LABELS.put("RESOLVED", "Resolved");
        STATUS_LABELS.put("REJECTED", "Rejected");
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.of("Asia/Kolkata"));

    public static List<StatusItem> allStatuses() {
        List<StatusItem> list = new ArrayList<>();
        STATUS_LABELS.forEach((code, label) -> list.add(new StatusItem(code, label)));
        return list;
    }

    public static StatusItem statusItem(String code) {
        return new StatusItem(code, statusLabel(code));
    }

    public static String statusLabel(String code) {
        return STATUS_LABELS.getOrDefault(code, code);
    }

    public static Color statusColor(String code) {
        return switch (code == null ? "" : code) {
            case "REPORTED" -> new Color(0xB26A00);
            case "UNDER_REVIEW" -> new Color(0x1565C0);
            case "IN_PROGRESS" -> new Color(0x6A1B9A);
            case "RESOLVED" -> new Color(0x1B7F4B);
            case "REJECTED" -> new Color(0xB3261E);
            default -> Color.DARK_GRAY;
        };
    }

    public static String severityLabel(String code) {
        return switch (code == null ? "" : code) {
            case "LOW" -> "Minor";
            case "MEDIUM" -> "Moderate";
            case "HIGH" -> "Large";
            case "DANGEROUS" -> "Dangerous";
            default -> String.valueOf(code);
        };
    }

    public static Instant parseInstant(String iso) {
        try {
            return iso == null ? Instant.EPOCH : Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }

    public static String formatInstant(Instant t) {
        return t == null || t.equals(Instant.EPOCH) ? "" : TIME_FORMAT.format(t);
    }

    public static String formatTime(String iso) {
        return formatInstant(parseInstant(iso));
    }

    public static String messageOf(Exception e) {
        return e instanceof ApiException ? e.getMessage() : "Unexpected error: " + e.getMessage();
    }

    /** Shows an error dialog; an expired login sends the user back to the login screen instead. */
    public static void error(Component parent, Exception e) {
        if (e instanceof ApiException a && a.status() == 401 && Session.current() != null) {
            Session.current().expire();
            return;
        }
        JOptionPane.showMessageDialog(parent, messageOf(e), "Road Watch", JOptionPane.ERROR_MESSAGE);
    }
}
