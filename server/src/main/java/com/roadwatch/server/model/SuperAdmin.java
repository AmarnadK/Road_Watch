package com.roadwatch.server.model;

import jakarta.persistence.*;

/** District-level account: approves panchayat admins and may modify any report. */
@Entity
@DiscriminatorValue("SUPER_ADMIN")
public class SuperAdmin extends User {

    protected SuperAdmin() { }

    public SuperAdmin(String username, String passwordHash, String fullName) {
        super(username, passwordHash, fullName, true);
    }

    @Override
    public Role getRole() { return Role.SUPER_ADMIN; }

    @Override
    public Panchayat getPanchayat() { return null; }

    @Override
    public boolean canModify(Report report) { return true; }
}
