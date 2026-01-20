package org.luigilp.lPChestShop.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.Map;

public final class Text {

    private Text() {}

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String stripColor(String s) {
        return ChatColor.stripColor(s);
    }

    public static String applyPlaceholders(String s, Map<String, String> ph) {
        String out = s;
        for (var e : ph.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    public static String prettyItemName(ItemStack it) {
        if (it == null) return "Item";
        var meta = it.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return stripColor(meta.getDisplayName());
        }
        return it.getType().name().toLowerCase().replace("_", " ");
    }

    public static String formatMoney(FileConfiguration cfg, long value) {
        boolean shortFmt = cfg.getBoolean("settings.economy.display-short", true);
        if (!shortFmt) {
            return new DecimalFormat("#,###").format(value);
        }
        if (value >= 1_000_000L) {
            double m = value / 1_000_000.0;
            return trimOneDecimal(m) + "m";
        }
        if (value >= 1_000L) {
            double k = value / 1_000.0;
            return trimOneDecimal(k) + "k";
        }
        return String.valueOf(value);
    }

    private static String trimOneDecimal(double v) {
        String s = new DecimalFormat("#.0").format(v);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    }
}
