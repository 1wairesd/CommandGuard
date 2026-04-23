package com.wairesdindustries.commandguard.core.model;

public class GlobalVariables {

    public static final String bungeeMainChannel = "commandguard:channel";
    public static final String bungeeActionsSubChannel = "ActionsSubChannel";

    /**
     * Sub-channel used by proxy to push the list of allowed tab-complete commands
     * to the backend server for a specific player.
     * Payload: playerUUID (UTF) + comma-separated command list (UTF)
     */
    public static final String tabSyncSubChannel = "TabSync";
}
