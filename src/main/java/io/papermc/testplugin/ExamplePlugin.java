package io.papermc.testplugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExamplePlugin extends JavaPlugin implements Listener {
    private HttpServer httpServer;

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> values = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";

            values.put(name, value);
        }

        return values;
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        } finally {
            exchange.close();
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8081), 0);
            httpServer.createContext("/give-item", this::givePlayerItem);
            httpServer.start();
        } catch (IOException e) {
            getLogger().severe("Failed to create HTTP server: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

    private void givePlayerItem(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "POST required");
            return;
        }

        Map<String, String> form;
        try {
            form = parseForm(exchange);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "Invalid form encoding");
            return;
        }
        String login = form.get("login");

        if (login == null || login.isBlank()) {
            sendResponse(exchange, 400, "Missing login");
            return;
        }

        String materialName = form.get("material");
        Material material = materialName == null ? null : Material.getMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()
                || !"1".equals(form.get("amount"))) {
            sendResponse(exchange, 400, "A valid material and amount=1 are required");
            return;
        }

        // Wait on the HTTP thread, never the Minecraft thread. Only report success
        // after the main-thread inventory mutation has actually completed.
        Future<Delivery> pending;
        try {
            pending = Bukkit.getScheduler().callSyncMethod(this, () -> deliver(login, material));
        } catch (RuntimeException e) {
            sendResponse(exchange, 503, "Plugin is stopping");
            return;
        }
        try {
            Delivery delivery = pending.get(5, TimeUnit.SECONDS);
            sendResponse(exchange, delivery.status(), delivery.message());
        } catch (TimeoutException e) {
            pending.cancel(false);
            sendResponse(exchange, 504, "Delivery confirmation timed out");
        } catch (InterruptedException e) {
            pending.cancel(false);
            Thread.currentThread().interrupt();
            sendResponse(exchange, 503, "Delivery interrupted");
        } catch (ExecutionException e) {
            getLogger().severe("Item delivery failed: " + e.getCause());
            sendResponse(exchange, 500, "Delivery failed");
        }
    }

    private record Delivery(int status, String message) {}

    private Delivery deliver(String login, Material material) {
        Player player = Bukkit.getPlayerExact(login);
        if (player == null || !player.isOnline()) return new Delivery(404, "Player is offline");
        // All case rewards have amount 1. Reserving an empty storage slot avoids
        // silently losing a reward when addItem cannot fit it in the inventory.
        int slot = player.getInventory().firstEmpty();
        if (slot < 0) return new Delivery(409, "Inventory is full");
        player.getInventory().setItem(slot, new ItemStack(material, 1));
        getLogger().info("Delivered " + material + " to " + login);
        return new Delivery(200, "Delivered");
    }
}
