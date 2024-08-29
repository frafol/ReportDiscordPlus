package me.francies.ReportDiscordPlus;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import me.francies.ReportDiscordPlus.commands.ReportCommand;
import me.francies.ReportDiscordPlus.utility.DiscordNotifier;
import me.francies.ReportDiscordPlus.utility.StaffNotifier;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class ReportDiscordPlus extends Plugin {

    private Configuration config;
    private Map<String, String> messages;
    public HashMap<String, Long> cooldowns = new HashMap<>();
    public String pingRoleID;
    String titleText;
    String subTitleText;

    private DiscordNotifier discordNotifier;
    private StaffNotifier staffNotifier;

    public void onEnable() {
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
        getLogger().info("REPORT ATTIVI");
    }

    public void onDisable() {
        getLogger().info("SPEGNIMENTO REPORT");
    }

    private void loadConfig() throws IOException {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            InputStream inputStream = getResourceAsStream("config.yml");
            try {
                if (inputStream != null) {
                    Scanner scanner = new Scanner(inputStream);
                    try {
                        OutputStream outputStream = new FileOutputStream(configFile);
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
                if (inputStream != null)
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
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
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
}
