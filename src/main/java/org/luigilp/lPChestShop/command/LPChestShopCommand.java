package org.luigilp.lPChestShop.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.model.Shop;
import org.luigilp.lPChestShop.session.CreateSession;
import org.luigilp.lPChestShop.util.ItemUtils;
import org.luigilp.lPChestShop.util.MoneyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LPChestShopCommand implements CommandExecutor, TabCompleter {

    private final LPChestShop plugin;

    public LPChestShopCommand(LPChestShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessages().sendLines(sender, "help.lines");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("lpchestshop.reload")) {
                plugin.getMessages().send(sender, "errors.no-permission");
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage("LPChestShop reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "errors.player-only");
            return true;
        }

        if (sub.equals("create")) {
            boolean permRequired = plugin.getConfig().getBoolean("settings.create.permission-required", false);
            if (permRequired && !player.hasPermission("lpchestshop.create")) {
                plugin.getMessages().send(player, "errors.no-permission");
                return true;
            }

            int max = plugin.getConfig().getInt("settings.create.max-shops-per-player", -1);
            if (max >= 0) {
                int owned = plugin.getShopManager().countOwnedBy(player.getUniqueId());
                if (owned >= max) {
                    player.sendMessage("You reached the maximum amount of shops (" + max + ").");
                    return true;
                }
            }

            int dist = plugin.getConfig().getInt("settings.create.max-target-distance", 5);
            Block chestBlock = player.getTargetBlockExact(dist);
            if (chestBlock == null || !plugin.getShopManager().isChestBlock(chestBlock)) {
                plugin.getMessages().send(player, "errors.not-looking-at-chest", Map.of("distance", String.valueOf(dist)));
                return true;
            }

            if (plugin.getShopManager().getByChest(chestBlock) != null) {
                player.sendMessage("That chest is already a shop.");
                return true;
            }

            if (plugin.getConfig().getBoolean("settings.create.require-empty-chest", true) && !plugin.getShopManager().isChestEmpty(chestBlock)) {
                plugin.getMessages().send(player, "errors.chest-not-empty");
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (ItemUtils.isAir(hand)) {
                plugin.getMessages().send(player, "errors.need-item-in-hand");
                return true;
            }

            if (args.length >= 3) {
                int amount;
                long price;
                try {
                    amount = Integer.parseInt(args[1].replace("x", "").trim());
                    price = MoneyParser.parseToLong(args[2].trim());
                } catch (Exception ex) {
                    plugin.getMessages().send(player, "errors.invalid-format");
                    return true;
                }
                if (amount <= 0 || price <= 0) {
                    plugin.getMessages().send(player, "errors.invalid-format");
                    return true;
                }

                var shop = plugin.getShopManager().createShop(player.getUniqueId(), chestBlock, hand.clone(), amount, price);
                if (shop == null) {
                    plugin.getMessages().send(player, "errors.sign-blocked");
                    return true;
                }

                plugin.getMessages().send(player, "create.created");
                return true;
            }

            boolean chatFallback = plugin.getConfig().getBoolean("settings.create.chat-fallback-enabled", true);
            if (!chatFallback) {
                plugin.getMessages().send(player, "errors.invalid-format");
                return true;
            }

            int timeoutSec = plugin.getConfig().getInt("settings.create.chat-timeout-seconds", 30);
            long expires = System.currentTimeMillis() + timeoutSec * 1000L;

            CreateSession session = new CreateSession(chestBlock.getLocation());
            session.setTemplate(hand.clone());
            session.setExpiresAtMs(expires);

            UUID uid = player.getUniqueId();
            plugin.getSessionManager().set(uid, session);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                CreateSession s = plugin.getSessionManager().get(uid);
                if (s != null && System.currentTimeMillis() >= s.getExpiresAtMs()) {
                    plugin.getSessionManager().clear(uid);
                    plugin.getMessages().send(player, "create.timeout");
                }
            }, timeoutSec * 20L);

            plugin.getMessages().sendLines(player, "create.started", Map.of("seconds", String.valueOf(timeoutSec)));
            return true;
        }

        if (sub.equals("info")) {
            Block target = player.getTargetBlockExact(6);
            Shop shop = (target == null) ? null : plugin.getShopManager().getByAnyShopBlock(target);
            if (shop == null) {
                player.sendMessage("Look at a shop sign/chest.");
                return true;
            }
            plugin.getMessages().send(player, "info.header");
            plugin.getMessages().sendLines(player, "info.lines", plugin.getShopManager().infoPlaceholders(shop));
            return true;
        }

        if (sub.equals("remove")) {
            Block target = player.getTargetBlockExact(6);
            Shop shop = (target == null) ? null : plugin.getShopManager().getByAnyShopBlock(target);
            if (shop == null) {
                player.sendMessage("Look at a shop sign/chest.");
                return true;
            }

            boolean owner = shop.getOwner().equals(player.getUniqueId());
            boolean can = (owner && player.hasPermission("lpchestshop.remove.own")) || player.hasPermission("lpchestshop.remove.any");
            if (!can) {
                plugin.getMessages().send(player, "errors.no-permission");
                return true;
            }

            plugin.getShopManager().removeShop(shop);
            player.sendMessage("Shop removed.");
            return true;
        }

        plugin.getMessages().sendLines(player, "help.lines");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("create");
            out.add("info");
            out.add("remove");
            if (sender.hasPermission("lpchestshop.reload")) out.add("reload");
            out.add("help");
        }
        return out;
    }
}
