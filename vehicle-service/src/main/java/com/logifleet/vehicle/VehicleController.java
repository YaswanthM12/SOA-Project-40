package com.logifleet.vehicle;

import java.util.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    final VehicleRepository r;

    VehicleController(VehicleRepository r) {
        this.r = r;
    }

    @GetMapping
    public List<Vehicle> all(
            @RequestParam(required = false) String status
    ) {
        return status == null
                ? r.findAll()
                : r.findAll()
                        .stream()
                        .filter(v -> status.equalsIgnoreCase(v.status))
                        .toList();
    }

    @GetMapping("/{id}")
    public Vehicle one(@PathVariable Long id) {

        return r.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Vehicle not found")
                );
    }

    @PostMapping
    public ResponseEntity<Vehicle> create(
            @RequestBody Vehicle v
    ) {
        v.id = null;

        if (v.status == null) {
            v.status = "AVAILABLE";
        }

        return ResponseEntity
                .status(201)
                .body(r.save(v));
    }

    @PutMapping("/{id}")
    public Vehicle update(
            @PathVariable Long id,
            @RequestBody Vehicle x
    ) {
        Vehicle v = one(id);

        v.registrationNumber = x.registrationNumber;
        v.type = x.type;
        v.status = x.status;
        v.location = x.location;

        return r.save(v);
    }

    @PatchMapping("/{id}/status")
    public Vehicle status(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        Vehicle v = one(id);

        v.status = status.toUpperCase();

        return r.save(v);
    }

    @PatchMapping("/{id}/location")
    public Vehicle location(
            @PathVariable Long id,
            @RequestParam String location
    ) {
        Vehicle v = one(id);

        v.location = location;

        return r.save(v);
    }
}