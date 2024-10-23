package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import java.util.HashMap;
import java.util.Map;

public class ReportCloseCommand extends Command {

    private final ReportDiscordPlus plugin;

    public ReportCloseCommand(ReportDiscordPlus plugin) {
        super("rpclose");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission("report.admin")){
            sender.sendMessage(plugin.getMessage("noPermission"));
            return;
        }
        if (args.length < 2) {
            // Messaggio di errore se gli argomenti non sono sufficienti
            sender.sendMessage(new TextComponent(plugin.getMessage("usageClose")));  // Puoi configurare il messaggio "usageClose" nel tuo file di configurazione
            return;
        }
        String reportId = args[1];

        // Verifica che il report esista
        if (plugin.getReportsConfig().contains("reports." + reportId)) {
            plugin.getReportsConfig().set("reports." + reportId + ".status", "closed");
            plugin.getReportsConfig().set("reports." + reportId + ".closed_by", sender.getName());
            plugin.getReportsConfig().set("reports." + reportId + ".closed_timestamp", System.currentTimeMillis());

            // Salva le modifiche nel file YAML
            plugin.saveReportsConfig();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String closeMessage = plugin.getConfig().getString("messages.reportClosed", "&aReport {id} chiuso con successo.");
            sender.sendMessage(new TextComponent(plugin.replacePlaceholders(closeMessage, placeholders)));
        } else {
            sender.sendMessage(new TextComponent(plugin.getMessage("reportNotFound")));
        }
    }
}
