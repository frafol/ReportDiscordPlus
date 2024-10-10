package velocity.dev.francies.reportdiscordplus.report;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.francies.reportdiscordplus.utility.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import okhttp3.*;
import org.spongepowered.configurate.ConfigurationNode;
import velocity.dev.francies.reportdiscordplus.config.ConfigurationManager;
import velocity.dev.francies.reportdiscordplus.config.MessageManager;

import java.io.IOException;
import java.time.Duration;


public class ReportManager {

    private final ProxyServer proxyServer;
    private final MessageManager messageManager;
    private final CooldownManager cooldownManager;
    private final ConfigurationManager configManager;

    private final String pingRoleID;
    private final String webhookUrl;
    private final String titleText;
    private final String subTitleText;

    public ReportManager(ProxyServer proxyServer, MessageManager messageManager, CooldownManager cooldownManager, ConfigurationManager configManager) {
        this.proxyServer = proxyServer;
        this.messageManager = messageManager;
        this.cooldownManager = cooldownManager;
        this.configManager = configManager;

        ConfigurationNode config = configManager.getConfig();
        this.pingRoleID = config.node("discord", "pingRoleID").getString();
        this.webhookUrl = config.node("discord", "webhookUrl").getString();
        this.titleText = messageManager.getMessage("title");
        this.subTitleText = messageManager.getMessage("subtitle");
    }

    public void sendReportToMinecraftStaff(Player reporter, String reportedPlayer, String reason, String server) {
        String teleportCommand = "/tp " + reportedPlayer;
        String sendstaff = "/" + server;
        TextComponent sendButton = Component.text("SERVER").color(NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(Component.text("Vai nel server del giocatore segnalato")))
                .clickEvent(ClickEvent.runCommand(sendstaff));
        TextComponent teleportButton = Component.text("TELEPORT").color(NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(Component.text("Vai dal giocatore segnalato")))
                .clickEvent(ClickEvent.runCommand(teleportCommand));

        String staffMessage = messageManager.getMessage("staffAlert")
                .replace("{player}", reporter.getUsername())
                .replace("{reportedPlayer}", reportedPlayer)
                .replace("{reason}", reason)
                .replace("{server}", server);

        TextComponent messageComponent = Component.text(staffMessage);

        for (Player staffMember : proxyServer.getAllPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {
                if (staffMember.hasPermission("report.tp")) {
                    messageComponent = messageComponent.append(teleportButton).append(sendButton);
                }
                staffMember.sendMessage(messageComponent);
                sendTitleToPlayer(staffMember, createTitle(titleText, subTitleText, 10, 70, 20));
            }
        }
    }

    public Title createTitle(String titleText, String subTitleText, int fadeIn, int stay, int fadeOut) {
        Duration fadeInDuration = Duration.ofMillis(fadeIn);
        Duration stayDuration = Duration.ofMillis(stay);
        Duration fadeOutDuration = Duration.ofMillis(fadeOut);

        Title.Times times = Title.Times.times(fadeInDuration, stayDuration, fadeOutDuration);

        Component titleComponent = Component.text(titleText);
        Component subTitleComponent = Component.text(subTitleText);

        return Title.title(titleComponent, subTitleComponent, times);
    }

    public void sendTitleToPlayer(Player player, Title title) {
        player.showTitle(title);
    }

    public void sendReportToDiscord(String reporter, String reportedPlayer, String reason, String server) {
        if (this.webhookUrl != null) {
            String allert = messageManager.getMessage("allert");
            String title = messageManager.getMessage("title");
            String pingRoleID = this.pingRoleID;
            String reaction = configManager.getConfig().node("discord", "reaction").getString();

            String messageContent = String.format("""
            {
                "content": "<@&%s>",  // Ping del ruolo
                "embeds": [{
                    "title": "%s",
                    "color": 15158332,
                    "fields": [
                        {
                            "name": "Reporter",
                            "value": "🟢 %s",
                            "inline": false
                        },
                        {
                            "name": "Reported",
                            "value": "🔴 %s",
                            "inline": false
                        },
                        {
                            "name": "Reason",
                            "value": "💬 %s",
                            "inline": false
                        },
                        {
                            "name": "Server",
                            "value": "💻 %s",
                            "inline": false
                        }
                    ]
                }],
                "reactions": ["%s"]  // Reazione
            }
            """, pingRoleID, title, reporter, reportedPlayer, reason, server, reaction);

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(
                    messageContent, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(this.webhookUrl)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.err.println("Failed to send report to Discord webhook: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                    if (!response.isSuccessful()) {
                        System.err.println("Received unsuccessful response from Discord webhook");
                    }
                }
            });

        } else {
            System.err.println("Webhook URL is not configured. Please set it in the config.yml.");
        }
    }


    public boolean isPlayerInBlacklist(Player player) {
        return player.hasPermission("report.protection");
    }
}
