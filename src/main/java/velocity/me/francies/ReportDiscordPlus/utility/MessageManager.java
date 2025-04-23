package velocity.me.francies.ReportDiscordPlus.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class MessageManager {

    private final ConfigurationNode config;
    private final String prefixString;  // Prefisso in formato stringa
    private final Component prefixComponent;  // Prefisso in formato Component

    public MessageManager(ConfigurationNode config) {
        this.config = config;
        this.prefixString = getRawMessage("messages.prefix");
        this.prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixString);
    }

    /**
     * Restituisce il messaggio senza modifiche, esattamente come salvato nel file di configurazione.
     */
    public String getRawMessage(String path) {
        return config.node((Object[]) path.split("\\.")).getString("");
    }

    /**
     * Restituisce il messaggio con i placeholder sostituiti.
     * Il prefisso compare solo se è presente {prefix}.
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        if (message == null) {
            return "";
        }

        // Se la mappa dei placeholder è nulla, ne creiamo una nuova per sicurezza
        if (placeholders == null) {
            placeholders = new HashMap<>();
        }

        // Aggiungiamo il placeholder {prefix} alla mappa per garantirne la sostituzione
        placeholders.put("prefix", prefixString);

        return replacePlaceholders(message, placeholders);
    }

    /**
     * Restituisce una lista di messaggi dal file di configurazione con i placeholder sostituiti.
     * Il prefisso compare solo se è presente {prefix}.
     */
    public List<String> getMessageList(String path, Map<String, String> placeholders) throws SerializationException {
        List<String> messages = config.node((Object[]) path.split("\\.")).getList(String.class, List.of());

        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // Se la mappa dei placeholder è nulla, ne creiamo una nuova per sicurezza
        if (placeholders == null) {
            placeholders = new HashMap<>();
        }

        // Aggiungiamo il placeholder {prefix} alla mappa
        placeholders.put("prefix", prefixString);

        // Sostituiamo i placeholder in ogni messaggio della lista
        Map<String, String> finalPlaceholders = placeholders;
        return messages.stream()
                .map(message -> replacePlaceholders(message, finalPlaceholders))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce un messaggio in formato Component con i placeholder sostituiti.
     * Il prefisso compare solo se è presente {prefix}.
     */
    public Component getComponentMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    /**
     * Sostituisce i placeholder nel messaggio.
     */
    public String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null) {
            return message != null ? message : "";
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * Converte una stringa con colori in un Component Adventure.
     */
    public Component deserializeMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
