package com.roadwatch.admin.api;

import java.util.List;

/** Mirrors the JSON shapes of the server's REST API. Enums arrive as plain strings. */
public final class Models {
    private Models() { }

    public record AuthResponse(String token, Long userId, String username, String fullName,
                               String role, Long panchayatId, String panchayatName) { }

    public record Panchayat(Long id, String name, String type) {
        @Override public String toString() { return name; }
    }

    public record User(Long id, String username, String fullName, String role,
                       Long panchayatId, String panchayatName, boolean active, String createdAt) { }

    public record HistoryEntry(String oldStatus, String newStatus, String note, String changedBy, String at) { }

    public record Report(Long id, String description, String severity, String landmark,
                         double latitude, double longitude, Double accuracyMeters,
                         Long panchayatId, String panchayatName,
                         String reporterName, String reporterPhone,
                         String status, String createdAt, String updatedAt,
                         boolean editable, List<String> allowedNext, List<HistoryEntry> history) { }
}
