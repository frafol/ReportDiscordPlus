package bungee.me.francies.ReportDiscordPlus.commands;

import bungee.me.francies.ReportDiscordPlus.ReportDiscordPlus;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class TabCompletionListener implements Listener {

    private final ReportDiscordPlus plugin;

    public TabCompletionListener(ReportDiscordPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String cursor = event.getCursor().toLowerCase();

        // Verifica se il comando inizia con /rp
        if (cursor.startsWith("/rp")) {
            List<String> suggestions = new ArrayList<>();

            // Aggiungi suggerimenti per i comandi
            if ("/rpclose".startsWith(cursor)) {
                suggestions.add("/rpclose");
            }
            if ("/rpdelete".startsWith(cursor)) {
                suggestions.add("/rpdelete");
            }
            if ("/rpreopen".startsWith(cursor)) {
                suggestions.add("/rpreopen");
            }

            // Aggiungi suggerimenti per gli ID dei report se il comando è completo
            if (cursor.startsWith("/rpclose ") || cursor.startsWith("/rpdelete ") || cursor.startsWith("/rpreopen ")) {
                String[] parts = cursor.split(" ");
                if (parts.length == 2) {
                    String subCommand = parts[0];
                    String partialId = parts[1];

                    // Ottieni tutti gli ID dei report
                    List<String> reportIds = new ArrayList<>(plugin.getReportsConfig().getSection("reports").getKeys());


                    for (String id : reportIds) {
                        if (id.startsWith(partialId)) {
                            suggestions.add(subCommand + " " + id);
                        }
                    }
                }
            }

            // Imposta i suggerimenti nell'evento
            event.getSuggestions().addAll(suggestions);
        }
    }
}
