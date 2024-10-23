package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;


import java.util.HashMap;
import java.util.Map;

public class ReportReopenCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public ReportReopenCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
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

        if (!sender.hasPermission("report.admin")) {
            Component noPermissionMessage = messageManager.getComponentMessage("messages.noPermission", null);
            sender.sendMessage(noPermissionMessage);
            return;
        }

        if (args.length < 1) {
            Component usageMessage = messageManager.getComponentMessage("messages.usageReopen", null);
            sender.sendMessage(usageMessage);
            return;
        }

        String reportId = args[0];

        ConfigurationNode reportsNode = plugin.getReportsConfig().node("reports", reportId);
        if (reportsNode.virtual()) {
            Component notFoundMessage = messageManager.getComponentMessage("messages.reportNotFound", null);
            sender.sendMessage(notFoundMessage);
            return;
        }

        // Riapri il report e aggiorna lo stato
        try {
            reportsNode.node("status").set("open");
            reportsNode.node("closed_by").set(null);
            reportsNode.node("closed_timestamp").set(null);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        plugin.saveReportsConfig();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", reportId);

        Component reopenMessage = messageManager.getComponentMessage("messages.reportReopened", placeholders);
        sender.sendMessage(reopenMessage);

    }
}
