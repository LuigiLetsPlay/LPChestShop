package org.luigilp.lPChestShop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ItemBuilder {

    private ItemBuilder() {}

    public static ItemStack fromConfig(ConfigurationSection sec) {
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack it = new ItemStack(mat);
        var meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(sec.getString("name", " ")));

            List<String> lore = sec.getStringList("lore");
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(Text::color).toList());
            }

            int cmd = sec.getInt("custom-model-data", 0);
            if (cmd > 0) meta.setCustomModelData(cmd);

            boolean glow = sec.getBoolean("glow", false);
            if (glow) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            it.setItemMeta(meta);
        }
        return it;
    }
}
