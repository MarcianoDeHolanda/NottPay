package ve.nottabaker.nottpay.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ve.nottabaker.nottpay.NottPay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages loading and access to the plugin's configuration files:
 * config.yml, messages.yml, and currencies.yml.
 */
public class ConfigManager {

    private final NottPay plugin;

    private FileConfiguration messagesConfig;
    private FileConfiguration currenciesConfig;

    public ConfigManager(NottPay plugin) {
        this.plugin = plugin;
    }

    /**
     * Load or reload all configuration files.
     */
    public void loadAll() {
        // config.yml - handled by Bukkit's default mechanism
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // messages.yml
        messagesConfig = loadCustomConfig("messages.yml");

        // currencies.yml
        currenciesConfig = loadCustomConfig("currencies.yml");
    }

    /**
     * Load a custom YAML config file, saving the default from resources if it doesn't exist.
     */
    private FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Set defaults from embedded resource
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
        return config;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getCurrenciesConfig() {
        return currenciesConfig;
    }

    /**
     * Get a message from messages.yml with the plugin prefix prepended.
     *
     * @param path The message path (e.g., "pay.success-sender")
     * @return The formatted message with color codes translated, or a fallback
     */
    public String getMessage(String path) {
        String prefix = plugin.getConfig().getString("settings.prefix", "&6&lNottPay &8» &r");
        String message = messagesConfig.getString(path);
        if (message == null) {
            return translateColors(prefix + "&cMissing message: " + path);
        }
        return translateColors(prefix + message);
    }

    /**
     * Get a raw message without prefix.
     */
    public String getRawMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return translateColors("&cMissing message: " + path);
        }
        return translateColors(message);
    }

    /**
     * Translate color codes (& → §) in a string.
     */
    public static String translateColors(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
