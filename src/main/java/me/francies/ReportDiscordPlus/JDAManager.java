package me.francies.ReportDiscordPlus;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class JDAManager {
    private ReportDiscordPlus plugin;
    private JDA jda;

    public JDAManager(ReportDiscordPlus plugin) {
        this.plugin = plugin;

    }


    public void setupJDA() {
        String discordToken = plugin.getConfig().getString("discord.token");
        jda = JDABuilder.createDefault(discordToken)
                .addEventListeners(new JDAEventListener(plugin))
                .build();
    }

    public JDA getJDA() {
        return jda;
    }
}
