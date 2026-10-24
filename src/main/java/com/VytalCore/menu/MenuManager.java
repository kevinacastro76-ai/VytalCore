package com.vytalcore.menu;

import com.vytalcore.VytalCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MenuManager {
    private final VytalCore plugin;
    private final Map<String, File> menuFiles = new HashMap<>();
    private final Map<String, YamlConfiguration> menus = new HashMap<>();
    private final Map<UUID, String> openMenu = new HashMap<>();
    private final Map<UUID, String> editingMenu = new HashMap<>();

    public MenuManager(VytalCore plugin) { this.plugin = plugin; }

    public void loadMenus() {
        menuFiles.clear();
        menus.clear();
        File folder = new File(plugin.getDataFolder(), plugin.getConfig().getString("menus.folder", "menus"));
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            menuFiles.put(name, file);
            menus.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    public boolean exists(String name) { return menus.containsKey(name.toLowerCase(Locale.ROOT)); }
    public String getOpenMenu(Player player) { return openMenu.get(player.getUniqueId()); }
    public String getEditingMenu(Player player) { return editingMenu.get(player.getUniqueId()); }
    public boolean isEditor(Player player) { return editingMenu.containsKey(player.getUniqueId()); }

    public void open(Player player, String name) {
        name = name.toLowerCase(Locale.ROOT);
        YamlConfiguration cfg = menus.get(name);
        if (cfg == null) {
            player.sendMessage(plugin.message("unknown-menu").replace("%menu%", name));
            return;
        }
        int size = cfg.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) size = 27;
        Inventory inventory = Bukkit.createInventory(new MenuHolder(name, false), size,
                plugin.color(cfg.getString("title", "&8Menu")));
        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                ConfigurationSection itemCfg = items.getConfigurationSection(id);
                if (itemCfg == null) continue;
                int slot = itemCfg.getInt("slot", -1);
                if (slot < 0 || slot >= size) continue;
                ItemStack stack = createItem(itemCfg, player);
                inventory.setItem(slot, stack);
            }
        }
        openMenu.put(player.getUniqueId(), name);
        player.openInventory(inventory);
    }

    public void openEditor(Player player, String name) {
        name = name.toLowerCase(Locale.ROOT);
        YamlConfiguration cfg = menus.get(name);
        if (cfg == null) {
            player.sendMessage(plugin.message("unknown-menu").replace("%menu%", name));
            return;
        }
        int size = cfg.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) size = 27;
        Inventory inv = Bukkit.createInventory(new MenuHolder(name, true), size,
                plugin.color("&5Editor: &d" + name));
        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                ConfigurationSection itemCfg = items.getConfigurationSection(id);
                if (itemCfg == null) continue;
                int slot = itemCfg.getInt("slot", -1);
                if (slot >= 0 && slot < size) {
                    ItemStack stack = createItem(itemCfg, player);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add(plugin.color("&8ID: " + id));
                        lore.add(plugin.color("&eClic: mover al siguiente slot"));
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                    }
                    inv.setItem(slot, stack);
                }
            }
        }
        editingMenu.put(player.getUniqueId(), name);
        player.openInventory(inv);
    }

    public ItemStack createItem(ConfigurationSection cfg, Player player) {
        Material material = Material.matchMaterial(cfg.getString("material", "PAPER"));
        if (material == null || material.isAir()) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(replace(cfg.getString("name", "&fItem"), player)));
            List<String> lore = new ArrayList<>();
            for (String line : cfg.getStringList("lore")) lore.add(plugin.color(replace(line, player)));
            if (!lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String replace(String input, Player player) {
        if (input == null) return "";
        return input.replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName());
    }

    public String action(String menuName, int slot) {
        YamlConfiguration cfg = menus.get(menuName);
        if (cfg == null) return null;
        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) return null;
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(id);
            if (item != null && item.getInt("slot", -1) == slot) return item.getString("action", "");
        }
        return null;
    }

    public void moveItemInEditor(Player player, int clickedSlot, int newSlot) {
        String name = editingMenu.get(player.getUniqueId());
        if (name == null) return;
        YamlConfiguration cfg = menus.get(name);
        if (cfg == null) return;
        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) return;
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(id);
            if (item != null && item.getInt("slot", -1) == clickedSlot) {
                item.set("slot", newSlot);
                try {
                    cfg.save(menuFiles.get(name));
                    loadMenus();
                    player.sendMessage(plugin.message("editor-saved").replace("%menu%", name));
                } catch (IOException ex) {
                    plugin.getLogger().severe("No se pudo guardar el menú " + name + ": " + ex.getMessage());
                    player.sendMessage(plugin.color("&cNo se pudo guardar el archivo YAML."));
                }
                return;
            }
        }
    }

    public void close(Player player) {
        openMenu.remove(player.getUniqueId());
        editingMenu.remove(player.getUniqueId());
    }
          }
