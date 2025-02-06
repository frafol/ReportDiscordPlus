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
import java.util.Optional;

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
            invocation.source().sendMessage(Component.text("Only player can execute this command."));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (!player.hasPermission("report.use")) {
            player.sendMessage(messageManager.getComponentMessage("messages.noPermission", null));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(messageManager.getComponentMessage("messages.noPlayerMentioned", null));
            return;
        }

        String reportedPlayerName = args[0];
        Optional<Player> reportedPlayerOptional = plugin.getProxy().getPlayer(reportedPlayerName);
        boolean allowOfflineReports = plugin.getConfig().node("allowOfflineReports").getBoolean(false);

        // Se il giocatore non è online e "allowOfflineReports" è false, blocchiamo il report
        if (reportedPlayerOptional.isEmpty() && !allowOfflineReports) {
            player.sendMessage(messageManager.getComponentMessage("messages.onlinePlayer", null));
            return;
        }

        Player reportedPlayer = reportedPlayerOptional.orElse(null);

        // Impediamo di segnalare sé stessi se il giocatore è online
        if (reportedPlayer != null && reportedPlayer.equals(player)) {
            player.sendMessage(messageManager.getComponentMessage("messages.myself", null));
            return;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        if (reason == null) {
            player.sendMessage(messageManager.getComponentMessage("messages.missingReason", null));
            return;
        }

        // Otteniamo la lunghezza minima e massima dal config
        int minLength = plugin.getMinReasonLength();
        int maxLength = plugin.getMaxReasonLength();

        if (reason.length() < minLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min", String.valueOf(minLength));
            player.sendMessage(messageManager.getComponentMessage("messages.reasonTooShort", placeholders));
            return;
        }

        if (reason.length() > maxLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(maxLength));
            player.sendMessage(messageManager.getComponentMessage("messages.reasonTooLong", placeholders));
            return;
        }

        if (reportedPlayer != null && plugin.isPlayerInBlacklist(reportedPlayer)) {
            player.sendMessage(messageManager.getComponentMessage("messages.cannotReportPlayer", null));
            return;
        }

        if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
            long cooldownTime = plugin.getCooldowns().get(player.getUsername());
            long currentTime = System.currentTimeMillis();
            long timeRemaining = (cooldownTime - currentTime) / 1000;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("timeRemaining", String.valueOf(timeRemaining));
            player.sendMessage(messageManager.getComponentMessage("messages.cooldownMessage", placeholders));
            return;
        }

        String reporter = player.getUsername();
        String reported = reportedPlayer != null ? reportedPlayer.getUsername() : reportedPlayerName;
        String server = reportedPlayer.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown");

        // Invia il report a Discord se abilitato
        if (plugin.getConfig().node("discord.enabled").getBoolean()) {
            plugin.getDiscordNotifier().sendReportToDiscord(reporter, reported, reason, server);
        }

        try {
            plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reported, reason, server);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        try {
            saveReportToYAML(reporter, reported, reason, server);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        plugin.setCooldown(player);
        player.sendMessage(messageManager.getComponentMessage("messages.reportSent", null));
    }

    private void saveReportToYAML(String reporter, String reported, String reason, String server) throws SerializationException {
        int reportId = plugin.getNextReportId();

        ConfigurationNode reportsNode = plugin.getReportsConfig().node("reports");
        if (reportsNode.isList()) {
            reportsNode.set(null);  // Resetta se è una lista, lo vogliamo come mappa
        }

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
