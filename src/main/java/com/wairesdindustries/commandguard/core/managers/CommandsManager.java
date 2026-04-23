package com.wairesdindustries.commandguard.core.managers;

import com.wairesdindustries.commandguard.core.model.ConfigStructure;
import com.wairesdindustries.commandguard.core.model.CustomCommandGroup;
import com.wairesdindustries.commandguard.core.model.TabCommandList;
import com.wairesdindustries.commandguard.core.model.internal.UseCommandResult;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.ArrayList;
import java.util.List;

public class CommandsManager {

    private ConfigStructure configStructure;

    public CommandsManager(YamlFile config) {
        load(config);
    }

    public void load(YamlFile config) {
        List<String> commands = config.getStringList("commands");
        List<String> blockedCommandDefaultActions = config.getStringList("blocked_command_default_actions");

        List<TabCommandList> tabCommands = new ArrayList<>();
        for (String key : config.getConfigurationSection("tab").getKeys(false)) {
            String path = "tab." + key;
            List<String> tab = config.getStringList(path + ".commands");
            int priority = config.getInt(path + ".priority");
            String extendTabName = config.getString(path + ".extends");
            List<String> servers = config.getStringList(path + ".servers"); // empty list if not set
            tabCommands.add(new TabCommandList(key, priority, tab, extendTabName, servers));
        }

        boolean useCommandsAsWhitelist = config.getBoolean("use_commands_as_whitelist");

        List<CustomCommandGroup> customCommandGroupList = new ArrayList<>();
        if (config.contains("custom_commands_actions")) {
            for (String key : config.getConfigurationSection("custom_commands_actions").getKeys(false)) {
                String path = "custom_commands_actions." + key;
                List<String> commandsList = config.getStringList(path + ".commands");
                List<String> actionsList = config.getStringList(path + ".actions");
                customCommandGroupList.add(new CustomCommandGroup(commandsList, actionsList));
            }
        }

        boolean blockColonCommands = config.getBoolean("block_colon_commands");

        configStructure = new ConfigStructure(commands, blockedCommandDefaultActions, tabCommands,
                useCommandsAsWhitelist, customCommandGroupList, blockColonCommands);
    }

    public List<String> getBlockCommandDefaultActions() {
        return configStructure.getBlockedCommandActions();
    }

    /**
     * Returns the allowed tab-complete commands for a player based on their permissions.
     * No server filtering — used on backend where server context is implicit.
     */
    public List<String> getTabCommands(List<String> permissions) {
        return getTabCommands(permissions, null);
    }

    /**
     * Returns the allowed tab-complete commands for a player based on their permissions
     * and the server they are currently on.
     *
     * Groups with the same highest priority are merged together.
     * Groups with a servers restriction only apply when the player is on that server.
     *
     * @param permissions list of permission nodes the player has
     * @param serverName  current server name, or null to skip server filtering
     */
    public List<String> getTabCommands(List<String> permissions, String serverName) {
        List<TabCommandList> tabCommandLists = configStructure.getTabCommandList();
        List<TabCommandList> matched = new ArrayList<>();
        TabCommandList defaultTab = null;
        int highestPriority = -1;

        for (TabCommandList t : tabCommandLists) {
            if (t.getName().equals("default")) {
                defaultTab = t;
                continue;
            }
            if (!permissions.contains(t.getPermission())) continue;
            if (serverName != null && !t.appliesToServer(serverName)) continue;

            if (t.getPriority() > highestPriority) {
                highestPriority = t.getPriority();
                matched.clear();
                matched.add(t);
            } else if (t.getPriority() == highestPriority) {
                // Same priority — merge
                matched.add(t);
            }
        }

        if (!matched.isEmpty()) {
            List<String> result = new ArrayList<>();
            for (TabCommandList t : matched) {
                addUnique(result, t.getCommands());
                if (t.getExtendTabName() != null) {
                    addUnique(result, getExtendsTabCommands(t.getExtendTabName(), new ArrayList<>(), 0));
                }
            }
            return result;
        }

        return defaultTab != null ? defaultTab.getCommands() : new ArrayList<>();
    }

    /**
     * Returns the name of the group(s) that would be applied to a player.
     * Useful for /ecb check.
     */
    public List<String> getActiveGroupNames(List<String> permissions, String serverName) {
        List<TabCommandList> tabCommandLists = configStructure.getTabCommandList();
        List<String> names = new ArrayList<>();
        int highestPriority = -1;

        for (TabCommandList t : tabCommandLists) {
            if (t.getName().equals("default")) continue;
            if (!permissions.contains(t.getPermission())) continue;
            if (serverName != null && !t.appliesToServer(serverName)) continue;

            if (t.getPriority() > highestPriority) {
                highestPriority = t.getPriority();
                names.clear();
                names.add(t.getName());
            } else if (t.getPriority() == highestPriority) {
                names.add(t.getName());
            }
        }

        if (names.isEmpty()) names.add("default");
        return names;
    }

    public List<String> getExtendsTabCommands(String name, List<String> result, int depth) {
        if (depth >= 30) return result; // prevent infinite recursion

        TabCommandList tab = getTabCommandListByName(name, configStructure.getTabCommandList());
        if (tab == null) return result;

        addUnique(result, tab.getCommands());
        if (tab.getExtendTabName() != null) {
            return getExtendsTabCommands(tab.getExtendTabName(), result, depth + 1);
        }
        return result;
    }

    public TabCommandList getTabCommandListByName(String name, List<TabCommandList> list) {
        for (TabCommandList t : list) {
            if (t.getName().equals(name)) return t;
        }
        return null;
    }

    public UseCommandResult useCommand(String command) {
        String[] commandWithArgs = command.toLowerCase().split(" ");

        if (commandWithArgs[0].contains(":") && configStructure.isBlockColonCommands()) {
            return new UseCommandResult(false, commandWithArgs[0]);
        }

        for (String blockedCommand : configStructure.getCommands()) {
            String[] blockedWithArgs = blockedCommand.toLowerCase().split(" ");
            int equal = 0;
            for (int i = 0; i < blockedWithArgs.length; i++) {
                if (i > commandWithArgs.length - 1) break;
                if (commandWithArgs[i].equals(blockedWithArgs[i])) equal++;
            }
            if (equal < blockedWithArgs.length) continue;

            if (configStructure.isUseCommandsAsWhitelist()) {
                return new UseCommandResult(true, blockedCommand);
            }
            return new UseCommandResult(false, blockedCommand);
        }

        if (configStructure.isUseCommandsAsWhitelist()) {
            return new UseCommandResult(false, commandWithArgs[0]);
        }
        return new UseCommandResult(true, null);
    }

    public List<String> getActionsForCustomCommand(String command) {
        for (CustomCommandGroup group : configStructure.getCustomCommands()) {
            if (group.getCommands().contains(command)) return group.getActions();
        }
        return null;
    }

    public ConfigStructure getConfigStructure() {
        return configStructure;
    }

    private void addUnique(List<String> target, List<String> source) {
        for (String s : source) {
            if (!target.contains(s)) target.add(s);
        }
    }
}
