package velocity.me.francies.ReportDiscordPlus.utility;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StaffNotifier {

    private final String titleText;
    private final String subTitleText;
    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public StaffNotifier(String titleText, String subTitleText, ReportDiscordPlus plugin, MessageManager messageManager) {
        this.titleText = titleText;
        this.subTitleText = subTitleText;
        this.plugin = plugin;
        this.messageManager = messageManager;
    }
    public void sendReportToMinecraftStaff(Player reporter, String reportedPlayer, String reason, String server) throws SerializationException {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", reporter.getUsername());
        placeholders.put("reportedPlayer", reportedPlayer);
        placeholders.put("reason", reason);
        placeholders.put("server", server);

        // Ottieni il messaggio staffAlert come lista di stringhe
        List<String> staffMessageList = messageManager.getRawMessageList("messages.staffAlert");

        // Se la lista è vuota o nulla, evita errori
        if (staffMessageList == null || staffMessageList.isEmpty()) {
            reporter.sendMessage(Component.text(ChatColor.RED + "Staff alert message not found in configuration."));
            return;
        }

        // Costruisce il messaggio sostituendo i placeholder in ogni riga
        StringBuilder staffMessageBuilder = new StringBuilder();
        for (String line : staffMessageList) {
            String formattedLine = messageManager.replacePlaceholders(line, placeholders);
            staffMessageBuilder.append(formattedLine).append("\n");
        }

        // Converte il messaggio in un componente testuale
        Component staffMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(staffMessageBuilder.toString());

        for (Player staffMember : plugin.getProxy().getAllPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {

                // Sostituisci i placeholder nel titolo e sottotitolo
                String titleMessage = messageManager.replacePlaceholders(titleText, placeholders);
                String subtitleMessage = messageManager.replacePlaceholders(subTitleText, placeholders);

                Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1));
                Title title = Title.title(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(titleMessage),
                        LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleMessage),
                        times
                );
                staffMember.showTitle(title);

                staffMember.sendMessage(staffMessage);
            }
        }
    }

    public void teleportToServer(Player staffMember, String serverName) {
        Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(serverName);
        if (!targetServer.isPresent()) {
            Component serverNotFoundMessage = messageManager.getComponentMessage("messages.serverNotFound", null);
            staffMember.sendMessage(serverNotFoundMessage);
            return;
        }

        if (!staffMember.getCurrentServer().get().getServerInfo().getName().equals(serverName)) {
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
    public void teleportToPlayer(Player staffMember, String reportedPlayerName, String serverName) {
        Optional<Player> optionalReportedPlayer = plugin.getProxy().getPlayer(reportedPlayerName);
        if (!optionalReportedPlayer.isPresent()) {
            Component notOnlineMessage = messageManager.getComponentMessage("messages.playerNotOnline", null);
            staffMember.sendMessage(notOnlineMessage);
            return;
        }

        Player reportedPlayer = optionalReportedPlayer.get();

        if (!staffMember.getCurrentServer().get().getServerInfo().getName().equals(serverName)) {
            Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(serverName);
            if (!targetServer.isPresent()) {
                Component serverNotFoundMessage = messageManager.getComponentMessage("messages.serverNotFound", null);
                staffMember.sendMessage(serverNotFoundMessage);
                return;
            }

            // Già sul server corretto, esegui direttamente il comando
            executeTeleportCommand(staffMember, reportedPlayerName);
        }
    }

    private void executeTeleportCommand(Player staffMember, String reportedPlayerName) {
        // Esegui il comando /tp <reportedPlayerName>
        staffMember.spoofChatInput("/tp " + reportedPlayerName);
    }



}
