# 🛰️ Orbital Traffic Control — Space Traffic Dashboard

A real-time Space Traffic Management Dashboard built with Java Spring Boot and WebSocket technology. The system simulates satellite movement using Keplerian orbital mechanics and provides live orbital monitoring, collision detection, and telemetry visualization through an interactive web interface.

## 🌐 Live Demo

https://space-traffic-dashboard-java.onrender.com

## 🚀 Features

* Real-time satellite tracking
* Orbital simulation using Keplerian mechanics
* Live telemetry updates via WebSocket
* Collision detection and alert monitoring
* Interactive dashboard visualization
* Responsive web interface
* REST API support

## 🛠️ Tech Stack

### Backend

* Java 17
* Spring Boot 3
* Spring Web
* Spring WebSocket
* Maven

### Frontend

* HTML5
* CSS3
* JavaScript
* Three.js

### Deployment

* Docker
* Render

## 📂 Project Structure

```text
space-traffic-dashboard-java/
├── pom.xml
├── Dockerfile
├── render.yaml
├── src/main/java/com/orbitaltraffic/
│   ├── OrbitalTrafficApplication.java
│   ├── model/Satellite.java
│   ├── service/OrbitalSimulationService.java
│   ├── controller/TrackingController.java
│   ├── websocket/TrackingWebSocketHandler.java
│   └── config/
└── src/main/resources/
    ├── application.properties
    └── static/
```

## ▶️ Run Locally

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

## 📡 API Endpoints

* GET /api/health
* GET /api/satellites
* GET /api/collisions
* WS /ws/tracking

## 👨‍💻 Author

Sharanabasava

## 📄 License

MIT License
