package velocity.dev.francies.reportdiscordplus.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.francies.reportdiscordplus.utility.CooldownManager;
import net.kyori.adventure.text.Component;
import velocity.dev.francies.reportdiscordplus.ReportDiscordPlus;
import velocity.dev.francies.reportdiscordplus.config.MessageManager;
import velocity.dev.francies.reportdiscordplus.report.ReportManager;

import java.util.Arrays;
import java.util.Optional;

public class ReportCommand implements SimpleCommand {

    private final ReportManager reportManager;
    private final MessageManager messageManager;
    private final CooldownManager cooldownManager;
    private final ReportDiscordPlus plugin;

    public ReportCommand(ReportManager reportManager, MessageManager messageManager, CooldownManager cooldownManager, ReportDiscordPlus plugin) {
        this.reportManager = reportManager;
        this.messageManager = messageManager;
        this.cooldownManager = cooldownManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player) {
            Player player = (Player) source;
            if (player.hasPermission("report.use")) {
                if (args.length == 0) {
                    player.sendMessage(Component.text(messageManager.getMessage("playerNotFound")));
                    return;
                }
                String reportedPlayerName = args[0];
                Optional<Player> reportedPlayerOpt = plugin.getProxyServer().getPlayer(reportedPlayerName);
                if (!reportedPlayerOpt.isPresent() || !reportedPlayerOpt.get().isActive()) {
                    player.sendMessage(Component.text(messageManager.getMessage("onlineplayer")));
                    return;
                }

                Player reportedPlayer = reportedPlayerOpt.get();
                if (reportedPlayer == player) {
                    player.sendMessage(Component.text(messageManager.getMessage("myself")));
                    return;
                }

                String reason = "";
                if (args.length > 1) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                } else {
                    player.sendMessage(Component.text(messageManager.getMessage("missingReason")));
                    return;
                }

                if (reportManager.isPlayerInBlacklist(reportedPlayer)) {
                    player.sendMessage(Component.text(messageManager.getMessage("cannotReportPlayer")));
                    return;
                }

                if (cooldownManager.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
                    long timeRemaining = cooldownManager.getCooldownRemaining(player);
                    player.sendMessage(Component.text(messageManager.getMessage("cooldownMessage").replace("{timeRemaining}", String.valueOf(timeRemaining))));
                    return;
                }

                String reporter = player.getUsername();
                String serverName = player.getCurrentServer().get().getServerInfo().getName();
                reportManager.sendReportToDiscord(reporter, reportedPlayer.getUsername(), reason, serverName);
                reportManager.sendReportToMinecraftStaff(player, reportedPlayer.getUsername(), reason, serverName);
                cooldownManager.setCooldown(player);
                player.sendMessage(Component.text(messageManager.getMessage("reportSent")));
            } else {
                player.sendMessage(Component.text(messageManager.getMessage("noPermission")));
            }
        } else {
            source.sendMessage(Component.text(messageManager.getMessage("consoleCommand")));
        }
    }
}
