package com.roadwatch.server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(length = 200)
    private String landmark;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** GPS accuracy in metres as reported by the phone; null if the pin was placed by hand. */
    private Double accuracyMeters;

    @ManyToOne(optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false)
    private Panchayat panchayat;

    @Column(length = 100)
    private String reporterName;

    @Column(length = 20)
    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.REPORTED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Report() { }

    public Report(String description, Severity severity, String landmark, double latitude, double longitude,
                  Double accuracyMeters, Panchayat panchayat, String reporterName, String reporterPhone) {
        this.description = description;
        this.severity = severity;
        this.landmark = landmark;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.panchayat = panchayat;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
    }

    /** Moves the report to a new status, enforcing the allowed lifecycle. */
    public void changeStatus(ReportStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Cannot move from " + status + " to " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public String getLandmark() { return landmark; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getAccuracyMeters() { return accuracyMeters; }
    public Panchayat getPanchayat() { return panchayat; }
    public String getReporterName() { return reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public ReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
