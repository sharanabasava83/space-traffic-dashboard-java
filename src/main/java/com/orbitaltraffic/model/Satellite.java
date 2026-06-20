package com.orbitaltraffic.model;

/**
 * Represents one satellite on a circular Keplerian orbit and knows how to
 * compute its own position at any point in time.
 *
 * This is a direct, verified port of the Python orbital_sim.py logic — no
 * external TLE data, no API keys, fully self-contained and deterministic
 * given a seed.
 */
public class Satellite {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double EARTH_MU = 398600.4418; // km^3/s^2
    public static final double EARTH_SIDEREAL_DAY_S = 86164.0905;
    public static final double EARTH_ROT_RATE = 2 * Math.PI / EARTH_SIDEREAL_DAY_S;

    private final String name;
    private final double altitudeKm;
    private final double inclinationDeg;
    private final double raanDeg;
    private final double meanAnomaly0Deg;
    private final double epoch; // unix seconds

    public Satellite(String name, double altitudeKm, double inclinationDeg,
                      double raanDeg, double meanAnomaly0Deg, double epoch) {
        this.name = name;
        this.altitudeKm = altitudeKm;
        this.inclinationDeg = inclinationDeg;
        this.raanDeg = raanDeg;
        this.meanAnomaly0Deg = meanAnomaly0Deg;
        this.epoch = epoch;
    }

    public String getName() { return name; }
    public double getAltitudeKm() { return altitudeKm; }
    public double getInclinationDeg() { return inclinationDeg; }
    public double getRaanDeg() { return raanDeg; }
    public double getMeanAnomaly0Deg() { return meanAnomaly0Deg; }

    public double semiMajorAxisKm() {
        return EARTH_RADIUS_KM + altitudeKm;
    }

    public double periodSeconds() {
        double a = semiMajorAxisKm();
        return 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / EARTH_MU);
    }

    public double speedKmS() {
        return Math.sqrt(EARTH_MU / semiMajorAxisKm());
    }

    /** Position in the Earth-Centered Inertial frame (km), at unix time t. */
    public double[] eciPositionKm(double t) {
        double a = semiMajorAxisKm();
        double n = 2 * Math.PI / periodSeconds();
        double dt = t - epoch;
        double M = Math.toRadians(meanAnomaly0Deg) + n * dt; // circular orbit: M == true anomaly

        double xP = a * Math.cos(M);
        double yP = a * Math.sin(M);

        double i = Math.toRadians(inclinationDeg);
        double raan = Math.toRadians(raanDeg);

        // Rotate by inclination around X axis
        double x1 = xP;
        double y1 = yP * Math.cos(i);
        double z1 = yP * Math.sin(i);

        // Rotate by RAAN around Z axis
        double x = x1 * Math.cos(raan) - y1 * Math.sin(raan);
        double y = x1 * Math.sin(raan) + y1 * Math.cos(raan);
        double z = z1;

        return new double[]{x, y, z};
    }

    /** Returns {lat_deg, lon_deg, altitude_km} at unix time t. */
    public double[] geodeticPosition(double t) {
        double[] p = eciPositionKm(t);
        double x = p[0], y = p[1], z = p[2];
        double r = Math.sqrt(x * x + y * y + z * z);
        double lat = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, z / r))));

        double lonEci = Math.atan2(y, x);
        double dt = t - epoch;
        double lonEcef = lonEci - EARTH_ROT_RATE * dt;
        double lon = Math.toDegrees(Math.atan2(Math.sin(lonEcef), Math.cos(lonEcef))); // wrap to [-180,180]

        double alt = r - EARTH_RADIUS_KM;
        return new double[]{lat, lon, alt};
    }
}
