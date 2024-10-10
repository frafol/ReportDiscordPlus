package velocity.dev.francies.reportdiscordplus.utility;

import com.velocitypowered.api.proxy.Player;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;

public class CooldownManager {

    private final HashMap<String, Long> cooldowns = new HashMap<>();
    private final int cooldownSeconds;

    public CooldownManager(ConfigurationNode config) {
        this.cooldownSeconds = config.node("cooldown", "time").getInt(60);
    }

    public void setCooldown(Player player) {
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
        this.cooldowns.put(player.getUsername(), cooldownTime);
    }

    public boolean hasCooldown(Player player) {
        return this.cooldowns.containsKey(player.getUsername()) && this.cooldowns.get(player.getUsername()) > System.currentTimeMillis();
    }

    public long getCooldownRemaining(Player player) {
        return (this.cooldowns.get(player.getUsername()) - System.currentTimeMillis()) / 1000L;
    }
}
