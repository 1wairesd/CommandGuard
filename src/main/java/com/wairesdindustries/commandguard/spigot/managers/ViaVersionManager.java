package com.wairesdindustries.commandguard.spigot.managers;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.wairesdindustries.commandguard.spigot.CommandGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ViaVersionManager {

    private CommandGuard plugin;
    private boolean enabled;
    public ViaVersionManager(CommandGuard plugin){
        this.plugin = plugin;
        this.enabled = false;
        if(Bukkit.getServer().getPluginManager().getPlugin("ViaVersion") != null) {
            this.enabled = true;
        }
    }

    public boolean playerIsLegacy(Player player){
        if(!enabled){
            return false;
        }

        ViaAPI api = Via.getAPI();
        int version = api.getPlayerVersion(player);
        if(version <= 340){
            return true;
        }
        return false;
    }
}
