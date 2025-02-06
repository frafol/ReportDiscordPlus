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
import net.kyori.adventure.text.event.ClickEvent;
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

@Plugin(id = "reportdiscordplus", name = "ReportDiscordPlus", version = "7.1.1", authors = {"Francies"})
public class ReportDiscordPlus {
    private final Path dataDirectory;
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

        Metrics metrics = metricsFactory.make(this, 23259);
        metrics.addCustomChart(new Metrics.SingleLineChart("chart_id", () -> Integer.valueOf("value")));

        // Inizializza DiscordNotifier e StaffNotifier
        this.discordNotifier = new DiscordNotifier(config, logger, messageManager);
        String titleText = messageManager.getRawMessage("title");
        String subTitleText = messageManager.getRawMessage("subtitle");
        this.staffNotifier = new StaffNotifier(this, messageManager, titleText, subTitleText);

        // Registra comandi ed eventi
        server.getEventManager().register(this, new PlayerLoginListener(this, server, messageManager));
        // TODO server.getCommandManager().register("verifyreport", new VerifyReportCommand(this, messageManager));
        server.getCommandManager().register("report", new ReportCommand(this, messageManager));
        server.getCommandManager().register("reports", new ReportListCommand(this, messageManager));
        server.getCommandManager().register("rpclose", new ReportCloseCommand(this, messageManager));
        server.getCommandManager().register("rpreopen", new ReportReopenCommand(this, messageManager));
        server.getCommandManager().register("rpdelete", new ReportDeleteCommand(this, messageManager));

        server.getEventManager().register(this, new PlayerJoinListenerReports(this, messageManager));

        // Avvia il controllo degli aggiornamenti
        checkForUpdates();
        String version = messageManager.getRawMessage("config_version");
        if (!version.equalsIgnoreCase("7")) {
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

    public ConfigurationNode getConfig(){
        return config;
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


    public ConfigurationNode getReportsConfig() {
        return reportsConfig;
    }
    public void checkForUpdates() {
        // Avvia un task asincrono con un leggero ritardo
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

                // Ottieni la versione attuale (del proxy o del plugin, a seconda di ciò che ti serve)
                String currentVersion = "7.0.0";

                // Se la versione è diversa, notifica gli admin
                if (!currentVersion.equals(latestVersion)) {
                    // Prepara i placeholder
                    Map<String, String> placeholders = Map.of(
                            "currentVersion", currentVersion,
                            "latestVersion", latestVersion,
                            "downloadUrl", downloadUrl1
                    );

                    // Carica la lista di messaggi dalla config
                    List<String> updateMessages = messageManager.getRawMessageList("updateMessage");

                    if (updateMessages != null && !updateMessages.isEmpty()) {
                        // Invia i messaggi di aggiornamento agli staff con permesso "report.admin"
                        for (Player staffMember : server.getAllPlayers()) {
                            if (staffMember.hasPermission("report.admin")) {
                                for (String line : updateMessages) {
                                    // Sostituisci i placeholder nel messaggio
                                    String formattedLine = messageManager.replacePlaceholders(line, placeholders);

                                    // Deserializziamo in componente Adventure
                                    Component messageComponent = messageManager.deserializeMessage(formattedLine);

                                    // Aggiungiamo l’evento di click per aprire il link
                                    messageComponent = messageComponent.clickEvent(ClickEvent.openUrl(downloadUrl1));

                                    // Invio del messaggio al giocatore
                                    staffMember.sendMessage(messageComponent);
                                }
                            }
                        }
                    } else {
                        logger.warn("updateMessage non è configurato correttamente nel file di configurazione.");
                    }
                }
            } catch (Exception e) {
                logger.error("Errore durante il controllo aggiornamenti: ", e);
            }
        }).delay(10, TimeUnit.SECONDS).schedule();
    }
}
