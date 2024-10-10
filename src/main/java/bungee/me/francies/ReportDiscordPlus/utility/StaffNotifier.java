package bungee.me.francies.ReportDiscordPlus.utility;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Map;

public class StaffNotifier {

    private final String titleText;
    private final String subTitleText;
    private final Map<String, String> messages;
    private ReportDiscordPlus plugin;

    public StaffNotifier(String titleText, String subTitleText, Map<String, String> messages, ReportDiscordPlus plugin) {
        this.titleText = titleText;
        this.subTitleText = subTitleText;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void sendReportToMinecraftStaff(ProxiedPlayer reporter, String reportedPlayer, String reason, String server) {
        ProxiedPlayer reported = ProxyServer.getInstance().getPlayer(reportedPlayer);
        if (reported == null) {
            reporter.sendMessage(new TextComponent(ChatColor.RED + "The reported player is not online."));
            return;
        }

        // Sostituzione dei placeholder nel messaggio
        String staffMessage = messages.get("staffAlert")
                .replace("{player}", reporter.getName())
                .replace("{reportedPlayer}", reportedPlayer)
                .replace("{reason}", reason)
                .replace("{server}", server);

        for (ProxiedPlayer staffMember : ProxyServer.getInstance().getPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {
                TextComponent messageComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', staffMessage));

                // Aggiungi una linea di separazione prima dei bottoni
                messageComponent.addExtra(new TextComponent("\n"));

                if (staffMember.hasPermission("report.tp")) {
                    // Aggiungi il bottone per il teletrasporto al server
                    TextComponent teleportButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("button.teleport_to_server")));
                    teleportButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("button.show_text_server_button")))}));
                    teleportButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/verifyreport " + reportedPlayer + " " + server));

                    // Aggiungi una linea di separazione tra i bottoni
                    TextComponent separator = new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("button.separator")));

                    // Aggiungi il bottone per il teletrasporto al giocatore
                    TextComponent tpButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("button.teleport_to_player")));
                    tpButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("button.show_text_tp_button")))}));
                    tpButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + reportedPlayer));

                    // Aggiungi i bottoni e il separatore al messaggio
                    messageComponent.addExtra(teleportButton);
                    messageComponent.addExtra(separator); // Separatore tra i due bottoni
                    messageComponent.addExtra(tpButton);
                }

                // Sostituisci i placeholder per il titolo e il sottotitolo
                String formattedTitleText = titleText
                        .replace("{player}", reporter.getName())
                        .replace("{reportedPlayer}", reportedPlayer)
                        .replace("{reason}", reason)
                        .replace("{server}", server);

                String formattedSubTitleText = subTitleText
                        .replace("{player}", reporter.getName())
                        .replace("{reportedPlayer}", reportedPlayer)
                        .replace("{reason}", reason)
                        .replace("{server}", server);

                // Invia il titolo e sottotitolo personalizzati
                sendTitleToPlayer(staffMember, createTitle(formattedTitleText, formattedSubTitleText, 10, 70, 20));
                staffMember.sendMessage(messageComponent);
            }
        }
    }


    // Questo metodo viene eseguito quando lo staff clicca sul bottone di teletrasporto
    public void teleportToPlayer(ProxiedPlayer staffMember, String reportedPlayer, String server) {
        ProxiedPlayer reported = ProxyServer.getInstance().getPlayer(reportedPlayer);
        if (reported == null) {
            staffMember.sendMessage(new TextComponent(ChatColor.RED + "The reported player is no longer online."));
            return;
        }

        // Controlla se lo staff è già sullo stesso server
        if (!staffMember.getServer().getInfo().getName().equals(server)) {
            // Cambia server se non è lo stesso
            staffMember.connect(ProxyServer.getInstance().getServerInfo(server), (result, error) -> {
            });
        }
    }


    public Title createTitle(String titleText, String subTitleText, int fadeIn, int stay, int fadeOut) {
        Title title = ProxyServer.getInstance().createTitle();
        BaseComponent titleComponent = new TextComponent(titleText);
        title.title(titleComponent);

        BaseComponent subTitleComponent = new TextComponent(subTitleText);
        title.subTitle(subTitleComponent);

        title.fadeIn(fadeIn);
        title.stay(stay);
        title.fadeOut(fadeOut);

        return title;
    }


    public void sendTitleToPlayer(ProxiedPlayer player, Title title) {
        title.send(player);
    }
}
