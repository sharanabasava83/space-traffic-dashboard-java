package com.orbitaltraffic.controller;

import com.orbitaltraffic.service.OrbitalSimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class TrackingController {

    private final OrbitalSimulationService simulationService;

    public TrackingController(OrbitalSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("time", System.currentTimeMillis() / 1000.0);
        body.put("satellites_tracked", simulationService.fleetSize());
        return body;
    }

    @GetMapping("/api/satellites")
    public Map<String, Object> satellites() {
        return simulationService.computeState();
    }

    @GetMapping("/api/collisions")
    public Map<String, Object> collisions() {
        Map<String, Object> state = simulationService.computeState();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("alert_count", state.get("alert_count"));
        body.put("alerts", state.get("alerts"));
        body.put("timestamp", state.get("timestamp"));
        return body;
    }
}
