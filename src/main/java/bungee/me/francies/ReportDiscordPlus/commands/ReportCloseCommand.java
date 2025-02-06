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
        // Controllo permessi
        if (!sender.hasPermission("report.admin")) {
            sender.sendMessage(new TextComponent(  plugin.getMessage("noPermission")));
            return;
        }

        if (args.length < 1) {
            // Manca sia il giocatore che l'id
            sender.sendMessage(new TextComponent( plugin.getMessage("usageClose")));
            return;
        } else if (args.length < 2) {
            // Ha specificato il giocatore, ma manca l’id
            sender.sendMessage(new TextComponent( plugin.getMessage("missingID")));
            return;
        }

        String reportId = args[1];

        // Verifica che il report esista
        if (plugin.getReportsConfig().contains("reports." + reportId)) {
            // Se esiste, impostiamo lo status su "closed"
            plugin.getReportsConfig().set("reports." + reportId + ".status", "closed");
            plugin.getReportsConfig().set("reports." + reportId + ".closed_by", sender.getName());
            plugin.getReportsConfig().set("reports." + reportId + ".closed_timestamp", System.currentTimeMillis());

            // Salva le modifiche nel file YAML
            plugin.saveReportsConfig();

            // Preparazione placeholder per messaggio di chiusura
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String closeMessage = plugin.getConfig().getString("messages.reportClosed");
            closeMessage = plugin.replacePlaceholders(closeMessage, placeholders);
            sender.sendMessage(new TextComponent(  closeMessage));
        } else {
            // Se il report non esiste, inviamo un messaggio di errore dedicato
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", reportId);

            String notFoundMessage = plugin.getMessage("reportNotFound");
            // Esempio di messaggio in config: "&cNessun report trovato con l'ID {id}."

            notFoundMessage = plugin.replacePlaceholders(notFoundMessage, placeholders);
            sender.sendMessage(new TextComponent(  notFoundMessage));
        }
    }
}
