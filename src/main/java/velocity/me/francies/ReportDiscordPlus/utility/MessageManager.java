package velocity.me.francies.ReportDiscordPlus.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final ConfigurationNode config;
    private final Component prefixComponent;      // Versione "Component" del prefisso
    private final String prefixString;            // Versione "String" del prefisso (utile per il placeholder {prefix})
    private final boolean prefixPlaceholder;      // Se true, mostra il prefisso solo se usiamo {prefix}

    public MessageManager(ConfigurationNode config) {
        this.config = config;
        // Recuperiamo il valore boolean dal config (di default false se non specificato)
        this.prefixPlaceholder = config.node("messages", "prefixPlaceholder").getBoolean(false);

        // Recuperiamo il prefisso in formato stringa
        this.prefixString = getRawMessage("messages.prefix");
        // Convertiamo il prefisso in formato Component
        this.prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixString);
    }

    /**
     * Restituisce la stringa pura dal config, senza alcuna sostituzione.
     *
     * @param path path nel file di configurazione
     * @return la stringa corrispondente alla path
     */
    public String getRawMessage(String path) {
        return config.node((Object[]) path.split("\\.")).getString("");
    }

    /**
     * Restituisce la stringa dal config, con i placeholder sostituiti.
     *
     * @param path path nel file di configurazione
     * @param placeholders mappa di placeholder da sostituire
     * @return la stringa con i placeholder sostituiti
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        if (message == null) {
            return "";
        }

        // Se non ci sono placeholder in ingresso, inizializziamo una mappa vuota
        if (placeholders == null) {
            placeholders = new HashMap<>();
        }

        // Se prefixPlaceholder è true, allora {prefix} verrà sostituito nel testo
        // solo se esiste esplicitamente. Non aggiungiamo automaticamente il prefisso davanti.
        // Per far funzionare {prefix}, aggiungiamo la chiave "prefix" nella mappa.
        if (prefixPlaceholder) {
            placeholders.put("prefix", prefixString);
        }

        return replacePlaceholders(message, placeholders);
    }

    /**
     * Restituisce una lista di stringhe dal config (ad esempio per messaggi multiline).
     *
     * @param path path nel file di configurazione
     * @return lista di stringhe
     * @throws SerializationException in caso di errori nella lettura della lista
     */
    public List<String> getRawMessageList(String path) throws SerializationException {
        return config.node((Object[]) path.split("\\.")).getList(String.class, List.of());
    }

    /**
     * Restituisce un Component (Adventure) corrispondente alla stringa recuperata dal config,
     * con eventuali placeholder sostituiti. Se prefixPlaceholder è false, il prefisso
     * verrà anteposto a tutti i messaggi che si trovano sotto "messages." (eccetto il prefisso stesso).
     *
     * @param path path nel file di configurazione
     * @param placeholders mappa di placeholder da sostituire
     * @return il messaggio in formato Component
     */
    public Component getComponentMessage(String path, Map<String, String> placeholders) {
        // Evitiamo di aggiungere il prefisso al path "messages.prefix" stesso
        boolean isPrefixItself = path.equals("messages.prefix");
        boolean isMessagePath = path.startsWith("messages.");

        String finalMessage = getMessage(path, placeholders);
        if (finalMessage.isEmpty()) {
            return Component.empty();
        }

        // Convertiamo la stringa in Component
        Component componentMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(finalMessage);

        // Se prefixPlaceholder è false, vogliamo anteporre il prefisso (solo se è un messaggio "normale")
        // e non è la key "messages.prefix" stessa.
        if (!prefixPlaceholder && isMessagePath && !isPrefixItself) {
            return prefixComponent.append(Component.space()).append(componentMessage);
        }

        // Se prefixPlaceholder è true, il prefisso compare solo se c'è {prefix} nel messaggio (già sostituito).
        // Quindi ritorniamo direttamente componentMessage.
        return componentMessage;
    }

    /**
     * Sostituisce i placeholder {chiave} con i rispettivi valori nella stringa di ingresso.
     *
     * @param message stringa di ingresso
     * @param placeholders mappa di chiavi-valori
     * @return la stringa con i placeholder sostituiti
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
     * Deserializza una stringa colorata in un Component Adventure.
     *
     * @param message stringa colorata
     * @return Component corrispondente
     */
    public Component deserializeMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
