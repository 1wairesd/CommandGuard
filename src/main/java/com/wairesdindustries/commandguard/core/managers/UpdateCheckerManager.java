package com.wairesdindustries.commandguard.core.managers;

import com.wairesdindustries.commandguard.core.model.internal.UpdateCheckerResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateCheckerManager {

    private String version;
    private String latestVersion;

    public UpdateCheckerManager(String version){
        this.version = version;
    }

    public UpdateCheckerResult check(){
        try {
            // Modrinth API: замени YOUR_PROJECT_ID на ID твоего проекта
            HttpURLConnection con = (HttpURLConnection) new URL(
                    "https://api.modrinth.com/v2/project/commandguard/version").openConnection();
            int timed_out = 1250;
            con.setConnectTimeout(timed_out);
            con.setReadTimeout(timed_out);
            con.setRequestProperty("User-Agent", "CommandGuard-UpdateChecker");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Парсим JSON вручную (простой способ без библиотек)
            String json = response.toString();
            // Ищем первую версию в массиве (самая новая)
            int versionStart = json.indexOf("\"version_number\":\"") + 18;
            int versionEnd = json.indexOf("\"", versionStart);
            
            if (versionStart > 17 && versionEnd > versionStart) {
                latestVersion = json.substring(versionStart, versionEnd);
                if (!version.equals(latestVersion)) {
                    return UpdateCheckerResult.noErrors(latestVersion);
                }
            }
            return UpdateCheckerResult.noErrors(null);
        } catch (Exception ex) {
            return UpdateCheckerResult.error();
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
