package com.crashgambling;

import com.crashgambling.command.CrashCommand;
import com.crashgambling.game.BetManager;
import com.crashgambling.game.GameManager;
import com.crashgambling.service.ActionBarService;
import com.crashgambling.service.HologramService;
import com.tcoded.folialib.FoliaLib;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CrashGambling extends JavaPlugin {
    private Economy economy;
    private BetManager betManager;
    private GameManager gameManager;
    private HologramService hologramService;
    private ActionBarService actionBarService;
    private FoliaLib foliaLib;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.foliaLib = new FoliaLib(this);
        
        if (!setupEconomy()) {
            getLogger().severe(getConfig().getString("messages.vault-not-found"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (!checkFancyHolograms()) {
            getLogger().severe("FancyHolograms not found - plugin disabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.betManager = new BetManager();
        this.hologramService = new HologramService(this);
        this.actionBarService = new ActionBarService(this);
        this.gameManager = new GameManager(this, economy);
        
        CrashCommand crashCommand = new CrashCommand(this);
        getCommand("crash").setExecutor(crashCommand);
        getCommand("crash").setTabCompleter(crashCommand);
        
        getLogger().info("CrashGambling enabled successfully (Folia compatible)");
    }
    
    @Override
    public void onDisable() {
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
        
        if (gameManager != null) {
            gameManager.shutdown();
        }
        
        if (hologramService != null) {
            hologramService.shutdown();
        }
        
        getLogger().info("CrashGambling disabled");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    private boolean checkFancyHolograms() {
        return FancyHologramsPlugin.isEnabled();
    }
}