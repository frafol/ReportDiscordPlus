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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportCommand extends Command {

    private final ReportDiscordPlus plugin;

    public ReportCommand(ReportDiscordPlus plugin) {
        super("report");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getMessage("prefix"));

        /* ───── Controllo che il sender sia un giocatore ───── */
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(plugin.getMessage("consoleCommand").replace("{prefix}", prefix));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        /* ───── Permesso report.use ───── */
        if (!player.hasPermission("report.use")) {
            player.sendMessage(plugin.getMessage("noPermission").replace("{prefix}", prefix));
            return;
        }

        /* ───────────────────────────────────────────────────────────────────────
           SERVER IGNORATI
           Se il server da cui il giocatore esegue il comando è contenuto
           in ignored-servers, blocchiamo il comando e inviamo cannotReportHere
        ─────────────────────────────────────────────────────────────────────── */
        String playerServerName = player.getServer() != null
                ? player.getServer().getInfo().getName()
                : "Unknown";

        List<String> ignoredServers = plugin.getConfig().getStringList("ignored-servers");
        if (ignoredServers == null) ignoredServers = new ArrayList<>();

        if (ignoredServers.contains(playerServerName)) {
            player.sendMessage(plugin.getMessage("cannotReportHere").replace("{prefix}", prefix));
            return;
        }
        /* ───────────────────────────────────────────────────────────────────── */

        /* ───── Sintassi /report <giocatore> [motivo…] ───── */
        if (args.length == 0) {
            player.sendMessage(plugin.getMessage("noPlayerMentioned").replace("{prefix}", prefix));
            return;
        }

        String reportedPlayerName = args[0];
        ProxiedPlayer reportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);

        boolean allowOfflineReports = plugin.getConfig().getBoolean("allowOfflineReports", false);

        // Se il target è offline e allowOfflineReports è false → blocco
        if ((reportedPlayer == null || !reportedPlayer.isConnected()) && !allowOfflineReports) {
            player.sendMessage(plugin.getMessage("onlinePlayer").replace("{prefix}", prefix));
            return;
        }

        // Impediamo di segnalare sé stessi
        if (reportedPlayer != null && reportedPlayer == player) {
            player.sendMessage(plugin.getMessage("myself").replace("{prefix}", prefix));
            return;
        }

        /* ───── Motivazione ───── */
        String reason;
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        } else {
            player.sendMessage(plugin.getMessage("missingReason").replace("{prefix}", prefix));
            return;
        }

        // Lunghezza minima/massima dal config.yml
        int minLength = plugin.getConfig().getInt("reason.minLength");
        int maxLength = plugin.getConfig().getInt("reason.maxLength");

        if (reason.length() < minLength) {
            player.sendMessage(new TextComponent(plugin.getMessage("reasonTooShort")
                    .replace("{min}", String.valueOf(minLength))
                    .replace("{prefix}", prefix)));
            return;
        }

        if (reason.length() > maxLength) {
            player.sendMessage(new TextComponent(plugin.getMessage("reasonTooLong")
                    .replace("{max}", String.valueOf(maxLength))
                    .replace("{prefix}", prefix)));
            return;
        }

        // Black-list dei giocatori
        if (reportedPlayer != null && plugin.isPlayerInBlacklist(reportedPlayer)) {
            player.sendMessage(plugin.getMessage("cannotReportPlayer").replace("{prefix}", prefix));
            return;
        }

        // Cool-down anti-spam
        if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
            long cooldownTime = plugin.getCooldowns().get(player.getName());
            long currentTime = System.currentTimeMillis();
            long timeRemaining = (cooldownTime - currentTime) / 1000L;
            player.sendMessage(plugin.getMessage("cooldownMessage")
                    .replace("{timeRemaining}", String.valueOf(timeRemaining))
                    .replace("{prefix}", prefix));
            return;
        }

        /* ───── Costruzione dati report ───── */
        String reporter = player.getName();
        String reported = reportedPlayer != null ? reportedPlayer.getName() : reportedPlayerName;
        String server = playerServerName; // ora usiamo il nome, non la MOTD

        /* ───── Notifiche ───── */
        if (plugin.getConfig().getBoolean("discord.enabled")) {
            plugin.getDiscordNotifier().sendReportToDiscord(reporter, reported, reason, server);
        }

        plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reported, reason, server);
        saveReportToYAML(reporter, reported, reason, server);

        // Impostiamo il cooldown
        plugin.setCooldown(player);

        player.sendMessage(plugin.getMessage("reportSent").replace("{prefix}", prefix));
    }

    /* ────────────────────────────────────────────────────────────────────────────
       Salvataggio su reports.yml
    ──────────────────────────────────────────────────────────────────────────── */
    private void saveReportToYAML(String reporter, String reported, String reason, String server) {
        try {
            File file = new File(plugin.getDataFolder(), "reports.yml");
            ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
            var config = provider.load(file);

            int reportId = plugin.getNextReportId();

            config.set("reports." + reportId + ".reporter", reporter);
            config.set("reports." + reportId + ".reported", reported);
            config.set("reports." + reportId + ".reason", reason);
            config.set("reports." + reportId + ".server", server);
            config.set("reports." + reportId + ".status", "open");
            config.set("reports." + reportId + ".timestamp", System.currentTimeMillis());

            provider.save(config, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
