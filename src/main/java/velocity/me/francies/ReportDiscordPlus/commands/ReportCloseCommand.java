package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;

import java.util.HashMap;
import java.util.Map;

public class ReportCloseCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public ReportCloseCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only player can do this command."));
            return;
        }

        Player sender = (Player) invocation.source();
        String[] args = invocation.arguments();

        // Controllo permessi
        if (!sender.hasPermission("report.admin")) {
            Component noPermissionMessage = messageManager.getComponentMessage("messages.noPermission", null);
            sender.sendMessage(noPermissionMessage);
            return;
        }

        // Controllo che almeno i primi 2 argomenti ci siano, es: /rpclose close <id>
        if (args.length < 2) {
            Component usageMessage = messageManager.getComponentMessage("messages.usageClose", null);
            sender.sendMessage(usageMessage);
            return;
        }

        // args[0] dovrebbe essere "close"
        if (!args[0].equalsIgnoreCase("close")) {
            // Messaggio d’uso se non è "close"
            Component usageMessage = messageManager.getComponentMessage("messages.usageClose", null);
            sender.sendMessage(usageMessage);
            return;
        }

        // Il vero ID sarà args[1]
        String reportId = args[1];

        // Verifico se il nodo "reports.<reportId>" non esiste
        if (plugin.getReportsConfig().node("reports", reportId).virtual()) {
            Component notFoundMessage = messageManager.getComponentMessage("messages.reportNotFound", null);
            sender.sendMessage(notFoundMessage);
            return;
        }

        // Imposto lo stato a "closed"
        try {
            plugin.getReportsConfig().node("reports", reportId, "status").set("closed");
            plugin.getReportsConfig().node("reports", reportId, "closed_by").set(sender.getUsername());
            plugin.getReportsConfig().node("reports", reportId, "closed_timestamp").set(System.currentTimeMillis());
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        // Salvo il config
        plugin.saveReportsConfig();

        // Invio il messaggio di conferma al player
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", reportId);
        Component closeMessage = messageManager.getComponentMessage("messages.reportClosed", placeholders);
        sender.sendMessage(closeMessage);
    }
}
