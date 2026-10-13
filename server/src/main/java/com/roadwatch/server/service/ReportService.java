package com.roadwatch.server.service;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.model.*;
import com.roadwatch.server.repository.*;
import com.roadwatch.server.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PHOTO_BYTES = 6L * 1024 * 1024;

    // Rough bounding box around Kozhikode district
    private static final double MIN_LAT = 10.9, MAX_LAT = 12.0, MIN_LNG = 75.3, MAX_LNG = 76.3;

    private final ReportRepository reports;
    private final ReportPhotoRepository photos;
    private final PanchayatRepository panchayats;
    private final StatusUpdateRepository history;
    private final boolean geofence;

    public ReportService(ReportRepository reports, ReportPhotoRepository photos, PanchayatRepository panchayats,
                         StatusUpdateRepository history,
                         @Value("${roadwatch.geofence.enabled}") boolean geofence) {
        this.reports = reports;
        this.photos = photos;
        this.panchayats = panchayats;
        this.history = history;
        this.geofence = geofence;
    }

    // ---------------------------------------------------------------- citizens

    @Transactional
    public Report submit(String description, Severity severity, String landmark, double latitude, double longitude,
                         Double accuracy, Long panchayatId, String reporterName, String reporterPhone,
                         MultipartFile photo) throws IOException {

        if (description == null || description.trim().length() < 5 || description.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Description must be 5 to 1000 characters");
        }
        if (Double.isNaN(latitude) || Double.isNaN(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid location");
        }
        if (geofence && (latitude < MIN_LAT || latitude > MAX_LAT || longitude < MIN_LNG || longitude > MAX_LNG)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This location is outside Kozhikode district");
        }
        if (photo == null || photo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A photo of the pothole is required");
        }
        if (photo.getSize() > MAX_PHOTO_BYTES || !IMAGE_TYPES.contains(photo.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG, PNG or WebP under 6 MB");
        }
        byte[] bytes = photo.getBytes();
        if (!looksLikeImage(bytes)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image");
        }
        Panchayat panchayat = panchayats.findById(panchayatId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Please choose a panchayat"));

        Report report = reports.save(new Report(
                description.trim(), severity, clean(landmark, 200), latitude, longitude, accuracy,
                panchayat, clean(reporterName, 100), clean(reporterPhone, 20)));
        photos.save(new ReportPhoto(report, photo.getContentType(), bytes));
        return report;
    }

    @Transactional(readOnly = true)
    public TrackResponse track(Long id) {
        Report r = find(id);
        return new TrackResponse(r.getId(), r.getStatus(), r.getPanchayat().getName(), r.getUpdatedAt());
    }

    // ---------------------------------------------------------------- admins

    @Transactional(readOnly = true)
    public List<ReportDto> list(User viewer, Long panchayatId, ReportStatus status) {
        Specification<Report> spec = (root, q, cb) -> cb.conjunction();
        if (panchayatId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("panchayat").get("id"), panchayatId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        return reports.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(r -> toDto(viewer, r, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDto get(User viewer, Long id) {
        Report r = find(id);
        List<HistoryEntry> entries = history.findByReportIdOrderByCreatedAtAsc(id).stream()
                .map(h -> new HistoryEntry(h.getOldStatus(), h.getNewStatus(), h.getNote(),
                        h.getAdmin().getFullName(), h.getCreatedAt()))
                .toList();
        return toDto(viewer, r, entries);
    }

    @Transactional(readOnly = true)
    public ReportPhoto photo(Long reportId) {
        return photos.findByReportId(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No photo for this report"));
    }

    /** THE panchayat rule lives here (via User.canModify), on the server, not in the Swing app. */
    @Transactional
    public ReportDto changeStatus(User actor, Long id, ReportStatus next, String note) {
        Report r = find(id);
        if (!actor.canModify(r)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only update reports in your own panchayat");
        }
        ReportStatus old = r.getStatus();
        if (!old.canMoveTo(next)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A report cannot move from " + old + " to " + next);
        }
        r.changeStatus(next);
        reports.save(r);
        history.save(new StatusUpdate(r, actor, old, next, clean(note, 500)));
        return get(actor, id);
    }

    // ---------------------------------------------------------------- helpers

    private Report find(Long id) {
        return reports.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private ReportDto toDto(User viewer, Report r, List<HistoryEntry> entries) {
        boolean editable = viewer.canModify(r);
        List<ReportStatus> next = editable ? new ArrayList<>(r.getStatus().allowedNext()) : List.of();
        Panchayat p = r.getPanchayat();
        return new ReportDto(r.getId(), r.getDescription(), r.getSeverity(), r.getLandmark(),
                r.getLatitude(), r.getLongitude(), r.getAccuracyMeters(),
                p.getId(), p.getName(), r.getReporterName(), r.getReporterPhone(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt(), editable, next, entries);
    }

    private static String clean(String s, int max) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Checks the magic bytes so a renamed .exe cannot pass as a photo. */
    private static boolean looksLikeImage(byte[] b) {
        if (b.length < 12) return false;
        boolean jpeg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean webp = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return jpeg || png || webp;
    }
}
