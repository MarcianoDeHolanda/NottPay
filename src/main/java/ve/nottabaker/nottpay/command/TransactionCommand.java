package ve.nottabaker.nottpay.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ve.nottabaker.nottpay.NottPay;
import ve.nottabaker.nottpay.config.ConfigManager;
import ve.nottabaker.nottpay.currency.CurrencyManager;
import ve.nottabaker.nottpay.transaction.Transaction;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Handles the /transacciones command to view payment history.
 * Usage: /transacciones [page]
 * DB queries are done asynchronously to avoid main-thread lag on remote MySQL.
 */
public class TransactionCommand implements CommandExecutor, TabCompleter {

    private final NottPay plugin;
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.##");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm");

    public TransactionCommand(NottPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("general.only-players"));
            return true;
        }

        String permission = plugin.getConfig().getString("transaction-command.permission", "nottpay.transactions");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage(config.getMessage("general.no-permission"));
            return true;
        }

        // Cooldown check (bypass for admins)
        if (!player.hasPermission("nottpay.bypass.cooldown")) {
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), "transactions");
            if (remaining > 0) {
                player.sendMessage(config.getMessage("general.cooldown")
                        .replace("{time}", String.valueOf(remaining)));
                return true;
            }
        }

        int perPage = plugin.getConfig().getInt("transaction-command.per-page", 10);
        int page = 1;

        if (args.length >= 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        final int finalPage = page;

        // Step 1: get total pages async, then step 2: get the page data async
        plugin.getTransactionManager().getTotalPages(player.getUniqueId(), perPage, totalPages -> {

            // Validate page number (this runs on main thread via callback)
            if (finalPage > totalPages && totalPages > 0) {
                player.sendMessage(config.getMessage("transactions.page-not-found")
                        .replace("{page}", String.valueOf(finalPage)));
                return;
            }

            // Step 2: fetch the transactions for this page async
            plugin.getTransactionManager().getTransactionsPaged(player.getUniqueId(), finalPage, perPage, transactions -> {
                if (transactions.isEmpty()) {
                    player.sendMessage(config.getMessage("transactions.no-transactions"));
                    return;
                }

                // Header
                player.sendMessage(config.getRawMessage("transactions.header")
                        .replace("{page}", String.valueOf(finalPage))
                        .replace("{max_page}", String.valueOf(totalPages)));

                // Transaction entries
                for (Transaction tx : transactions) {
                    String dateStr = DATE_FORMAT.format(new Date(tx.getTimestamp()));
                    String formattedAmount = AMOUNT_FORMAT.format(tx.getAmount());

                    CurrencyManager.CurrencyEntry currencyEntry = plugin.getCurrencyManager().getCurrency(tx.getCurrency());
                    String currencyDisplay = currencyEntry != null
                            ? ConfigManager.translateColors(currencyEntry.displayName())
                            : tx.getCurrency();

                    boolean isSender = tx.getSender().equals(player.getUniqueId());

                    String message;
                    if (isSender) {
                        message = config.getRawMessage("transactions.sent")
                                .replace("{date}", dateStr)
                                .replace("{amount}", formattedAmount)
                                .replace("{currency}", currencyDisplay)
                                .replace("{receiver}", tx.getReceiverName());
                    } else {
                        message = config.getRawMessage("transactions.received")
                                .replace("{date}", dateStr)
                                .replace("{amount}", formattedAmount)
                                .replace("{currency}", currencyDisplay)
                                .replace("{sender}", tx.getSenderName());
                    }
                    player.sendMessage(message);
                }

                // Footer
                player.sendMessage(config.getRawMessage("transactions.footer"));

                // Set cooldown after successful display
                plugin.getCooldownManager().setCooldown(player.getUniqueId(), "transactions");
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Page number suggestions are now async-only; return empty for simplicity
        return Collections.emptyList();
    }
}
