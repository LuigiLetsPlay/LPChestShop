package org.luigilp.lPChestShop.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.gui.GuiHolder;
import org.luigilp.lPChestShop.session.CreateSession;
import org.luigilp.lPChestShop.util.ItemUtils;

public final class GuiListener implements Listener {

    private final LPChestShop plugin;

    public GuiListener(LPChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!plugin.getConfig().getBoolean("settings.create.use-create-gui", false)) return;

        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof GuiHolder holder)) return;

        event.setCancelled(true);

        if (holder.getType() == GuiHolder.Type.CREATE) {
            handleCreateClick(player, event);
            return;
        }

    }

    private void handleCreateClick(Player player, InventoryClickEvent event) {
        var cfg = plugin.getConfig().getConfigurationSection("gui.create");
        if (cfg == null) return;

        int center = cfg.getInt("preview-slot", 13);
        int confirm = cfg.getInt("confirm.slot", 15);
        int cancel = cfg.getInt("cancel.slot", 11);

        int raw = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (raw == center) {
            event.setCancelled(false);
            return;
        }

        if (raw == confirm) {
            ItemStack item = top.getItem(center);
            if (ItemUtils.isAir(item)) {

                player.sendMessage("You must place an item in the middle slot.");
                return;
            }

            CreateSession session = plugin.getSessionManager().get(player.getUniqueId());
            if (session == null) return;

            session.setTemplate(item.clone());
            session.setStage(CreateSession.Stage.AWAITING_PRICE);

            player.closeInventory();

            plugin.getMessages().sendLines(player, "create.started", java.util.Map.of(
                    "seconds", String.valueOf(plugin.getConfig().getInt("settings.create.chat-timeout-seconds", 30))
            ));
        }

        if (raw == cancel) {
            plugin.getSessionManager().clear(player.getUniqueId());
            player.closeInventory();
            plugin.getMessages().send(player, "create.cancelled");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {

        if (!plugin.getConfig().getBoolean("settings.create.use-create-gui", false)) return;

        if (!(event.getPlayer() instanceof Player)) return;
        Inventory top = event.getInventory();
        if (!(top.getHolder() instanceof GuiHolder holder)) return;
        if (holder.getType() != GuiHolder.Type.CREATE) return;

        plugin.getSessionManager().clear(((Player) event.getPlayer()).getUniqueId());
    }
}
