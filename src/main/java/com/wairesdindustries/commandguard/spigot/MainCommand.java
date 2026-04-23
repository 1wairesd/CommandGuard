package com.wairesdindustries.commandguard.spigot;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.spigot.utils.MessagesUtils;
import com.wairesdindustries.commandguard.spigot.utils.OtherUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MainCommand implements CommandExecutor {

    private final CommandGuard plugin;

    public MainCommand(CommandGuard plugin) {
        this.plugin = plugin;
    }

    private String msg(String key, Object... replacements) {
        return MessagesUtils.getColoredMessage(
                CommandGuard.getPrefix() + " " + plugin.getLangManager().get(key, replacements));
    }

    private String msgRaw(String key, Object... replacements) {
        return MessagesUtils.getColoredMessage(plugin.getLangManager().get(key, replacements));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("commandguard.admin")) {
            sender.sendMessage(msg("cmd.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
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
        return true;
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    private void check(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(msg("cmd.check.not_found", "player", playerName));
            return;
        }
        CommandsManager cm = plugin.getCommandsManager();
        LangManager lang = plugin.getLangManager();
        List<String> permissions = OtherUtils.getPlayerPermissionsList(player);
        List<String> groups = cm.getActiveGroupNames(permissions, null);
        List<String> commands = cm.getTabCommands(permissions, null);

        sender.sendMessage(msg("cmd.check.player", "player", player.getName()));
        sender.sendMessage(msgRaw("cmd.check.groups", "groups", String.join(", ", groups)));
        sender.sendMessage(msgRaw("cmd.check.commands", "count", commands.size(), "commands", String.join(", ", commands)));
    }

    private void updateGroup(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(msg("cmd.updategroup.not_found", "player", playerName));
            return;
        }
        player.updateCommands();
        sender.sendMessage(msg("cmd.updategroup.success", "player", player.getName()));
    }
}
