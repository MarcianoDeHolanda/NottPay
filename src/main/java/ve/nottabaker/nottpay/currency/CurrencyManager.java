package ve.nottabaker.nottpay.currency;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ve.nottabaker.nottpay.NottPay;
import ve.nottabaker.nottpay.currency.provider.EdToolsProvider;
import ve.nottabaker.nottpay.currency.provider.VaultProvider;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages currency providers and maps currency names to their providers.
 * Reads currency definitions from currencies.yml.
 */
public class CurrencyManager {

    private final NottPay plugin;
    private final Map<String, CurrencyProvider> providers = new HashMap<>();
    private final Map<String, CurrencyEntry> currencies = new LinkedHashMap<>();

    public CurrencyManager(NottPay plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all providers and loads currency definitions.
     */
    public void setup() {
        Logger log = plugin.getLogger();
        providers.clear();
        currencies.clear();

        // Register providers
        registerProvider(new VaultProvider(), log);
        registerProvider(new EdToolsProvider(), log);

        // Load currency definitions from currencies.yml
        ConfigurationSection section = plugin.getConfigManager().getCurrenciesConfig().getConfigurationSection("currencies");
        if (section == null) {
            log.warning("No currencies defined in currencies.yml!");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection currSec = section.getConfigurationSection(key);
            if (currSec == null) continue;

            String providerName = currSec.getString("provider", "").toLowerCase();
            String displayName = currSec.getString("display-name", key);
            String edtoolsCurrency = currSec.getString("edtools-currency", key);

            CurrencyProvider provider = providers.get(providerName);
            if (provider == null) {
                log.warning("Currency '" + key + "' uses unknown provider: " + providerName);
                continue;
            }

            if (!provider.isAvailable()) {
                log.warning("Currency '" + key + "' skipped - provider '" + providerName + "' is not available.");
                continue;
            }

            String providerCurrencyId = providerName.equals("edtools") ? edtoolsCurrency : key;
            // Bypass booster by default for EdTools to prevent payment duplication
            boolean bypassBooster = currSec.getBoolean("bypass-booster", true);
            currencies.put(key.toLowerCase(), new CurrencyEntry(key, displayName, provider, providerCurrencyId, bypassBooster));
            log.info("Registered currency: " + key + " (provider: " + providerName + ", bypass-booster: " + bypassBooster + ")");
        }

        log.info("Loaded " + currencies.size() + " currencies.");
    }

    private void registerProvider(CurrencyProvider provider, Logger log) {
        if (provider.isAvailable()) {
            providers.put(provider.getName(), provider);
            log.info("Economy provider registered: " + provider.getName());
        } else {
            log.info("Economy provider not available: " + provider.getName());
        }
    }

    /**
     * @return An unmodifiable set of currency names
     */
    public Set<String> getCurrencyNames() {
        return Collections.unmodifiableSet(currencies.keySet());
    }

    /**
     * @param name Currency name (case-insensitive)
     * @return The currency entry, or null if not found
     */
    public CurrencyEntry getCurrency(String name) {
        return currencies.get(name.toLowerCase());
    }

    /**
     * Get a player's balance for a currency.
     */
    public double getBalance(Player player, String currencyName) {
        CurrencyEntry entry = getCurrency(currencyName);
        if (entry == null) return 0;
        return entry.provider().getBalance(player, entry.providerCurrencyId());
    }

    /**
     * Withdraw from a player's balance.
     */
    public boolean withdraw(Player player, String currencyName, double amount) {
        CurrencyEntry entry = getCurrency(currencyName);
        if (entry == null) return false;
        return entry.provider().withdraw(player, entry.providerCurrencyId(), amount);
    }

    /**
     * Deposit into a player's balance, respecting bypass-booster config.
     */
    public boolean deposit(Player player, String currencyName, double amount) {
        CurrencyEntry entry = getCurrency(currencyName);
        if (entry == null) return false;
        // If EdTools provider and bypass-booster is enabled, use direct set
        if (entry.provider() instanceof ve.nottabaker.nottpay.currency.provider.EdToolsProvider edtp) {
            return edtp.deposit(player, entry.providerCurrencyId(), amount, entry.bypassBooster());
        }
        return entry.provider().deposit(player, entry.providerCurrencyId(), amount);
    }

    /**
     * Represents a registered currency mapping.
     *
     * @param bypassBooster If true (default), EdTools deposits use setCurrency() to avoid
     *                      triggering EdToolsCurrencyAddEvent (where boosters hook).
     *                      Set to false in currencies.yml if you WANT boosters to apply.
     */
    public record CurrencyEntry(String id, String displayName, CurrencyProvider provider,
                                String providerCurrencyId, boolean bypassBooster) {
    }
}
