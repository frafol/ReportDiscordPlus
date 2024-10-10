package velocity.dev.francies.reportdiscordplus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;
import dev.francies.reportdiscordplus.utility.CooldownManager;

import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import velocity.dev.francies.reportdiscordplus.commands.ReportCommand;
import velocity.dev.francies.reportdiscordplus.config.ConfigurationManager;
import velocity.dev.francies.reportdiscordplus.config.MessageManager;
import velocity.dev.francies.reportdiscordplus.report.ReportManager;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import velocity.dev.francies.reportdiscordplus.utility.PlayerLoginListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

@Plugin(id = "reportdiscordplus", name = "ReportDiscordPlus", version = "6.0", description = "Report plugin with Discord integration", authors = {"Francies"})
public class ReportDiscordPlus {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigurationManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private ReportManager reportManager;
    private final Metrics.Factory metricsFactory;
    private final String versionUrl = "https://www.francescoferrara.it/api/reportdiscordplus.json";
    @Inject
    public ReportDiscordPlus(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 23259;
        Metrics metrics = metricsFactory.make(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("players", proxyServer::getPlayerCount));
        if (!isVelocity()) {
            return;
        }
        proxyServer.getEventManager().register(this, new PlayerLoginListener(this, proxyServer.getScheduler()));
        try {
            configManager = new ConfigurationManager(dataDirectory);
            messageManager = new MessageManager(configManager.getConfig());
            cooldownManager = new CooldownManager(configManager.getConfig());
            reportManager = new ReportManager(proxyServer, messageManager, cooldownManager, configManager);

            proxyServer.getCommandManager().register(String.valueOf(proxyServer.getPluginManager().fromInstance(this).get()), new ReportCommand(reportManager, messageManager, cooldownManager, this), "report");



            logger.info("  +----------------------------------------------------------------------------------------------+");
            logger.info("  |   ____                       _   ____  _                       _ ____  _                     |");
            logger.info("  |  |  _ \\ ___ _ __   ___  _ __| |_|  _ \\(_)___  ___ ___  _ __ __| |  _ \\| |_   _ ___           |");
            logger.info("  |  | |_) / _ \\ '_ \\ / _ \\| '__| __| | | | / __|/ __/ _ \\| '__/ _` | |_) | | | | / __|          |");
            logger.info("  |  |  _ <  __/ |_) | (_) | |  | |_| |_| | \\__ \\ (_| (_) | | | (_| |  __/| | |_| \\__ \\          |");
            logger.info("  |  |_| \\_\\___| .__/ \\___/|_|   \\__|____/|_|___/\\___\\___/|_|  \\__,_|_|   |_|\\__,_|___/          |");
            logger.info("  |            |_|                                                                               |");
            logger.info("  |                                                                                              |");
            logger.info("  +----------------------------------------------------------------------------------------------+");
            logger.info("                                    Version: 6.0");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean isVelocity() {
        try {
            Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("  ______   _______ ");
        logger.info(" | __ ) \\ / / ____|");
        logger.info(" |  _ \\\\ V /|  _|  ");
        logger.info(" | |_) || | | |___ ");
        logger.info(" |____/ |_| |_____|");
        logger.info("                   ");
        logger.info(" Version 6.0");
    }
    public void checkForUpdates() {
        try {
            URL url = new URL(versionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();

            String latestVersion = json.get("version").getAsString();
            String downloadUrl1 = json.get("downloadUrl1").getAsString();
            String downloadUrl2 = json.get("downloadUrl2").getAsString();

            String currentVersion = "6.0";

            if (!currentVersion.equals(latestVersion)) {
                for (Player staffMember : proxyServer.getAllPlayers()) {
                    staffMember.sendMessage(Component.text("§c§l[§6§lRDPLUS§c§l]§r §eA newer version of §9ReportDiscordPlus §eis available: §f" + latestVersion));
                    staffMember.sendMessage(Component.text("§c§l[§6§lRDPLUS§c§l]§r §bDownload link 1: §f" + downloadUrl1));
                    staffMember.sendMessage(Component.text("§c§l[§6§lRDPLUS§c§l]§r §bDownload link 2: §f" + downloadUrl2));
                }
            }

        } catch (Exception e) {
            logger.error("Error checking for updates: ", e);
        }
    }
    public ProxyServer getProxyServer(){
        return proxyServer;
    }
}
