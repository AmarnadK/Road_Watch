package com.roadwatch.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "panchayats")
public class Panchayat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PanchayatType type;

    protected Panchayat() { }

    public Panchayat(String name, PanchayatType type) {
        this.name = name;
        this.type = type;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public PanchayatType getType() { return type; }
}
