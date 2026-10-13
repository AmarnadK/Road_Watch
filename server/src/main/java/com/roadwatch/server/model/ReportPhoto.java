package com.roadwatch.server.model;

import jakarta.persistence.*;

/** Photo bytes live in their own table so report lists stay light. */
@Entity
@Table(name = "report_photos")
public class ReportPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, length = 10_000_000)
    private byte[] data;

    protected ReportPhoto() { }

    public ReportPhoto(Report report, String contentType, byte[] data) {
        this.report = report;
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}
