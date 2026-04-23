package com.wairesdindustries.commandguard.core.model;

import java.util.List;

public class TabCommandList {
    private String name;
    private int priority;
    private List<String> commands;
    private String extendTabName;
    /**
     * Optional list of server names this group applies to (proxy-side only).
     * Empty / null means the group applies on all servers.
     */
    private List<String> servers;

    public TabCommandList(String name, int priority, List<String> commands,
                          String extendTabName, List<String> servers) {
        this.name = name;
        this.priority = priority;
        this.commands = commands;
        this.extendTabName = extendTabName;
        this.servers = servers;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }

    public String getExtendTabName() { return extendTabName; }
    public void setExtendTabName(String extendTabName) { this.extendTabName = extendTabName; }

    public List<String> getServers() { return servers; }
    public void setServers(List<String> servers) { this.servers = servers; }

    /** Returns true if this group applies to the given server name (or has no server restriction). */
    public boolean appliesToServer(String serverName) {
        if (servers == null || servers.isEmpty()) return true;
        return servers.stream().anyMatch(s -> s.equalsIgnoreCase(serverName));
    }

    public String getPermission() {
        return "commandguard.tab." + name;
    }
}
