package velocity.dev.francies.reportdiscordplus.config;

import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(ConfigurationNode config) {
        loadMessages(config);
    }

    private void loadMessages(ConfigurationNode config) {
        ConfigurationNode messagesNode = config.node("messages");
        if (!messagesNode.empty()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : messagesNode.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                String message = entry.getValue().getString();
                this.messages.put(key, translateAlternateColorCodes('&', message));
            }
        }
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "");
    }

    private String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char COLOR_CHAR = '§';
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }
}
