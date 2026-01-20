package org.luigilp.lPChestShop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiHolder implements InventoryHolder {

    public enum Type { CREATE, DETAILS }

    private final Type type;
    private final String shopId;

    public GuiHolder(Type type, String shopId) {
        this.type = type;
        this.shopId = shopId;
    }

    public Type getType() {
        return type;
    }

    public String getShopId() {
        return shopId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
