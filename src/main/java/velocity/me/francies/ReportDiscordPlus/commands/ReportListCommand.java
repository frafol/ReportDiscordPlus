package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;


import java.util.*;

public class ReportListCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;
    private final int reportsPerPage = 10; // Numero di report per pagina

    public ReportListCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Only player can do this command"));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("report.admin")) {
            Component noPermissionMessage = messageManager.getComponentMessage("messages.noPermission", null);
            player.sendMessage(noPermissionMessage);
            return;
        }

        int page = 1; // Pagina predefinita
        String statusFilter = "all"; // Default mostra tutti i report

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("closed")) {
                statusFilter = "closed";
            } else if (args[0].equalsIgnoreCase("opened")) {
                statusFilter = "open";
            } else {
                try {
                    page = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    Component invalidPageMessage = messageManager.getComponentMessage("messages.invalidPageNumber", null);
                    player.sendMessage(invalidPageMessage);
                    return;
                }
            }
        } else if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);

                if (args[0].equalsIgnoreCase("closed")) {
                    statusFilter = "closed";
                } else if (args[0].equalsIgnoreCase("opened")) {
                    statusFilter = "open";
                } else {
                    Component invalidPageMessage = messageManager.getComponentMessage("messages.invalidPageNumber", null);
                    player.sendMessage(invalidPageMessage);
                    return;
                }

            } catch (NumberFormatException e) {
                Component invalidPageMessage = messageManager.getComponentMessage("messages.invalidPageNumber", null);
                player.sendMessage(invalidPageMessage);
                return;
            }
        }

        listReports(player, page, statusFilter);
    }

    private void listReports(Player player, int page, String statusFilter) {
        try {
            ConfigurationNode config = plugin.getReportsConfig();

            // Ottenere tutti i report
            Set<Object> keys = config.node("reports").childrenMap().keySet();
            List<String> reportKeys = new ArrayList<>();
            keys.forEach(key -> reportKeys.add(key.toString()));

            // Filtro per lo status
            if (!statusFilter.equals("all")) {
                reportKeys.removeIf(key -> !config.node("reports", key, "status").getString().equalsIgnoreCase(statusFilter));
            }

            int totalReports = reportKeys.size();
            int totalPages = (int) Math.ceil((double) totalReports / reportsPerPage);

            // Verifica se la pagina richiesta è valida
            if (page > totalPages || page < 1) {
                Component invalidPageMessage = messageManager.getComponentMessage("messages.invalidPageNumber", null);
                player.sendMessage(invalidPageMessage);
                return;
            }

            Map<String, String> headerPlaceholders = new HashMap<>();
            headerPlaceholders.put("page", String.valueOf(page));
            headerPlaceholders.put("totalPages", String.valueOf(totalPages));
            Component headerMessage = messageManager.getComponentMessage("messages.reportListHeader", headerPlaceholders);
            player.sendMessage(headerMessage);

            int start = (page - 1) * reportsPerPage;
            int end = Math.min(start + reportsPerPage, totalReports);

            if (totalReports == 0) {
                Component noReportsMessage = messageManager.getComponentMessage("messages.noReportsFound", null);
                player.sendMessage(noReportsMessage);
                return;
            }

            for (int i = start; i < end; i++) {
                String key = reportKeys.get(i);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", key);
                placeholders.put("reporter", config.node("reports", key, "reporter").getString());
                placeholders.put("reported", config.node("reports", key, "reported").getString());
                placeholders.put("reason", config.node("reports", key, "reason").getString());
                placeholders.put("status", config.node("reports", key, "status").getString());
                Component reportMessage = messageManager.getComponentMessage("messages.reportListItem", placeholders);
                player.sendMessage(reportMessage);
            }

        } catch (Exception e) {
            Component errorListingMessage = messageManager.getComponentMessage("messages.errorListingReports", null);
            player.sendMessage(errorListingMessage);
            e.printStackTrace();
        }
    }
}
