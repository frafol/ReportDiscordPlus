package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReportCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public ReportCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only player can do this command."));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (!player.hasPermission("report.use")) {
            Component noPermissionMessage = messageManager.getComponentMessage("messages.noPermission", null);
            player.sendMessage(noPermissionMessage);
            return;
        }

        if (args.length == 0) {
            Component playerNotFoundMessage = messageManager.getComponentMessage("messages.playerNotFound", null);
            player.sendMessage(playerNotFoundMessage);
            return;
        }

        String reportedPlayerName = args[0];
        Player reportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName).orElse(null);

        if (reportedPlayer == null || !reportedPlayer.isActive()) {
            Component onlinePlayerMessage = messageManager.getComponentMessage("messages.onlineplayer", null);
            player.sendMessage(onlinePlayerMessage);
            return;
        }

        if (reportedPlayer.equals(player)) {
            Component selfReportMessage = messageManager.getComponentMessage("messages.myself", null);
            player.sendMessage(selfReportMessage);
            return;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        if (reason == null) {
            Component missingReasonMessage = messageManager.getComponentMessage("messages.missingReason", null);
            player.sendMessage(missingReasonMessage);
            return;
        }

        // Get the minimum and maximum length from the main class
        int minLength = plugin.getMinReasonLength();
        int maxLength = plugin.getMaxReasonLength();

        // Check if the reason is too short
        if (reason.length() < minLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min", String.valueOf(minLength));
            Component reasonTooShortMessage = messageManager.getComponentMessage("messages.reasonTooShort", placeholders);
            player.sendMessage(reasonTooShortMessage);
            return;
        }

        // Check if the reason is too long
        if (reason.length() > maxLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(maxLength));
            Component reasonTooLongMessage = messageManager.getComponentMessage("messages.reasonTooLong", placeholders);
            player.sendMessage(reasonTooLongMessage);
            return;
        }

        if (plugin.isPlayerInBlacklist(reportedPlayer)) {
            Component cannotReportPlayerMessage = messageManager.getComponentMessage("messages.cannotReportPlayer", null);
            player.sendMessage(cannotReportPlayerMessage);
            return;
        }

        if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
            long cooldownTime = plugin.getCooldowns().get(player.getUsername());
            long currentTime = System.currentTimeMillis();
            long timeRemaining = (cooldownTime - currentTime) / 1000;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("timeRemaining", String.valueOf(timeRemaining));
            Component cooldownMessage = messageManager.getComponentMessage("messages.cooldownMessage", placeholders);
            player.sendMessage(cooldownMessage);
            return;
        }

        String reporter = player.getUsername();
        String server = player.getCurrentServer().get().getServerInfo().getName();

        plugin.getDiscordNotifier().sendReportToDiscord(reporter, reportedPlayer.getUsername(), reason, server);
        plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reportedPlayer.getUsername(), reason, server);
        try {
            saveReportToYAML(reporter, reportedPlayer.getUsername(), reason, server);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        plugin.setCooldown(player);
        Component reportSentMessage = messageManager.getComponentMessage("messages.reportSent", null);
        player.sendMessage(reportSentMessage);
    }


    private void saveReportToYAML(String reporter, String reported, String reason, String server) throws SerializationException {
        int reportId = plugin.getNextReportId();

        // Assicurati che il nodo "reports" sia una mappa
        ConfigurationNode reportsNode = plugin.getReportsConfig().node("reports");
        if (reportsNode.isList()) {
            reportsNode.set(null);  // Resetta se è una lista, lo vogliamo come mappa
        }

        // Imposta i valori nel nodo del report
        ConfigurationNode reportNode = reportsNode.node(String.valueOf(reportId));
        reportNode.node("reporter").set(reporter);
        reportNode.node("reported").set(reported);
        reportNode.node("reason").set(reason);
        reportNode.node("server").set(server);
        reportNode.node("status").set("open");
        reportNode.node("timestamp").set(System.currentTimeMillis());

        plugin.saveReportsConfig();
    }
}
