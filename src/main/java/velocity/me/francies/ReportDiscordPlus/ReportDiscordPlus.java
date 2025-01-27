package velocity.me.francies.ReportDiscordPlus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import velocity.me.francies.ReportDiscordPlus.commands.*;
import velocity.me.francies.ReportDiscordPlus.utility.*;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Plugin(id = "reportdiscordplus", name = "ReportDiscordPlus", version = "6.5", authors = {"Francies"})
public class ReportDiscordPlus {
    private final Path dataDirectory;
    private static final String ENCODED_KEYS_URL = "aHR0cHM6Ly93d3cuZnJhbmNlc2NvZmVycmFyYS5pdC9hcGkva2V5cy5qc29u";
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final ProxyServer server;
    private final Logger logger;
    private ConfigurationNode reportsConfig;
    private ConfigurationNode config;
    private DiscordNotifier discordNotifier;
    private StaffNotifier staffNotifier;
    private MessageManager messageManager;
    private final String versionUrl = "https://www.francescoferrara.it/api/reportdiscordplus.json";
    private final Metrics.Factory metricsFactory;
    private int minLength;
    private int maxLength;
    @Inject
    public ReportDiscordPlus(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.dataDirectory = dataDirectory;
        this.server = server;
        this.logger = logger;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        loadConfig();
        loadReportsConfig();
        loadReasonLengthLimits();
        this.messageManager = new MessageManager(config);
//        try {
//            // Verifica e genera file.key
//            File keyFile = new File(dataDirectory.toFile(), "file.key");
//            String localId;
//
//            if (!keyFile.exists()) {
//                localId = UUID.randomUUID().toString();
//                saveKeyFile(localId);
//            } else {
//                localId = readKeyFile();
//            }
//
//            String key = config.node("licenseKey").getString("");
//            if (!verifyKey(key, localId)) {
//                logger.error("Wait, an id should be set to your plugin soon");
//                return;
//            }
//        } catch (IOException e) {
//            logger.error("Error during plugin initialization: " + e.getMessage());
//        }

        Metrics metrics = metricsFactory.make(this, 23259);
        metrics.addCustomChart(new Metrics.SingleLineChart("chart_id", () -> Integer.valueOf("value")));

        // Inizializza DiscordNotifier e StaffNotifier
        this.discordNotifier = new DiscordNotifier(config, logger, messageManager);
        String titleText = messageManager.getRawMessage("title");
        String subTitleText = messageManager.getRawMessage("subtitle");
        this.staffNotifier = new StaffNotifier(titleText, subTitleText, this, messageManager);

        // Registra comandi ed eventi
        server.getEventManager().register(this, new PlayerLoginListener(this, server, messageManager));
        //server.getCommandManager().register("verifyreport", new VerifyReportCommand(this, messageManager));
        server.getCommandManager().register("report", new ReportCommand(this, messageManager));
        server.getCommandManager().register("reports", new ReportListCommand(this, messageManager));
        server.getCommandManager().register("reportclose", new ReportCloseCommand(this, messageManager));
        server.getCommandManager().register("reportreopen", new ReportReopenCommand(this, messageManager));
        server.getCommandManager().register("reportdelete", new ReportDeleteCommand(this, messageManager));
        server.getEventManager().register(this, new PlayerJoinListenerReports(this, messageManager));

        // Avvia il controllo degli aggiornamenti
        checkForUpdates();
        String version = messageManager.getRawMessage("config_version");
        if (!version.equalsIgnoreCase("3")) {
            logger.error("YOUR CONFIG IS NOT UPDATED, CHECK HERE: https://discord.gg/SGtHSCTaEX");
        }
    }

    public ProxyServer getProxy() {
        return server;
    }

    private void loadConfig() {
        try {
            // Crea la directory se non esiste
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);  // Questo creerà tutte le sottocartelle necessarie
            }

            Path configFile = dataDirectory.resolve("config.yml");

            // Se il file config.yml non esiste, lo copia dalle risorse
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .build();
            this.config = loader.load();

        } catch (IOException e) {
            logger.error (e.toString());
        }
    }


    private void loadReportsConfig() {
        try {
            // Crea la directory se non esiste
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);  // Crea la directory principale
            }

            Path reportsFilePath = dataDirectory.resolve("reports.yml");

            // Crea il file reports.yml se non esiste
            if (!Files.exists(reportsFilePath)) {
                Files.createFile(reportsFilePath);
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .path(reportsFilePath)
                        .build();
                ConfigurationNode root = loader.createNode();
                root.node("lastReportId").set(0);
                loader.save(root);
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(reportsFilePath)
                    .build();
            this.reportsConfig = loader.load();

        } catch (IOException e) {
            logger.error(e.toString());
        }
    }


    public void saveReportsConfig() {
        try {
            Path reportsFilePath = dataDirectory.resolve("reports.yml");
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(reportsFilePath)
                    .build();
            loader.save(this.reportsConfig);
        } catch (ConfigurateException e) {
            logger.error("Error saving reports.yml", e);
        }
    }

    public int getNextReportId() throws SerializationException {
        int lastReportId = reportsConfig.node("lastReportId").getInt(0);
        int newReportId = lastReportId + 1;
        reportsConfig.node("lastReportId").set(newReportId);
        saveReportsConfig();
        return newReportId;
    }

//    private void saveKeyFile(String id) throws IOException {
//        File keyFile = new File(dataDirectory.toFile(), "file.key");
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyFile))) {
//            writer.write(id);
//        }
//    }
//
//    private String readKeyFile() throws IOException {
//        File keyFile = new File(dataDirectory.toFile(), "file.key");
//        return new String(Files.readAllBytes(keyFile.toPath()));
//    }

//    public boolean verifyKey(String key, String localId) {
//        try {
//            String keysUrl = new String(Base64.getDecoder().decode(ENCODED_KEYS_URL));
//
//            HttpURLConnection connection = (HttpURLConnection) new URL(keysUrl).openConnection();
//            connection.setRequestMethod("GET");
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder content = new StringBuilder();
//            String inputLine;
//            while ((inputLine = in.readLine()) != null) {
//                content.append(inputLine);
//            }
//            in.close();
//            connection.disconnect();
//
//            JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
//
//            if (!json.has(key)) {
//                logger.error("KEY NOT FOUND. IF YOU PURCHASED THE PLUGIN OPEN A TICKET HERE: https://discord.gg/SGtHSCTaEX");
//                return false;
//            }
//
//            String registeredId = json.get(key).getAsJsonObject().get("id").getAsString();
//
//            if (registeredId.isEmpty()) {
//                logger.error("ID is missing for the key. Sending to Discord...");
//                sendKeyToDiscord(key, localId);
//                return false;
//            } else if (registeredId.equals(localId)) {
//                return true;
//            } else {
//                logger.error("KEY ALREADY IN USE WITH A DIFFERENT ID");
//                return false;
//            }
//
//        } catch (IOException e) {
//            logger.error("Error during key verification: " + e.getMessage());
//            return false;
//        }
//    }

    public boolean isPlayerInBlacklist(Player player) {
        return player.hasPermission("report.protection");
    }
    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }

    public StaffNotifier getStaffNotifier() {
        return staffNotifier;
    }
    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public void setCooldown(Player player) {
        int cooldownSeconds = config.node("cooldown").getInt(120);
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        this.cooldowns.put(player.getUsername(), cooldownTime);
    }
    private void loadReasonLengthLimits() {
        minLength = config.node("reason", "minLength").getInt();
        maxLength = config.node("reason", "maxLength").getInt();
    }

    // Provide getter methods to access these values in other classes
    public int getMinReasonLength() {
        return minLength;
    }

    public int getMaxReasonLength() {
        return maxLength;
    }
    public boolean hasCooldown(Player player) {
        return this.cooldowns.containsKey(player.getUsername()) && this.cooldowns.get(player.getUsername()) > System.currentTimeMillis();
    }
//    private void sendKeyToDiscord(String key, String localId) {
//        try {
//            // URL del tuo webhook di Discord
//            String webhookUrl = "https://discord.com/api/webhooks/1294797101863145553/IcT4GnnWeQKHVEb-IivqVCngxrIs8CK3jg1zQjFrv6rVhl6KbRiE0aGtLK7T42YVjhWS";
//
//            // Corpo del messaggio da inviare
//            String message = "{"
//                    + "\"content\": \"<@&1146046554348724296> Devi assegnare questo id\","
//                    + "\"embeds\": [{"
//                    + "\"title\": \"Assegnazione key\","
//                    + "\"description\": \"Generato un id nuovo:\","
//                    + "\"fields\": ["
//                    + "{ \"name\": \"Key\", \"value\": \"" + key + "\" },"
//                    + "{ \"name\": \"ID\", \"value\": \"" + localId + "\" }"
//                    + "],"
//                    + "\"color\": 5814783"
//                    + "}]"
//                    + "}";
//
//            // Creazione della connessione
//            URL url = new URL(webhookUrl);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//            connection.setDoOutput(true);
//
//            // Invia il messaggio JSON al webhook
//            try (OutputStream os = connection.getOutputStream()) {
//                byte[] input = message.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//
//            // Leggi la risposta dal server
//            int responseCode = connection.getResponseCode();
//            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
//                logger.info("Key and ID successfully sent to Discord.");
//            } else {
//                logger.error("Failed to send key and ID to Discord. Response code: " + responseCode);
//            }
//        } catch (IOException e) {
//            logger.error("Error during Discord webhook communication: " + e.getMessage());
//        }
//    }

    public ConfigurationNode getReportsConfig() {
        return reportsConfig;
    }
    public void checkForUpdates() {
        server.getScheduler().buildTask(this, () -> {
            try {
                // Connessione per ottenere le informazioni sulla versione
                URL url = new URL(versionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                connection.disconnect();

                // Parsing della risposta JSON
                JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
                String latestVersion = json.get("version").getAsString();
                String downloadUrl1 = json.get("downloadUrl1").getAsString();

                // Ottieni la versione attuale del plugin
                String currentVersion = getProxy().getVersion().getVersion();

                // Controllo se è necessaria un'update
                if (!currentVersion.equals(latestVersion)) {
                    // Preparazione dei segnaposto
                    Map<String, String> placeholders = Map.of(
                            "currentVersion", currentVersion,
                            "latestVersion", latestVersion,
                            "downloadUrl", downloadUrl1
                    );

                    // Leggi la lista di messaggi dalla configurazione
                    List<String> updateMessages = messageManager.getRawMessageList("updateMessage");

                    if (updateMessages != null && !updateMessages.isEmpty()) {
                        // Invio dei messaggi di aggiornamento agli admin
                        for (Player staffMember : server.getAllPlayers()) {
                            if (staffMember.hasPermission("report.admin")) {
                                for (String line : updateMessages) {
                                    String formattedLine = messageManager.replacePlaceholders(line, placeholders);
                                    staffMember.sendMessage(messageManager.deserializeMessage(formattedLine));
                                }
                            }
                        }
                    } else {

                        logger.warn("updateMessage have not been set properly.");
                    }
                }

            } catch (Exception e) {
                logger.error("Error during the update check: ", e);
            }
        }).delay(10, TimeUnit.SECONDS).schedule();
    }


}
