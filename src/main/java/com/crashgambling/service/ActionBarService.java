package com.crashgambling.service;

import com.crashgambling.CrashGambling;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ActionBarService {
    private final CrashGambling plugin;
    
    public ActionBarService(CrashGambling plugin) {
        this.plugin = plugin;
    }
    
    public void sendToAll(double multiplier) {
        if (!plugin.getConfig().getBoolean("actionbar.enabled", true)) {
            return;
        }
        
        Location hologramLocation = plugin.getHologramService().getLocation();
        double distance = plugin.getConfig().getDouble("actionbar.distance", 50.0);
        
        String messageTemplate = plugin.getConfig().getString("actionbar.text", "&6⚡ &eCrash: &a&l{multiplier}x &6⚡");
        String message = messageTemplate
            .replace("{multiplier}", String.format("%.2f", multiplier))
            .replace("&", "§");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hologramLocation == null || !isNearHologram(player, hologramLocation, distance)) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            }
        }
    }
    
    private boolean isNearHologram(Player player, Location hologramLocation, double threshold) {
        if (!player.getWorld().equals(hologramLocation.getWorld())) {
            return false;
        }
        
        return player.getLocation().distance(hologramLocation) <= threshold;
    }
}