package ve.nottabaker.nottpay.util;

import org.bukkit.configuration.ConfigurationSection;
import ve.nottabaker.nottpay.NottPay;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses amount strings with optional format suffixes (e.g., "10k", "1.5m", "500").
 * Format multipliers are loaded from config.yml under "currency-formats".
 */
public class AmountParser {

    private final Map<String, Double> formatMultipliers = new HashMap<>();
    private boolean enabled;
    private boolean allowDecimals;

    public AmountParser(NottPay plugin) {
        reload(plugin);
    }

    /**
     * Reload format settings from config.
     */
    public void reload(NottPay plugin) {
        formatMultipliers.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("currency-formats");
        if (section == null) {
            enabled = false;
            allowDecimals = true;
            return;
        }

        enabled = section.getBoolean("enabled", true);
        allowDecimals = section.getBoolean("allow-decimals", true);

        ConfigurationSection formats = section.getConfigurationSection("formats");
        if (formats != null) {
            for (String key : formats.getKeys(false)) {
                formatMultipliers.put(key.toLowerCase(), formats.getDouble(key));
            }
        }
    }

    /**
     * Parse an amount string into a double value.
     *
     * @param input The input string (e.g., "10", "10k", "1.5m")
     * @return The parsed amount, or -1 if invalid
     */
    public double parse(String input) {
        if (input == null || input.isEmpty()) return -1;

        input = input.toLowerCase().replace(",", "");

        if (!enabled) {
            try {
                double val = Double.parseDouble(input);
                return val > 0 ? val : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // Check if the last character is a format suffix
        String lastChar = input.substring(input.length() - 1);
        Double multiplier = formatMultipliers.get(lastChar);

        if (multiplier != null) {
            String numberPart = input.substring(0, input.length() - 1);
            if (numberPart.isEmpty()) return -1;

            try {
                double number = Double.parseDouble(numberPart);
                if (!allowDecimals && numberPart.contains(".")) return -1;
                double result = number * multiplier;
                return result > 0 ? result : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // No suffix, parse as plain number
        try {
            double val = Double.parseDouble(input);
            if (!allowDecimals && input.contains(".")) return -1;
            return val > 0 ? val : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
