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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the /pay command for transferring currencies between players.
 * Also supports /pay all (Admin & Console).
 */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final NottPay plugin;
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.##");
    private static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("Console".getBytes());

    public PayCommand(NottPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        // Player permission check
        if (isPlayer) {
            String permission = plugin.getConfig().getString("pay-command.permission", "nottpay.pay");
            if (!permission.isEmpty() && !player.hasPermission(permission)) {
                player.sendMessage(config.getMessage("general.no-permission"));
                return true;
            }
        }

        // Usage check
        if (args.length < 3) {
            sender.sendMessage(config.getMessage("pay.usage").replace("{command}", label));
            return true;
        }

        String targetName = args[0];
        String currencyName = args[1];
        String amountInput = args[2];

        // Validate currency
        CurrencyManager currencyManager = plugin.getCurrencyManager();
        CurrencyManager.CurrencyEntry currency = currencyManager.getCurrency(currencyName);
        if (currency == null) {
            sender.sendMessage(config.getMessage("pay.invalid-currency")
                    .replace("{currency}", currencyName));
            return true;
        }

        // Parse amount
        double amount = plugin.getAmountParser().parse(amountInput);
        if (amount <= 0) {
            sender.sendMessage(config.getMessage("pay.invalid-amount")
                    .replace("{input}", amountInput));
            return true;
        }

        if (amount < 0.01) {
            sender.sendMessage(config.getMessage("pay.min-amount")
                    .replace("{min}", "0.01"));
            return true;
        }

        String formattedAmount = AMOUNT_FORMAT.format(amount);
        String displayName = ConfigManager.translateColors(currency.displayName());

        // Handle /pay all
        if (targetName.equalsIgnoreCase("all") || targetName.equals("*")) {
            if (!sender.hasPermission("nottpay.admin") && !sender.hasPermission("nottpay.admin.payall")) {
                sender.sendMessage(config.getMessage("general.no-permission"));
                return true;
            }

            List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !isPlayer || !p.getUniqueId().equals(player.getUniqueId()))
                    .collect(Collectors.toList());

            if (onlinePlayers.isEmpty()) {
                sender.sendMessage(config.getMessage("pay.all-no-players"));
                return true;
            }

            double totalAmount = amount * onlinePlayers.size();

            // If player, check if they can afford the total
            if (isPlayer) {
                double balance = currencyManager.getBalance(player, currencyName);
                if (balance < totalAmount) {
                    player.sendMessage(config.getMessage("pay.insufficient-funds")
                            .replace("{currency}", displayName)
                            .replace("{balance}", AMOUNT_FORMAT.format(balance)));
                    return true;
                }
                
                if (!currencyManager.withdraw(player, currencyName, totalAmount)) {
                    player.sendMessage(config.getMessage("pay.insufficient-funds")
                            .replace("{currency}", displayName)
                            .replace("{balance}", AMOUNT_FORMAT.format(balance)));
                    return true;
                }
            }

            // Distribute to all
            String senderName = isPlayer ? player.getName() : "Console";
            UUID senderId = isPlayer ? player.getUniqueId() : CONSOLE_UUID;
            int successfulDeposits = 0;

            for (Player target : onlinePlayers) {
                if (currencyManager.deposit(target, currencyName, amount)) {
                    successfulDeposits++;
                    
                    target.sendMessage(config.getMessage("pay.success-receiver")
                            .replace("{amount}", formattedAmount)
                            .replace("{currency}", displayName)
                            .replace("{sender}", senderName));

                    Transaction transaction = new Transaction(
                            senderId, target.getUniqueId(), senderName, target.getName(),
                            currencyName, amount, System.currentTimeMillis()
                    );
                    plugin.getTransactionManager().addTransaction(transaction);
                }
            }

            sender.sendMessage(config.getMessage("pay.all-success")
                    .replace("{amount}", formattedAmount)
                    .replace("{currency}", displayName)
                    .replace("{count}", String.valueOf(successfulDeposits)));
            return true;
        }

        // Handle single payment
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(config.getMessage("pay.player-not-found")
                    .replace("{player}", targetName));
            return true;
        }

        if (isPlayer && target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(config.getMessage("pay.self-pay"));
            return true;
        }

        if (isPlayer) {
            double balance = currencyManager.getBalance(player, currencyName);
            if (balance < amount) {
                player.sendMessage(config.getMessage("pay.insufficient-funds")
                        .replace("{currency}", displayName)
                        .replace("{balance}", AMOUNT_FORMAT.format(balance)));
                return true;
            }

            if (!currencyManager.withdraw(player, currencyName, amount)) {
                player.sendMessage(config.getMessage("pay.insufficient-funds")
                        .replace("{currency}", displayName)
                        .replace("{balance}", AMOUNT_FORMAT.format(balance)));
                return true;
            }
        }

        if (!currencyManager.deposit(target, currencyName, amount)) {
            if (isPlayer) {
                currencyManager.deposit(player, currencyName, amount); // Rollback
            }
            sender.sendMessage(config.getMessage("pay.invalid-currency")
                    .replace("{currency}", currencyName));
            return true;
        }

        String senderName = isPlayer ? player.getName() : "Console";
        UUID senderId = isPlayer ? player.getUniqueId() : CONSOLE_UUID;

        sender.sendMessage(config.getMessage("pay.success-sender")
                .replace("{amount}", formattedAmount)
                .replace("{currency}", displayName)
                .replace("{receiver}", target.getName()));

        target.sendMessage(config.getMessage("pay.success-receiver")
                .replace("{amount}", formattedAmount)
                .replace("{currency}", displayName)
                .replace("{sender}", senderName));

        Transaction transaction = new Transaction(
                senderId, target.getUniqueId(), senderName, target.getName(),
                currencyName, amount, System.currentTimeMillis()
        );
        plugin.getTransactionManager().addTransaction(transaction);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isPlayer = sender instanceof Player;
        boolean isAdmin = sender.hasPermission("nottpay.admin") || sender.hasPermission("nottpay.admin.payall");

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> list = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !isPlayer || !p.getUniqueId().equals(((Player) sender).getUniqueId()))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
            if (isAdmin && "all".startsWith(input)) {
                list.add("all");
            }
            return list;
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return plugin.getCurrencyManager().getCurrencyNames().stream()
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
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
