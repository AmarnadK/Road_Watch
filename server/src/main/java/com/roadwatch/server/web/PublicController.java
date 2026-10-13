package com.roadwatch.server.web;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.repository.PanchayatRepository;
import com.roadwatch.server.service.ReportService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints anyone can call without logging in. */
@RestController
public class PublicController {

    private final PanchayatRepository panchayats;
    private final ReportService reports;

    public PublicController(PanchayatRepository panchayats, ReportService reports) {
        this.panchayats = panchayats;
        this.reports = reports;
    }

    @GetMapping("/api/panchayats")
    public List<PanchayatDto> panchayats() {
        return panchayats.findAll(Sort.by("name")).stream().map(PanchayatDto::of).toList();
    }

    @GetMapping("/api/public/reports/{id}")
    public TrackResponse track(@PathVariable Long id) {
        return reports.track(id);
    }
}
