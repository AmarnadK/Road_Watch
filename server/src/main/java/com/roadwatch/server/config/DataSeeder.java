package com.roadwatch.server.config;

import com.roadwatch.server.model.Panchayat;
import com.roadwatch.server.model.PanchayatType;
import com.roadwatch.server.model.SuperAdmin;
import com.roadwatch.server.repository.PanchayatRepository;
import com.roadwatch.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** On first start: loads panchayats from panchayats.csv and creates the super admin. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private final PanchayatRepository panchayats;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String superUser;
    private final String superPassword;

    public DataSeeder(PanchayatRepository panchayats, UserRepository users, PasswordEncoder encoder,
                      @Value("${roadwatch.superadmin.username}") String superUser,
                      @Value("${roadwatch.superadmin.password}") String superPassword) {
        this.panchayats = panchayats;
        this.users = users;
        this.encoder = encoder;
        this.superUser = superUser;
        this.superPassword = superPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        if (panchayats.count() == 0) {
            try (var reader = new BufferedReader(new InputStreamReader(
                    new ClassPathResource("panchayats.csv").getInputStream(), StandardCharsets.UTF_8))) {
                reader.readLine(); // header
                String line;
                int n = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(",");
                    panchayats.save(new Panchayat(parts[0].trim(), PanchayatType.valueOf(parts[1].trim())));
                    n++;
                }
                log.info("Seeded {} panchayats", n);
            }
        }
        if (!users.existsByUsernameIgnoreCase(superUser)) {
            users.save(new SuperAdmin(superUser.toLowerCase(), encoder.encode(superPassword), "District Admin"));
            log.info("Created super admin '{}'", superUser);
            if (DEFAULT_PASSWORD.equals(superPassword)) {
                log.warn("Super admin is using the DEFAULT password. Set SUPERADMIN_PASSWORD before going live!");
            }
        }
    }
}
