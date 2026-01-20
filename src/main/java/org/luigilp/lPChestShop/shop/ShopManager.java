package org.luigilp.lPChestShop.shop;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.WallSign;
import org.luigilp.lPChestShop.LPChestShop;
import org.luigilp.lPChestShop.model.Shop;
import org.luigilp.lPChestShop.util.ItemUtils;
import org.luigilp.lPChestShop.util.Text;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ShopManager {

    private final LPChestShop plugin;

    private final Map<String, Shop> shopsById = new HashMap<>();
    private final Map<String, String> chestKeyToId = new HashMap<>();
    private final Map<String, String> signKeyToId = new HashMap<>();

    private File file;
    private YamlConfiguration yml;

    public ShopManager(LPChestShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public void load() {
        shopsById.clear();
        chestKeyToId.clear();
        signKeyToId.clear();

        this.yml = YamlConfiguration.loadConfiguration(file);
        var root = yml.getConfigurationSection("shops");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            var sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            UUID owner = UUID.fromString(sec.getString("owner", ""));
            Location chest = readLocation(sec.getConfigurationSection("chest"));
            Location sign = readLocation(sec.getConfigurationSection("sign"));
            ItemStack item = sec.getItemStack("item");
            int amount = sec.getInt("amount", 1);
            long price = sec.getLong("price", 0);

            if (owner == null || chest == null || sign == null || item == null) continue;

            Shop shop = new Shop(id, owner, chest, sign, item, amount, price);
            shopsById.put(id, shop);
            chestKeyToId.put(key(chest), id);
            signKeyToId.put(key(sign), id);
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        var root = out.createSection("shops");

        for (Shop shop : shopsById.values()) {
            var sec = root.createSection(shop.getId());
            sec.set("owner", shop.getOwner().toString());

            writeLocation(sec.createSection("chest"), shop.getChest());
            writeLocation(sec.createSection("sign"), shop.getSign());

            sec.set("item", shop.getItemTemplate());
            sec.set("amount", shop.getAmount());
            sec.set("price", shop.getPrice());
        }

        try {
            out.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shops.yml: " + e.getMessage());
        }
        this.yml = out;
    }

    public int countOwnedBy(UUID owner) {
        int c = 0;
        for (Shop s : shopsById.values()) if (s.getOwner().equals(owner)) c++;
        return c;
    }

    public boolean isChestBlock(Block b) {
        if (b == null) return false;
        return b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST;
    }

    public boolean isSignBlock(Block b) {
        if (b == null) return false;
        return Tag.WALL_SIGNS.isTagged(b.getType());
    }

    public Shop getByChest(Block chest) {
        if (chest == null) return null;
        String id = chestKeyToId.get(key(chest.getLocation()));
        return id == null ? null : shopsById.get(id);
    }

    public Shop getBySign(Block sign) {
        if (sign == null) return null;
        String id = signKeyToId.get(key(sign.getLocation()));
        return id == null ? null : shopsById.get(id);
    }

    public Shop getByAnyShopBlock(Block b) {
        Shop s = getBySign(b);
        if (s != null) return s;
        return getByChest(b);
    }

    public boolean isChestEmpty(Block chestBlock) {
        var state = chestBlock.getState();
        if (!(state instanceof org.bukkit.block.Chest chest)) return true;
        Inventory inv = chest.getBlockInventory();
        for (ItemStack it : inv.getContents()) {
            if (!ItemUtils.isAir(it)) return false;
        }
        return true;
    }

    public Map<String, String> infoPlaceholders(Shop shop) {
        int stock = getStock(shop);
        OfflinePlayer op = Bukkit.getOfflinePlayer(shop.getOwner());

        Map<String, String> ph = new HashMap<>();
        ph.put("owner", op.getName() != null ? op.getName() : shop.getOwner().toString());
        ph.put("item", Text.prettyItemName(shop.getItemTemplate()));
        ph.put("amount", String.valueOf(shop.getAmount()));
        ph.put("price", Text.formatMoney(plugin.getConfig(), shop.getPrice()));
        ph.put("stock", String.valueOf(stock));
        return ph;
    }

    public Shop createShop(UUID owner, Block chestBlock, ItemStack template, int amount, long price) {
        if (!isChestBlock(chestBlock)) return null;

        BlockFace facing = BlockFace.NORTH;
        if (chestBlock.getBlockData() instanceof Chest chestData) {
            facing = chestData.getFacing();
        }

        Block signBlock = chestBlock.getRelative(facing);
        if (!signBlock.getType().isAir()) {
            return null;
        }

        Material signMat = Material.OAK_WALL_SIGN;
        signBlock.setType(signMat);

        if (!(signBlock.getBlockData() instanceof WallSign ws)) {
            return null;
        }
        ws.setFacing(facing);
        signBlock.setBlockData(ws, false);

        Location chestLoc = chestBlock.getLocation();
        Location signLoc = signBlock.getLocation();

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Shop shop = new Shop(id, owner, chestLoc, signLoc, template.clone(), amount, price);
        shopsById.put(id, shop);
        chestKeyToId.put(key(chestLoc), id);
        signKeyToId.put(key(signLoc), id);

        refreshSign(shop);
        save();
        return shop;
    }

    public void removeShop(Shop shop) {
        if (shop == null) return;

        Block signBlock = shop.getSign().getBlock();
        if (isSignBlock(signBlock)) {
            signBlock.setType(Material.AIR);
        }

        shopsById.remove(shop.getId());
        chestKeyToId.remove(key(shop.getChest()));
        signKeyToId.remove(key(shop.getSign()));
        save();
    }

    public int getStock(Shop shop) {
        Inventory inv = getShopInventory(shop);
        if (inv == null) return 0;

        int count = 0;
        for (ItemStack it : inv.getContents()) {
            if (ItemUtils.isAir(it)) continue;
            if (shop.getItemTemplate().isSimilar(it)) count += it.getAmount();
        }
        return count;
    }

    public List<ItemStack> takeFromStock(Shop shop, int amount) {
        Inventory inv = getShopInventory(shop);
        if (inv == null) return null;

        int need = amount;
        List<ItemStack> taken = new ArrayList<>();

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && need > 0; i++) {
            ItemStack it = contents[i];
            if (ItemUtils.isAir(it)) continue;
            if (!shop.getItemTemplate().isSimilar(it)) continue;

            int take = Math.min(need, it.getAmount());
            ItemStack removed = it.clone();
            removed.setAmount(take);
            taken.add(removed);

            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) contents[i] = null;

            need -= take;
        }

        inv.setContents(contents);

        if (need > 0) {

            putBackToStock(shop, taken);
            return null;
        }
        return taken;
    }

    public void putBackToStock(Shop shop, List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return;
        Inventory inv = getShopInventory(shop);
        if (inv == null) return;
        for (ItemStack st : stacks) {
            inv.addItem(st);
        }
    }

    public void refreshAllSigns() {
        for (Shop shop : shopsById.values()) refreshSign(shop);
    }

    public void refreshSign(Shop shop) {
        Block signBlock = shop.getSign().getBlock();
        if (!isSignBlock(signBlock)) return;

        var state = signBlock.getState();
        if (!(state instanceof Sign sign)) return;

        boolean showStock = plugin.getConfig().getBoolean("settings.stock.show-on-sign", true);
        int stock = getStock(shop);

        String stockText = showStock ? String.valueOf(stock) : "-";
        if (showStock && stock <= 0) stockText = plugin.getConfig().getString("settings.stock.out-of-stock-text", "Out");

        List<String> lines = plugin.getConfig().getStringList("sign.lines");
        while (lines.size() < 4) lines.add("");

        for (int i = 0; i < 4; i++) {
            String raw = lines.get(i);
            raw = raw.replace("{item}", Text.prettyItemName(shop.getItemTemplate()))
                    .replace("{amount}", String.valueOf(shop.getAmount()))
                    .replace("{price}", Text.formatMoney(plugin.getConfig(), shop.getPrice()))
                    .replace("{stock}", stockText);

            sign.getSide(Side.FRONT).line(i, Component.text(Text.stripColor(Text.color(raw))));
        }

        sign.update(true, false);
    }

    private Inventory getShopInventory(Shop shop) {
        Block chestBlock = shop.getChest().getBlock();
        if (!isChestBlock(chestBlock)) return null;

        var state = chestBlock.getState();
        if (!(state instanceof org.bukkit.block.Chest chest)) return null;
        return chest.getInventory();
    }

    private String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private Location readLocation(org.bukkit.configuration.ConfigurationSection sec) {
        if (sec == null) return null;
        World w = Bukkit.getWorld(sec.getString("world", ""));
        if (w == null) return null;
        int x = sec.getInt("x");
        int y = sec.getInt("y");
        int z = sec.getInt("z");
        return new Location(w, x, y, z);
    }

    private void writeLocation(org.bukkit.configuration.ConfigurationSection sec, Location loc) {
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getBlockX());
        sec.set("y", loc.getBlockY());
        sec.set("z", loc.getBlockZ());
    }
}
