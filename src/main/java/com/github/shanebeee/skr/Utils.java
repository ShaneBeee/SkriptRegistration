package com.github.shanebeee.skr;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods used by {@link Registration}.
 */
@SuppressWarnings("unused")
public class Utils {

    private static boolean DEBUG = false;
    private static String PREFIX = "<aqua>Sk<dark_aqua>Registration";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Utils() {
    }

    /**
     * Set whether debug messages should be printed to the console.
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

    public static String getPrefix(boolean error) {
        if (error) {
            return "<grey>[" + PREFIX + " <red>ERROR" + "<grey>] ";
        }
        return "<grey>[" + PREFIX + "<grey>] ";
    }

    /**
     * Does nothing now
     *
     * @param prefix Nothing
     */
    @Deprecated(forRemoval = true)
    public static void setPrefixError(String prefix) {
    }

    /**
     * Get a formatted message in MiniMessage format.
     *
     * @param format  Format of the message
     * @param objects Objects to format the message with
     * @return Formatted message in MiniMessage format
     */
    public static Component getMini(String format, Object... objects) {
        return MINI_MESSAGE.deserialize(String.format(format, objects));
    }

    /**
     * Log a message to console.
     *
     * @param format  Format of log message
     * @param objects Objects to format log message with
     */
    public static void log(String format, Object... objects) {
        String log = String.format(format, objects);
        Bukkit.getConsoleSender().sendMessage(getMini(getPrefix(false) + log));
    }

    /**
     * Prints a skript error to console and Skript log.
     *
     * @param format  Format of error message
     * @param objects Objects to format error message with
     */
    @Deprecated(forRemoval = true)
    public static void skriptError(String format, Object... objects) {
        error(format, objects);
    }

    /**
     * Prints an error to console.
     *
     * @param format  Format of error message
     * @param objects Objects to format error message with
     */
    public static void error(String format, Object... objects) {
        String error = String.format(format, objects);
        Bukkit.getConsoleSender().sendMessage(getMini(getPrefix(true) + "<red>" + error));

    }

    /**
     * Print a debug message to the console if debug is enabled.
     *
     * @param format  Format of the debug message
     * @param objects Objects to format the debug message with
     */
    public static void debug(String format, Object... objects) {
        if (DEBUG) {
            String debug = String.format(format, objects);
            Bukkit.getConsoleSender().sendMessage(getMini(getPrefix(true) + debug));
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
                error("An invalid key was provided, key must be less than 32767 characters: %s", key);
            return null;
        }
        key = key.toLowerCase();
        if (key.contains(" ")) {
            key = key.replace(" ", "_");
        }

        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null && error)
            error("An invalid key was provided, that didn't follow [a-z0-9/._-:]. key: %s", key);
        return namespacedKey;
    }

}
