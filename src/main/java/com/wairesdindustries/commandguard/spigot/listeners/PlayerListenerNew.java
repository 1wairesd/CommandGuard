package com.wairesdindustries.commandguard.spigot.listeners;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.spigot.CommandGuard;
import com.wairesdindustries.commandguard.spigot.managers.BungeeMessagingManager;
import com.wairesdindustries.commandguard.spigot.utils.OtherUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerListenerNew implements Listener {

    private final CommandGuard plugin;

    public PlayerListenerNew(CommandGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        boolean isNetwork = plugin.getConfigManager().getConfig().getBoolean("is_network");

        // bypass.tab or OP in non-network mode — skip all filtering
        if (player.hasPermission("commandguard.bypass.tab") || player.isOp()) {
            if (!isNetwork) return;

            // In network mode: OP and bypass players still get proxy commands injected,
            // but we do NOT clear or filter the existing command list.
            BungeeMessagingManager bungeeManager = plugin.getBungeeMessagingManager();
            if (bungeeManager != null) {
                List<String> proxyCommands = bungeeManager.getProxyTabCommands(player.getUniqueId());
                if (proxyCommands != null) {
                    for (String command : proxyCommands) {
                        command = command.replaceFirst("/", "").split(" ")[0];
                        if (!event.getCommands().contains(command)) {
                            event.getCommands().add(command);
                        }
                    }
                }
            }
            return;
        }

        // Normal player — apply local config filtering
        CommandsManager commandsManager = plugin.getCommandsManager();
        List<String> localCommands = commandsManager.getTabCommands(OtherUtils.getPlayerPermissionsList(player));

        event.getCommands().clear();

        if (localCommands != null) {
            for (String command : localCommands) {
                command = command.replaceFirst("/", "").split(" ")[0];
                if (!event.getCommands().contains(command)) {
                    event.getCommands().add(command);
                }
            }
        }

        // Inject proxy commands on top of local ones
        if (isNetwork) {
            BungeeMessagingManager bungeeManager = plugin.getBungeeMessagingManager();
            if (bungeeManager != null) {
                List<String> proxyCommands = bungeeManager.getProxyTabCommands(player.getUniqueId());
                if (proxyCommands != null) {
                    for (String command : proxyCommands) {
                        command = command.replaceFirst("/", "").split(" ")[0];
                        if (!event.getCommands().contains(command)) {
                            event.getCommands().add(command);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BungeeMessagingManager bungeeManager = plugin.getBungeeMessagingManager();
        if (bungeeManager != null) {
            bungeeManager.clearCache(event.getPlayer().getUniqueId());
        }
    }
}
