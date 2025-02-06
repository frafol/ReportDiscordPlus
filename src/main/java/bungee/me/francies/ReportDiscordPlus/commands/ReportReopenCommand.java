package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;

public class ReportReopenCommand extends Command {

    private final ReportDiscordPlus plugin;

    public ReportReopenCommand(ReportDiscordPlus plugin) {
        super("rpreopen");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&',  plugin.getMessage("prefix"));
        if (!sender.hasPermission("report.admin")) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("noPermission").replace("{prefix}", prefix)));
            return;
        }

        // Controllo che ci siano almeno 2 argomenti (sub-comando "reopen" e l'ID)
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("usageReopen").replace("{prefix}", prefix)));
            return;
        }

        // Controllo che il primo argomento sia "reopen"
        if (!args[0].equalsIgnoreCase("reopen")) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("usageReopen").replace("{prefix}", prefix)));
            return;
        }

        String reportId = args[1];

        // Verifica che il report esista
        if (plugin.getReportsConfig().contains("reports." + reportId)) {
            plugin.getReportsConfig().set("reports." + reportId + ".status", "open");
            plugin.getReportsConfig().set("reports." + reportId + ".closed_by", null);
            plugin.getReportsConfig().set("reports." + reportId + ".closed_timestamp", null);
            plugin.saveReportsConfig();

            // Sostituzione placeholder
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String reopenMessage = plugin.getConfig().getString("messages.reportReopened").replace("{prefix}", prefix);
            reopenMessage = plugin.replacePlaceholders(reopenMessage, placeholders);
            sender.sendMessage(new TextComponent(  reopenMessage));
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String notFoundMessage = plugin.getMessage("reportNotFound").replace("{prefix}", prefix);
            notFoundMessage = plugin.replacePlaceholders(notFoundMessage, placeholders);
            sender.sendMessage(new TextComponent(  notFoundMessage));
        }
    }
}
