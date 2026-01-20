package org.luigilp.lPChestShop.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.session.CreateSession;
import org.luigilp.lPChestShop.util.MoneyParser;

public final class ChatListener implements Listener {

    private final LPChestShop plugin;

    public ChatListener(LPChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        CreateSession session = plugin.getSessionManager().get(player.getUniqueId());
        if (session == null) return;

        if (System.currentTimeMillis() > session.getExpiresAtMs()) {
            plugin.getSessionManager().clear(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("cancel")) {
            plugin.getSessionManager().clear(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMessages().send(player, "create.cancelled"));
            return;
        }

        Parsed parsed;
        try {
            parsed = parseDeal(msg);
        } catch (IllegalArgumentException ex) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMessages().send(player, "errors.invalid-format"));
            return;
        }

        int amount = parsed.amount;
        long price = parsed.price;
        ItemStack template = session.getTemplate();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Location chestLoc = session.getChestLocation();
            Block chestBlock = chestLoc.getBlock();

            if (!plugin.getShopManager().isChestBlock(chestBlock)) {
                plugin.getSessionManager().clear(player.getUniqueId());
                plugin.getMessages().send(player, "errors.not-looking-at-chest",
                        java.util.Map.of("distance", String.valueOf(plugin.getConfig().getInt("settings.create.max-target-distance", 5))));
                return;
            }

            if (plugin.getShopManager().getByChest(chestBlock) != null) {
                plugin.getSessionManager().clear(player.getUniqueId());
                player.sendMessage("That chest is already a shop.");
                return;
            }

            if (plugin.getConfig().getBoolean("settings.create.require-empty-chest", true)
                    && !plugin.getShopManager().isChestEmpty(chestBlock)) {
                plugin.getSessionManager().clear(player.getUniqueId());
                plugin.getMessages().send(player, "errors.chest-not-empty");
                return;
            }

            var shop = plugin.getShopManager().createShop(player.getUniqueId(), chestBlock, template, amount, price);
            plugin.getSessionManager().clear(player.getUniqueId());

            if (shop == null) {
                plugin.getMessages().send(player, "errors.sign-blocked");
                return;
            }

            plugin.getMessages().send(player, "create.created");
        });
    }

    private Parsed parseDeal(String input) {
        String s = input.trim();
        String[] parts = s.split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("need 2 parts");

        int amount = Integer.parseInt(parts[0].replace("x", "").trim());
        if (amount <= 0) throw new IllegalArgumentException("bad amount");

        long price = MoneyParser.parseToLong(parts[1].trim());
        if (price <= 0) throw new IllegalArgumentException("bad price");

        return new Parsed(amount, price);
    }

    private record Parsed(int amount, long price) {}
}
