package ve.nottabaker.nottpay.currency.provider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ve.nottabaker.nottpay.currency.CurrencyProvider;

/**
 * Currency provider backed by the Vault economy API.
 * Vault only supports a single server-wide currency.
 */
public class VaultProvider implements CurrencyProvider {

    private Economy economy;

    @Override
    public String getName() {
        return "vault";
    }

    @Override
    public boolean isAvailable() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.economy = rsp.getProvider();
                return this.economy != null;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public double getBalance(Player player, String currency) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    @Override
    public boolean withdraw(Player player, String currency, double amount) {
        if (economy == null) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, String currency, double amount) {
        if (economy == null) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }
}
