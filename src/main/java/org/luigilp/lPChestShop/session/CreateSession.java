package org.luigilp.lPChestShop.session;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class CreateSession {

    public enum Stage { AWAITING_PRICE }

    private final Location chestLocation;
    private Stage stage;
    private ItemStack template;

    private long expiresAtMs;

    public CreateSession(Location chestLocation) {
        this.chestLocation = chestLocation;
        this.stage = Stage.AWAITING_PRICE;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public ItemStack getTemplate() {
        return template;
    }

    public void setTemplate(ItemStack template) {
        this.template = template;
    }

    public long getExpiresAtMs() {
        return expiresAtMs;
    }

    public void setExpiresAtMs(long expiresAtMs) {
        this.expiresAtMs = expiresAtMs;
    }
}
