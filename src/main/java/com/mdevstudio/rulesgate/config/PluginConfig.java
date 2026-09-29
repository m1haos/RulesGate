package com.mdevstudio.rulesgate.config;

import java.time.Duration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public record PluginConfig(String language, int rulesVersion, Duration answerTimeout) {

    private static final int VERSION = 1;
    private static final String DEFAULT_LANGUAGE = "en";

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration yaml = plugin.getConfig();
        Logger logger = plugin.getSLF4JLogger();

        if (yaml.getInt("config-version") < VERSION) {
            // Admins keep their values; only keys added in newer versions are written in.
            yaml.options().copyDefaults(true);
            yaml.set("config-version", VERSION);
            plugin.saveConfig();
        }

        String language = yaml.getString("language", DEFAULT_LANGUAGE);
        if (!Messages.LANGUAGES.contains(language)) {
            logger.warn("config.yml: language '{}' is not available, expected one of {}. Using '{}'.",
                    language, Messages.LANGUAGES, DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }

        int rulesVersion = yaml.getInt("rules-version", 1);
        if (rulesVersion < 1) {
            logger.warn("config.yml: rules-version should be 1 or more, got {}. Using 1.", rulesVersion);
            rulesVersion = 1;
        }

        int minutes = yaml.getInt("answer-timeout", 5);
        if (minutes < 1) {
            logger.warn("config.yml: answer-timeout should be at least 1 minute, got {}. Using 5.", minutes);
            minutes = 5;
        }
        return new PluginConfig(language, rulesVersion, Duration.ofMinutes(minutes));
    }
}
