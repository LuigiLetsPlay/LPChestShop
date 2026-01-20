package org.luigilp.lPChestShop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ItemUtils {
    private ItemUtils() {}

    public static boolean isAir(ItemStack it) {
        return it == null || it.getType() == Material.AIR;
    }
}
