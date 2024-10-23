package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.serialize.SerializationException;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;


import java.util.HashMap;
import java.util.Map;

public class ReportDeleteCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public ReportDeleteCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
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
            Component usageMessage = messageManager.getComponentMessage("messages.usageDelete", null);
            sender.sendMessage(usageMessage);
            return;
        }

        String reportId = args[0];

        if (plugin.getReportsConfig().node("reports", reportId).virtual()) {
            Component notFoundMessage = messageManager.getComponentMessage("messages.reportNotFound", null);
            sender.sendMessage(notFoundMessage);
            return;
        }

        try {
            plugin.getReportsConfig().node("reports", reportId).set(null);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        plugin.saveReportsConfig();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", reportId);

        Component deleteMessage = messageManager.getComponentMessage("messages.reportDeleted", placeholders);
        sender.sendMessage(deleteMessage);

    }
}
