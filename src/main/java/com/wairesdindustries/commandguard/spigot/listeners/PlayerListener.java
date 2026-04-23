package com.wairesdindustries.commandguard.spigot.listeners;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.model.internal.UseCommandResult;
import com.wairesdindustries.commandguard.spigot.CommandGuard;
import com.wairesdindustries.commandguard.spigot.api.CommandBlockedEvent;
import com.wairesdindustries.commandguard.spigot.managers.BungeeMessagingManager;
import com.wairesdindustries.commandguard.spigot.utils.ActionsUtils;
import com.wairesdindustries.commandguard.spigot.utils.MessagesUtils;
import com.wairesdindustries.commandguard.spigot.utils.OtherUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final CommandGuard plugin;

    public PlayerListener(CommandGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("commandguard.bypass.commands")) {
            return;
        }

        CommandsManager commandsManager = plugin.getCommandsManager();
        UseCommandResult result = commandsManager.useCommand(event.getMessage());
        if (!result.isCanUseCommand()) {
            CommandBlockedEvent commandBlockedEvent = new CommandBlockedEvent(
                    player, result.getFoundCommand(), event.getMessage());
            plugin.getServer().getPluginManager().callEvent(commandBlockedEvent);

            if (!commandBlockedEvent.isCancelled()) {
                List<String> actions = commandsManager.getActionsForCustomCommand(result.getFoundCommand());
                if (actions == null) actions = commandsManager.getBlockCommandDefaultActions();
                ActionsUtils.executeActions(actions, player);
                event.setCancelled(true);
            }
        }
    }

    /**
     * Spy-client protection: cancel server-side tab-complete responses for commands
     * the player is not allowed to see. This prevents hack clients from discovering
     * hidden commands by sending raw TAB_COMPLETE packets.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player player = (Player) event.getSender();

        if (player.isOp() || player.hasPermission("commandguard.bypass.tab")) return;

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        // Build the allowed command list for this player
        boolean isNetwork = plugin.getConfigManager().getConfig().getBoolean("is_network");
        List<String> allowedCommands;

        BungeeMessagingManager bungeeManager = plugin.getBungeeMessagingManager();
        if (isNetwork && bungeeManager != null) {
            List<String> proxyCommands = bungeeManager.getProxyTabCommands(player.getUniqueId());
            List<String> localCommands = plugin.getCommandsManager()
                    .getTabCommands(OtherUtils.getPlayerPermissionsList(player));
            allowedCommands = new java.util.ArrayList<>(localCommands);
            if (proxyCommands != null) {
                for (String c : proxyCommands) {
                    if (!allowedCommands.contains(c)) allowedCommands.add(c);
                }
            }
        } else {
            allowedCommands = plugin.getCommandsManager()
                    .getTabCommands(OtherUtils.getPlayerPermissionsList(player));
        }

        if (allowedCommands == null) return;

        // Extract the root command from the buffer (e.g. "/gam" -> "gam")
        String typed = buffer.substring(1).split(" ")[0].toLowerCase();

        // Remove completions that suggest commands the player shouldn't see
        event.getCompletions().removeIf(completion -> {
            String completionCmd = completion.startsWith("/")
                    ? completion.substring(1).split(" ")[0].toLowerCase()
                    : completion.split(" ")[0].toLowerCase();
            // Only filter if this looks like a command suggestion
            if (!buffer.contains(" ")) {
                // Completing the command name itself — check if it's allowed
                return allowedCommands.stream().noneMatch(a ->
                        a.replaceFirst("/", "").split(" ")[0].equalsIgnoreCase(completionCmd));
            }
            return false;
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String latestVersion = plugin.getUpdateCheckerManager().getLatestVersion();
        if (player.isOp()
                && plugin.getConfigManager().getConfig().getBoolean("update_notify")
                && !plugin.version.equals(latestVersion)) {
            player.sendMessage(MessagesUtils.getColoredMessage(
                    plugin.prefix + " " + plugin.getLangManager().get("update.available", "version", latestVersion)));
            player.sendMessage(MessagesUtils.getColoredMessage(
                    plugin.getLangManager().get("update.download")));
        }
    }
}
