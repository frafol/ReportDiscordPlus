package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ReportCommand extends Command {

    private final ReportDiscordPlus plugin;

    public ReportCommand(ReportDiscordPlus plugin) {
        super("report");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&',  plugin.getMessage("prefix"));
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (player.hasPermission("report.use")) {
                if (args.length == 0) {
                    player.sendMessage(plugin.getMessage("noPlayerMentioned").replace("{prefix}", prefix));
                    return;
                }
                String reportedPlayerName = args[0];
                ProxiedPlayer reportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);

                boolean allowOfflineReports = plugin.getConfig().getBoolean("allowOfflineReports", false);

                // Se il giocatore non è online e "allowOfflineReports" è false, blocchiamo il report
                if ((reportedPlayer == null || !reportedPlayer.isConnected()) && !allowOfflineReports) {
                    player.sendMessage(plugin.getMessage("onlinePlayer").replace("{prefix}", prefix));
                    return;
                }

                // Impediamo di segnalare sé stessi
                if (reportedPlayer != null && reportedPlayer == sender) {
                    player.sendMessage(plugin.getMessage("myself").replace("{prefix}", prefix));
                    return;
                }

                String reason = "";
                if (args.length > 1) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                } else {
                    player.sendMessage(plugin.getMessage("missingReason").replace("{prefix}", prefix));
                    return;
                }

                // Otteniamo la lunghezza minima e massima dal config.yml
                int minLength = plugin.getConfig().getInt("reason.minLength");
                int maxLength = plugin.getConfig().getInt("reason.maxLength");

                // Controlliamo se la motivazione è troppo corta
                if (reason.length() < minLength) {
                    player.sendMessage(new TextComponent(plugin.getMessage("reasonTooShort")
                            .replace("{min}", String.valueOf(minLength))
                            .replace("{prefix}", prefix)
                    ));
                    return;
                }

                // Controlliamo se la motivazione è troppo lunga
                if (reason.length() > maxLength) {
                    player.sendMessage(new TextComponent(plugin.getMessage("reasonTooLong")
                            .replace("{max}", String.valueOf(maxLength))
                            .replace("{prefix}", prefix)
                    ));
                    return;
                }

                // Controlliamo se il giocatore segnalato è in una blacklist
                if (reportedPlayer != null && plugin.isPlayerInBlacklist(reportedPlayer)) {
                    player.sendMessage(plugin.getMessage("cannotReportPlayer").replace("{prefix}", prefix));
                    return;
                }

                // Controllo cooldown per evitare spam di segnalazioni
                if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
                    long cooldownTime = plugin.getCooldowns().get(player.getName());
                    long currentTime = System.currentTimeMillis();
                    long timeRemaining = cooldownTime - currentTime;
                    player.sendMessage(plugin.getMessage("cooldownMessage")
                            .replace("{timeRemaining}", String.valueOf(timeRemaining / 1000L))
                            .replace("{prefix}", prefix));
                    return;
                }

                String reporter = player.getName();
                String reported = reportedPlayer != null ? reportedPlayer.getName() : reportedPlayerName;
                String server = player.getServer().getInfo().getMotd();

                // Invio a Discord
                if (plugin.getConfig().getBoolean("discord.enabled")) {
                    plugin.getDiscordNotifier().sendReportToDiscord(reporter, reported, reason, server);
                }
                // Notifica allo staff su Minecraft
                plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reported, reason, server);
                // Salvataggio del report nel file YAML
                saveReportToYAML(reporter, reported, reason, server);

                // Imposta il cooldown per il giocatore
                plugin.setCooldown(player);

                player.sendMessage(plugin.getMessage("reportSent").replace("{prefix}", prefix));
            } else {
                player.sendMessage(plugin.getMessage("noPermission").replace("{prefix}", prefix));
            }
        } else {
            sender.sendMessage(plugin.getMessage("consoleCommand").replace("{prefix}", prefix));
        }
    }

    private void saveReportToYAML(String reporter, String reported, String reason, String server) {
        try {
            // Carica il file YAML dei report
            File file = new File(plugin.getDataFolder(), "reports.yml");
            ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
            var config = provider.load(file);

            // Ottieni il prossimo ID autoincrementato
            int reportId = plugin.getNextReportId();

            // Aggiungi i dati del report nel file YAML
            config.set("reports." + reportId + ".reporter", reporter);
            config.set("reports." + reportId + ".reported", reported);
            config.set("reports." + reportId + ".reason", reason);
            config.set("reports." + reportId + ".server", server);
            config.set("reports." + reportId + ".status", "open");
            config.set("reports." + reportId + ".timestamp", System.currentTimeMillis());

            // Salva il file YAML
            provider.save(config, file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
