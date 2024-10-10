package bungee.me.francies.ReportDiscordPlus.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import net.md_5.bungee.config.Configuration;

public class DiscordNotifier {

    private final Configuration config;
    private final Logger logger;
    private final Map<String, String> messages;

    public DiscordNotifier(Configuration config, Logger logger, Map<String, String> messages) {
        this.config = config;
        this.logger = logger;
        this.messages = messages;
    }

    public void sendReportToDiscord(String reporter, String reportedPlayer, String reason, String server) {
        String webhookUrl = this.config.getString("discord.webhookUrl");
        String title = this.config.getString("discord.title");
        String alert = this.config.getString("discord.allert");
        String pingRoleId = this.config.getString("discord.pingRoleID");

        String content = "<@&" + pingRoleId + "> " + alert;

        String jsonPayload = "{"
                + "\"content\": \"" + content + "\","
                + "\"embeds\": [{"
                + "\"title\": \"" + title + "\","
                + "\"color\": 16711680,"
                + "\"fields\": ["
                + "{ \"name\": \"Reporter\", \"value\": \"🟢 " + reporter + "\", \"inline\": false },"
                + "{ \"name\": \"Reported\", \"value\": \"🔴 " + reportedPlayer + "\", \"inline\": false },"
                + "{ \"name\": \"Reason\", \"value\": \"💬 " + reason + "\", \"inline\": false },"
                + "{ \"name\": \"Server\", \"value\": \"💻 " + server + "\", \"inline\": false }"
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
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                logger.info(messages.get("reportSent"));
            } else {
                logger.warning("Failed to send report to Discord. Response Code: " + responseCode);

                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        Scanner scanner = new Scanner(errorStream).useDelimiter("\\A");
                        String responseBody = scanner.hasNext() ? scanner.next() : "";
                        logger.warning("Error response from Discord: " + responseBody);
                    }
                }
            }

        } catch (IOException e) {
            logger.warning(messages.get("cannotReportPlayer") + ": " + e.getMessage());
        }
    }
}
