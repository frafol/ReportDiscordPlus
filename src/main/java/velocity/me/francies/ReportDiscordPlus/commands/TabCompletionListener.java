package velocity.me.francies.ReportDiscordPlus.commands;


import com.velocitypowered.api.command.SimpleCommand;
import velocity.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TabCompletionListener implements SimpleCommand {

    private final ReportDiscordPlus plugin;

    public TabCompletionListener(ReportDiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            // Suggeriamo i comandi principali: close, delete, reopen
            return CompletableFuture.completedFuture(List.of("close", "delete", "reopen"));
        } else if (args.length == 2) {
            // Suggeriamo gli ID dei report disponibili
            List<String> reportIds = getReportIds();
            return CompletableFuture.completedFuture(reportIds);
        }

        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public void execute(Invocation invocation) {}

    private List<String> getReportIds() {
        ConfigurationNode reportsNode = plugin.getReportsConfig().node("reports");

        if (reportsNode.isMap()) {
            return reportsNode.childrenMap().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
