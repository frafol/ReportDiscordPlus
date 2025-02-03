package velocity.me.francies.ReportDiscordPlus.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageManager {

    private final ConfigurationNode config;
    private final Component prefix;

    public MessageManager(ConfigurationNode config) {
        this.config = config;
        // Recuperiamo il prefisso una volta e lo convertiamo in un Component
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(getRawMessage("messages.prefix"));
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

    public List<String> getRawMessageList(String path) throws SerializationException {
        return config.node((Object[]) path.split("\\.")).getList(String.class, List.of());
    }

    public Component getComponentMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        Component componentMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        // Applichiamo il prefisso solo se il path inizia con "messages."
        if (path.startsWith("messages.") && !path.equals("messages.prefix")) {
            return prefix.append(Component.space()).append(componentMessage);
        }

        return componentMessage;
    }

    public List<String> getMessageList(String path, Map<String, String> placeholders) throws SerializationException {
        List<String> messages = getRawMessageList(path);
        return messages.stream()
                .map(message -> replacePlaceholders(message, placeholders))
                .collect(Collectors.toList());
    }

    public List<Component> getComponentMessageList(String path, Map<String, String> placeholders) throws SerializationException {
        List<String> messages = getMessageList(path, placeholders);
        return messages.stream()
                .map(LegacyComponentSerializer.legacyAmpersand()::deserialize)
                .collect(Collectors.toList());
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
