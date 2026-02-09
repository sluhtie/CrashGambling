package com.crashgambling.game;

import com.crashgambling.data.PlayerBet;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BetManager {
    private final Map<UUID, PlayerBet> activeBets = new ConcurrentHashMap<>();
    
    @Getter
    private int playerCount = 0;
    
    public boolean placeBet(UUID playerId, double amount) {
        if (activeBets.containsKey(playerId)) {
            return false;
        }
        
        PlayerBet bet = new PlayerBet(playerId, amount);
        activeBets.put(playerId, bet);
        playerCount = activeBets.size();
        return true;
    }
    
    public Optional<PlayerBet> getBet(UUID playerId) {
        return Optional.ofNullable(activeBets.get(playerId));
    }
    
    public boolean hasBet(UUID playerId) {
        return activeBets.containsKey(playerId);
    }
    
    public boolean cashout(UUID playerId, double multiplier) {
        PlayerBet bet = activeBets.get(playerId);
        if (bet == null || bet.isCashedOut()) {
            return false;
        }
        
        bet.setCashedOut(true);
        bet.setCashoutMultiplier(multiplier);
        updatePlayerCount();
        return true;
    }
    
    public int getActivePlayerCount() {
        return (int) activeBets.values().stream()
            .filter(bet -> !bet.isCashedOut())
            .count();
    }
    
    private void updatePlayerCount() {
        playerCount = getActivePlayerCount();
    }
    
    public Map<UUID, PlayerBet> getAllBets() {
        return new HashMap<>(activeBets);
    }
    
    public void clear() {
        activeBets.clear();
        playerCount = 0;
    }
}