package com.wairesdindustries.commandguard.velocity.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.wairesdindustries.commandguard.core.model.GlobalVariables;

import java.util.List;

public class PluginMessagingUtils {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(GlobalVariables.bungeeMainChannel);

    public static void sendMessage(Player player, String subChannel, String data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        out.writeUTF(data);

        player.getCurrentServer().ifPresent((ServerConnection serverConnection) -> {
            serverConnection.sendPluginMessage(IDENTIFIER, out.toByteArray());
        });
    }

    /**
     * Sends the allowed tab-complete command list to the backend server so that
     * the Spigot-side plugin can apply the same filtering as the proxy.
     */
    public static void sendTabSync(Player player, List<String> allowedCommands) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(GlobalVariables.tabSyncSubChannel);
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(String.join(",", allowedCommands));

        player.getCurrentServer().ifPresent(serverConnection ->
                serverConnection.sendPluginMessage(IDENTIFIER, out.toByteArray()));
    }
}
