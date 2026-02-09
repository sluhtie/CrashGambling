package com.crashgambling.data;

import lombok.Data;

import java.util.UUID;

@Data
public class PlayerBet {
    private final UUID playerId;
    private final double amount;
    private boolean cashedOut;
    private double cashoutMultiplier;
    
    public PlayerBet(UUID playerId, double amount) {
        this.playerId = playerId;
        this.amount = amount;
        this.cashedOut = false;
        this.cashoutMultiplier = 0.0;
    }
}