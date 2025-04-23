package velocity.me.francies.ReportDiscordPlus.utility;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerJoinListenerReports {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public PlayerJoinListenerReports(ReportDiscordPlus plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        // Verifica se il giocatore ha il permesso report.admin
        if (player.hasPermission("report.notify")) {
            // Ottieni i report aperti
            ConfigurationNode config = plugin.getReportsConfig();
            Collection<Object> keys = config.node("reports").childrenMap().keySet();
            long openReports = keys.stream()
                    .filter(key -> "open".equalsIgnoreCase(config.node("reports", key, "status").getString()))
                    .count();

            // Se ci sono report aperti, invia il messaggio
            if (openReports > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", String.valueOf(openReports));
                placeholders.put("player", player.getUsername());

                try {
                    // Ottiene la lista di messaggi dal file di configurazione
                    List<String> messageList = messageManager.getMessageList("messages.openReportsMessage", placeholders);

                    // Verifica che la lista non sia vuota
                    if (!messageList.isEmpty()) {
                        for (String line : messageList) {
                            // Sostituiamo i placeholder manualmente e convertiamo la stringa in Component
                            String formattedMessage = messageManager.replacePlaceholders(line, placeholders);
                            Component message = messageManager.deserializeMessage(formattedMessage);
                            player.sendMessage(message);
                        }
                    }
                } catch (SerializationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
