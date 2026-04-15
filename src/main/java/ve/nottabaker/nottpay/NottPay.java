package ve.nottabaker.nottpay;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ve.nottabaker.nottpay.command.NottPayCommand;
import ve.nottabaker.nottpay.command.PayCommand;
import ve.nottabaker.nottpay.command.TransactionCommand;
import ve.nottabaker.nottpay.config.ConfigManager;
import ve.nottabaker.nottpay.currency.CurrencyManager;
import ve.nottabaker.nottpay.transaction.TransactionManager;
import ve.nottabaker.nottpay.util.AmountParser;
import ve.nottabaker.nottpay.util.CooldownManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/**
 * NottPay - Multi-currency payment plugin for Paper servers.
 * Supports Vault and EdTools economy providers with configurable commands
 * and transaction history.
 *
 * @author nottabaker
 */
public class NottPay extends JavaPlugin {

    private static NottPay instance;

    private ConfigManager configManager;
    private CurrencyManager currencyManager;
    private TransactionManager transactionManager;
    private AmountParser amountParser;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load configurations
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Initialize amount parser
        amountParser = new AmountParser(this);

        // Initialize currency manager
        currencyManager = new CurrencyManager(this);
        currencyManager.setup();

        // Initialize transaction manager
        transactionManager = new TransactionManager(this);

        // Initialize cooldown manager and register as listener
        cooldownManager = new CooldownManager(this);
        Bukkit.getPluginManager().registerEvents(cooldownManager, this);

        // Register commands dynamically
        registerCommands();


        getLogger().info("NottPay v" + getPluginMeta().getVersion() + " enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Close database pool on shutdown
        if (transactionManager != null) {
            transactionManager.close();
        }

        getLogger().info("NottPay disabled.");
    }

    /**
     * Registers commands dynamically based on config.yml settings.
     * Uses reflection to create PluginCommand instances with custom names and aliases.
     */
    private void registerCommands() {
        // Pay command
        String payName = getConfig().getString("pay-command.name", "pay");
        List<String> payAliases = getConfig().getStringList("pay-command.aliases");
        PayCommand payExecutor = new PayCommand(this);

        PluginCommand payCommand = createPluginCommand(payName);
        if (payCommand != null) {
            payCommand.setAliases(payAliases);
            payCommand.setExecutor(payExecutor);
            payCommand.setTabCompleter(payExecutor);
            payCommand.setDescription("Transfer currency to another player");
            payCommand.setUsage("/<command> <player> <currency> <amount>");
            registerToCommandMap(payCommand);
            getLogger().info("Registered command: /" + payName + " (aliases: " + payAliases + ")");
        }

        // Transaction command
        String txName = getConfig().getString("transaction-command.name", "transacciones");
        List<String> txAliases = getConfig().getStringList("transaction-command.aliases");
        TransactionCommand txExecutor = new TransactionCommand(this);

        PluginCommand txCommand = createPluginCommand(txName);
        if (txCommand != null) {
            txCommand.setAliases(txAliases);
            txCommand.setExecutor(txExecutor);
            txCommand.setTabCompleter(txExecutor);
            txCommand.setDescription("View your transaction history");
            txCommand.setUsage("/<command> [page]");
            registerToCommandMap(txCommand);
            getLogger().info("Registered command: /" + txName + " (aliases: " + txAliases + ")");
        }

        // Admin command
        NottPayCommand adminExecutor = new NottPayCommand(this);
        PluginCommand adminCommand = createPluginCommand("nottpay");
        if (adminCommand != null) {
            adminCommand.setExecutor(adminExecutor);
            adminCommand.setTabCompleter(adminExecutor);
            adminCommand.setDescription("Admin commands for NottPay");
            registerToCommandMap(adminCommand);
            getLogger().info("Registered command: /nottpay");
        }
    }

    /**
     * Creates a PluginCommand instance via reflection (since the constructor is package-private).
     */
    private PluginCommand createPluginCommand(String name) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, this);
        } catch (Exception e) {
            getLogger().severe("Failed to create command: " + name);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Registers a PluginCommand to the server's CommandMap via reflection.
     */
    private void registerToCommandMap(PluginCommand command) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            commandMap.register(getPluginMeta().getName().toLowerCase(), command);
        } catch (Exception e) {
            getLogger().severe("Failed to register command to CommandMap: " + command.getName());
            e.printStackTrace();
        }
    }

    // ---- Accessors ----

    public static NottPay getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public AmountParser getAmountParser() {
        return amountParser;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
