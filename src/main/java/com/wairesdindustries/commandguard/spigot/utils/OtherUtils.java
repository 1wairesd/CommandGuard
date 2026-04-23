package com.wairesdindustries.commandguard.spigot.utils;

import com.wairesdindustries.commandguard.spigot.CommandGuard;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OtherUtils {

    public static boolean serverIsNew() {
        ServerVersion serverVersion = CommandGuard.serverVersion;
        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_16_R1)){
            return true;
        }
        return false;
    }

    public static boolean serverIsLegacy() {
        ServerVersion serverVersion = CommandGuard.serverVersion;
        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_13_R1)) {
            return false;
        }
        return true;
    }

    public static List<String> getPlayerPermissionsList(Player player){
        List<String> permissions = new ArrayList<String>();
        Set<PermissionAttachmentInfo> pai = player.getEffectivePermissions();
        for(PermissionAttachmentInfo p : pai){
            if(p.getValue()){
                permissions.add(p.getPermission());
            }
        }
        return permissions;
    }
}
