package bungee.me.francies.ReportDiscordPlus.utility;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.config.Configuration;

import java.util.Collection;
import java.util.List;

public class PlayerJoinListenerReports implements Listener {

    private final ReportDiscordPlus plugin;

    public PlayerJoinListenerReports(ReportDiscordPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // Verifica se il giocatore ha il permesso report.admin
        if (player.hasPermission("report.admin")) {
            // Ottieni i report aperti
            Configuration config = plugin.getReportsConfig();
            Collection<String> keys = config.getSection("reports").getKeys();
            long openReports = keys.stream()
                    .filter(key -> "open".equalsIgnoreCase(config.getString("reports." + key + ".status")))
                    .count();

            if (openReports > 0) {
                List<String> messageTemplate = plugin.getConfig().getStringList("messages.openReportsMessage");

                // Assicurati che il messaggio non sia nullo e non sia vuoto
                if (messageTemplate != null && !messageTemplate.isEmpty()) {
                    for (String line : messageTemplate) {
                        // Sostituisci i segnaposto e traduci i codici colore
                        String message = ChatColor.translateAlternateColorCodes('&',
                                line.replace("{amount}", String.valueOf(openReports))
                                        .replace("{player}", player.getName()));

                        // Invia il messaggio al giocatore
                        player.sendMessage(new TextComponent(plugin.getMessage("prefix") + message));
                    }
                }else {
                    // Log in caso il messaggio non sia definito
                    plugin.getLogger().warning("Message 'openReportsMessage' is not configured properly.");
                }
            }
        }
    }
}
