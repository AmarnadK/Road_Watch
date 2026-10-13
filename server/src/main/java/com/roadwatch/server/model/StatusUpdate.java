package com.roadwatch.server.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Audit trail: who changed a report's status, from what, to what, and why. */
@Entity
@Table(name = "status_updates")
public class StatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus newStatus;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected StatusUpdate() { }

    public StatusUpdate(Report report, User admin, ReportStatus oldStatus, ReportStatus newStatus, String note) {
        this.report = report;
        this.admin = admin;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.note = note;
    }

    public User getAdmin() { return admin; }
    public ReportStatus getOldStatus() { return oldStatus; }
    public ReportStatus getNewStatus() { return newStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
