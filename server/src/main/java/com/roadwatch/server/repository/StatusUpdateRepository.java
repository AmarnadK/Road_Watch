package com.roadwatch.server.repository;

import com.roadwatch.server.model.StatusUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusUpdateRepository extends JpaRepository<StatusUpdate, Long> {
    List<StatusUpdate> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
