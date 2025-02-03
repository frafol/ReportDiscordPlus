package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
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
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (player.hasPermission("report.use")) {
                if (args.length == 0) {
                    player.sendMessage(plugin.getMessage("playerNotFound"));
                    return;
                }
                String reportedPlayerName = args[0];
                ProxiedPlayer reportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);
                if (reportedPlayer == null || !reportedPlayer.isConnected()) {
                    player.sendMessage(plugin.getMessage("onlineplayer"));
                    return;
                }

                if (reportedPlayer == sender) {
                    player.sendMessage(plugin.getMessage("myself"));
                    return;
                }
                String reason = "";
                if (args.length > 1) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                } else {
                    player.sendMessage(plugin.getMessage("missingReason"));
                    return;
                }

                // Get the min and max length from config.yml
                int minLength = plugin.getConfig().getInt("reason.minLength");
                int maxLength = plugin.getConfig().getInt("reason.maxLength");

                // Check if the reason is too short
                if (reason.length() < minLength) {
                    player.sendMessage(new TextComponent(plugin.getMessage("reasonTooShort")
                            .replace("{min}", String.valueOf(minLength))));
                    return;
                }

                // Check if the reason is too long
                if (reason.length() > maxLength) {
                    player.sendMessage(new TextComponent(plugin.getMessage("reasonTooLong")
                            .replace("{max}", String.valueOf(maxLength))));
                    return;
                }

                if (plugin.isPlayerInBlacklist(reportedPlayer)) {
                    player.sendMessage(plugin.getMessage("cannotReportPlayer"));
                    return;
                }

                if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
                    long cooldownTime = plugin.getCooldowns().get(player.getName());
                    long currentTime = System.currentTimeMillis();
                    long timeRemaining = cooldownTime - currentTime;
                    player.sendMessage(plugin.getMessage("cooldownMessage").replace("{timeRemaining}", String.valueOf(timeRemaining / 1000L)));
                    return;
                }

                String reporter = player.getName();
                String server = player.getServer().getInfo().getMotd();

                // Invio a Discord
                plugin.getDiscordNotifier().sendReportToDiscord(reporter, reportedPlayer.getName(), reason, server);

                // Notifica allo staff su Minecraft
                plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reportedPlayer.getName(), reason, server);

                // Salvataggio del report nel file YAML
                saveReportToYAML(reporter, reportedPlayer.getName(), reason, server);

                plugin.setCooldown(player);
                player.sendMessage(plugin.getMessage("reportSent"));
            } else {
                player.sendMessage(plugin.getMessage("noPermission"));
            }
        } else {
            sender.sendMessage(plugin.getMessage("consoleCommand"));
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
