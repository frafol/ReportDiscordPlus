












// TODO in future








package velocity.me.francies.ReportDiscordPlus.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import velocity.me.francies.ReportDiscordPlus.utility.MessageManager;


public class VerifyReportCommand implements SimpleCommand {

    private final ReportDiscordPlus plugin;
    private final MessageManager messageManager;

    public VerifyReportCommand(ReportDiscordPlus plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only player can do this command."));
            return;
        }

        Player staffMember = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (!staffMember.hasPermission("report.tp")) {
            Component noPermissionMessage = messageManager.getComponentMessage("messages.noPermission", null);
            staffMember.sendMessage(noPermissionMessage);
            return;
        }

        if (args.length < 3) {
            Component usageMessage = messageManager.getComponentMessage("messages.usageVerifyReport", null);
            staffMember.sendMessage(usageMessage);
            return;
        }

        String action = args[0];  // "server" o "player"
        String reportedPlayer = args[1];
        String server = args[2];

        if (action.equalsIgnoreCase("server")) {
          //  plugin.getStaffNotifier().teleportToServer(staffMember, server);
        } else if (action.equalsIgnoreCase("player")) {
           // plugin.getStaffNotifier().teleportToPlayer(staffMember, reportedPlayer, server);
        } else {
            Component usageMessage = messageManager.getComponentMessage("messages.usageVerifyReport", null);
            staffMember.sendMessage(usageMessage);
        }
    }
}
