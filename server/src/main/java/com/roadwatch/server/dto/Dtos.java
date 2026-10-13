package com.roadwatch.server.dto;

import com.roadwatch.server.model.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/** Request/response shapes of the REST API. The Swing app mirrors these. */
public final class Dtos {
    private Dtos() { }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username may contain letters, digits, . _ - only") String username,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Long panchayatId) { }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }

    public record AuthResponse(String token, Long userId, String username, String fullName,
                               Role role, Long panchayatId, String panchayatName) {
        public static AuthResponse of(String token, User u) {
            Panchayat p = u.getPanchayat();
            return new AuthResponse(token, u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                    p == null ? null : p.getId(), p == null ? null : p.getName());
        }
    }

    public record PanchayatDto(Long id, String name, PanchayatType type) {
        public static PanchayatDto of(Panchayat p) { return new PanchayatDto(p.getId(), p.getName(), p.getType()); }
    }

    public record CreatePanchayatRequest(@NotBlank @Size(max = 100) String name, @NotNull PanchayatType type) { }

    public record UserDto(Long id, String username, String fullName, Role role,
                          Long panchayatId, String panchayatName, boolean active, Instant createdAt) {
        public static UserDto of(User u) {
            Panchayat p = u.getPanchayat();
            return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                    p == null ? null : p.getId(), p == null ? null : p.getName(), u.isActive(), u.getCreatedAt());
        }
    }

    public record StatusChangeRequest(@NotNull ReportStatus status, @Size(max = 500) String note) { }

    public record HistoryEntry(ReportStatus oldStatus, ReportStatus newStatus, String note,
                               String changedBy, Instant at) { }

    public record ReportDto(Long id, String description, Severity severity, String landmark,
                            double latitude, double longitude, Double accuracyMeters,
                            Long panchayatId, String panchayatName,
                            String reporterName, String reporterPhone,
                            ReportStatus status, Instant createdAt, Instant updatedAt,
                            boolean editable, List<ReportStatus> allowedNext,
                            List<HistoryEntry> history) { }

    public record TrackResponse(Long id, ReportStatus status, String panchayatName, Instant updatedAt) { }
}
