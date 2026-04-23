package com.wairesdindustries.commandguard.velocity.listeners;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.model.internal.UseCommandResult;
import com.wairesdindustries.commandguard.velocity.CommandGuard;
import com.wairesdindustries.commandguard.velocity.api.CommandBlockedEvent;
import com.wairesdindustries.commandguard.velocity.utils.ActionsUtils;
import com.wairesdindustries.commandguard.velocity.utils.OtherUtils;
import com.wairesdindustries.commandguard.velocity.utils.PluginMessagingUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerListener {

    private final CommandGuard plugin;

    /**
     * Cached reference to the internal Velocity dispatcher root.
     * Obtained once via reflection so we can inject full command nodes with arguments.
     */
    private RootCommandNode<CommandSource> internalDispatcherRoot = null;

    public PlayerListener(CommandGuard plugin) {
        this.plugin = plugin;
        initDispatcherRoot();
    }

    @SuppressWarnings("unchecked")
    private void initDispatcherRoot() {
        try {
            Field dispatcherField = plugin.getServer().getCommandManager()
                    .getClass().getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            CommandDispatcher<CommandSource> dispatcher =
                    (CommandDispatcher<CommandSource>) dispatcherField.get(
                            plugin.getServer().getCommandManager());
            internalDispatcherRoot = dispatcher.getRoot();
        } catch (Exception e) {
            plugin.getServer().getConsoleCommandSource().sendMessage(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    plugin.getPrefix() + " <red>Could not access internal command dispatcher via reflection: "
                    + e.getMessage() + ". Tab-complete arguments for proxy commands may not work."));
        }
    }

    // -------------------------------------------------------------------------
    // Command blocking
    // -------------------------------------------------------------------------

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getCommandSource();
        String command = "/" + event.getCommand();

        boolean isProxyCommand = plugin.getServer().getCommandManager()
                .hasCommand(event.getCommand().split(" ")[0].toLowerCase());
        if (!isProxyCommand) {
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
            plugin.getServer().getEventManager().fire(commandBlockedEvent).thenAccept(finalEvent -> {
                if (!finalEvent.getResult().isAllowed()) {
                    return;
                }
                List<String> actions = commandsManager.getActionsForCustomCommand(result.getFoundCommand());
                if (actions == null) {
                    actions = commandsManager.getBlockCommandDefaultActions();
                }
                ActionsUtils.executeActions(actions, player);
                event.setResult(CommandExecuteEvent.CommandResult.denied());
            });
        }
    }

    // -------------------------------------------------------------------------
    // Tab-complete filtering
    // -------------------------------------------------------------------------

    /**
     * Filters the command tree that Velocity sends to the client.
     *
     * Standalone mode  (is_network=false):
     *   Remove every command not in the player's allowed list.
     *
     * Network mode (is_network=true):
     *   Only filter proxy-owned commands. Backend commands are managed by the
     *   backend plugin via TabSync. Additionally, inject any allowed proxy
     *   commands that are missing from the tree (can happen when the event fires
     *   before the backend tree is merged).
     */
    @Subscribe(order = PostOrder.FIRST)
    public void onAvailableCommands(PlayerAvailableCommandsEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("commandguard.bypass.tab")) {
            return;
        }

        boolean isNetwork = plugin.getConfigManager().getConfig().getBoolean("is_network");
        CommandsManager commandsManager = plugin.getCommandsManager();
        List<String> permissions = OtherUtils.getPermissions(player, commandsManager);
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName()).orElse(null);
        List<String> allowedCommands = commandsManager.getTabCommands(permissions, serverName);

        // Step 1 — remove disallowed proxy commands from the tree
        event.getRootNode().getChildren().removeIf(child -> {
            String name = child.getName().toLowerCase();
            boolean isProxyCommand = plugin.getServer().getCommandManager().hasCommand(name);
            if (isNetwork && !isProxyCommand) {
                return false; // backend commands are handled by the backend plugin
            }
            return !allowedCommands.contains("/" + name);
        });

        // Step 2 — in network mode, inject allowed proxy commands that may be missing
        if (isNetwork) {
            Set<String> existing = event.getRootNode().getChildren().stream()
                    .map(c -> c.getName().toLowerCase())
                    .collect(Collectors.toSet());

            @SuppressWarnings("unchecked")
            RootCommandNode<CommandSource> root =
                    (RootCommandNode<CommandSource>) (RootCommandNode<?>) event.getRootNode();

            for (String cmd : allowedCommands) {
                String name = cmd.replaceFirst("/", "").split(" ")[0].toLowerCase();
                if (!plugin.getServer().getCommandManager().hasCommand(name)) {
                    continue;
                }
                if (existing.contains(name)) {
                    continue;
                }
                // Prefer the original registered node so arguments are preserved
                CommandNode<CommandSource> original = internalDispatcherRoot != null
                        ? internalDispatcherRoot.getChild(name) : null;
                if (original != null) {
                    root.addChild(original);
                } else {
                    root.addChild(BrigadierCommand.literalArgumentBuilder(name)
                            .executes(ctx -> 1).build());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cross-server TabSync
    // -------------------------------------------------------------------------

    /**
     * When a player connects (or switches) to a backend server, push the allowed
     * tab-complete list so the backend plugin can apply the same filtering.
     * Uses ServerPostConnectEvent — fires after the connection is fully established.
     */
    @Subscribe(order = PostOrder.LAST)
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("commandguard.bypass.tab")) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().getBoolean("is_network")) {
            return;
        }

        CommandsManager commandsManager = plugin.getCommandsManager();
        List<String> permissions = OtherUtils.getPermissions(player, commandsManager);
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName()).orElse(null);
        List<String> allowedCommands = commandsManager.getTabCommands(permissions, serverName);

        // Connection is fully established at this point — send immediately.
        PluginMessagingUtils.sendTabSync(player, allowedCommands);
    }
}
