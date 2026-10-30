package com.logifleet.trip;

import java.util.*;

import feign.RequestInterceptor;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@FeignClient(
        name = "vehicle-service",
        configuration = FleetClients.Config.class
)
interface VehicleClient {

    @GetMapping("/vehicles/{id}")
    Map<String, Object> get(
            @PathVariable("id") Long id
    );

    @PatchMapping("/vehicles/{id}/status")
    void status(
            @PathVariable("id") Long id,
            @RequestParam("status") String status
    );
}

@FeignClient(
        name = "maintenance-service",
        configuration = FleetClients.Config.class
)
interface MaintenanceClient {

    @GetMapping("/maintenance/vehicle/{id}/clear")
    Map<String, Boolean> clear(
            @PathVariable("id") Long vehicleId
    );
}

public final class FleetClients {

    private FleetClients() {
    }

    @Configuration
    static class Config {

        @Bean
        RequestInterceptor authorizationForwarder() {

            return t -> {
                RequestAttributes a =
                        RequestContextHolder.getRequestAttributes();

                if (a instanceof ServletRequestAttributes s) {

                    String h =
                            s.getRequest()
                                    .getHeader("Authorization");

                    if (h != null) {
                        t.header("Authorization", h);
                    }
                }
            };
        }
    }
}