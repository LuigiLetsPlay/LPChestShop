package org.luigilp.ezChestShop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.luigilp.ezChestShop.command.EzChestShopCommand;
import org.luigilp.ezChestShop.gui.GuiFactory;
import org.luigilp.ezChestShop.listeners.*;
import org.luigilp.ezChestShop.model.Shop;
import org.luigilp.ezChestShop.session.SessionManager;
import org.luigilp.ezChestShop.shop.ShopManager;
import org.luigilp.ezChestShop.util.GitHubReleaseUpdateChecker;
import org.luigilp.ezChestShop.util.Messages;
import org.luigilp.ezChestShop.util.Text;

import java.util.List;
import java.util.Map;

public final class EzChestShop extends JavaPlugin {

    private Messages messages;
    private SessionManager sessionManager;
    private ShopManager shopManager;
    private GuiFactory guiFactory;

    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("shops.yml", false);

        this.messages = new Messages(this);
        this.sessionManager = new SessionManager();
        this.shopManager = new ShopManager(this);
        this.guiFactory = new GuiFactory(this);

        hookVault();

        shopManager.load();
        shopManager.refreshAllSigns();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new InteractListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new ChestDepositListener(this), this);
        pm.registerEvents(new ChestStockUpdateListener(this), this);

        var cmd = getCommand("ezchestshop");
        if (cmd != null) {
            var executor = new EzChestShopCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        GitHubReleaseUpdateChecker.check(this, "LuigiLetsPlay", "EzChestShop");

        getLogger().info("EzChestShop enabled.");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) shopManager.save();
        getLogger().info("EzChestShop disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        shopManager.load();
        shopManager.refreshAllSigns();
        hookVault();
    }

    private void hookVault() {
        boolean require = getConfig().getBoolean("settings.economy.require-vault", true);

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            if (require) getLogger().warning("Vault not found. Buying will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            economy = null;
            if (require) getLogger().warning("No Economy provider found. Install an economy plugin.");
            return;
        }

        economy = rsp.getProvider();
        getLogger().info("Hooked economy provider: " + economy.getName());
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public Messages getMessages() {
        return messages;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public void openDetails(Player player, Shop shop) {
        player.openInventory(guiFactory.createDetailsGui(shop));
        messages.send(player, "details.opened");
    }

    public void buy(Player buyer, Shop shop) {

        boolean allowOwnerBuy = getConfig().getBoolean("settings.economy.allow-owner-buy", false);
        if (!allowOwnerBuy && buyer.getUniqueId().equals(shop.getOwner())) {
            messages.send(buyer, "errors.cannot-buy-own");
            return;
        }

        boolean require = getConfig().getBoolean("settings.economy.require-vault", true);
        if (require && !hasEconomy()) {
            messages.send(buyer, "errors.economy-missing");
            return;
        }

        int stock = shopManager.getStock(shop);
        if (stock < shop.getAmount()) {
            messages.send(buyer, "errors.out-of-stock");
            shopManager.refreshSign(shop);
            return;
        }

        long price = shop.getPrice();

        if (hasEconomy()) {
            double bal = economy.getBalance(buyer);
            if (bal + 0.0001 < price) {
                messages.send(buyer, "errors.not-enough-money");
                return;
            }

            var withdraw = economy.withdrawPlayer(buyer, (double) price);
            if (!withdraw.transactionSuccess()) {
                messages.send(buyer, "errors.buy-failed");
                return;
            }

            List<ItemStack> taken = shopManager.takeFromStock(shop, shop.getAmount());
            if (taken == null) {
                economy.depositPlayer(buyer, (double) price);
                messages.send(buyer, "errors.buy-failed");
                shopManager.refreshSign(shop);
                return;
            }

            ItemStack toGive = shop.getItemTemplate().clone();
            toGive.setAmount(shop.getAmount());
            Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(toGive);
            if (!overflow.isEmpty()) {
                economy.depositPlayer(buyer, (double) price);
                shopManager.putBackToStock(shop, taken);
                messages.send(buyer, "errors.buy-failed");
                return;
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwner());
            var deposit = economy.depositPlayer(owner, (double) price);
            if (!deposit.transactionSuccess()) {
                economy.depositPlayer(buyer, (double) price);
                shopManager.putBackToStock(shop, taken);
                buyer.getInventory().removeItem(toGive);
                messages.send(buyer, "errors.buy-failed");
                return;
            }

            messages.send(buyer, "buy.success", Map.of(
                    "amount", String.valueOf(shop.getAmount()),
                    "item", Text.prettyItemName(shop.getItemTemplate()),
                    "price", Text.formatMoney(getConfig(), price)
            ));
            shopManager.refreshSign(shop);
            return;
        }

        List<ItemStack> taken = shopManager.takeFromStock(shop, shop.getAmount());
        if (taken == null) {
            messages.send(buyer, "errors.buy-failedd");
            return;
        }
        ItemStack toGive = shop.getItemTemplate().clone();
        toGive.setAmount(shop.getAmount());
        buyer.getInventory().addItem(toGive);

        messages.send(buyer, "buy.success", Map.of(
                "amount", String.valueOf(shop.getAmount()),
                "item", Text.prettyItemName(shop.getItemTemplate()),
                "price", Text.formatMoney(getConfig(), price)
        ));
        shopManager.refreshSign(shop);
    }
}
