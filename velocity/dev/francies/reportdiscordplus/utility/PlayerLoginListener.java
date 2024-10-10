package velocity.dev.francies.reportdiscordplus.utility;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.Scheduler;
import velocity.dev.francies.reportdiscordplus.ReportDiscordPlus;

import java.util.concurrent.TimeUnit;

public class PlayerLoginListener {

    private final ReportDiscordPlus plugin;
    private final Scheduler scheduler;

    public PlayerLoginListener(ReportDiscordPlus plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("report.admin")) {

            scheduler.buildTask(plugin, plugin::checkForUpdates)
                    .delay(2, TimeUnit.SECONDS)
                    .schedule();
        }
    }
}
