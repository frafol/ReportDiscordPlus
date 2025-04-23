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

    public StaffNotifier(ReportDiscordPlus plugin, MessageManager messageManager, String titleText, String subTitleText) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.titleText = titleText;
        this.subTitleText = subTitleText;

        // Registriamo i comandi "teleportServer" e "teleportPlayer" direttamente QUI
        ProxyServer proxy = plugin.getProxy();
        proxy.getCommandManager().register("teleportPlayer", this);   // /teleportPlayer
    }

    /**
     * 1) COSTRUISCE I PULSANTI CLICCABILI (RUN COMMAND)
     */
    private Component buildTeleportButtons(String staff, String serverName, String reportedPlayerName) {

        // Lettura dal config
        String showTextServerButton = plugin.getConfig()
                .node("button", "show_text_server_button")
                .getString();
        String teleportToServerText = plugin.getConfig()
                .node("button", "teleport_to_server")
                .getString();

        String showTextTpButton = plugin.getConfig()
                .node("button", "show_text_tp_button")
                .getString();
        String teleportToPlayerText = plugin.getConfig()
                .node("button", "teleport_to_player")
                .getString();

        String separator = plugin.getConfig()
                .node("button", "separator")
                .getString();

        // Pulsante "TP-SERVER" con hover e click -> /teleportServer <serverName>
        Component serverButton = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(teleportToServerText)
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(showTextServerButton)
                ))
                // Esegue il comando "/teleportServer <nomeServer>"
                .clickEvent(ClickEvent.runCommand("/rpserver " + staff + " " + serverName));

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
        List<String> staffMessageList = messageManager.getMessageList("messages.staffAlert", null);
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
                if (staffMember.hasPermission("report.mcreport")) {
                    // Pulsanti cliccabili (TP-SERVER / TP-PLAYER)
                    Component buttonMessage = buildTeleportButtons(reporter.getUsername(), server, reportedPlayer);
                    staffMember.sendMessage(buttonMessage);
                }
            }
        }
    }

    /**
     * 4) TELETRASPORTA STAFFER AD UN PLAYER, EVENTUALMENTE CAMBIANDO SERVER
     */
    public void teleportToPlayer(Player staffMember, String reportedPlayerName, String serverName) {
        Optional<Player> optionalReportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);
        if (!optionalReportedPlayer.isPresent()) {
            Component notOnlineMessage = messageManager.getComponentMessage("messages.onlinePlayer", null);
            staffMember.sendMessage(notOnlineMessage);
            return;
        }

            executeTeleportCommand(staffMember, reportedPlayerName);
    }

    /**
     * Teletrasporto diretto /tp <playerName>.
     * (assume che il server abbia un comando /tp)
     */
    private void executeTeleportCommand(Player staffMember, String reportedPlayerName) {
        staffMember.spoofChatInput("/tp " + reportedPlayerName);
    }


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


        // COMANDO: /teleportPlayer <playerName> <serverName>
        if (alias.equalsIgnoreCase("teleportPlayer")) {
            if (args.length < 2) {
                return;
            }
            String targetPlayer = args[0];
            String serverName = args[1];
            teleportToPlayer(staffMember, targetPlayer, serverName);
        }
    }


}
