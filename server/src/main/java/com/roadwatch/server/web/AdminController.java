package com.roadwatch.server.web;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** District-level (SUPER_ADMIN) endpoints. Access is enforced in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) { this.admin = admin; }

    @GetMapping("/users")
    public List<UserDto> users(@RequestParam(defaultValue = "false") boolean pending) {
        return admin.listUsers(pending);
    }

    @PostMapping("/users/{id}/approve")
    public UserDto approve(@PathVariable Long id) { return admin.setActive(id, true); }

    @PostMapping("/users/{id}/deactivate")
    public UserDto deactivate(@PathVariable Long id) { return admin.setActive(id, false); }

    @PostMapping("/panchayats")
    @ResponseStatus(HttpStatus.CREATED)
    public PanchayatDto createPanchayat(@Valid @RequestBody CreatePanchayatRequest req) {
        return admin.createPanchayat(req);
    }
}
