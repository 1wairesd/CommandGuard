package com.wairesdindustries.commandguard.waterfall.listeners;

import com.wairesdindustries.commandguard.core.model.GlobalVariables;
import com.wairesdindustries.commandguard.waterfall.CommandGuard;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerListener implements Listener {

    private CommandGuard plugin;
    public ServerListener(CommandGuard plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event){
        // Check if the identifier matches first, no matter the source.
        if (!event.getTag().equals(GlobalVariables.bungeeMainChannel)) {
            return;
        }

        event.setCancelled(true);
    }
}
