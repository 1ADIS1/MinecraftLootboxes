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

public class ExamplePlugin extends JavaPlugin implements Listener {
    private HttpServer httpServer;

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

    private void givePlayerItem(HttpExchange exchange) {
        getLogger().info("Giving items to players!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        }
    }
}
