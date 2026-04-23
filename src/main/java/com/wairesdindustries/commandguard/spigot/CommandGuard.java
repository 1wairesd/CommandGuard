package com.wairesdindustries.commandguard.spigot;

import com.wairesdindustries.commandguard.core.managers.CommandsManager;
import com.wairesdindustries.commandguard.core.managers.ConfigManager;
import com.wairesdindustries.commandguard.core.managers.LangManager;
import com.wairesdindustries.commandguard.core.managers.UpdateCheckerManager;
import com.wairesdindustries.commandguard.core.model.GlobalVariables;
import com.wairesdindustries.commandguard.core.model.internal.UpdateCheckerResult;
import com.wairesdindustries.commandguard.spigot.listeners.PlayerListener;
import com.wairesdindustries.commandguard.spigot.listeners.PlayerListenerNew;
import com.wairesdindustries.commandguard.spigot.managers.BungeeMessagingManager;
import com.wairesdindustries.commandguard.spigot.managers.ProtocolLibManager;
import com.wairesdindustries.commandguard.spigot.managers.ViaVersionManager;
import com.wairesdindustries.commandguard.spigot.utils.MessagesUtils;
import com.wairesdindustries.commandguard.spigot.utils.OtherUtils;
import com.wairesdindustries.commandguard.spigot.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandGuard extends JavaPlugin {

    /** @deprecated use {@link #getPrefix()} instead */
    public static String prefix = "&8[&bCommand&9Guard&8]";

    public static String getPrefix() {
        if (instance != null && instance.langManager != null) {
            return instance.langManager.getPrefix();
        }
        return prefix;
    }

    private static CommandGuard instance;
    private PluginDescriptionFile pdfFile = getDescription();
    public String version = pdfFile.getVersion();
    public static ServerVersion serverVersion;
    private ProtocolLibManager protocolLibManager;
    private ViaVersionManager viaVersionManager;
    private BungeeMessagingManager bungeeMessagingManager;
    private CommandsManager commandsManager;
    private ConfigManager configManager;
    private LangManager langManager;
    private UpdateCheckerManager updateCheckerManager;

    public void onEnable(){
        instance = this;
        setVersion();
        this.configManager = new ConfigManager(this.getDataFolder().toPath(),"config.yml","config.yml",false);
        this.configManager.registerConfig();
        this.configManager.checkMessagesUpdate();
        commandsManager = new CommandsManager(configManager.getConfig());
        langManager = new LangManager(this.getDataFolder().toPath());
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));
        registerCommands();
        registerEvents();
        bungeeMessagingManager = new BungeeMessagingManager(this);
        // Register outgoing channel so the proxy can send plugin messages to us
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, GlobalVariables.bungeeMainChannel);
        if(this.configManager.getConfig().getBoolean("legacy_support")){
            protocolLibManager = new ProtocolLibManager(this);
        }
        viaVersionManager = new ViaVersionManager(this);

        Bukkit.getConsoleSender().sendMessage(MessagesUtils.getColoredMessage(prefix+" "+langManager.get("console.enabled", "version", version)));

        updateCheckerManager = new UpdateCheckerManager(version);
        updateMessage(updateCheckerManager.check());
    }

    public void onDisable(){
        Bukkit.getConsoleSender().sendMessage(MessagesUtils.getColoredMessage(prefix+" "+langManager.get("console.disabled", "version", version)));
    }

    public void setVersion(){
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String bukkitVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];
        switch(bukkitVersion){
            case "1.20.5":
            case "1.20.6":
                serverVersion = ServerVersion.v1_20_R4;
                break;
            case "1.21":
            case "1.21.1":
                serverVersion = ServerVersion.v1_21_R1;
                break;
            case "1.21.2":
            case "1.21.3":
                serverVersion = ServerVersion.v1_21_R2;
                break;
            case "1.21.4":
                serverVersion = ServerVersion.v1_21_R3;
                break;
            case "1.21.5":
                serverVersion = ServerVersion.v1_21_R4;
                break;
            case "1.21.6":
            case "1.21.7":
            case "1.21.8":
                serverVersion = ServerVersion.v1_21_R5;
                break;
            case "1.21.9":
            case "1.21.10":
                serverVersion = ServerVersion.v1_21_R6;
                break;
            case "1.21.11":
                serverVersion = ServerVersion.v1_21_R7;
                break;
            case "26.1":
                serverVersion = ServerVersion.v26_1;
                break;
            default:
                try{
                    serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
                }catch(Exception e){
                    serverVersion = ServerVersion.v26_1;
                }
        }
    }

    public void customReload(){
        configManager.registerConfig();
        this.commandsManager.load(configManager.getConfig());
        langManager.load(configManager.getConfig().getString("lang", "en_EN"));
    }

    public void registerCommands(){
        this.getCommand("cg").setExecutor(new MainCommand(this));
    }

    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        if(!OtherUtils.serverIsLegacy()){
            pm.registerEvents(new PlayerListenerNew(this), this);
        }
    }

    public ProtocolLibManager getProtocolLibManager() {
        return protocolLibManager;
    }

    public ViaVersionManager getViaVersionManager() {
        return viaVersionManager;
    }

    public BungeeMessagingManager getBungeeMessagingManager() {
        return bungeeMessagingManager;
    }

    public UpdateCheckerManager getUpdateCheckerManager() {
        return updateCheckerManager;
    }

    public CommandsManager getCommandsManager() {
        return commandsManager;
    }

    public void updateMessage(UpdateCheckerResult result){
        if(!result.isError()){
            String latestVersion = result.getLatestVersion();
            if(latestVersion != null){
                Bukkit.getConsoleSender().sendMessage(MessagesUtils.getColoredMessage("&cThere is a new version available. &e(&7"+latestVersion+"&e)"));
                Bukkit.getConsoleSender().sendMessage(MessagesUtils.getColoredMessage("&cYou can download it at: &fhttps://modrinth.com/plugin/easy-command-blocker"));
            }
        }else{
            Bukkit.getConsoleSender().sendMessage(MessagesUtils.getColoredMessage(prefix+" &cError while checking update."));
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }
}
