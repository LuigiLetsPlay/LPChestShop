package org.luigilp.lPChestShop.listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.model.Shop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BlockBreakListener implements Listener {

    private final LPChestShop plugin;

    private static final class Pending {
        final String shopId;
        final long expiresAt;

        Pending(String shopId, long expiresAt) {
            this.shopId = shopId;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, Pending> pending = new HashMap<>();

    public BlockBreakListener(LPChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (plugin.getShopManager().isChestBlock(block)) {
            Shop shop = plugin.getShopManager().getByChest(block);
            if (shop != null) {
                event.setCancelled(true);
                plugin.getMessages().send(player, "errors.shop-chest-protected");
                return;
            }
        }

        if (!Tag.WALL_SIGNS.isTagged(block.getType())) return;

        Shop shop = plugin.getShopManager().getBySign(block);
        if (shop == null) return;

        boolean requireAxe = plugin.getConfig().getBoolean("settings.removal.require-axe", true);
        if (requireAxe && !isAxe(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "remove.need-axe");
            return;
        }

        boolean owner = shop.getOwner().equals(player.getUniqueId());
        boolean canRemove = (owner && player.hasPermission("lpchestshop.remove.own")) || player.hasPermission("lpchestshop.remove.any");
        if (!canRemove) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "remove.not-owner");
            return;
        }

        int seconds = plugin.getConfig().getInt("settings.removal.confirm-seconds", 8);
        long now = System.currentTimeMillis();

        Pending p = pending.get(player.getUniqueId());
        if (p == null || now > p.expiresAt || !p.shopId.equals(shop.getId())) {
            pending.put(player.getUniqueId(), new Pending(shop.getId(), now + seconds * 1000L));
            event.setCancelled(true);
            plugin.getMessages().send(player, "remove.confirm", Map.of("seconds", String.valueOf(seconds)));
            return;
        }

        pending.remove(player.getUniqueId());
        event.setCancelled(true);

        plugin.getShopManager().removeShop(shop);
        block.setType(Material.AIR);

        plugin.getMessages().send(player, "remove.removed");
    }

    private boolean isAxe(ItemStack it) {
        if (it == null) return false;
        return it.getType().name().endsWith("_AXE");
    }
}
