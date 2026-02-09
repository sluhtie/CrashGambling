package com.crashgambling.service;

import com.crashgambling.CrashGambling;
import com.crashgambling.game.GameState;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HologramService {
    private final CrashGambling plugin;
    private final HologramManager hologramManager;
    private Hologram hologram;
    private String hologramId;
    
    public HologramService(CrashGambling plugin) {
        this.plugin = plugin;
        this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
        this.hologramId = plugin.getConfig().getString("hologram.hologram-id", "crash_game");
        
        loadHologram();
    }
    
    private void loadHologram() {
        Optional<Hologram> hologramOpt = hologramManager.getHologram(hologramId);
        
        if (hologramOpt.isPresent()) {
            this.hologram = hologramOpt.get();
            plugin.getLogger().info("Connected to hologram: " + hologramId);
            update(GameState.IDLE, 0.0, 0);
        } else {
            plugin.getLogger().warning("Hologram '" + hologramId + "' not found! Please create it first with /hologram create " + hologramId);
            plugin.getLogger().warning("The plugin will only update the hologram lines, you need to position and style it yourself.");
        }
    }
    
    public Location getLocation() {
        if (hologram == null) {
            return null;
        }
        return hologram.getData().getLocation();
    }
    
    public void update(GameState state, double multiplier, int countdown) {
        if (hologram == null) {
            return;
        }
        
        if (!(hologram.getData() instanceof TextHologramData)) {
            plugin.getLogger().warning("Hologram '" + hologramId + "' is not a text hologram!");
            return;
        }
        
        TextHologramData data = (TextHologramData) hologram.getData();
        List<String> newLines = getLines(state, multiplier, countdown);
        data.setText(newLines);
        
        hologram.refreshForViewersInWorld();
    }
    
    private List<String> getLines(GameState state, double multiplier, int countdown) {
        String stateKey = state.name().toLowerCase();
        List<String> lines = plugin.getConfig().getStringList("hologram.lines." + stateKey);
        
        if (lines.isEmpty()) {
            return List.of("&6Crash Game", String.format("&e%.2fx", multiplier));
        }
        
        List<String> processed = new ArrayList<>();
        int playerCount = plugin.getBetManager().getPlayerCount();
        
        for (String line : lines) {
            String processedLine = line
                .replace("{multiplier}", String.format("%.2f", multiplier))
                .replace("{countdown}", String.valueOf(countdown))
                .replace("{player_count}", String.valueOf(playerCount))
                .replace("&", "§");
            processed.add(processedLine);
        }
        
        return processed;
    }
    
    public void shutdown() {
    }
    
    public void reload() {
        this.hologramId = plugin.getConfig().getString("hologram.hologram-id", "crash_game");
        loadHologram();
    }
}