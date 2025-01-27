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

            // Se ci sono report aperti, invia il messaggio
            if (openReports > 0) {
                String messageTemplate = plugin.getConfig().getString("messages.openReportsMessage");

                // Assicurati che il messaggio non sia nullo
                if (messageTemplate != null) {
                    // Sostituisci il segnaposto e traduci i codici colore
                    String message = ChatColor.translateAlternateColorCodes('&',
                            messageTemplate.replace("{amount}", String.valueOf(openReports)));

                    // Invia il messaggio al giocatore
                    player.sendMessage(new TextComponent(message));
                } else {
                    // Log in caso il messaggio non sia definito
                    plugin.getLogger().warning("Il messaggio 'openReportsMessage' is not configured properly.");
                }
            }
        }
    }
}
