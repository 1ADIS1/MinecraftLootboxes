package com.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

@Service
public class CaseService {
    private final URI pluginUri;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final SecureRandom random = new SecureRandom();

    public CaseService(@Value("${lootboxes.plugin-url:http://127.0.0.1:8081/give-item}") String pluginUrl) {
        this.pluginUri = URI.create(pluginUrl);
    }

    public CaseCatalog.Drop open(String login, String caseId) {
        CaseCatalog.Drop reward = CaseCatalog.select(caseId, random);
        String body = "login=" + URLEncoder.encode(login, StandardCharsets.UTF_8)
                + "&material=" + reward.material() + "&amount=1";
        HttpRequest request = HttpRequest.newBuilder(pluginUri)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Join the Minecraft server before opening a case.");
            }
            if (response.statusCode() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Your inventory is full. Free a slot and try again.");
            }
            // The old plugin returned 202 before delivery. Only accept confirmed delivery.
            if (response.statusCode() != 200 || !response.body().equals("Delivered")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The Minecraft server could not confirm delivery.");
            }
            return reward;
        } catch (HttpTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "Delivery confirmation timed out. Check your inventory before opening another case.");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cannot confirm delivery. Check the Minecraft server and your inventory before trying again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Opening interrupted. Check your inventory.");
        }
    }
}
