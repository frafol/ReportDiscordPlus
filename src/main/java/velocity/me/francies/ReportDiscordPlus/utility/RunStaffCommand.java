package velocity.me.francies.ReportDiscordPlus.utility;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public class RunStaffCommand implements SimpleCommand {

    private final ProxyServer proxyServer;

    public RunStaffCommand(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 2) {
            invocation.source().sendMessage(Component.text(" "));
            return;
        }

        String staff = invocation.arguments()[0];
        String serverName = invocation.arguments()[1];
        proxyServer.getCommandManager().executeAsync(
                proxyServer.getConsoleCommandSource(),
                "send " + staff + " " + serverName
        );
    }
}