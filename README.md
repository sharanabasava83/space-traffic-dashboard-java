# Orbital Traffic Control — Java / Spring Boot Edition

Same live satellite-tracking dashboard as the Python version, rewritten on
a Java/Spring Boot backend. The frontend (3D globe, telemetry table,
collision alerts) is untouched — it's plain HTML/JS, so it doesn't care
what language serves it.

No API keys, no external data — the fleet is simulated locally using real
Keplerian orbital mechanics (`Satellite.java`).

## Tech stack

- **Backend:** Java 17, Spring Boot 3 (Web + WebSocket)
- **Frontend:** plain HTML/CSS/JS + Three.js (no build step)
- **Build tool:** Maven

## Requirements

- JDK 17 or newer
- Maven 3.6+

## Run it locally

```bash
mvn spring-boot:run
```

Open **http://127.0.0.1:8080** in your browser.

## Build a runnable jar

```bash
mvn clean package
java -jar target/space-traffic-dashboard-1.0.0.jar
```

## Project structure

```
space-traffic-dashboard-java/
├── pom.xml
├── Dockerfile               # used for free deployment (e.g. Render)
├── render.yaml
└── src/main/
    ├── java/com/orbitaltraffic/
    │   ├── OrbitalTrafficApplication.java   # main entry point
    │   ├── model/Satellite.java              # orbital mechanics
    │   ├── service/OrbitalSimulationService.java  # fleet + collision detection
    │   ├── controller/TrackingController.java      # REST API
    │   ├── websocket/TrackingWebSocketHandler.java  # live feed
    │   └── config/                                   # WebSocket + CORS wiring
    └── resources/
        ├── application.properties
        └── static/            # the dashboard frontend (unchanged from the Python version)
```

## API

Identical contract to the Python version, so the frontend works against either backend unmodified:

- `GET /api/health`
- `GET /api/satellites`
- `GET /api/collisions`
- `WS /ws/tracking` — pushes a full state update every 2 seconds

## Deploy for free (Render, via Docker)

1. Push this folder to its own GitHub repo.
2. On render.com: **New + → Web Service** → connect the repo.
3. Render detects `render.yaml` and `Dockerfile` automatically → pick the **Free** plan → **Create Web Service**.
4. First build takes a few minutes (Maven download + compile inside the Docker build). You'll get a public URL when it's done.

## A note on this environment

This project was written and the orbital-mechanics math was independently
verified (compiled and run standalone, output checked) inside the
sandbox. The full Spring Boot build itself could **not** be compiled
end-to-end here, because this sandbox's network whitelist doesn't include
Maven Central (`repo.maven.apache.org`) — only a fixed list of approved
domains. Your own machine has normal internet access, so `mvn
spring-boot:run` will download dependencies and run normally. If you hit
any build error, paste it back and I'll help fix it directly.

## License

MIT
