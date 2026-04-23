package com.wairesdindustries.commandguard.core.managers;

import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class LangManager {

    private YamlFile lang;
    private final Path dataDirectory;

    public LangManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void load(String langCode) {
        String fileName = "lang/" + langCode + ".yml";
        File langDir = new File(dataDirectory.toFile(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        File langFile = new File(langDir, langCode + ".yml");

        // Extract bundled lang files if they don't exist yet
        extractIfMissing("lang/en_EN.yml");
        extractIfMissing("lang/ru_RU.yml");

        // If requested lang file doesn't exist, fall back to en_EN
        if (!langFile.exists()) {
            langFile = new File(langDir, "en_EN.yml");
        }

        lang = new YamlFile(langFile.getAbsolutePath());
        try {
            lang.loadWithComments();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractIfMissing(String resourcePath) {
        File target = new File(dataDirectory.toFile(), resourcePath);
        if (target.exists()) return;
        target.getParentFile().mkdirs();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) Files.copy(in, target.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a message string with placeholders replaced.
     * %prefix% is always available and resolves to the lang file prefix.
     */
    public String get(String key, Object... replacements) {
        String value = lang != null ? lang.getString(key, key) : key;
        // Replace %prefix% automatically
        value = value.replace("%prefix%", getPrefix());
        if (replacements.length % 2 != 0) return value;
        for (int i = 0; i < replacements.length; i += 2) {
            value = value.replace("%" + replacements[i] + "%", String.valueOf(replacements[i + 1]));
        }
        return value;
    }

    /** Returns the plugin prefix defined in the lang file. */
    public String getPrefix() {
        return lang != null ? lang.getString("prefix", "&8[&cCommand&fGuard&8]") : "&8[&cCommand&fGuard&8]";
    }

    public YamlFile getLang() {
        return lang;
    }
}
