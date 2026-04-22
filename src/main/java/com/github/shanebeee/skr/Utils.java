package com.github.shanebeee.skr;

import ch.njol.skript.Skript;
import ch.njol.skript.log.ErrorQuality;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods used by {@link Registration}.
 */
@SuppressWarnings("unused")
public class Utils {

    private static boolean DEBUG = false;
    private static String PREFIX = "&7[&bSk&3Registration&7] ";
    private static String PREFIX_ERROR = "&7[&bSk&3Registration &cERROR&7] ";
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f\\d]){6}>");

    private Utils() {
    }

    /**
     * Set whether debug messages should be printed to console.
     *
     * @param debug Whether debug messages should be printed
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * Set the prefix for all messages printed to console.
     *
     * @param prefix Prefix to use for messages
     */
    public static void setPrefix(String prefix) {
        PREFIX = prefix;
    }

    /**
     * Set the prefix for all error messages printed to console.
     *
     * @param prefix Prefix to use for error messages
     */
    public static void setPrefixError(String prefix) {
        PREFIX_ERROR = prefix;
    }

    @SuppressWarnings("deprecation") // Paper deprecation
    private static String getColString(String string) {
        Matcher matcher = HEX_PATTERN.matcher(string);
        while (matcher.find()) {
            final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = string.substring(0, matcher.start());
            final String after = string.substring(matcher.end());
            string = before + hexColor + after;
            matcher = HEX_PATTERN.matcher(string);
        }

        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /** Log a message to console.
     * @param format  Format of log message
     * @param objects Objects to format log message with
     */
    public static void log(String format, Object... objects) {
        String log = String.format(format, objects);
        Bukkit.getConsoleSender().sendMessage(getColString(PREFIX + log));
    }

    /**
     * Prints a skript error to console and Skript log.
     *
     * @param format  Format of error message
     * @param objects Objects to format error message with
     */
    public static void skriptError(String format, Object... objects) {
        String error = String.format(format, objects);
        Skript.error(getColString(PREFIX_ERROR + error), ErrorQuality.SEMANTIC_ERROR);
    }

    /**
     * Print a debug message to console if debug is enabled.
     *
     * @param format  Format of debug message
     * @param objects Objects to format debug message with
     */
    public static void debug(String format, Object... objects) {
        if (DEBUG) {
            String debug = String.format(format, objects);
            Bukkit.getConsoleSender().sendMessage(getColString(PREFIX_ERROR + debug));
        }
    }


    /**
     * Gets a Minecraft NamespacedKey from string
     * <p>If a namespace is not provided, it will default to "minecraft:" namespace</p>
     *
     * @param key   Key for new Minecraft NamespacedKey
     * @param error Whether to send a skript/console error if one occurs
     * @return new Minecraft NamespacedKey
     */
    @Nullable
    public static NamespacedKey getNamespacedKey(@Nullable String key, boolean error) {
        if (key == null) return null;
        if (!key.contains(":")) key = "minecraft:" + key;
        if (key.length() > Short.MAX_VALUE) {
            if (error)
                skriptError("An invalid key was provided, key must be less than 32767 characters: %s", key);
            return null;
        }
        key = key.toLowerCase();
        if (key.contains(" ")) {
            key = key.replace(" ", "_");
        }

        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null && error)
            skriptError("An invalid key was provided, that didn't follow [a-z0-9/._-:]. key: %s", key);
        return namespacedKey;
    }

}
