package com.roadwatch.server.web;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.model.*;
import com.roadwatch.server.service.ReportService;
import com.roadwatch.server.service.SubmissionRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;
    private final SubmissionRateLimiter limiter;

    public ReportController(ReportService reports, SubmissionRateLimiter limiter) {
        this.reports = reports;
        this.limiter = limiter;
    }

    /** Public: a citizen submits a pothole report from the mobile web app. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submit(
            @RequestParam String description,
            @RequestParam Severity severity,
            @RequestParam(required = false) String landmark,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double accuracy,
            @RequestParam Long panchayatId,
            @RequestParam(required = false) String reporterName,
            @RequestParam(required = false) String reporterPhone,
            @RequestParam(required = false) MultipartFile photo,
            HttpServletRequest request) throws IOException {

        limiter.check(clientIp(request));
        Report r = reports.submit(description, severity, landmark, latitude, longitude, accuracy,
                panchayatId, reporterName, reporterPhone, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", r.getId(),
                "reference", "RW-" + r.getId(),
                "status", r.getStatus()));
    }

    /** Admins: list reports, optionally filtered. Every admin can view all; only some can edit. */
    @GetMapping
    public List<ReportDto> list(@AuthenticationPrincipal User me,
                                @RequestParam(required = false) Long panchayatId,
                                @RequestParam(required = false) ReportStatus status) {
        return reports.list(me, panchayatId, status);
    }

    @GetMapping("/{id}")
    public ReportDto get(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return reports.get(me, id);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        ReportPhoto p = reports.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(p.getContentType()))
                .cacheControl(CacheControl.noStore())
                .body(p.getData());
    }

    @PatchMapping("/{id}/status")
    public ReportDto changeStatus(@AuthenticationPrincipal User me, @PathVariable Long id,
                                  @Valid @RequestBody StatusChangeRequest req) {
        return reports.changeStatus(me, id, req.status(), req.note());
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
