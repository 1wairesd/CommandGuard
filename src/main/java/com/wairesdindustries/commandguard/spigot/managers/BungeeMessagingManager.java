package com.wairesdindustries.commandguard.spigot.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.wairesdindustries.commandguard.core.model.GlobalVariables;
import com.wairesdindustries.commandguard.spigot.CommandGuard;
import com.wairesdindustries.commandguard.spigot.utils.ActionsUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;

public class BungeeMessagingManager implements PluginMessageListener {

    private final CommandGuard plugin;

    /** Allowed tab-complete commands pushed from the proxy, keyed by player UUID. */
    private final Map<UUID, List<String>> proxyTabCache = new HashMap<>();

    public BungeeMessagingManager(CommandGuard plugin) {
        this.plugin = plugin;
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(
                plugin, GlobalVariables.bungeeMainChannel, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equalsIgnoreCase(GlobalVariables.bungeeMainChannel)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();

        if (subChannel.equalsIgnoreCase(GlobalVariables.bungeeActionsSubChannel)) {
            if (!plugin.getConfigManager().getConfig().getBoolean("is_network")) {
                return;
            }
            ActionsUtils.executeAction(in.readUTF(), player);

        } else if (subChannel.equalsIgnoreCase(GlobalVariables.tabSyncSubChannel)) {
            String uuidStr = in.readUTF();
            String commandsCsv = in.readUTF();

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[ECB] TabSync received invalid UUID: " + uuidStr);
                return;
            }

            proxyTabCache.put(uuid, commandsCsv.isEmpty()
                    ? Collections.emptyList()
                    : Arrays.asList(commandsCsv.split(",")));

            // Refresh the client's command tree immediately after receiving the sync.
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, online::updateCommands);
            }
        }
    }

    /** Returns the proxy-provided tab command list, or null if not yet received. */
    public List<String> getProxyTabCommands(UUID playerUUID) {
        return proxyTabCache.get(playerUUID);
    }

    /** Cleans up cached data when a player disconnects. */
    public void clearCache(UUID playerUUID) {
        proxyTabCache.remove(playerUUID);
    }
}
