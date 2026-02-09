package com.crashgambling.game;

import com.crashgambling.CrashGambling;
import com.crashgambling.data.PlayerBet;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameManager {
    private final CrashGambling plugin;
    private final Economy economy;
    private final Random random = new Random();
    
    @Getter
    private GameState state = GameState.IDLE;
    
    @Getter
    private double currentMultiplier = 1.0;
    
    @Getter
    private int prestartCountdown;
    
    private double targetCrashPoint;
    private int ticksSinceLastSecond;
    private WrappedTask gameTask;
    
    public GameManager(CrashGambling plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        
        startGameLoop();
    }
    
    private void startGameLoop() {
        long tickInterval = plugin.getConfig().getLong("game.tick-interval", 2);
        
        gameTask = plugin.getFoliaLib().getScheduler().runTimer(() -> {
            switch (state) {
                case IDLE:
                    checkStartConditions();
                    break;
                case PRESTART:
                    handlePrestart();
                    break;
                case RUNNING:
                    handleRunning();
                    break;
                case CRASHED:
                    handleCrashed();
                    break;
            }
        }, 0L, tickInterval);
    }
    
    private void checkStartConditions() {
        int minPlayers = plugin.getConfig().getInt("game.min-players", 1);
        
        if (plugin.getBetManager().getPlayerCount() >= minPlayers) {
            startPrestart();
        } else {
            plugin.getHologramService().update(GameState.IDLE, 0.0, 0);
        }
    }
    
    private void startPrestart() {
        state = GameState.PRESTART;
        prestartCountdown = plugin.getConfig().getInt("game.prestart-duration", 10);
        ticksSinceLastSecond = 0;
        
        targetCrashPoint = calculateCrashPoint();
        
        plugin.getHologramService().update(GameState.PRESTART, 0.0, prestartCountdown);
    }
    
    private void handlePrestart() {
        long tickInterval = plugin.getConfig().getLong("game.tick-interval", 2);
        ticksSinceLastSecond += tickInterval;
        
        if (ticksSinceLastSecond >= 20) {
            ticksSinceLastSecond = 0;
            prestartCountdown--;
            
            plugin.getHologramService().update(GameState.PRESTART, 0.0, prestartCountdown);
        }
        
        if (prestartCountdown <= 0) {
            startGame();
        }
    }
    
    private void startGame() {
        state = GameState.RUNNING;
        currentMultiplier = 1.0;
        
        plugin.getHologramService().update(GameState.RUNNING, currentMultiplier, 0);
    }
    
    private void handleRunning() {
        double increment = plugin.getConfig().getDouble("game.multiplier-increment", 0.01);
        currentMultiplier += increment;
        
        currentMultiplier = Math.round(currentMultiplier * 100.0) / 100.0;
        
        plugin.getHologramService().update(GameState.RUNNING, currentMultiplier, 0);
        plugin.getActionBarService().sendToAll(currentMultiplier);
        
        if (currentMultiplier >= targetCrashPoint) {
            crash();
        }
    }
    
    private double calculateCrashPoint() {
        double houseEdge = plugin.getConfig().getDouble("game.house-edge", 0.01);
        double r = random.nextDouble();
        
        if (r <= 0.0001) {
            r = 0.0001;
        }
        
        double crashPoint = (0.99 / r);
        crashPoint = Math.floor(crashPoint * 100.0) / 100.0;
        crashPoint = Math.max(1.00, crashPoint);
        
        return crashPoint;
    }
    
    private void crash() {
        state = GameState.CRASHED;
        
        processLosses();
        
        plugin.getHologramService().update(GameState.CRASHED, currentMultiplier, 0);
        
        plugin.getFoliaLib().getScheduler().runLater(this::resetGame, 100L);
    }
    
    private void handleCrashed() {
    }
    
    private void resetGame() {
        plugin.getBetManager().clear();
        state = GameState.IDLE;
        currentMultiplier = 1.0;
        plugin.getHologramService().update(GameState.IDLE, 0.0, 0);
    }
    
    public boolean placeBet(Player player, double amount) {
        GameState currentState = state;
        if (currentState != GameState.IDLE && currentState != GameState.PRESTART) {
            return false;
        }
        
        double minBet = plugin.getConfig().getDouble("game.min-bet", 10.0);
        double maxBet = plugin.getConfig().getDouble("game.max-bet", 10000.0);
        
        if (amount < minBet || amount > maxBet) {
            return false;
        }
        
        if (economy.getBalance(player) < amount) {
            return false;
        }
        
        if (!plugin.getBetManager().placeBet(player.getUniqueId(), amount)) {
            return false;
        }
        
        economy.withdrawPlayer(player, amount);
        return true;
    }
    
    public boolean cashout(Player player) {
        if (state != GameState.RUNNING) {
            return false;
        }
        
        double minCashoutMultiplier = plugin.getConfig().getDouble("game.min-cashout-multiplier", 1.0);
        if (currentMultiplier < minCashoutMultiplier) {
            plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                String message = plugin.getConfig().getString("messages.cashout-too-early", "");
                message = message
                    .replace("{min}", String.format("%.2f", minCashoutMultiplier))
                    .replace("{current}", String.format("%.2f", currentMultiplier))
                    .replace("&", "§");
                
                player.sendMessage(getPrefix() + message);
            });
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (!plugin.getBetManager().cashout(playerId, currentMultiplier)) {
            return false;
        }
        
        PlayerBet bet = plugin.getBetManager().getBet(playerId).orElse(null);
        if (bet == null) {
            return false;
        }
        
        double winnings = bet.getAmount() * currentMultiplier;
        economy.depositPlayer(player, winnings);
        
        final double finalMultiplier = currentMultiplier;
        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            String message = plugin.getConfig().getString("messages.cashout-success", "");
            message = message
                .replace("{multiplier}", String.format("%.2fx", finalMultiplier))
                .replace("{amount}", String.format("%.2f", winnings))
                .replace("&", "§");
            
            player.sendMessage(getPrefix() + message);
        });
        
        return true;
    }
    
    private void processLosses() {
        Map<UUID, PlayerBet> bets = plugin.getBetManager().getAllBets();
        final double finalMultiplier = currentMultiplier;
        
        for (Map.Entry<UUID, PlayerBet> entry : bets.entrySet()) {
            PlayerBet bet = entry.getValue();
            
            if (!bet.isCashedOut()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                        String message = plugin.getConfig().getString("messages.lost", "");
                        message = message
                            .replace("{amount}", String.format("%.2f", bet.getAmount()))
                            .replace("{multiplier}", String.format("%.2fx", finalMultiplier))
                            .replace("&", "§");
                        
                        player.sendMessage(getPrefix() + message);
                    });
                }
            }
        }
    }
    
    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&6[Crash] &r").replace("&", "§");
    }
    
    public void shutdown() {
        if (gameTask != null) {
            gameTask.cancel();
        }
    }
}