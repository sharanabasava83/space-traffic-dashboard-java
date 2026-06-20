package com.orbitaltraffic.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitaltraffic.service.OrbitalSimulationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Pushes a full state update (positions + collision alerts) to each
 * connected client every UPDATE_INTERVAL_S seconds — equivalent to the
 * Python backend's `while True: send_json(...); sleep(...)` loop, just
 * implemented with a scheduled task per session instead of asyncio.
 */
@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private static final long UPDATE_INTERVAL_S = 2;

    private final OrbitalSimulationService simulationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    public TrackingWebSocketHandler(OrbitalSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    Map<String, Object> state = simulationService.computeState();
                    String json = objectMapper.writeValueAsString(state);
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                // Connection likely closed mid-send; the close handler below cleans up.
            }
        }, 0, UPDATE_INTERVAL_S, TimeUnit.SECONDS);

        activeTasks.put(session.getId(), task);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ScheduledFuture<?> task = activeTasks.remove(session.getId());
        if (task != null) {
            task.cancel(true);
        }
    }
}
