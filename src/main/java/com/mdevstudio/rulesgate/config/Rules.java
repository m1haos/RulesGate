package com.mdevstudio.rulesgate.config;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * Rules text in every language that has a file in {@code rules/}.
 */
public final class Rules {

    public record Text(Component title, List<Component> body, Component accept, Component decline, Component close) {
    }

    private static final String FOLDER = "rules";
    private static final List<String> BUNDLED = List.of("en", "ru");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<String, Text> byLanguage;
    private final Text fallback;

    private Rules(Map<String, Text> byLanguage, Text fallback) {
        this.byLanguage = byLanguage;
        this.fallback = fallback;
    }

    public static Rules load(JavaPlugin plugin, String defaultLanguage) {
        File folder = new File(plugin.getDataFolder(), FOLDER);
        if (!folder.isDirectory()) {
            BUNDLED.forEach(language -> plugin.saveResource(FOLDER + "/" + language + ".yml", false));
        }

        Logger logger = plugin.getSLF4JLogger();
        Map<String, Text> byLanguage = new HashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        for (File file : files != null ? files : new File[0]) {
            String language = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            byLanguage.put(language, read(YamlConfiguration.loadConfiguration(file), FOLDER + "/" + file.getName(),
                    logger));
        }

        Text fallback = byLanguage.get(defaultLanguage);
        if (fallback == null) {
            fallback = byLanguage.getOrDefault("en", byLanguage.values().stream().findFirst().orElse(null));
        }
        if (fallback == null) {
            logger.error("{}/ has no rules files, players will see an empty dialog until one is added.", FOLDER);
            fallback = new Text(Component.text("Rules"), List.of(), Component.text("Accept"),
                    Component.text("Decline"), Component.text("Close"));
        }
        return new Rules(byLanguage, fallback);
    }

    /**
     * Rules for a client locale such as {@code ru_ru} or {@code en_US}, or the default ones.
     */
    public Text forLocale(String locale) {
        if (locale == null) {
            return fallback;
        }
        String language = locale.toLowerCase(Locale.ROOT).split("[_-]")[0];
        return byLanguage.getOrDefault(language, fallback);
    }

    private static Text read(YamlConfiguration yaml, String file, Logger logger) {
        List<Component> body = yaml.getStringList("body").stream().map(MINI_MESSAGE::deserialize).toList();
        if (body.isEmpty()) {
            logger.warn("{} has no body lines, the dialog will only show the title and buttons.", file);
        }
        return new Text(
                parse(yaml, "title", "Rules"),
                body,
                parse(yaml, "accept", "Accept"),
                parse(yaml, "decline", "Decline"),
                parse(yaml, "close", "Close"));
    }

    private static Component parse(YamlConfiguration yaml, String key, String fallback) {
        return MINI_MESSAGE.deserialize(yaml.getString(key, fallback));
    }
}
