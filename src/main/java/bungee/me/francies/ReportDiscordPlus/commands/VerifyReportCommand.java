package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class VerifyReportCommand extends Command {

    private final ReportDiscordPlus plugin;

    public VerifyReportCommand(ReportDiscordPlus plugin) {
        super("verifyreport");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(plugin.getMessage("prefix") +"Only players can use this command."));
            return;
        }

        ProxiedPlayer staffMember = (ProxiedPlayer) sender;

        // Verifica che lo staff abbia il permesso "report.tp"
        if (!staffMember.hasPermission("report.tp")) {
            staffMember.sendMessage(new TextComponent(plugin.getMessage("prefix") + plugin.getMessage("noPermission")));
            return;
        }

        // Controlla che ci siano abbastanza argomenti
        if (args.length < 2) {
            staffMember.sendMessage(new TextComponent(plugin.getMessage("prefix") + "Usage: /verifyreport <player> <server>"));
            return;
        }

        String reportedPlayer = args[0];
        String server = args[1];

        plugin.getStaffNotifier().teleportToPlayer(staffMember, reportedPlayer, server);
    }
}
