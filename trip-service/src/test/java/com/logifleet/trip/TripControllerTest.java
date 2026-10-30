package com.logifleet.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class TripControllerTest {

    @Test
    void assign_shouldRequireAvailableVehicleAndClearedMaintenance() {
        TripRepository repo = mock(TripRepository.class);
        VehicleClient vehicles = mock(VehicleClient.class);
        MaintenanceClient maintenance = mock(MaintenanceClient.class);
        TripController controller = new TripController(repo, vehicles, maintenance);

        when(vehicles.get(10L)).thenReturn(Map.of("status", "AVAILABLE"));
        when(maintenance.clear(10L)).thenReturn(Map.of("clear", true));
        when(repo.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.id = 99L;
            return trip;
        });

        Trip trip = new Trip();
        trip.vehicleId = 10L;
        trip.driverId = "DRV-101";
        trip.origin = "Bengaluru";
        trip.destination = "Chennai";

        ResponseEntity<?> response = controller.assign(trip);

        assertEquals(201, response.getStatusCode().value());
        Trip saved = (Trip) response.getBody();
        assertNotNull(saved);
        assertEquals("ASSIGNED", saved.tripStatus);
    }

    @Test
    void status_shouldMoveTripFromAssignedToInProgress() {
        TripRepository repo = mock(TripRepository.class);
        VehicleClient vehicles = mock(VehicleClient.class);
        MaintenanceClient maintenance = mock(MaintenanceClient.class);
        TripController controller = new TripController(repo, vehicles, maintenance);

        Trip trip = new Trip();
        trip.id = 7L;
        trip.vehicleId = 11L;
        trip.driverId = "DRV-110";
        trip.tripStatus = "ASSIGNED";

        when(repo.findById(7L)).thenReturn(Optional.of(trip));
        when(repo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip updated = controller.status(7L, "IN_PROGRESS");

        assertEquals("IN_PROGRESS", updated.tripStatus);
        assertNotNull(updated.startedAt);
    }
}
