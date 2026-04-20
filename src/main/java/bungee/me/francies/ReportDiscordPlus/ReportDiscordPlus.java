package bungee.me.francies.ReportDiscordPlus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bungee.me.francies.ReportDiscordPlus.commands.*;
import bungee.me.francies.ReportDiscordPlus.utility.DiscordNotifier;
import bungee.me.francies.ReportDiscordPlus.utility.PlayerJoinListenerReports;
import bungee.me.francies.ReportDiscordPlus.utility.PlayerLoginListener;
import bungee.me.francies.ReportDiscordPlus.utility.StaffNotifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;


public class ReportDiscordPlus extends Plugin {
    private Configuration config;
    private Map<String, String> messages;
    public HashMap<String, Long> cooldowns = new HashMap<>();
    String titleText;
    String subTitleText;
    private Configuration reportsConfig;
    private File reportsFile;
    private DiscordNotifier discordNotifier;
    private StaffNotifier staffNotifier;
    //private final String versionUrl = "https://www.francescoferrara.it/api/reportdiscordplus.json";

    public void onEnable() {
        int pluginId = 23259;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.SingleLineChart("players", () -> ProxyServer.getInstance().getOnlineCount()));
        getLogger().info(" ____                       _   ____  _                       _ ____  _");
        getLogger().info("|  _ \\ ___ _ __   ___  _ __| |_|  _ \\(_)___  ___ ___  _ __ __| |  _ \\| |_   _ ___");
        getLogger().info("| |_) / _ \\ '_ \\ / _ \\| '__| __| | | | / __|/ __/ _ \\| '__/ _` | |_) | | | | / __|");
        getLogger().info("|  _ <  __/ |_) | (_) | |  | |_| |_| | \\__ \\ (_| (_) | | | (_| |  __/| | |_| \\__ \\");
        getLogger().info("|_| \\_\\___| .__/ \\___/|_|   \\__|____/|_|___/\\___\\___/|_|  \\__,_|_|   |_|\\__,_|___/");
        getLogger().info("                                                                                           ");
        getLogger().info("                     Version: " + getDescription().getVersion());
        File pluginFolder = this.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        loadReportsConfig();
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        titleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("title"));
        subTitleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("subtitle"));
        this.discordNotifier = new DiscordNotifier(config, getLogger(), messages);
        this.staffNotifier = new StaffNotifier(titleText, subTitleText, messages, this);
        getProxy().getPluginManager().registerCommand(this, new VerifyReportCommand(this)); // blind
        getProxy().getPluginManager().registerCommand(this, new ReportCommand(this));
        getProxy().getPluginManager().registerListener(this, new PlayerLoginListener(this));
        getProxy().getPluginManager().registerCommand(this, new ReportCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReportListCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReportCloseCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReportReopenCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReportDeleteCommand(this));
        getProxy().getPluginManager().registerListener(this, new PlayerJoinListenerReports(this));
        String version = getConfig().getString("config_version");
        if (!version.equalsIgnoreCase("8")) {
            getLogger().severe("YOUR CONFIG IS NOT UPDATED, CHECK HERE: https://discord.gg/SGtHSCTaEX");
        }

    }

    public void onDisable() {
        getLogger().info("  ______   _______ ");
        getLogger().info(" | __ ) \\ / / ____|");
        getLogger().info(" |  _ \\\\ V /|  _|  ");
        getLogger().info(" | |_) || | | |___ ");
        getLogger().info(" |____/ |_| |_____|");
        getLogger().info("                   ");
        getLogger().info(" Version " + getDescription().getVersion());
    }

    public String replacePlaceholders(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void loadReportsConfig() {
        reportsFile = new File(getDataFolder(), "reports.yml");
        if (!reportsFile.exists()) {
            try {
                reportsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            reportsConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(reportsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Se non esiste un 'lastReportId', inizializzalo a 0
        if (!reportsConfig.contains("lastReportId")) {
            reportsConfig.set("lastReportId", 0);
            saveReportsConfig();
        }
    }

    // Metodo per ottenere il prossimo ID autoincrementato
    public int getNextReportId() {
        // Recupera l'ultimo ID usato
        int lastReportId = reportsConfig.getInt("lastReportId", 0);

        // Incrementa l'ID e aggiornalo nel file YAML
        int newReportId = lastReportId + 1;
        reportsConfig.set("lastReportId", newReportId);
        saveReportsConfig();

        return newReportId;
    }

    public void saveReportsConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(reportsConfig, reportsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Configuration getReportsConfig() {
        return reportsConfig;
    }

    private void loadConfig() throws IOException {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            }
        }

        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        loadMessages();
    }

    private void loadMessages() {
        this.messages = new HashMap<>();
        if (this.config.getSection("messages") != null) {
            Configuration messagesSection = this.config.getSection("messages");
            for (String key : messagesSection.getKeys()) {
                String message = messagesSection.getString(key);
                this.messages.put(key, ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    public Configuration getConfig() {
        return this.config;
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "");
    }

    public boolean isPlayerInBlacklist(ProxiedPlayer player) {
        return player.hasPermission("report.protection");
    }

    public void setCooldown(ProxiedPlayer player) {
        int cooldownSeconds = this.config.getInt("cooldown", 120);
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        this.cooldowns.put(player.getName(), cooldownTime);
    }

    public boolean hasCooldown(ProxiedPlayer player) {
        return (this.cooldowns.containsKey(player.getName()) && this.cooldowns.get(player.getName()) > System.currentTimeMillis());
    }

    public HashMap<String, Long> getCooldowns() {
        return this.cooldowns;
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }

    public StaffNotifier getStaffNotifier() {
        return staffNotifier;
    }
    public void checkForUpdates() {
        try {
            /*URL url = new URL(versionUrl);
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
*/          String latestVersion =  "7.1.5";
            String downloadUrl1 = "https://www.spigotmc.org/resources/%E2%AD%90-reportdiscordplus-%E2%AD%90.111055/";
            // Ottieni la versione attuale del plugin
            String currentVersion = this.getDescription().getVersion();

            // Controllo se è necessaria un'update
            if (!currentVersion.equals(latestVersion)) {
                // Leggi la lista di messaggi dalla configurazione
                List<String> updateMessages = getConfig().getStringList("updateMessage");

                if (updateMessages != null && !updateMessages.isEmpty()) {
                    // Invio dei messaggi di aggiornamento agli admin
                    for (ProxiedPlayer staffMember : ProxyServer.getInstance().getPlayers()) {
                        if (staffMember.hasPermission("report.admin")) {
                            for (String line : updateMessages) {
                                // Sostituiamo i placeholder
                                String formattedMessage = ChatColor.translateAlternateColorCodes('&',
                                        line.replace("{currentVersion}", currentVersion)
                                                .replace("{latestVersion}", latestVersion)
                                                .replace("{downloadUrl}", downloadUrl1)
                                );

                                // Creiamo una TextComponent da stringa legacy
                                TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(formattedMessage));

                                // Impostiamo un evento cliccabile sul link
                                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl1));

                                // Mandiamo il messaggio al giocatore
                                staffMember.sendMessage(textComponent);
                            }
                        }
                    }
                } else {
                    ProxyServer.getInstance().getLogger().warning("updateMessage non è configurato correttamente.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

