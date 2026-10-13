package com.roadwatch.server.service;

import com.roadwatch.server.dto.Dtos.*;
import com.roadwatch.server.model.Admin;
import com.roadwatch.server.model.Panchayat;
import com.roadwatch.server.model.User;
import com.roadwatch.server.repository.PanchayatRepository;
import com.roadwatch.server.repository.UserRepository;
import com.roadwatch.server.security.JwtService;
import com.roadwatch.server.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PanchayatRepository panchayats;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PanchayatRepository panchayats, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.panchayats = panchayats;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Creates an INACTIVE admin account bound to a panchayat; the super admin must approve it. */
    @Transactional
    public void register(RegisterRequest req) {
        String username = req.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is already taken");
        }
        Panchayat p = panchayats.findById(req.panchayatId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown panchayat"));
        users.save(new Admin(username, encoder.encode(req.password()), req.fullName().trim(), p, false));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsernameIgnoreCase(req.username().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account is waiting for approval by the district admin");
        }
        return AuthResponse.of(jwt.issue(user), user);
    }
}
