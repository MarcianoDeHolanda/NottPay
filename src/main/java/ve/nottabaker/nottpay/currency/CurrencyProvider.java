package ve.nottabaker.nottpay.currency;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface for currency economy providers.
 * Each provider wraps an external economy plugin (Vault, EdTools, etc.)
 */
public interface CurrencyProvider {

    /**
     * @return The internal name of this provider (e.g., "vault", "edtools")
     */
    String getName();

    /**
     * @return true if the backing economy plugin is loaded and available
     */
    boolean isAvailable();

    /**
     * Get the balance for a player in a specific currency.
     *
     * @param player   The player
     * @param currency The currency identifier within this provider
     * @return The player's balance
     */
    double getBalance(Player player, String currency);

    /**
     * Withdraw an amount from a player's balance.
     *
     * @param player   The player
     * @param currency The currency identifier
     * @param amount   The amount to withdraw
     * @return true if the withdrawal was successful
     */
    boolean withdraw(Player player, String currency, double amount);

    /**
     * Deposit an amount into a player's balance.
     *
     * @param player   The player
     * @param currency The currency identifier
     * @param amount   The amount to deposit
     * @return true if the deposit was successful
     */
    boolean deposit(Player player, String currency, double amount);
}
