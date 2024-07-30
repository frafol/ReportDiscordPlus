package me.francies.ReportDiscordPlus;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class JDAEventListener implements EventListener {
    private ReportDiscordPlus plugin;

    public JDAEventListener(ReportDiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent) {
            SlashCommandInteractionEvent slashEvent = (SlashCommandInteractionEvent) event;
            if (slashEvent.getName().equals("report")) {
                if (slashEvent.getSubcommandName().equals("player")) {
                    String reporter = slashEvent.getUser().getName();
                    String reportedPlayer = slashEvent.getOption("nome_giocatore").getAsString();
                    String reason = slashEvent.getOption("motivo").getAsString();


                    plugin.sendReportToDiscord(reporter, reportedPlayer, reason, "Survival");
                }
            }
        }
    }
}
