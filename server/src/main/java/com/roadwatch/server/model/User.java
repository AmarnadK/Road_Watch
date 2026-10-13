package com.roadwatch.server.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Base class for everyone who can log in to the admin app.
 * Subclasses decide their own role and what they are allowed to modify (polymorphism).
 */
@Entity
@Table(name = "app_users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", length = 20)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected User() { }

    protected User(String username, String passwordHash, String fullName, boolean active) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
    }

    public abstract Role getRole();

    /** The panchayat this user is bound to, or null if they are not bound to one. */
    public abstract Panchayat getPanchayat();

    /** May this user change the status of the given report? */
    public abstract boolean canModify(Report report);

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
