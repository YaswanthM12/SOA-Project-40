package com.logifleet.maintenance;

import java.time.*;

import jakarta.persistence.*;

@Entity
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long vehicleId;
    public String issue;
    public String status;
    public String notes;
    public LocalDate openedOn;
    public LocalDate completedOn;
}