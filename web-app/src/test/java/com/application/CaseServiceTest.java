package com.application;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CaseServiceTest {
    private HttpServer server;
    private CaseService service;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> response = new AtomicReference<>("Delivered");
    private final AtomicReference<Map<String, String>> received = new AtomicReference<>();

    @BeforeEach
    void startPluginStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/give-item", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                form.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
            received.set(form);
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        service = new CaseService("http://127.0.0.1:" + server.getAddress().getPort() + "/give-item");
    }

    @AfterEach
    void stopPluginStub() { server.stop(0); }

    @Test
    void displayedWinnerIsTheDeliveredMaterialForEveryCase() {
        for (String caseId : CaseCatalog.all().keySet()) {
            CaseCatalog.Drop reward = service.open("Player+One", caseId);
            assertTrue(CaseCatalog.items(caseId).contains(reward));
            assertEquals(reward.material(), received.get().get("material"));
            assertEquals("Player+One", received.get().get("login"));
            assertEquals("1", received.get().get("amount"));
            assertFalse(received.get().containsKey("password"));
        }
    }

    @Test
    void offlineAndFullInventoryNeverReportSuccess() {
        status.set(404);
        var offline = assertThrows(ResponseStatusException.class, () -> service.open("Player", "armour"));
        assertEquals(HttpStatus.CONFLICT, offline.getStatusCode());
        assertTrue(offline.getReason().contains("Join"));
        status.set(409);
        var full = assertThrows(ResponseStatusException.class, () -> service.open("Player", "weapon"));
        assertTrue(full.getReason().contains("full"));
    }

    @Test
    void oldPluginAcceptanceAndServerFailuresAreNotDeliveryConfirmation() {
        for (int code : new int[]{202, 400, 500, 503, 504}) {
            status.set(code);
            assertThrows(ResponseStatusException.class, () -> service.open("Player", "tool"));
        }
        status.set(200);
        response.set("Case opened");
        assertThrows(ResponseStatusException.class, () -> service.open("Player", "tool"));
    }

    @Test
    void invalidCaseDoesNotContactPlugin() {
        assertThrows(ResponseStatusException.class, () -> service.open("Player", "invalid"));
        assertNull(received.get());
    }
}
