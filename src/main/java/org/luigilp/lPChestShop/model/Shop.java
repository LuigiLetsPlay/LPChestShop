package org.luigilp.lPChestShop.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class Shop {
    private final String id;
    private final UUID owner;
    private final Location chest;
    private final Location sign;
    private final ItemStack itemTemplate;
    private final int amount;
    private final long price;

    public Shop(String id, UUID owner, Location chest, Location sign, ItemStack itemTemplate, int amount, long price) {
        this.id = id;
        this.owner = owner;
        this.chest = chest;
        this.sign = sign;
        this.itemTemplate = itemTemplate;
        this.amount = amount;
        this.price = price;
    }

    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public Location getChest() { return chest; }
    public Location getSign() { return sign; }
    public ItemStack getItemTemplate() { return itemTemplate; }
    public int getAmount() { return amount; }
    public long getPrice() { return price; }
}
