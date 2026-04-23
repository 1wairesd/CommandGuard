package com.wairesdindustries.commandguard.waterfall.listeners;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.model.internal.UseCommandResult;
import com.wairesdindustries.commandguard.waterfall.CommandGuard;
import com.wairesdindustries.commandguard.waterfall.api.CommandBlockedEvent;
import com.wairesdindustries.commandguard.waterfall.utils.ActionsUtils;
import com.wairesdindustries.commandguard.waterfall.utils.PluginMessagingUtils;
import io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlayerListener implements Listener {

    private final CommandGuard plugin;

    public PlayerListener(CommandGuard plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Command blocking
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String command = event.getMessage();

        if (!command.startsWith("/")) {
            return;
        }
        if (player.hasPermission("commandguard.bypass.commands")) {
            return;
        }

        CommandsManager commandsManager = plugin.getCommandsManager();
        UseCommandResult result = commandsManager.useCommand(command);
        if (!result.isCanUseCommand()) {
            CommandBlockedEvent commandBlockedEvent = new CommandBlockedEvent(
                    player, result.getFoundCommand(), command);
            plugin.getProxy().getPluginManager().callEvent(commandBlockedEvent);

            if (!commandBlockedEvent.isCancelled()) {
                List<String> actions = commandsManager.getActionsForCustomCommand(result.getFoundCommand());
                if (actions == null) {
                    actions = commandsManager.getBlockCommandDefaultActions();
                }
                ActionsUtils.executeActions(actions, player);
                event.setCancelled(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tab-complete filtering
    // -------------------------------------------------------------------------

    /**
     * Filters proxy-side commands before they are sent to the client.
     * Note: ProxyDefineCommandsEvent only contains proxy-registered commands.
     * Backend commands are managed by the backend plugin via TabSync.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDefineCommands(ProxyDefineCommandsEvent event) {
        if (!(event.getReceiver() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        if (player.hasPermission("commandguard.bypass.tab")) {
            return;
        }

        CommandsManager commandsManager = plugin.getCommandsManager();
        List<String> permissions = new ArrayList<>(player.getPermissions());
        List<String> allowedCommands = commandsManager.getTabCommands(permissions);

        Iterator<Map.Entry<String, Command>> iter = event.getCommands().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Command> entry = iter.next();
            if (!allowedCommands.contains("/" + entry.getKey().toLowerCase())) {
                iter.remove();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cross-server TabSync
    // -------------------------------------------------------------------------

    /**
     * When a player connects (or switches) to a backend server, push the allowed
     * tab-complete list so the backend plugin can apply the same filtering.
     */
    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.hasPermission("commandguard.bypass.tab")) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().getBoolean("is_network")) {
            return;
        }

        CommandsManager commandsManager = plugin.getCommandsManager();
        List<String> permissions = new ArrayList<>(player.getPermissions());
        List<String> allowedCommands = commandsManager.getTabCommands(permissions);

        // Send immediately and retry after 1s in case the first message
        // arrives before the backend has fully registered the player.
        PluginMessagingUtils.sendTabSync(player, allowedCommands);
        plugin.getProxy().getScheduler().schedule(plugin,
                () -> PluginMessagingUtils.sendTabSync(player, allowedCommands),
                1, TimeUnit.SECONDS);
    }
}
