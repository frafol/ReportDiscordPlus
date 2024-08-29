package me.francies.ReportDiscordPlus.utility;

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

    public StaffNotifier(String titleText, String subTitleText, Map<String, String> messages) {
        this.titleText = titleText;
        this.subTitleText = subTitleText;
        this.messages = messages;
    }

    public void sendReportToMinecraftStaff(ProxiedPlayer reporter, String reportedPlayer, String reason, String server) {
        String teleportCommand = "/tp " + reportedPlayer;
        String sendstaff = "/" + server;
        TextComponent sendButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &eSERVER"));
        TextComponent teleportButton = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &eTELEPORT"));
        teleportButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Vai dal giocatore segnalato")}));
        sendButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Vai nel server del giocatore segnalato")}));
        teleportButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand));
        sendButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, sendstaff));

        String staffMessage = messages.get("staffAlert")
                .replace("{player}", reporter.getName())
                .replace("{reportedPlayer}", reportedPlayer)
                .replace("{reason}", reason)
                .replace("{server}", server);

        for (ProxiedPlayer staffMember : ProxyServer.getInstance().getPlayers()) {
            if (staffMember.hasPermission("report.mcreport")) {
                TextComponent messageComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', staffMessage));
                if (staffMember.hasPermission("report.tp")) {
                    messageComponent.addExtra(teleportButton);
                    messageComponent.addExtra(sendButton);
                    sendTitleToPlayer(staffMember, createTitle(titleText, subTitleText, 10, 70, 20));
                }
                staffMember.sendMessage(messageComponent);
            }
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
