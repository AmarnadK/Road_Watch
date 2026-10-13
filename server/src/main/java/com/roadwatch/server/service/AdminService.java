package com.roadwatch.server.service;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.model.Panchayat;
import com.roadwatch.server.model.Role;
import com.roadwatch.server.model.User;
import com.roadwatch.server.repository.PanchayatRepository;
import com.roadwatch.server.repository.UserRepository;
import com.roadwatch.server.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository users;
    private final PanchayatRepository panchayats;

    public AdminService(UserRepository users, PanchayatRepository panchayats) {
        this.users = users;
        this.panchayats = panchayats;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers(boolean pendingOnly) {
        List<User> list = pendingOnly ? users.findByActiveFalseOrderByCreatedAtDesc()
                                      : users.findAllByOrderByCreatedAtDesc();
        return list.stream().map(UserDto::of).toList();
    }

    @Transactional
    public UserDto setActive(Long id, boolean active) {
        User u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() == Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The super admin account cannot be changed here");
        }
        u.setActive(active);
        return UserDto.of(users.save(u));
    }

    @Transactional
    public PanchayatDto createPanchayat(CreatePanchayatRequest req) {
        String name = req.name().trim();
        if (panchayats.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "That panchayat already exists");
        }
        return PanchayatDto.of(panchayats.save(new Panchayat(name, req.type())));
    }
}
