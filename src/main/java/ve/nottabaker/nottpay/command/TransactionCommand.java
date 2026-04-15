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

        // Only players can use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("general.only-players"));
            return true;
        }

        // Permission check
        String permission = plugin.getConfig().getString("transaction-command.permission", "nottpay.transactions");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage(config.getMessage("general.no-permission"));
            return true;
        }

        int perPage = plugin.getConfig().getInt("transaction-command.per-page", 10);
        int page = 1;

        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int totalPages = plugin.getTransactionManager().getTotalPages(player.getUniqueId(), perPage);
        List<Transaction> transactions = plugin.getTransactionManager()
                .getTransactionsPaged(player.getUniqueId(), page, perPage);

        // No transactions
        if (transactions.isEmpty() && page == 1) {
            player.sendMessage(config.getMessage("transactions.no-transactions"));
            return true;
        }

        // Page out of range
        if (transactions.isEmpty()) {
            player.sendMessage(config.getMessage("transactions.page-not-found")
                    .replace("{page}", String.valueOf(page)));
            return true;
        }

        // Header
        player.sendMessage(config.getRawMessage("transactions.header")
                .replace("{page}", String.valueOf(page))
                .replace("{max_page}", String.valueOf(totalPages)));

        // Transaction entries
        for (Transaction tx : transactions) {
            String dateStr = DATE_FORMAT.format(new Date(tx.getTimestamp()));
            String formattedAmount = AMOUNT_FORMAT.format(tx.getAmount());

            // Resolve display name for the currency
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

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest page numbers
            if (sender instanceof Player player) {
                int perPage = plugin.getConfig().getInt("transaction-command.per-page", 10);
                int totalPages = plugin.getTransactionManager().getTotalPages(player.getUniqueId(), perPage);
                List<String> pages = new java.util.ArrayList<>();
                for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                    pages.add(String.valueOf(i));
                }
                return pages;
            }
        }
        return Collections.emptyList();
    }
}
