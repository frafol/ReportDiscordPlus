package velocity.dev.francies.reportdiscordplus.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;


public class ConfigurationManager {

    private final Path dataDirectory;
    private ConfigurationNode config;

    public ConfigurationManager(Path dataDirectory) throws IOException {
        this.dataDirectory = dataDirectory;
        loadConfig();
    }

    private void loadConfig() throws IOException {
        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdir();
        }

        File configFile = dataDirectory.resolve("config.yml").toFile();
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        if (!configFile.exists()) {
            try (OutputStream outputStream = new FileOutputStream(configFile)) {
                String defaultConfig = getDefaultConfig();
                outputStream.write(defaultConfig.getBytes());
            }
        }

        this.config = loader.load();
    }

    private String getDefaultConfig() {
        return """
            discord:
              webhookUrl: "YOUR_DISCORD_WEBHOOK_URL"
              pingRoleID: "YOUR_DISCORD_PING_ROLE_ID"
              allert: ":warning: New report available! :warning:"
              title: ":hammer_pick: ReportDiscordPlus :hammer_pick:"
              reaction: "\\uD83D\\uDC4D"

            cooldown: 60  # If left empty, the default value will be 60 seconds

            messages:
              playerNotFound: "&6&lReport&7&lDSPL &cPlayer not found."
              onlineplayer: "&6&lReport&7&lDSPL &cThis player is not online"
              cannotReportPlayer: "&6&lReport&7&lDSPL &cYou cannot report this player."
              missingReason: "&6&lReport&7&lDSPL &cYou must specify a reason for the report."
              cooldownMessage: "&6&lReport&7&lDSPL &cYou have to wait &9{timeRemaining} &cseconds before you can send a new report."
              reportSent: "&6&lReport&7&lDSPL &aYour report has been sent successfully!"
              noPermission: "&6&lReport&7&lDSPL &cYou do not have permission to run this command."
              consoleCommand: "&6&lReport&7&lDSPL &cThis command can only be performed by a player."
              staffAlert: "&6&lReport&7&lDSPL &6&lNew report available!&r &6Player: &9{player} &6- Reported Player: &9{reportedPlayer} &6- Reason: &9{reason} &6- Server: &9{server}"
              myself: "&6&lReport&7&lDSPL &cYou cannot report yourself!"

            title: "&6&lNEW REPORT"
            subtitle: "&9READ BELOW"

            # Permissions:
            # report.use            - Enables the use of the report command
            # report.bypasscooldown - Grants immunity to the cooldown
            # report.mcreport       - Permit to view a staff alert message when a new report is made
            # report.protection     - Grant immunity to be reported
            """;
    }

    public ConfigurationNode getConfig() {
        return this.config;
    }
}
