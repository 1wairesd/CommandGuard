package com.wairesdindustries.commandguard.waterfall;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.ConfigManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.core.managers.UpdateCheckerManager;
import com.wairesdindustries.commandguard.core.model.GlobalVariables;
import com.wairesdindustries.commandguard.core.model.internal.UpdateCheckerResult;
import com.wairesdindustries.commandguard.waterfall.listeners.PlayerListener;
import com.wairesdindustries.commandguard.waterfall.listeners.ServerListener;
import com.wairesdindustries.commandguard.waterfall.utils.MessagesUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class CommandGuard extends Plugin {

    public String prefix = "&8[&bCommand&9Guard&8]";

    public String getPrefix() {
        return langManager != null ? langManager.getPrefix() : prefix;
    }
    public String version = getDescription().getVersion();

    private CommandsManager commandsManager;
    private ConfigManager configManager;
    private LangManager langManager;
    private UpdateCheckerManager updateCheckerManager;

    public void onEnable() {
        getProxy().registerChannel(GlobalVariables.bungeeMainChannel);
        this.configManager = new ConfigManager(this.getDataFolder().toPath(), "bungee-config.yml", "config.yml", true);
        this.configManager.registerConfig();
        this.configManager.checkMessagesUpdate();
        commandsManager = new CommandsManager(configManager.getConfig());
        langManager = new LangManager(this.getDataFolder().toPath());
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));
        registerCommands();
        registerEvents();

        log("console.enabled", "version", version);
        updateCheckerManager = new UpdateCheckerManager(version);
        updateMessage(updateCheckerManager.check());
    }

    public void onDisable() {
        log("console.disabled", "version", version);
    }

    public void customReload() {
        configManager.registerConfig();
        commandsManager.load(configManager.getConfig());
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));
    }

    public void registerCommands() {
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new MainCommand(this));
    }

    public void registerEvents() {
        PluginManager pm = getProxy().getPluginManager();
        pm.registerListener(this, new PlayerListener(this));
        pm.registerListener(this, new ServerListener(this));
    }

    public void updateMessage(UpdateCheckerResult result) {
        if (!result.isError()) {
            String latestVersion = result.getLatestVersion();
            if (latestVersion != null) {
                log("console.update_available", "version", latestVersion);
                ProxyServer.getInstance().getConsole().sendMessage(
                        MessagesUtils.getColoredMessage(langManager.get("console.update_download")));
            }
        } else {
            log("console.update_error");
        }
    }

    private void log(String key, Object... replacements) {
        ProxyServer.getInstance().getConsole().sendMessage(
                MessagesUtils.getColoredMessage(prefix + " " + langManager.get(key, replacements)));
    }

    public CommandsManager getCommandsManager() { return commandsManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public LangManager getLangManager() { return langManager; }
}
