package me.francies.ReportDiscordPlus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.francies.ReportDiscordPlus.commands.ReportCommand;
import me.francies.ReportDiscordPlus.utility.DiscordNotifier;
import me.francies.ReportDiscordPlus.utility.PlayerLoginListener;
import me.francies.ReportDiscordPlus.utility.StaffNotifier;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;

public class ReportDiscordPlus extends Plugin {

    private Configuration config;
    private Map<String, String> messages;
    public HashMap<String, Long> cooldowns = new HashMap<>();
    public String pingRoleID;
    String titleText;
    String subTitleText;

    private DiscordNotifier discordNotifier;
    private StaffNotifier staffNotifier;
    private final String versionUrl = "https://www.franciesdev.it/api/reportdiscordplus.json";
    int updateInterval = getConfig().getInt("update-check-interval", 86400);

    public void onEnable() {
        checkForUpdates();
        int pluginId = 23259;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new SingleLineChart("players", () -> ProxyServer.getInstance().getOnlineCount()));
        getLogger().info("  +----------------------------------------------------------------------------------------------+");
        getLogger().info("  |   ____                       _   ____  _                       _ ____  _                     |");
        getLogger().info("  |  |  _ \\ ___ _ __   ___  _ __| |_|  _ \\(_)___  ___ ___  _ __ __| |  _ \\| |_   _ ___           |");
        getLogger().info("  |  | |_) / _ \\ '_ \\ / _ \\| '__| __| | | | / __|/ __/ _ \\| '__/ _` | |_) | | | | / __|          |");
        getLogger().info("  |  |  _ <  __/ |_) | (_) | |  | |_| |_| | \\__ \\ (_| (_) | | | (_| |  __/| | |_| \\__ \\          |");
        getLogger().info("  |  |_| \\_\\___| .__/ \\___/|_|   \\__|____/|_|___/\\___\\___/|_|  \\__,_|_|   |_|\\__,_|___/          |");
        getLogger().info("  |            |_|                                                                               |");
        getLogger().info("  |                                                                                              |");
        getLogger().info("  +----------------------------------------------------------------------------------------------+");
        getLogger().info("                                    Versione: 5.2");

        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        titleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("title"));
        subTitleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("subtitle"));

        this.discordNotifier = new DiscordNotifier(config, getLogger(), messages);
        this.staffNotifier = new StaffNotifier(titleText, subTitleText, messages);

        getProxy().getPluginManager().registerCommand(this, new ReportCommand(this));
        getProxy().getPluginManager().registerListener(this, new PlayerLoginListener(this));
    }

    public void onDisable() {
        getLogger().info("  ______   _______ ");
        getLogger().info(" | __ ) \\ / / ____|");
        getLogger().info(" |  _ \\\\ V /|  _|  ");
        getLogger().info(" | |_) || | | |___ ");
        getLogger().info(" |____/ |_| |_____|");
        getLogger().info("                   ");
        getLogger().info(" Version 5.2");
    }

    private void loadConfig() throws IOException {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File configFile = new File(getDataFolder(), "src/main/resources/config.yml");
        if (!configFile.exists()) {
            InputStream inputStream = getResourceAsStream("src/main/resources/config.yml");
            try {
                if (inputStream != null) {
                    Scanner scanner = new Scanner(inputStream);
                    try {
                        OutputStream outputStream = Files.newOutputStream(configFile.toPath());
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            outputStream.write(line.getBytes());
                            outputStream.write(System.lineSeparator().getBytes());
                        }
                        outputStream.close();
                        scanner.close();
                    } catch (Throwable throwable) {
                        scanner.close();
                        throw throwable;
                    }
                }
                if (inputStream != null)
                    inputStream.close();
            } catch (Throwable throwable) {
                inputStream.close();
                throw throwable;
            }
        }
        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        this.pingRoleID = this.config.getString("discord.pingRoleID");
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
        ProxyServer.getInstance().getScheduler().schedule(this, () -> {

            try {

                URL url = new URL(versionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                connection.disconnect();

                JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();

                String latestVersion = json.get("version").getAsString();
                String downloadUrl1 = json.get("downloadUrl1").getAsString();
                String downloadUrl2 = json.get("downloadUrl2").getAsString();

                String currentVersion = this.getDescription().getVersion();

                if (!currentVersion.equals(latestVersion)) {

                    for (ProxiedPlayer staffMember : ProxyServer.getInstance().getPlayers()) {
                            staffMember.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&c[&6REPORTDISCORDPLUS&c]" + " &eA newer version is available: &f" + latestVersion)));
                            staffMember.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&c[&6REPORTDISCORDPLUS&c]" + "&3Download link 1: &f" + downloadUrl1)));
                            staffMember.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&c[&6REPORTDISCORDPLUS&c]" + "&bDownload link 2: &f" + downloadUrl2)));
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, updateInterval, TimeUnit.SECONDS);
    }

}
