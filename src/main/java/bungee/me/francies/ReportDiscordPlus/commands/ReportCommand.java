package bungee.me.francies.ReportDiscordPlus.commands;


import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

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
                plugin.getDiscordNotifier().sendReportToDiscord(reporter, reportedPlayer.getName(), reason, server);
                plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reportedPlayer.getName(), reason, server);
                plugin.setCooldown(player);
                player.sendMessage(plugin.getMessage("reportSent"));
            } else {
                player.sendMessage(plugin.getMessage("noPermission"));
            }
        } else {
            sender.sendMessage(plugin.getMessage("consoleCommand"));
        }
    }
}
