package velocity.me.francies.ReportDiscordPlus.utility;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;

import java.time.Duration;
import java.util.*;

public class StaffNotifier implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;
    private final String titleText;
    private final String subTitleText;

    /**
     * Costruttore unico che riceve i parametri necessari:
     * - plugin (per accedere a Proxy e config)
     * - messageManager (per i messaggi)
     * - eventuali testi del titolo e sottotitolo
     */
    public StaffNotifier(ReportDiscordPlus plugin, MessageManager messageManager, String titleText, String subTitleText) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.titleText = titleText;
        this.subTitleText = subTitleText;

        // Registriamo i comandi "teleportServer" e "teleportPlayer" direttamente QUI
        ProxyServer proxy = plugin.getProxy();
        proxy.getCommandManager().register("teleportServer", this);   // /teleportServer
        proxy.getCommandManager().register("teleportPlayer", this);   // /teleportPlayer
    }

    /**
     * 1) COSTRUISCE I PULSANTI CLICCABILI (RUN COMMAND)
     */
    private Component buildTeleportButtons(String serverName, String reportedPlayerName) {

        // Lettura dal config
        String showTextServerButton = plugin.getConfig()
                .node("button", "show_text_server_button")
                .getString("&bTeleport to the server");
        String teleportToServerText = plugin.getConfig()
                .node("button", "teleport_to_server")
                .getString("&6TP-SERVER");

        String showTextTpButton = plugin.getConfig()
                .node("button", "show_text_tp_button")
                .getString("&bTeleport to the reported player");
        String teleportToPlayerText = plugin.getConfig()
                .node("button", "teleport_to_player")
                .getString("&aTP-PLAYER");

        String separator = plugin.getConfig()
                .node("button", "separator")
                .getString("&6&l-");

        // Pulsante "TP-SERVER" con hover e click -> /teleportServer <serverName>
        Component serverButton = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(teleportToServerText)
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(showTextServerButton)
                ))
                // Esegue il comando "/teleportServer <nomeServer>"
                .clickEvent(ClickEvent.runCommand("/teleportServer " + serverName));

        // Pulsante "TP-PLAYER" con hover e click -> /teleportPlayer <playerName> <serverName>
        Component playerButton = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(teleportToPlayerText)
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(showTextTpButton)
                ))
                // Esegue il comando "/teleportPlayer <nomeGiocatore> <serverName>"
                .clickEvent(ClickEvent.runCommand("/teleportPlayer " + reportedPlayerName + " " + serverName));

        // Separatore
        Component separatorComponent = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" " + separator + " ");

        // Uniamo i pulsanti con il separatore
        return serverButton.append(separatorComponent).append(playerButton);
    }

    /**
     * 2) INVIA LO STAFF ALERT + I PULSANTI
     */
    public void sendReportToMinecraftStaff(Player reporter, String reportedPlayer, String reason, String server) throws SerializationException {

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", reporter.getUsername());
        placeholders.put("reportedPlayer", reportedPlayer);
        placeholders.put("reason", reason);
        placeholders.put("server", server);

        // Messaggi multilinea da config
        List<String> staffMessageList = messageManager.getRawMessageList("messages.staffAlert");
        if (staffMessageList == null || staffMessageList.isEmpty()) {
            reporter.sendMessage(
                    LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(ChatColor.RED + "Staff alert message not found in configuration.")
            );
            return;
        }

        // Sostituiamo i placeholder
        StringBuilder staffMessageBuilder = new StringBuilder();
        for (String line : staffMessageList) {
            String formattedLine = messageManager.replacePlaceholders(line, placeholders);
            staffMessageBuilder.append(formattedLine).append("\n");
        }
        Component staffMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(staffMessageBuilder.toString());

        // Inviamo a tutti gli staffer con permesso
        for (Player staffMember : plugin.getProxy().getAllPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {

                // Titolo e sottotitolo
                String titleMsg = messageManager.replacePlaceholders(titleText, placeholders);
                String subtitleMsg = messageManager.replacePlaceholders(subTitleText, placeholders);

                Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1));
                Title title = Title.title(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(titleMsg),
                        LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleMsg),
                        times
                );
                staffMember.showTitle(title);

                // Messaggio testuale
                staffMember.sendMessage(staffMessage);

                // Pulsanti cliccabili (TP-SERVER / TP-PLAYER)
                Component buttonMessage = buildTeleportButtons(server, reportedPlayer);
                staffMember.sendMessage(buttonMessage);
            }
        }
    }

    /**
     * 3) TELETRASPORTA STAFFER SU UN CERTO SERVER
     */
    public void teleportToServer(Player staffMember, String serverName) {
        Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(serverName);
        if (!targetServer.isPresent()) {
            Component serverNotFoundMessage = messageManager.getComponentMessage("messages.serverNotFound", null);
            staffMember.sendMessage(serverNotFoundMessage);
            return;
        }

        String currentServerName = staffMember.getCurrentServer().get().getServerInfo().getName();
        if (!currentServerName.equals(serverName)) {
            staffMember.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    Component successMessage = messageManager.getComponentMessage("messages.connectedToServer", null);
                    staffMember.sendMessage(successMessage);
                } else {
                    Component errorMessage = messageManager.getComponentMessage("messages.couldNotConnect", null);
                    staffMember.sendMessage(errorMessage);
                }
            });
        } else {
            Component alreadyOnServerMessage = messageManager.getComponentMessage("messages.alreadyOnServer", null);
            staffMember.sendMessage(alreadyOnServerMessage);
        }
    }

    /**
     * 4) TELETRASPORTA STAFFER AD UN PLAYER, EVENTUALMENTE CAMBIANDO SERVER
     */
    public void teleportToPlayer(Player staffMember, String reportedPlayerName, String serverName) {
        Optional<Player> optionalReportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);
        if (!optionalReportedPlayer.isPresent()) {
            Component notOnlineMessage = messageManager.getComponentMessage("messages.playerNotOnline", null);
            staffMember.sendMessage(notOnlineMessage);
            return;
        }

        Player reportedPlayer = optionalReportedPlayer.get();
        String currentServerName = staffMember.getCurrentServer().get().getServerInfo().getName();

        if (!currentServerName.equals(serverName)) {
            Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(serverName);
            if (!targetServer.isPresent()) {
                Component serverNotFoundMessage = messageManager.getComponentMessage("messages.serverNotFound", null);
                staffMember.sendMessage(serverNotFoundMessage);
                return;
            }
            // Ci spostiamo sul server di destinazione
            staffMember.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    // Una volta connessi, teletrasporto
                    executeTeleportCommand(staffMember, reportedPlayerName);
                } else {
                    Component errorMessage = messageManager.getComponentMessage("messages.couldNotConnect", null);
                    staffMember.sendMessage(errorMessage);
                }
            });
        } else {
            // Già sul server corretto
            executeTeleportCommand(staffMember, reportedPlayerName);
        }
    }

    /**
     * Teletrasporto diretto /tp <playerName>.
     * (assume che il server abbia un comando /tp)
     */
    private void executeTeleportCommand(Player staffMember, String reportedPlayerName) {
        staffMember.spoofChatInput("/tp " + reportedPlayerName);
    }

    // ----------------------------------------------------------------------
    // 5) GESTIONE COMANDI VELOCITY (SimpleCommand)
    // Registrati nel costruttore, e gestiti qui
    // ----------------------------------------------------------------------

    @Override
    public void execute(Invocation invocation) {
        String alias = invocation.alias();
        String[] args = invocation.arguments();

        // Chi esegue il comando?
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command."));
            return;
        }
        Player staffMember = (Player) invocation.source();

        // Controllo permesso
        if (!staffMember.hasPermission("report.mcreport")) {
            staffMember.sendMessage(messageManager.getComponentMessage("messages.noPermission", null));
            return;
        }

        // COMANDO: /teleportServer <serverName>
        if (alias.equalsIgnoreCase("teleportServer")) {
            if (args.length < 1) {
                staffMember.sendMessage(Component.text("Usage: /teleportServer <serverName>"));
                return;
            }
            String serverName = args[0];
            teleportToServer(staffMember, serverName);
            return;
        }

        // COMANDO: /teleportPlayer <playerName> <serverName>
        if (alias.equalsIgnoreCase("teleportPlayer")) {
            if (args.length < 2) {
                staffMember.sendMessage(Component.text("Usage: /teleportPlayer <playerName> <serverName>"));
                return;
            }
            String targetPlayer = args[0];
            String serverName = args[1];
            teleportToPlayer(staffMember, targetPlayer, serverName);
        }
    }

    /**
     * Facoltativamente puoi gestire suggerimenti per il tab-complete dei comandi.
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        return Collections.emptyList(); // Nessun suggerimento specifico
    }
}
