package com.vytalcore.menu;

import com.vytalcore.VytalCore;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.Action;

public final class MenuListener implements Listener {
    private final VytalCore plugin;
    public MenuListener(VytalCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (holder.isEditor()) {
            if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            plugin.getMenuManager().moveItemInEditor(player, event.getSlot(), (event.getSlot() + 1) % event.getView().getTopInventory().getSize());
            plugin.getMenuManager().openEditor(player, holder.getMenuName());
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        String action = plugin.getMenuManager().action(holder.getMenuName(), event.getSlot());
        if (action != null && !action.isBlank()) execute(player, action);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) return;
                plugin.getMenuManager().close(player);
            });
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING);
        if (action != null) {
            event.setCancelled(true);
            execute(event.getPlayer(), action);
        }
    }

    public void execute(Player player, String raw) {
        String[] split = raw.split(":", 2);
        if (split.length < 2 && !raw.equalsIgnoreCase("close") && !raw.equalsIgnoreCase("toggle-players")) return;
        String type = split[0].toLowerCase();
        String value = split.length > 1 ? split[1] : "";
        switch (type) {
            case "open" -> plugin.getMenuManager().open(player, value);
            case "message" -> player.sendMessage(plugin.color(plugin.getConfig().getString("settings.prefix", "&8[&dvytalCore&8] &r") + value));
            case "player" -> {
                if (!value.isBlank()) player.performCommand(value.startsWith("/") ? value.substring(1) : value);
            }
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("%player_name%", player.getName()));
            case "server" -> {
                // Requires a proxy (e.g. Velocity) and matching server name in velocity.toml.
                player.sendMessage(plugin.color("&7Conectando a &d" + value + "&7..."));
                player.sendPluginMessage(plugin, "BungeeCord", ("Connect\0" + value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            case "sound" -> {
                try { player.playSound(player.getLocation(), Sound.valueOf(value.toUpperCase()), 1f, 1f); }
                catch (IllegalArgumentException ignored) {}
            }
            case "close" -> player.closeInventory();
            case "toggle-players" -> plugin.getHubManager().togglePlayers(player);
            default -> { }
        }
    }
            }
              
