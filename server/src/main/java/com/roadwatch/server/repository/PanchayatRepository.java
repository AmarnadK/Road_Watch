package com.roadwatch.server.repository;

import com.roadwatch.server.model.Panchayat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PanchayatRepository extends JpaRepository<Panchayat, Long> {
    boolean existsByNameIgnoreCase(String name);
}
