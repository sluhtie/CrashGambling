package com.crashgambling.command;

import com.crashgambling.CrashGambling;
import com.crashgambling.data.PlayerBet;
import com.crashgambling.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CrashCommand implements CommandExecutor, TabCompleter {
    private final CrashGambling plugin;
    
    public CrashCommand(CrashGambling plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "bet":
                return handleBet(sender, args);
            case "cashout":
                return handleCashout(sender);
            case "status":
                return handleStatus(sender);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleBet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&cOnly players can use this command"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(msg("&cUsage: /crash bet <amount>"));
            return true;
        }
        
        Player player = (Player) sender;
        
        GameState state = plugin.getGameManager().getState();
        if (state != GameState.IDLE && state != GameState.PRESTART) {
            sender.sendMessage(msg("messages.cannot-bet-now"));
            return true;
        }
        
        if (plugin.getBetManager().hasBet(player.getUniqueId())) {
            sender.sendMessage(msg("messages.already-bet"));
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg("&cInvalid amount"));
            return true;
        }
        
        if (amount <= 0) {
            sender.sendMessage(msg("&cAmount must be positive"));
            return true;
        }
        
        double minBet = plugin.getConfig().getDouble("game.min-bet");
        double maxBet = plugin.getConfig().getDouble("game.max-bet");
        
        if (amount < minBet) {
            String message = plugin.getConfig().getString("messages.bet-too-low")
                .replace("{min}", String.format("%.2f", minBet));
            sender.sendMessage(msg(message));
            return true;
        }
        
        if (amount > maxBet) {
            String message = plugin.getConfig().getString("messages.bet-too-high")
                .replace("{max}", String.format("%.2f", maxBet));
            sender.sendMessage(msg(message));
            return true;
        }
        
        if (plugin.getEconomy().getBalance(player) < amount) {
            String message = plugin.getConfig().getString("messages.insufficient-funds")
                .replace("{amount}", String.format("%.2f", amount));
            sender.sendMessage(msg(message));
            return true;
        }
        
        if (plugin.getGameManager().placeBet(player, amount)) {
            String message = plugin.getConfig().getString("messages.bet-placed")
                .replace("{amount}", String.format("%.2f", amount));
            sender.sendMessage(msg(message));
        } else {
            sender.sendMessage(msg("&cFailed to place bet"));
        }
        
        return true;
    }
    
    private boolean handleCashout(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&cOnly players can use this command"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (plugin.getGameManager().getState() != GameState.RUNNING) {
            sender.sendMessage(msg("messages.cashout-not-running"));
            return true;
        }
        
        if (!plugin.getBetManager().hasBet(player.getUniqueId())) {
            sender.sendMessage(msg("messages.cashout-no-bet"));
            return true;
        }
        
        Optional<PlayerBet> betOpt = plugin.getBetManager().getBet(player.getUniqueId());
        if (betOpt.isPresent() && betOpt.get().isCashedOut()) {
            sender.sendMessage(msg("messages.cashout-already"));
            return true;
        }
        
        if (!plugin.getGameManager().cashout(player)) {
            sender.sendMessage(msg("&cFailed to cashout"));
        }
        
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        GameState state = plugin.getGameManager().getState();
        
        String stateMessage;
        switch (state) {
            case IDLE:
                stateMessage = plugin.getConfig().getString("messages.status-idle");
                break;
            case PRESTART:
                stateMessage = plugin.getConfig().getString("messages.status-prestart")
                    .replace("{countdown}", String.valueOf(plugin.getGameManager().getPrestartCountdown()))
                    .replace("{player_count}", String.valueOf(plugin.getBetManager().getPlayerCount()));
                break;
            case RUNNING:
                stateMessage = plugin.getConfig().getString("messages.status-running")
                    .replace("{multiplier}", String.format("%.2f", plugin.getGameManager().getCurrentMultiplier()));
                break;
            default:
                stateMessage = "&7Game is transitioning...";
                break;
        }
        
        sender.sendMessage(msg(stateMessage));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Optional<PlayerBet> betOpt = plugin.getBetManager().getBet(player.getUniqueId());
            
            if (betOpt.isPresent()) {
                PlayerBet bet = betOpt.get();
                if (bet.isCashedOut()) {
                    String message = plugin.getConfig().getString("messages.status-bet-cashed")
                        .replace("{multiplier}", String.format("%.2f", bet.getCashoutMultiplier()));
                    sender.sendMessage(msg(message));
                } else {
                    String message = plugin.getConfig().getString("messages.status-bet-active")
                        .replace("{amount}", String.format("%.2f", bet.getAmount()));
                    sender.sendMessage(msg(message));
                }
            } else {
                sender.sendMessage(msg("messages.status-no-bet"));
            }
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("crash.admin")) {
            sender.sendMessage(msg("&cNo permission"));
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getHologramService().reload();
        sender.sendMessage(msg("messages.reload-success"));
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("&6&l⚡ Crash Gambling Commands ⚡"));
        sender.sendMessage(msg("&e/crash bet <amount> &7- Place a bet"));
        sender.sendMessage(msg("&e/crash cashout &7- Cashout during game"));
        sender.sendMessage(msg("&e/crash status &7- View game status"));
        
        if (sender.hasPermission("crash.admin")) {
            sender.sendMessage(msg("&e/crash reload &7- Reload configuration"));
        }
    }
    
    private String msg(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&6[Crash] &r");
        String message;
        
        if (key != null && (key.startsWith("&") || key.startsWith("§"))) {
            message = key;
        } else {
            message = plugin.getConfig().getString(key, key);
        }
        
        return (prefix + message).replace("&", "§");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("bet");
            completions.add("cashout");
            completions.add("status");
            
            if (sender.hasPermission("crash.admin")) {
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bet")) {
            completions.add("100");
            completions.add("500");
            completions.add("1000");
        }
        
        return completions;
    }
}