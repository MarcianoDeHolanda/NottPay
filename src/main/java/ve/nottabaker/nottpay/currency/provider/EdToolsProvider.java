package ve.nottabaker.nottpay.currency.provider;

import es.edwardbelt.edgens.iapi.EdToolsAPI;
import es.edwardbelt.edgens.iapi.EdToolsCurrencyAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ve.nottabaker.nottpay.currency.CurrencyProvider;

/**
 * Currency provider backed by the EdTools plugin.
 * Supports multiple named currencies defined in EdTools.
 */
public class EdToolsProvider implements CurrencyProvider {

    private EdToolsCurrencyAPI currencyAPI;

    @Override
    public String getName() {
        return "edtools";
    }

    @Override
    public boolean isAvailable() {
        if (Bukkit.getPluginManager().getPlugin("EdTools") == null) {
            return false;
        }
        try {
            EdToolsAPI api = EdToolsAPI.getInstance();
            if (api != null) {
                this.currencyAPI = api.getCurrencyAPI();
                return this.currencyAPI != null;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public double getBalance(Player player, String currency) {
        if (currencyAPI == null) return 0;
        return currencyAPI.getCurrency(player.getUniqueId(), currency);
    }

    @Override
    public boolean withdraw(Player player, String currency, double amount) {
        if (currencyAPI == null) return false;
        double balance = currencyAPI.getCurrency(player.getUniqueId(), currency);
        if (balance < amount) return false;
        currencyAPI.removeCurrency(player.getUniqueId(), currency, amount);
        return true;
    }

    @Override
    public boolean deposit(Player player, String currency, double amount) {
        if (currencyAPI == null) return false;
        currencyAPI.addCurrency(player.getUniqueId(), currency, amount);
        return true;
    }
}
