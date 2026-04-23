package com.wairesdindustries.commandguard.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.velocity.utils.OtherUtils;
import com.wairesdindustries.commandguard.velocity.utils.PluginMessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Optional;

public class MainCommand implements SimpleCommand {

    private final CommandGuard plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public MainCommand(CommandGuard plugin) {
        this.plugin = plugin;
    }

    private void send(CommandSource source, String key, Object... replacements) {
        String raw = plugin.getLangManager().get(key, replacements);
        source.sendMessage(LEGACY.deserialize(plugin.getPrefix().isEmpty() ? raw : "&r" + raw));
    }

    private void sendPrefixed(CommandSource source, String key, Object... replacements) {
        LangManager lang = plugin.getLangManager();
        // prefix is MiniMessage format, message is legacy — convert both
        source.sendMessage(MM.deserialize(plugin.getPrefix() + " <reset>"
                + MM.serialize(LEGACY.deserialize(lang.get(key, replacements)))));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("commandguard.admin")) {
            sendPrefixed(source, "cmd.no_permission");
            return;
        }

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reload(source);
                break;
            case "check":
                if (args.length < 2) {
                    sendPrefixed(source, "cmd.usage.check");
                } else {
                    check(source, args[1]);
                }
                break;
            case "updategroup":
                if (args.length < 2) {
                    sendPrefixed(source, "cmd.usage.updategroup");
                } else {
                    updateGroup(source, args[1]);
                }
                break;
            default:
                sendHelp(source);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // permission checked inside execute()
    }

    private void sendHelp(CommandSource source) {
        LangManager lang = plugin.getLangManager();
        sendPrefixed(source, "cmd.help.header");
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.help.reload")));
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.help.check")));
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.help.updategroup")));
    }

    private void reload(CommandSource source) {
        plugin.customReload();
        sendPrefixed(source, "cmd.reload.success");

        if (plugin.getConfigManager().getConfig().getBoolean("is_network")) {
            int count = 0;
            for (Player player : plugin.getServer().getAllPlayers()) {
                sendTabSyncToPlayer(player);
                count++;
            }
            sendPrefixed(source, "cmd.reload.tabsync_sent", "count", count);
        }
    }

    private void check(CommandSource source, String playerName) {
        Optional<Player> opt = plugin.getServer().getPlayer(playerName);
        if (opt.isEmpty()) {
            sendPrefixed(source, "cmd.check.not_found", "player", playerName);
            return;
        }
        Player player = opt.get();
        CommandsManager cm = plugin.getCommandsManager();
        LangManager lang = plugin.getLangManager();
        List<String> permissions = OtherUtils.getPermissions(player, cm);
        String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown");
        List<String> groups = cm.getActiveGroupNames(permissions, serverName);
        List<String> commands = cm.getTabCommands(permissions, serverName);

        sendPrefixed(source, "cmd.check.player", "player", player.getUsername());
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.check.server", "server", serverName)));
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.check.groups", "groups", String.join(", ", groups))));
        source.sendMessage(LEGACY.deserialize(lang.get("cmd.check.commands",
                "count", commands.size(), "commands", String.join(", ", commands))));
    }

    private void updateGroup(CommandSource source, String playerName) {
        Optional<Player> opt = plugin.getServer().getPlayer(playerName);
        if (opt.isEmpty()) {
            sendPrefixed(source, "cmd.updategroup.not_found", "player", playerName);
            return;
        }
        Player player = opt.get();
        sendTabSyncToPlayer(player);
        sendPrefixed(source, "cmd.updategroup.success", "player", player.getUsername());
    }

    public void sendTabSyncToPlayer(Player player) {
        if (player.hasPermission("commandguard.bypass.tab")) return;
        CommandsManager cm = plugin.getCommandsManager();
        List<String> permissions = OtherUtils.getPermissions(player, cm);
        String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(null);
        List<String> allowed = cm.getTabCommands(permissions, serverName);
        PluginMessagingUtils.sendTabSync(player, allowed);
    }
}
