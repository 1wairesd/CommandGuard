package com.wairesdindustries.commandguard.waterfall;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.waterfall.utils.MessagesUtils;
import com.wairesdindustries.commandguard.waterfall.utils.PluginMessagingUtils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.ArrayList;
import java.util.List;

public class MainCommand extends Command {

    private final CommandGuard plugin;

    public MainCommand(CommandGuard plugin) {
        super("commandguardvelocity", null, "cgv");
        this.plugin = plugin;
    }

    private BaseComponent[] msg(String key, Object... replacements) {
        return MessagesUtils.getColoredMessage(
                plugin.getPrefix() + " " + plugin.getLangManager().get(key, replacements));
    }

    private BaseComponent[] msgRaw(String key, Object... replacements) {
        return MessagesUtils.getColoredMessage(plugin.getLangManager().get(key, replacements));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandguard.admin")) {
            sender.sendMessage(msg("cmd.no_permission"));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reload(sender);
                break;
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(msg("cmd.usage.check"));
                } else {
                    check(sender, args[1]);
                }
                break;
            case "updategroup":
                if (args.length < 2) {
                    sender.sendMessage(msg("cmd.usage.updategroup"));
                } else {
                    updateGroup(sender, args[1]);
                }
                break;
            default:
                sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("cmd.help.header"));
        sender.sendMessage(msgRaw("cmd.help.reload"));
        sender.sendMessage(msgRaw("cmd.help.check"));
        sender.sendMessage(msgRaw("cmd.help.updategroup"));
    }

    private void reload(CommandSender sender) {
        plugin.customReload();
        sender.sendMessage(msg("cmd.reload.success"));

        if (plugin.getConfigManager().getConfig().getBoolean("is_network")) {
            int count = 0;
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                sendTabSyncToPlayer(player);
                count++;
            }
            sender.sendMessage(msg("cmd.reload.tabsync_sent", "count", count));
        }
    }

    private void check(CommandSender sender, String playerName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(msg("cmd.check.not_found", "player", playerName));
            return;
        }
        CommandsManager cm = plugin.getCommandsManager();
        LangManager lang = plugin.getLangManager();
        List<String> permissions = new ArrayList<>(player.getPermissions());
        String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : "unknown";
        List<String> groups = cm.getActiveGroupNames(permissions, serverName);
        List<String> commands = cm.getTabCommands(permissions, serverName);

        sender.sendMessage(msg("cmd.check.player", "player", player.getName()));
        sender.sendMessage(msgRaw("cmd.check.server", "server", serverName));
        sender.sendMessage(msgRaw("cmd.check.groups", "groups", String.join(", ", groups)));
        sender.sendMessage(msgRaw("cmd.check.commands", "count", commands.size(), "commands", String.join(", ", commands)));
    }

    private void updateGroup(CommandSender sender, String playerName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(msg("cmd.updategroup.not_found", "player", playerName));
            return;
        }
        sendTabSyncToPlayer(player);
        sender.sendMessage(msg("cmd.updategroup.success", "player", player.getName()));
    }

    public void sendTabSyncToPlayer(ProxiedPlayer player) {
        if (player.hasPermission("commandguard.bypass.tab")) return;
        CommandsManager cm = plugin.getCommandsManager();
        List<String> permissions = new ArrayList<>(player.getPermissions());
        String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : null;
        List<String> allowed = cm.getTabCommands(permissions, serverName);
        PluginMessagingUtils.sendTabSync(player, allowed);
    }
}
