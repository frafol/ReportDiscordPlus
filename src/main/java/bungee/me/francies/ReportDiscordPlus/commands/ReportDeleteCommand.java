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
        if(!sender.hasPermission("report.admin")){
            sender.sendMessage(plugin.getMessage("prefix") +plugin.getMessage("noPermission"));
            return;
        }
        if (args.length < 2) {
            // Messaggio di errore se gli argomenti non sono sufficienti
            sender.sendMessage(new TextComponent(plugin.getMessage("prefix") +plugin.getMessage("usageDelete")));  // Puoi configurare il messaggio "usageClose" nel tuo file di configurazione
            return;
        }
        String reportId = args[1];

        // Verifica che il report esista
        if (plugin.getReportsConfig().contains("reports." + reportId)) {
            // Rimuove il report dal file YAML
            plugin.getReportsConfig().set("reports." + reportId, null);

            // Salva le modifiche nel file YAML
            plugin.saveReportsConfig();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String deleteMessage = plugin.getConfig().getString("messages.reportDeleted", "&aReport {id} cancellato con successo.");
            sender.sendMessage(new TextComponent(plugin.getMessage("prefix") +plugin.replacePlaceholders(deleteMessage, placeholders)));
        } else {
            sender.sendMessage(new TextComponent(plugin.getMessage("prefix") +plugin.getMessage("reportNotFound")));
        }
    }
}
