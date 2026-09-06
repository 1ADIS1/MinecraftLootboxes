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

        Map<String, String> form = parseForm(exchange);
        String login = form.get("login");

        if (login == null || login.isBlank()) {
            sendResponse(exchange, 400, "Missing login");
            return;
        }

        // The built-in HTTP server handles requests outside the Minecraft server
        // thread, so all Bukkit API access must be scheduled on the server thread.
        Bukkit.getScheduler().runTask(this, () -> {
            Player player = Bukkit.getPlayerExact(login);

            if (player == null) {
                getLogger().warning("Couldn't find online player: " + login);
                return;
            }

            Material.getMaterial("");

            player.getInventory().addItem(new ItemStack(Material.LEATHER_CHESTPLATE, 1));
            getLogger().info("Gave items to player " + login);
        });

        sendResponse(exchange, 202, "Case opened");
    }
}
