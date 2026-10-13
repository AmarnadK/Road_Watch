package com.roadwatch.server.repository;

import com.roadwatch.server.model.ReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, Long> {
    Optional<ReportPhoto> findByReportId(Long reportId);
}
