package me.francies.ReportDiscordPlus.utility;

import me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.concurrent.TimeUnit;


public class PlayerLoginListener implements Listener {

    private final ReportDiscordPlus plugin;

    public PlayerLoginListener(ReportDiscordPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.hasPermission("report.admin")) {
            ProxyServer.getInstance().getScheduler().schedule(plugin, plugin::checkForUpdates, 2, TimeUnit.SECONDS);
        }
    }
}