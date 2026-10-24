package com.vytalcore.hub;

import com.vytalcore.VytalCore;
import com.vytalcore.menu.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public final class HubManager implements Listener {
    private final VytalCore plugin;
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final Map<UUID, Long> lastJump = new HashMap<>();
    private BukkitTask scoreboardTask;

    public HubManager(VytalCore plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("hub.scoreboard.enabled", false)) {
            scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateScoreboards, 20L, 40L);
        }
    }

    public void reload() {
        if (scoreboardTask != null) scoreboardTask.cancel();
        scoreboardTask = null;
        if (plugin.getConfig().getBoolean("hub.scoreboard.enabled", false)) {
            scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateScoreboards, 20L, 40L);
        }
    }

    public void shutdown() { if (scoreboardTask != null) scoreboardTask.cancel(); }

    public boolean isProtected(Player player) {
        if (!plugin.getConfig().getBoolean("hub.enabled", true)) return false;
        return plugin.getConfig().getStringList("hub.protected-worlds").contains(player.getWorld().getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("hub.enabled", true)) return;
        if (plugin.getConfig().getBoolean("hub.teleport-to-spawn-on-join", false)) teleportSpawn(player);
        if (plugin.getConfig().getBoolean("hub.clear-inventory-on-join", false) && isProtected(player)) player.getInventory().clear();
        giveJoinItems(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) updateScoreboard(player);
        }, 2L);
    }

    public void teleportSpawn(Player player) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("hub.spawn");
        if (s == null) return;
        World world = Bukkit.getWorld(s.getString("world", "world"));
        if (world == null) return;
        player.teleport(new Location(world, s.getDouble("x", .5), s.getDouble("y", 80),
                s.getDouble("z", .5), (float)s.getDouble("yaw", 0), (float)s.getDouble("pitch", 0)));
    }

    public void setSpawn(Player player) {
        Location l = player.getLocation();
        plugin.getConfig().set("hub.spawn.world", l.getWorld().getName());
        plugin.getConfig().set("hub.spawn.x", l.getX());
        plugin.getConfig().set("hub.spawn.y", l.getY());
        plugin.getConfig().set("hub.spawn.z", l.getZ());
        plugin.getConfig().set("hub.spawn.yaw", l.getYaw());
        plugin.getConfig().set("hub.spawn.pitch", l.getPitch());
        plugin.saveConfig();
    }

    private void giveJoinItems(Player player) {
        if (!plugin.getConfig().getBoolean("hub.join-items.enabled", true) || !isProtected(player)) return;
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("hub.join-items.items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection cfg = items.getConfigurationSection(key);
            if (cfg == null) continue;
            Material mat = Material.matchMaterial(cfg.getString("material", "PAPER"));
            if (mat == null || mat.isAir()) mat = Material.PAPER;
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.color(cfg.getString("name", "&fItem")));
                List<String> lore = new ArrayList<>();
                for (String line : cfg.getStringList("lore")) lore.add(plugin.color(line));
                if (!lore.isEmpty()) meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, cfg.getString("action", ""));
                stack.setItemMeta(meta);
            }
            int slot = cfg.getInt("slot", 0);
            if (slot >= 0 && slot < 9) player.getInventory().setItem(slot, stack);
        }
    }

    public void togglePlayers(Player player) {
        if (hiddenPlayers.remove(player.getUniqueId())) {
            for (Player target : Bukkit.getOnlinePlayers()) player.showPlayer(plugin, target);
            player.sendMessage(plugin.color("&aAhora ves a todos los jugadores."));
        } else {
            hiddenPlayers.add(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) if (!target.equals(player)) player.hidePlayer(plugin, target);
            player.sendMessage(plugin.color("&cAhora ocultas a los demás jugadores."));
        }
    }

    @EventHandler
    public void onJoinVisibility(PlayerJoinEvent event) {
        for (UUID id : hiddenPlayers) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer != null && !viewer.equals(event.getPlayer())) viewer.hidePlayer(plugin, event.getPlayer());
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (isProtected(e.getPlayer()) && plugin.getConfig().getBoolean("hub.disable-block-break", true) && !e.getPlayer().hasPermission("vytalcore.bypass")) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (isProtected(e.getPlayer()) && plugin.getConfig().getBoolean("hub.disable-block-place", true) && !e.getPlayer().hasPermission("vytalcore.bypass")) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (isProtected(e.getPlayer()) && plugin.getConfig().getBoolean("hub.disable-item-drop", true) && !e.getPlayer().hasPermission("vytalcore.bypass")) e.setCancelled(true); }
    @EventHandler public void onPickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && isProtected(p) && plugin.getConfig().getBoolean("hub.disable-item-pickup", false)) e.setCancelled(true); }
    @EventHandler public void onFood(FoodLevelChangeEvent e) { if (e.getEntity() instanceof Player p && isProtected(p) && plugin.getConfig().getBoolean("hub.disable-hunger", true)) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isProtected(p)) return;
        if (plugin.getConfig().getBoolean("hub.disable-entity-damage", true) ||
                (e.getCause() == EntityDamageEvent.DamageCause.FALL && plugin.getConfig().getBoolean("hub.disable-fall-damage", true))) e.setCancelled(true);
    }
    @EventHandler public void onPvp(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player target && e.getDamager() instanceof Player attacker &&
                (isProtected(target) || isProtected(attacker)) && plugin.getConfig().getBoolean("hub.disable-pvp", true)) e.setCancelled(true);
    }
    @EventHandler public void onMove(PlayerMoveEvent e) {
        if (!plugin.getConfig().getBoolean("hub.double-jump.enabled", true) || !isProtected(e.getPlayer())) return;
        if (e.getTo() == null || e.getFrom().getY() == e.getTo().getY()) return;
        Player p = e.getPlayer();
        if (p.isOnGround()) p.setAllowFlight(true);
    }
    @EventHandler public void onFlightToggle(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("hub.double-jump.enabled", true) || !isProtected(p) ||
                !plugin.getConfig().getStringList("hub.double-jump.worlds").contains(p.getWorld().getName())) return;
        if (p.hasPermission("vytalcore.bypass")) return;
        e.setCancelled(true);
        p.setAllowFlight(false);
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("hub.double-jump.cooldown-ms", 1000);
        if (now - lastJump.getOrDefault(p.getUniqueId(), 0L) < cooldown) return;
        lastJump.put(p.getUniqueId(), now);
        p.setVelocity(p.getLocation().getDirection().multiply(0.7).setY(plugin.getConfig().getDouble("hub.double-jump.velocity", .8)));
    }

    @EventHandler public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (isProtected(p) && plugin.getConfig().getBoolean("hub.join-items.enabled", true)) giveJoinItems(p);
        if (!isProtected(p)) p.setAllowFlight(false);
    }

    @EventHandler public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (isProtected(p)) {
            ConfigurationSection s = plugin.getConfig().getConfigurationSection("hub.spawn");
            if (s != null && Bukkit.getWorld(s.getString("world", "world")) != null) {
                World w = Bukkit.getWorld(s.getString("world", "world"));
                e.setRespawnLocation(new Location(w, s.getDouble("x", .5), s.getDouble("y", 80), s.getDouble("z", .5)));
            }
        }
    }

    private void updateScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) updateScoreboard(player);
    }

    private void updateScoreboard(Player player) {
        if (!isProtected(player) || !plugin.getConfig().getBoolean("hub.scoreboard.enabled", false)) return;
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        org.bukkit.scoreboard.Objective objective = board.registerNewObjective("vytalcore", "dummy",
                plugin.color(plugin.getConfig().getString("hub.scoreboard.title", "&dvytalCore")));
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        List<String> lines = plugin.getConfig().getStringList("hub.scoreboard.lines");
        int score = lines.size();
        Set<String> used = new HashSet<>();
        for (String raw : lines) {
            String line = raw.replace("%player_name%", player.getName())
                    .replace("%world%", player.getWorld().getName())
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            // Expand PlaceholderAPI if installed, without a hard dependency.
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                try {
                    Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    line = (String) papi.getMethod("setPlaceholders", Player.class, String.class)
                            .invoke(null, player, line);
                } catch (ReflectiveOperationException ignored) {}
            }
            line = plugin.color(line);
            String unique = line;
            while (!used.add(unique)) unique = line + org.bukkit.ChatColor.values()[score % org.bukkit.ChatColor.values().length];
            objective.getScore(unique).setScore(score--);
        }
        player.setScoreboard(board);
    }
              }
      
