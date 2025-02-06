package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;

public class ReportDeleteCommand extends Command {

    private final ReportDiscordPlus plugin;

    public ReportDeleteCommand(ReportDiscordPlus plugin) {
        super("rpdelete");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("report.admin")) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("noPermission")));
            return;
        }

        // Controllo che ci siano almeno 2 argomenti (sub-comando "delete" e l'ID)
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("usageDelete")));
            return;
        }

        // Controllo che il primo argomento sia "delete"
        if (!args[0].equalsIgnoreCase("delete")) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("usageDelete")));
            return;
        }

        String reportId = args[1];

        // Verifica che il report esista
        if (plugin.getReportsConfig().contains("reports." + reportId)) {
            // Rimuove il report dal file YAML
            plugin.getReportsConfig().set("reports." + reportId, null);
            plugin.saveReportsConfig();

            // Sostituzione placeholder
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String deleteMessage = plugin.getConfig().getString("messages.reportDeleted");
            deleteMessage = plugin.replacePlaceholders(deleteMessage, placeholders);
            sender.sendMessage(new TextComponent(  deleteMessage));
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String notFoundMessage = plugin.getMessage("reportNotFound");
            notFoundMessage = plugin.replacePlaceholders(notFoundMessage, placeholders);
            sender.sendMessage(new TextComponent(  notFoundMessage));
        }
    }
}
