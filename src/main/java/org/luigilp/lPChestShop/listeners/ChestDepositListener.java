package org.luigilp.lPChestShop.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.model.Shop;
import org.luigilp.lPChestShop.util.ItemUtils;

public final class ChestDepositListener implements Listener {

    private final LPChestShop plugin;

    public ChestDepositListener(LPChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        Shop shop = resolveShopFromTopInventory(top);
        if (shop == null) return;

        boolean bypass = player.hasPermission("lpchestshop.bypass");
        boolean owner = shop.getOwner().equals(player.getUniqueId());

        if (!owner && !bypass) {
            event.setCancelled(true);
            return;
        }

        ItemStack template = shop.getItemTemplate();

        if (event.isShiftClick() && event.getClickedInventory() != null) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                ItemStack moving = event.getCurrentItem();
                if (!ItemUtils.isAir(moving) && !template.isSimilar(moving) && !bypass) {
                    event.setCancelled(true);
                    plugin.getMessages().send(player, "deposit.not-allowed");
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopManager().refreshSign(shop));
            }
            return;
        }

        if (event.getRawSlot() < top.getSize()) {
            ItemStack cursor = event.getCursor();
            if (!ItemUtils.isAir(cursor) && !template.isSimilar(cursor) && !bypass) {
                event.setCancelled(true);
                plugin.getMessages().send(player, "deposit.not-allowed");
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopManager().refreshSign(shop));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        Shop shop = resolveShopFromTopInventory(top);
        if (shop == null) return;

        boolean bypass = player.hasPermission("lpchestshop.bypass");
        boolean owner = shop.getOwner().equals(player.getUniqueId());

        if (!owner && !bypass) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursor = event.getOldCursor();
        if (ItemUtils.isAir(cursor)) return;

        if (!bypass && !shop.getItemTemplate().isSimilar(cursor)) {
            for (int raw : event.getRawSlots()) {
                if (raw < top.getSize()) {
                    event.setCancelled(true);
                    plugin.getMessages().send(player, "deposit.not-allowed");
                    return;
                }
            }
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopManager().refreshSign(shop));
    }

    private Shop resolveShopFromTopInventory(Inventory top) {

        Location loc = top.getLocation();
        if (loc != null) {
            Block base = loc.getBlock();
            Shop s = plugin.getShopManager().getByChest(base);
            if (s != null) return s;

            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adj = base.getRelative(face);
                s = plugin.getShopManager().getByChest(adj);
                if (s != null) return s;
            }
        }

        return null;
    }
}
