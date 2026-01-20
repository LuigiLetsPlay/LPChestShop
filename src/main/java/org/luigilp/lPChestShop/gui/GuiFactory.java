package org.luigilp.lPChestShop.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.model.Shop;
import org.luigilp.lPChestShop.util.ItemBuilder;
import org.luigilp.lPChestShop.util.Text;

public final class GuiFactory {

    private final LPChestShop plugin;

    public GuiFactory(LPChestShop plugin) {
        this.plugin = plugin;
    }

    public Inventory createCreateGui(org.bukkit.entity.Player player, ItemStack initial) {
        var cfg = plugin.getConfig().getConfigurationSection("gui.create");
        int size = cfg.getInt("size", 27);
        String title = Text.color(cfg.getString("title", "&aCreate Chest Shop"));
        int center = cfg.getInt("center-slot", 13);

        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiHolder.Type.CREATE, null), size, title);

        ItemStack bg = buildItem(cfg.getConfigurationSection("background"));
        for (int i = 0; i < size; i++) inv.setItem(i, bg);

        int confirmSlot = cfg.getInt("confirm-slot", 15);
        int cancelSlot = cfg.getInt("cancel-slot", 11);
        int infoSlot = cfg.getInt("info-slot", 22);

        inv.setItem(confirmSlot, buildItem(cfg.getConfigurationSection("confirm-item")));
        inv.setItem(cancelSlot, buildItem(cfg.getConfigurationSection("cancel-item")));
        inv.setItem(infoSlot, buildItem(cfg.getConfigurationSection("info-item")));

        inv.setItem(center, initial);
        return inv;
    }

    public Inventory createDetailsGui(Shop shop) {
        var cfg = plugin.getConfig().getConfigurationSection("gui.details");
        int size = cfg.getInt("size", 27);

        String titleRaw = cfg.getString("title", "&aShop: &f{item}");
        String title = Text.color(titleRaw.replace("{item}", Text.prettyItemName(shop.getItemTemplate())));

        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiHolder.Type.DETAILS, shop.getId()), size, title);

        ItemStack bg = buildItem(cfg.getConfigurationSection("background"));
        for (int i = 0; i < size; i++) inv.setItem(i, bg);

        int itemSlot = cfg.getInt("item-slot", 13);
        ItemStack show = shop.getItemTemplate().clone();
        show.setAmount(shop.getAmount());
        inv.setItem(itemSlot, show);

        int hintSlot = cfg.getInt("hint-slot", 22);
        inv.setItem(hintSlot, buildItem(cfg.getConfigurationSection("hint-item")));

        return inv;
    }

    private ItemStack buildItem(ConfigurationSection sec) {
        if (sec == null) return new ItemStack(org.bukkit.Material.AIR);
        return ItemBuilder.fromConfig(sec);
    }
}
