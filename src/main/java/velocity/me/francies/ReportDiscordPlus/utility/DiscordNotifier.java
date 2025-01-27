package velocity.me.francies.ReportDiscordPlus.utility;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DiscordNotifier {

    private final ConfigurationNode config;
    private final Logger logger;
    private final MessageManager messageManager;

    public DiscordNotifier(ConfigurationNode config, Logger logger, MessageManager messageManager) {
        this.config = config;
        this.logger = logger;
        this.messageManager = messageManager;
    }

    public void sendReportToDiscord(String reporter, String reportedPlayer, String reason, String server) {
        String webhookUrl = this.config.node("discord", "webhookUrl").getString();
        String title = this.config.node("discord", "title").getString();
        String alert = this.config.node("discord", "alert").getString();
        String pingRoleId = this.config.node("discord", "pingRoleID").getString();
        int color = this.config.node("discord", "embedColor").getInt(16711680); // Colore di default rosso

        // Titoli configurabili
        String reporterTitle = this.config.node("discord", "fields", "reporter").getString("Reporter");
        String reportedTitle = this.config.node("discord", "fields", "reported").getString("Reported");
        String reasonTitle = this.config.node("discord", "fields", "reason").getString("Reason");
        String serverTitle = this.config.node("discord", "fields", "server").getString("Server");

        String content = "<@&" + pingRoleId + "> " + alert;

        String jsonPayload = "{"
                + "\"content\": \"" + content + "\","
                + "\"embeds\": [{"
                + "\"title\": \"" + title + "\","
                + "\"color\": " + color + ","
                + "\"fields\": ["
                + "{ \"name\": \"" + reporterTitle + "\", \"value\": \"🟢 " + reporter + "\", \"inline\": false },"
                + "{ \"name\": \"" + reportedTitle + "\", \"value\": \"🔴 " + reportedPlayer + "\", \"inline\": false },"
                + "{ \"name\": \"" + reasonTitle + "\", \"value\": \"💬 " + reason + "\", \"inline\": false },"
                + "{ \"name\": \"" + serverTitle + "\", \"value\": \"💻 " + server + "\", \"inline\": false }"
                + "]"
                + "}]"
                + "}";

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                logger.info("Report inviato a Discord con successo.");
            } else {
                logger.warn("Impossibile inviare il report a Discord. Codice di risposta: " + responseCode);

                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        Scanner scanner = new Scanner(errorStream).useDelimiter("\\A");
                        String responseBody = scanner.hasNext() ? scanner.next() : "";
                        logger.warn("Risposta di errore da Discord: " + responseBody);
                    }
                }
            }

        } catch (IOException e) {
            String errorMsg = messageManager.getRawMessage("messages.cannotReportPlayer");
            logger.warn(errorMsg + ": " + e.getMessage());
        }
    }
}
