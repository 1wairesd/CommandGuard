package com.wairesdindustries.commandguard.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.ConfigManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.core.managers.UpdateCheckerManager;
import com.wairesdindustries.commandguard.core.model.internal.UpdateCheckerResult;
import com.wairesdindustries.commandguard.velocity.listeners.PlayerListener;
import com.wairesdindustries.commandguard.velocity.utils.PluginMessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "commandguard", name = "CommandGuard",
        version = "2025.04.1", authors = {"Ajneb97"})
public class CommandGuard {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private CommandsManager commandsManager;
    private ConfigManager configManager;
    private LangManager langManager;
    public String prefix = "<dark_gray>[<aqua>Command<blue>Guard<dark_gray>]";

    public String getPrefix() {
        return langManager != null ? langManager.getPrefix() : prefix;
    }
    private final PluginContainer container;

    private UpdateCheckerManager updateCheckerManager;

    @Inject
    public CommandGuard(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer container) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.container = container;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(dataDirectory,"velocity-config.yml","config.yml",true);
        this.configManager.registerConfig();
        this.configManager.checkMessagesUpdate();
        commandsManager = new CommandsManager(configManager.getConfig());
        langManager = new LangManager(dataDirectory);
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));

        server.getEventManager().register(this, new PlayerListener(this));
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("commandguardvelocity").aliases("cgv").build(),
                new MainCommand(this));

        server.getChannelRegistrar().register(PluginMessagingUtils.IDENTIFIER);

        updateCheckerManager = new UpdateCheckerManager(container.getDescription().getVersion().orElse("Unknown"));
        updateMessage(updateCheckerManager.check());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Check if the identifier matches first, no matter the source.
        if (!PluginMessagingUtils.IDENTIFIER.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    public ProxyServer getServer(){
        return this.server;
    }

    public CommandsManager getCommandsManager() {
        return commandsManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public void customReload(){
        configManager.registerConfig();
        this.commandsManager.load(configManager.getConfig());
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public void updateMessage(UpdateCheckerResult result){
        MiniMessage mm = MiniMessage.miniMessage();
        if(!result.isError()){
            String latestVersion = result.getLatestVersion();
            if(latestVersion != null){
                getServer().getConsoleCommandSource().sendMessage(mm.deserialize(
                        prefix + " " + langManager.get("console.update_available", "version", latestVersion)));
                getServer().getConsoleCommandSource().sendMessage(mm.deserialize(
                        langManager.get("console.update_download")));
            }
        }else{
            getServer().getConsoleCommandSource().sendMessage(mm.deserialize(
                    prefix + " " + langManager.get("console.update_error")));
        }
    }
}
