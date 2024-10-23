package velocity.me.francies.ReportDiscordPlus.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;

public class MessageManager {

    private final ConfigurationNode config;

    public MessageManager(ConfigurationNode config) {
        this.config = config;
    }

    public String getRawMessage(String path) {
        return config.node((Object[]) path.split("\\.")).getString("");
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        if (message == null) {
            return "";
        }
        return replacePlaceholders(message, placeholders);
    }

    public Component getComponentMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null) {
            return message != null ? message : "";
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public Component deserializeMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
