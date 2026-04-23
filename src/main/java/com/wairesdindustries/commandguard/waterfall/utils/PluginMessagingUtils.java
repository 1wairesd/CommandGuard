package com.wairesdindustries.commandguard.waterfall.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.wairesdindustries.commandguard.core.model.GlobalVariables;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public class PluginMessagingUtils {

    public static void sendMessage(ProxiedPlayer player, String subChannel, String data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        out.writeUTF(data);

        player.getServer().getInfo().sendData(GlobalVariables.bungeeMainChannel, out.toByteArray());
    }

    /**
     * Sends the allowed tab-complete command list to the backend server so that
     * the Spigot-side plugin can apply the same filtering as the proxy.
     */
    public static void sendTabSync(ProxiedPlayer player, List<String> allowedCommands) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(GlobalVariables.tabSyncSubChannel);
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(String.join(",", allowedCommands));

        player.getServer().getInfo().sendData(GlobalVariables.bungeeMainChannel, out.toByteArray());
    }
}
