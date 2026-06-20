package com.orbitaltraffic.service;

import com.orbitaltraffic.model.Satellite;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds the demo satellite fleet once at startup, and computes the live
 * state (positions + collision alerts) on demand for both the REST API
 * and the WebSocket feed.
 */
@Service
public class OrbitalSimulationService {

    private static final double CRITICAL_KM = 30;
    private static final double HIGH_KM = 75;
    private static final double MEDIUM_KM = 150;

    private static final String[] SATELLITE_NAMES = {
        "AURORA-1", "POLARIS-2", "MERIDIAN-3", "HORIZON-4", "VANGUARD-5",
        "SENTINEL-6", "ZENITH-7", "ECLIPSE-8", "ORBITAL-9", "BEACON-10",
        "CASCADE-11", "NOVA-12", "RELAY-13", "PROBE-14", "WATCHTOWER-15",
        "COMPASS-16", "PIONEER-17", "ATLAS-18",
    };

    private final List<Satellite> fleet;

    public OrbitalSimulationService() {
        this.fleet = generateFleet(42);
    }

    public int fleetSize() {
        return fleet.size();
    }

    /**
     * Builds the demo fleet, including two engineered "close call" pairs
     * (near-identical orbits, offset by under a degree of mean anomaly)
     * so collision alerts are visible immediately instead of waiting on
     * random chance.
     */
    private List<Satellite> generateFleet(long seed) {
        Random rng = new Random(seed);
        List<Satellite> result = new ArrayList<>();
        double now = System.currentTimeMillis() / 1000.0;

        for (String nm : SATELLITE_NAMES) {
            result.add(new Satellite(
                nm,
                380 + rng.nextDouble() * (1200 - 380),
                rng.nextDouble() * 98,
                rng.nextDouble() * 360,
                rng.nextDouble() * 360,
                now
            ));
        }

        Satellite base = result.get(0);
        result.add(new Satellite(
            "DEBRIS-X1",
            base.getAltitudeKm(),
            base.getInclinationDeg() + (rng.nextDouble() * 0.1 - 0.05),
            base.getRaanDeg() + (rng.nextDouble() * 0.1 - 0.05),
            base.getMeanAnomaly0Deg() + 0.9,
            now
        ));

        Satellite base2 = result.get(3);
        result.add(new Satellite(
            "DEBRIS-X2",
            base2.getAltitudeKm(),
            base2.getInclinationDeg() + (rng.nextDouble() * 0.1 - 0.05),
            base2.getRaanDeg() + (rng.nextDouble() * 0.1 - 0.05),
            base2.getMeanAnomaly0Deg() + 0.6,
            now
        ));

        return result;
    }

    public Map<String, Object> computeState() {
        return computeState(System.currentTimeMillis() / 1000.0);
    }

    public Map<String, Object> computeState(double t) {
        List<Map<String, Object>> satellites = new ArrayList<>();
        Map<String, double[]> eciCache = new LinkedHashMap<>();

        for (Satellite sat : fleet) {
            double[] geo = sat.geodeticPosition(t);
            eciCache.put(sat.getName(), sat.eciPositionKm(t));

            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", sat.getName());
            s.put("lat", round(geo[0], 3));
            s.put("lon", round(geo[1], 3));
            s.put("altitude_km", round(geo[2], 1));
            s.put("velocity_km_s", round(sat.speedKmS(), 3));
            s.put("inclination_deg", round(sat.getInclinationDeg(), 2));
            satellites.add(s);
        }

        List<Map<String, Object>> alerts = new ArrayList<>();
        List<String> names = new ArrayList<>(eciCache.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                String n1 = names.get(i), n2 = names.get(j);
                double[] p1 = eciCache.get(n1), p2 = eciCache.get(n2);
                double dist = Math.sqrt(
                    Math.pow(p1[0] - p2[0], 2) +
                    Math.pow(p1[1] - p2[1], 2) +
                    Math.pow(p1[2] - p2[2], 2)
                );

                String level = null;
                int score = 0;
                if (dist < CRITICAL_KM) { level = "critical"; score = 95; }
                else if (dist < HIGH_KM) { level = "high"; score = 70; }
                else if (dist < MEDIUM_KM) { level = "medium"; score = 40; }

                if (level != null) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("id", n1 + "-" + n2);
                    a.put("satellite1", n1);
                    a.put("satellite2", n2);
                    a.put("distance_km", round(dist, 2));
                    a.put("risk_level", level);
                    a.put("risk_score", score);
                    alerts.add(a);
                }
            }
        }
        alerts.sort((a, b) -> (Integer) b.get("risk_score") - (Integer) a.get("risk_score"));

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("type", "position_update");
        state.put("timestamp", t);
        state.put("satellite_count", satellites.size());
        state.put("satellites", satellites);
        state.put("alerts", alerts);
        state.put("alert_count", alerts.size());
        return state;
    }

    private static double round(double v, int places) {
        double factor = Math.pow(10, places);
        return Math.round(v * factor) / factor;
    }
}
