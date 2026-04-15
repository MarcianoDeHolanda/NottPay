package ve.nottabaker.nottpay.command;

import org.bukkit.Bukkit;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /pay command for transferring currencies between players.
 * Usage: /pay <player> <currency> <amount>
 */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final NottPay plugin;
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.##");

    public PayCommand(NottPay plugin) {
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
        String permission = plugin.getConfig().getString("pay-command.permission", "nottpay.pay");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage(config.getMessage("general.no-permission"));
            return true;
        }

        // Usage check
        if (args.length < 3) {
            player.sendMessage(config.getMessage("pay.usage")
                    .replace("{command}", label));
            return true;
        }

        String targetName = args[0];
        String currencyName = args[1];
        String amountInput = args[2];

        // Find target player (must be online)
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(config.getMessage("pay.player-not-found")
                    .replace("{player}", targetName));
            return true;
        }

        // Can't pay yourself
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(config.getMessage("pay.self-pay"));
            return true;
        }

        // Validate currency
        CurrencyManager currencyManager = plugin.getCurrencyManager();
        CurrencyManager.CurrencyEntry currency = currencyManager.getCurrency(currencyName);
        if (currency == null) {
            player.sendMessage(config.getMessage("pay.invalid-currency")
                    .replace("{currency}", currencyName));
            return true;
        }

        // Parse amount
        double amount = plugin.getAmountParser().parse(amountInput);
        if (amount <= 0) {
            player.sendMessage(config.getMessage("pay.invalid-amount")
                    .replace("{input}", amountInput));
            return true;
        }

        // Check minimum amount
        if (amount < 0.01) {
            player.sendMessage(config.getMessage("pay.min-amount")
                    .replace("{min}", "0.01"));
            return true;
        }

        // Check balance
        double balance = currencyManager.getBalance(player, currencyName);
        String formattedAmount = AMOUNT_FORMAT.format(amount);
        String displayName = ConfigManager.translateColors(currency.displayName());

        if (balance < amount) {
            player.sendMessage(config.getMessage("pay.insufficient-funds")
                    .replace("{currency}", displayName)
                    .replace("{balance}", AMOUNT_FORMAT.format(balance)));
            return true;
        }

        // Perform transaction
        boolean withdrawn = currencyManager.withdraw(player, currencyName, amount);
        if (!withdrawn) {
            player.sendMessage(config.getMessage("pay.insufficient-funds")
                    .replace("{currency}", displayName)
                    .replace("{balance}", AMOUNT_FORMAT.format(balance)));
            return true;
        }

        boolean deposited = currencyManager.deposit(target, currencyName, amount);
        if (!deposited) {
            // Rollback: give money back to sender
            currencyManager.deposit(player, currencyName, amount);
            player.sendMessage(config.getMessage("pay.invalid-currency")
                    .replace("{currency}", currencyName));
            return true;
        }

        // Send success messages
        player.sendMessage(config.getMessage("pay.success-sender")
                .replace("{amount}", formattedAmount)
                .replace("{currency}", displayName)
                .replace("{receiver}", target.getName()));

        target.sendMessage(config.getMessage("pay.success-receiver")
                .replace("{amount}", formattedAmount)
                .replace("{currency}", displayName)
                .replace("{sender}", player.getName()));

        // Record transaction
        Transaction transaction = new Transaction(
                player.getUniqueId(),
                target.getUniqueId(),
                player.getName(),
                target.getName(),
                currencyName,
                amount,
                System.currentTimeMillis()
        );
        plugin.getTransactionManager().addTransaction(transaction);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        String permission = plugin.getConfig().getString("pay-command.permission", "nottpay.pay");
        if (!permission.isEmpty() && !sender.hasPermission(permission)) return Collections.emptyList();

        if (args.length == 1) {
            // Tab complete: online player names (exclude self)
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(((Player) sender).getUniqueId()))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // Tab complete: currency names
            String input = args[1].toLowerCase();
            return plugin.getCurrencyManager().getCurrencyNames().stream()
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            // Tab complete: suggested amounts
            List<String> suggestions = new ArrayList<>();
            suggestions.add("1");
            suggestions.add("10");
            suggestions.add("100");
            suggestions.add("1k");
            suggestions.add("10k");
            suggestions.add("100k");
            suggestions.add("1m");
            String input = args[2].toLowerCase();
            return suggestions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
