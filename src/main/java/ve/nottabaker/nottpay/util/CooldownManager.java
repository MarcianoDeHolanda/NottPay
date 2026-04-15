package ve.nottabaker.nottpay.util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ve.nottabaker.nottpay.NottPay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player, per-command cooldowns using an in-memory HashMap.
 * Cleans up entries when players disconnect to avoid memory leaks.
 * Cooldown durations are read from config.yml under "cooldowns".
 */
public class CooldownManager implements Listener {

    private final NottPay plugin;

    // Map<PlayerUUID, Map<CommandKey, LastUsedTimestamp>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager(NottPay plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a player is currently on cooldown for a command.
     *
     * @param uuid    Player UUID
     * @param command Command key (e.g., "pay", "transactions")
     * @return true if the player must wait
     */
    public boolean isOnCooldown(UUID uuid, String command) {
        return getRemainingSeconds(uuid, command) > 0;
    }

    /**
     * Get how many seconds remain on a player's cooldown.
     *
     * @param uuid    Player UUID
     * @param command Command key
     * @return Remaining seconds, or 0 if not on cooldown
     */
    public long getRemainingSeconds(UUID uuid, String command) {
        int cooldownSeconds = plugin.getConfig().getInt("cooldowns." + command, 0);
        if (cooldownSeconds <= 0) return 0; // Cooldown disabled

        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;

        Long lastUsed = playerCooldowns.get(command);
        if (lastUsed == null) return 0;

        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        long remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Start the cooldown for a player on a specific command.
     *
     * @param uuid    Player UUID
     * @param command Command key
     */
    public void setCooldown(UUID uuid, String command) {
        cooldowns
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .put(command, System.currentTimeMillis());
    }

    /**
     * Clean up cooldown data when a player disconnects to prevent memory leaks.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }
}
