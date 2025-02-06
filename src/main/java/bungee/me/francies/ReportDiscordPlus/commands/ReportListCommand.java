package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.*;

public class ReportListCommand extends Command {

    private final ReportDiscordPlus plugin;
    private final int reportsPerPage = 10; // Numero di report per pagina

    public ReportListCommand(ReportDiscordPlus plugin) {
        super("reports");
        this.plugin = plugin;

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&',  plugin.getMessage("prefix"));
        if (sender instanceof ProxiedPlayer && sender.hasPermission("report.admin")) {
            int page = 1; // Pagina predefinita
            String statusFilter = "all"; // Default mostra tutti i report

            if (args.length == 1) {
                // Controlla se l'argomento è "closed" o "opened"
                if (args[0].equalsIgnoreCase("closed")) {
                    statusFilter = "closed";
                } else if (args[0].equalsIgnoreCase("opened")) {
                    statusFilter = "open";
                } else {
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponent( plugin.getMessage("invalidPageNumber").replace("{prefix}", prefix)));
                        return;
                    }
                }
            } else if (args.length == 2) {
                try {
                    page = Integer.parseInt(args[1]);

                    // Verifica il filtro "closed" o "opened"
                    if (args[0].equalsIgnoreCase("closed")) {
                        statusFilter = "closed";
                    } else if (args[0].equalsIgnoreCase("opened")) {
                        statusFilter = "open";
                    } else {
                        sender.sendMessage(new TextComponent( plugin.getMessage("invalidPageNumber").replace("{prefix}", prefix)));
                        return;
                    }

                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent( plugin.getMessage("invalidPageNumber").replace("{prefix}", prefix)));
                    return;
                }
            }

            listReports((ProxiedPlayer) sender, page, statusFilter);
        } else {
            sender.sendMessage(new TextComponent( plugin.getMessage("noPermission").replace("{prefix}", prefix)));
        }
    }

    private void listReports(ProxiedPlayer player, int page, String statusFilter) {
        String prefix = ChatColor.translateAlternateColorCodes('&',  plugin.getMessage("prefix"));
        try {
            // Usa il metodo centralizzato per ottenere il file YAML
            Configuration config = plugin.getReportsConfig();

            // Ottenere tutti i report aperti
            Collection<String> keys = config.getSection("reports").getKeys();
            List<String> reportKeys = new ArrayList<>(keys);

            // Filtro per lo status
            if (!statusFilter.equals("all")) {
                reportKeys.removeIf(key -> !config.getString("reports." + key + ".status").equalsIgnoreCase(statusFilter));
            }

            int totalReports = reportKeys.size();
            int totalPages = (int) Math.ceil((double) totalReports / reportsPerPage);

            // Verifica se la pagina richiesta è valida
            if (page > totalPages || page < 1) {
                player.sendMessage(new TextComponent( plugin.getMessage("invalidPageNumber").replace("{prefix}", prefix)));
                return;
            }

            // Messaggio di intestazione
            String headerMessage = plugin.getConfig().getString("messages.reportListHeader", "&6&lReports (Page {page}/{totalPages}):").replace("{prefix}", prefix);
            Map<String, String> headerPlaceholders = new HashMap<>();
            headerPlaceholders.put("page", String.valueOf(page));
            headerPlaceholders.put("totalPages", String.valueOf(totalPages));
            player.sendMessage(new TextComponent( plugin.replacePlaceholders(headerMessage, headerPlaceholders)));

            // Mostra i report nella pagina corrente
            int start = (page - 1) * reportsPerPage;
            int end = Math.min(start + reportsPerPage, totalReports);

            if (totalReports == 0) {
                player.sendMessage(new TextComponent( plugin.getMessage("noReportsFound").replace("{prefix}", prefix)));
                return;
            }

            for (int i = start; i < end; i++) {
                String key = reportKeys.get(i);
                String reportMessage = plugin.getConfig().getString("messages.reportListItem", "&7- &eReport {id}: {reporter} -> {reported} ({reason}) [Status: {status}]").replace("{prefix}", prefix);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", key);
                placeholders.put("reporter", config.getString("reports." + key + ".reporter"));
                placeholders.put("reported", config.getString("reports." + key + ".reported"));
                placeholders.put("reason", config.getString("reports." + key + ".reason"));
                placeholders.put("status", config.getString("reports." + key + ".status"));  // Aggiungi lo status
                player.sendMessage(new TextComponent( plugin.replacePlaceholders(reportMessage, placeholders)));
            }

        } catch (Exception e) {
            player.sendMessage(new TextComponent( plugin.getMessage("errorListingReports").replace("{prefix}", prefix)));
            e.printStackTrace();
        }
    }
}
