package com.roadwatch.server.model;

import jakarta.persistence.*;

/** A panchayat official: may only modify reports that belong to their own panchayat. */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @ManyToOne
    @JoinColumn(name = "panchayat_id")
    private Panchayat panchayat;

    protected Admin() { }

    public Admin(String username, String passwordHash, String fullName, Panchayat panchayat, boolean active) {
        super(username, passwordHash, fullName, active);
        this.panchayat = panchayat;
    }

    @Override
    public Role getRole() { return Role.ADMIN; }

    @Override
    public Panchayat getPanchayat() { return panchayat; }

    @Override
    public boolean canModify(Report report) {
        return panchayat != null
                && report.getPanchayat().getId().equals(panchayat.getId());
    }
}
