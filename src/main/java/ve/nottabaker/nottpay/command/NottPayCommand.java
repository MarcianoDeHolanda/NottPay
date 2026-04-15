package ve.nottabaker.nottpay.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ve.nottabaker.nottpay.NottPay;
import ve.nottabaker.nottpay.config.ConfigManager;

import java.util.Collections;
import java.util.List;

/**
 * Handles admin interactions with the plugin.
 */
public class NottPayCommand implements CommandExecutor, TabCompleter {
    private final NottPay plugin;

    public NottPayCommand(NottPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        if (!sender.hasPermission("nottpay.admin")) {
            sender.sendMessage(config.getMessage("general.no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            config.loadAll();
            plugin.getAmountParser().reload(plugin);
            plugin.getCurrencyManager().setup();
            sender.sendMessage(config.getMessage("general.reload-success"));
            return true;
        }

        sender.sendMessage(config.getMessage("general.admin-usage")
                .replace("{command}", label));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("nottpay.admin") && args.length == 1) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
