package org.luigilp.lPChestShop.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class Messages {

    private final JavaPlugin plugin;
    private YamlConfiguration yml;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        this.yml = YamlConfiguration.loadConfiguration(f);
    }

    public void send(CommandSender to, String key) {
        send(to, key, Map.of());
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        String prefix = Text.color(yml.getString("prefix", ""));
        String msg = yml.getString(key);
        if (msg == null) return;
        msg = Text.applyPlaceholders(msg, placeholders);
        to.sendMessage(prefix + Text.color(msg));
    }

    public void sendLines(CommandSender to, String key) {
        sendLines(to, key, Map.of());
    }

    public void sendLines(CommandSender to, String key, Map<String, String> placeholders) {
        String prefix = Text.color(yml.getString("prefix", ""));
        List<String> lines = yml.getStringList(key);
        for (String line : lines) {
            line = Text.applyPlaceholders(line, placeholders);
            to.sendMessage(prefix + Text.color(line));
        }
    }
}
