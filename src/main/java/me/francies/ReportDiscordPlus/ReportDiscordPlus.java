package me.francies.ReportDiscordPlus;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;



public class ReportDiscordPlus extends Plugin {
    private Configuration config;

    private Map<String, String> messages;

    public HashMap<String, Long> cooldowns = new HashMap<>();

    public String pingRoleID;

    public List<String> blacklist;

    public JDAManager jdaManager;
    String titleText ;
    String subTitleText;
    private DiscordBot discordBot;

    public void onEnable() {
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        discordBot = DiscordBot.getInstance(config.getString("discord.token"));
        discordBot.startBot();
        getProxy().getPluginManager().registerCommand(this, new ReportCommand());
        this.jdaManager = new JDAManager(this);
        this.jdaManager.setupJDA();
        getLogger().info("The ReportDiscordPlus plugin has been enabled!");

         titleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("title"));
         subTitleText = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("subtitle"));

    }

    public void onDisable() {
        getLogger().info("The ReportDiscordPlus plugin has been disabled!");
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
                        try {
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                outputStream.write(line.getBytes());
                                outputStream.write(System.lineSeparator().getBytes());
                            }
                            outputStream.close();
                        } catch (Throwable throwable) {
                            try {
                                outputStream.close();
                            } catch (Throwable throwable1) {
                                throwable.addSuppressed(throwable1);
                            }
                            throw throwable;
                        }
                        scanner.close();
                    } catch (Throwable throwable) {
                        try {
                            scanner.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                        throw throwable;
                    }
                }
                if (inputStream != null)
                    inputStream.close();
            } catch (Throwable throwable) {
                if (inputStream != null)
                    try {
                        inputStream.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
        }
        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        this.pingRoleID = this.config.getString("discord.pingRoleID");
        this.blacklist = this.config.getStringList("blacklist");
        loadMessages();
    }

    private void loadMessages() {
        this.messages = new HashMap<>();
        if (this.config.getSection("messages") != null) {
            Configuration messagesSection = this.config.getSection("messages");
            Set<String> keys = (Set<String>)messagesSection.getKeys();
            for (String key : keys) {
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

    public boolean isPlayerInBlacklist(String playerName) {

        for (String blacklistedPlayer : this.blacklist) {
            if (blacklistedPlayer.equalsIgnoreCase(playerName))
                return true;
        }
        return false;
    }

    public void setCooldown(ProxiedPlayer player) {
        int cooldownSeconds = this.config.getInt("cooldown", 120);
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
        this.cooldowns.put(player.getName(), cooldownTime);
    }

    public boolean hasCooldown(ProxiedPlayer player) {
        return (this.cooldowns.containsKey(player.getName()) && (Long) this.cooldowns.get(player.getName()) > System.currentTimeMillis());
    }

    public HashMap<String, Long> getCooldowns() {
        return this.cooldowns;
    }

    public class ReportCommand extends Command {
        public ReportCommand() {
            super("report");
        }

        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer)sender;
                if (player.hasPermission("report.use")) {
                    if (args.length == 0) {
                        player.sendMessage(ReportDiscordPlus.this.getMessage("playerNotFound"));
                        return;
                    }
                    String reportedPlayerName = args[0];
                    ProxiedPlayer reportedPlayer = ReportDiscordPlus.this.getProxy().getPlayer(reportedPlayerName);
                    if (reportedPlayer == null || !reportedPlayer.isConnected()) {
                        String NoON = ReportDiscordPlus.this.config.getString("messages.onlineplayer");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', NoON));
                        return;
                    }

                    if (reportedPlayer == sender){
                        String myself = ReportDiscordPlus.this.config.getString("messages.myself");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', myself));
                        return;
                    }
                    String reason = "";
                    if (args.length > 1) {
                        reason = String.join(" ", Arrays.<CharSequence>copyOfRange((CharSequence[])args, 1, args.length));
                    } else {
                        player.sendMessage(ReportDiscordPlus.this.getMessage("missingReason"));
                        return;
                    }
                    if (ReportDiscordPlus.this.isPlayerInBlacklist(reportedPlayer.getName())) {
                        player.sendMessage(ReportDiscordPlus.this.getMessage("cannotReportPlayer"));
                        return;
                    }
                    if (ReportDiscordPlus.this.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
                        long cooldownTime = ((Long)ReportDiscordPlus.this.getCooldowns().get(player.getName())).longValue();
                        long currentTime = System.currentTimeMillis();
                        long timeRemaining = cooldownTime - currentTime;
                        player.sendMessage(ReportDiscordPlus.this.getMessage("cooldownMessage").replace("{timeRemaining}", String.valueOf(timeRemaining / 1000L)));
                        return;
                    }
                    String reporter = player.getName();
                    String server = player.getServer().getInfo().getMotd();
                    ReportDiscordPlus.this.sendReportToDiscord(reporter, reportedPlayer.getName(), reason, server);
                    ReportDiscordPlus.this.sendReportToMinecraftStaff(player, reportedPlayer.getName(), reason, server);
                    ReportDiscordPlus.this.setCooldown(player);
                    String confirmationMessage = ReportDiscordPlus.this.getMessage("reportSent");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', confirmationMessage));
                } else {
                    player.sendMessage(ReportDiscordPlus.this.getMessage("noPermission"));
                }
            } else {
                sender.sendMessage(ReportDiscordPlus.this.getMessage("consoleCommand"));
            }
        }
    }

    private void sendReportToMinecraftStaff(ProxiedPlayer reporter, String reportedPlayer, String reason, String server) {
        String teleportCommand = "/tp " + reportedPlayer;
        String sendstaff = "/" + server;
        TextComponent sendButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &eSERVER"));
        TextComponent teleportButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &eTELEPORT"));
        teleportButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (BaseComponent[])new TextComponent[] { new TextComponent("Vai dal giocatore segnalato") }));
        sendButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (BaseComponent[])new TextComponent[] { new TextComponent("Vai nel server del giocatore segnalato") }));
        teleportButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand));
        sendButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, sendstaff));
        String staffMessage = getMessage("staffAlert").replace("{player}", reporter.getName()).replace("{reportedPlayer}", reportedPlayer).replace("{reason}", reason).replace("{server}", server).replace("{teleportButton}", "Vai dal giocatore segnalato");

        for (ProxiedPlayer staffMember : ProxyServer.getInstance().getPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {
                TextComponent messageComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', staffMessage));
                if (staffMember.hasPermission("report.tp")) {
                    messageComponent.addExtra((BaseComponent)teleportButton);
                    messageComponent.addExtra((BaseComponent)sendButton);
                    sendTitleToPlayer(staffMember,  createTitle(titleText, subTitleText, 10, 70, 20 ));

                }
                staffMember.sendMessage((BaseComponent)messageComponent);
            }
        }
    }

    public void sendReportToDiscord(String reporter, String reportedPlayer, String reason, String server) {
        String discordChannelId = this.config.getString("discord.channelId");
        if (this.jdaManager.getJDA() != null && discordChannelId != null && this.pingRoleID != null) {
            TextChannel channel = this.jdaManager.getJDA().getTextChannelById(discordChannelId);
            Role pingRole = channel.getGuild().getRoleById(this.pingRoleID);
            String allert = this.config.getString("discord.allert");
            String title = this.config.getString("discord.title");
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(title)
                    .setColor(Color.RED)
                    .addField("Reporter", "🟢 " + reporter, false)
                    .addField("Reported", "🔴 " + reportedPlayer, false)
                    .addField("Reason", "💬 " + reason, false)
                    .addField("Server", "💻 " + server, false);

            String emojiString = this.config.getString("discord.reaction");
            UnicodeEmoji unicodeEmoji = Emoji.fromUnicode(emojiString);
            channel.sendMessageEmbeds(embedBuilder.build(), new net.dv8tion.jda.api.entities.MessageEmbed[0]).queue(response -> response.addReaction(unicodeEmoji).queue());
            if (pingRole != null) {
                String messageContent = pingRole.getAsMention();
                channel.sendMessage(messageContent + allert).queue();
            } else {
                getLogger().warning("The configured ping role ID is invalid or the bot does not have access to that role.");
            }
        } else {
            getLogger().warning("The config.yml was not set correctly. Check the channel ID, ping role ID, and bot token!");
        }
    }

    public Title createTitle(String titleText, String subTitleText, int fadeIn, int stay, int fadeOut) {
        Title title = ProxyServer.getInstance().createTitle();

        // Imposta il titolo
        BaseComponent titleComponent = new TextComponent(titleText);
        title.title(titleComponent);

        // Imposta il sottotitolo
        BaseComponent subTitleComponent = new TextComponent(subTitleText);
        title.subTitle(subTitleComponent);

        // Imposta i tempi di animazione
        title.fadeIn(fadeIn);
        title.stay(stay);
        title.fadeOut(fadeOut);

        return title;
    }
    public void sendTitleToPlayer(ProxiedPlayer player, Title title) {
        // Invia il titolo al giocatore
        title.send(player);
    }


}
