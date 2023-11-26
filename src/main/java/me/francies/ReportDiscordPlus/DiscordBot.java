package me.francies.ReportDiscordPlus;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot extends ListenerAdapter {

    private static DiscordBot instance;
    private JDA jda;

    private DiscordBot(String botToken) {
        jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT) // Abilita l'intent MESSAGE_CONTENT
                .addEventListeners(this)
                .build();
    }

    public void startBot() {
        // Il bot è già avviato nell'istanza del costruttore, non è necessario fare nulla qui.
    }

    public static DiscordBot getInstance(String botToken) {
        if (instance == null) {
            instance = new DiscordBot(botToken);
        }
        return instance;
    }



}
