package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

        /* ───── Controllo: solo un giocatore può eseguire il comando ───── */
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only player can execute this command."));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        /* ───── Permessi ───── */
        if (!player.hasPermission("report.use")) {
            player.sendMessage(messageManager.getComponentMessage("messages.noPermission", null));
            return;
        }

        /* ───── Sintassi /report <player> [reason...] ───── */
        if (args.length == 0) {
            player.sendMessage(messageManager.getComponentMessage("messages.noPlayerMentioned", null));
            return;
        }

        /* ────────────────────────────────────────────────────────────────
           SERVER IGNORATI
           Se il server corrente del giocatore è in ignored-servers
           blocchiamo il comando e mostriamo "messages.cannotReportHere"
        ──────────────────────────────────────────────────────────────── */
        String reporterServer = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName()) // ServerConnection → ServerInfo → nome
                .orElse("Unknown");

        List<String> ignoredServers = null;
        try {
            ignoredServers = plugin.getConfig()
                    .node("ignored-servers")
                    .getList(String.class, new ArrayList<>());
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        if (ignoredServers.contains(reporterServer)) {
            player.sendMessage(messageManager.getComponentMessage("messages.cannotReportHere", null));
            return;
        }

        /* ───── Recuperiamo il giocatore segnalato ───── */
        String reportedPlayerName = args[0];
        Optional<Player> reportedPlayerOptional = plugin.getProxy().getPlayer(reportedPlayerName);
        boolean allowOfflineReports = plugin.getConfig().node("allowOfflineReports").getBoolean(false);

        if (reportedPlayerOptional.isEmpty() && !allowOfflineReports) {
            player.sendMessage(messageManager.getComponentMessage("messages.onlinePlayer", null));
            return;
        }

        Player reportedPlayer = reportedPlayerOptional.orElse(null);

        /* ───── Evitiamo che un giocatore segnali sé stesso ───── */
        if (reportedPlayer != null && reportedPlayer.equals(player)) {
            player.sendMessage(messageManager.getComponentMessage("messages.myself", null));
            return;
        }

        /* ───── Motivazione ───── */
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        if (reason == null) {
            player.sendMessage(messageManager.getComponentMessage("messages.missingReason", null));
            return;
        }

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

        /* ───── Blacklist ───── */
        if (reportedPlayer != null && plugin.isPlayerInBlacklist(reportedPlayer)) {
            player.sendMessage(messageManager.getComponentMessage("messages.cannotReportPlayer", null));
            return;
        }

        /* ───── Cool-down ───── */
        if (plugin.hasCooldown(player) && !player.hasPermission("report.bypasscooldown")) {
            long cooldownTime = plugin.getCooldowns().get(player.getUsername());
            long currentTime = System.currentTimeMillis();
            long timeRemaining = (cooldownTime - currentTime) / 1000;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("timeRemaining", String.valueOf(timeRemaining));
            player.sendMessage(messageManager.getComponentMessage("messages.cooldownMessage", placeholders));
            return;
        }

        /* ───── Costruzione dati report ───── */
        String reporter = player.getUsername();
        String reported = reportedPlayer != null ? reportedPlayer.getUsername() : reportedPlayerName;
        String targetServer = (reportedPlayer != null && reportedPlayer.getCurrentServer().isPresent())
                ? reportedPlayer.getCurrentServer().get().getServerInfo().getName()
                : "Offline";

        /* ───── Notifiche Discord / Staff ───── */
        boolean discordEnabled = plugin.getConfig().node("discord", "enabled").getBoolean(false);
        if (discordEnabled) {
            plugin.getDiscordNotifier().sendReportToDiscord(reporter, reported, reason, targetServer);
        }

        try {
            plugin.getStaffNotifier().sendReportToMinecraftStaff(player, reported, reason, targetServer);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        /* ───── Salvataggio YAML ───── */
        try {
            saveReportToYAML(reporter, reported, reason, targetServer);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        plugin.setCooldown(player);
        player.sendMessage(messageManager.getComponentMessage("messages.reportSent", null));
    }

    private void saveReportToYAML(String reporter, String reported, String reason, String server)
            throws SerializationException {

        int reportId = plugin.getNextReportId();

        ConfigurationNode reportsNode = plugin.getReportsConfig().node("reports");
        if (reportsNode.isList()) {
            reportsNode.set(null); // resetta se era lista: serve come mappa
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
