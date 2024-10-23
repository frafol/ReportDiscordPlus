package velocity.me.francies.ReportDiscordPlus.utility;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;

public class PlayerLoginListener {

    private final ReportDiscordPlus plugin;
    private final ProxyServer server;
    private final MessageManager messageManager;

    public PlayerLoginListener(ReportDiscordPlus plugin, ProxyServer server, MessageManager messageManager) {
        this.plugin = plugin;
        this.server = server;
        this.messageManager = messageManager;
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        // Controlla se il giocatore ha il permesso admin
        if (player.hasPermission("report.admin")) {
            // Controlla gli aggiornamenti per l'admin appena loggato
            plugin.checkForUpdates();
        }
    }
}
