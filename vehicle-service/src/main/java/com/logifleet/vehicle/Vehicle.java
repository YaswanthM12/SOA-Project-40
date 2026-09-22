package com.logifleet.vehicle;

import jakarta.persistence.*;

@Entity
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String registrationNumber;

    public String type;
    public String status;
    public String location;
}