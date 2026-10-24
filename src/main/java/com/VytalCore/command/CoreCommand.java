package com.vytalcore.command;

import com.vytalcore.VytalCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CoreCommand implements CommandExecutor, TabCompleter {
    private final VytalCore plugin;
    public CoreCommand(VytalCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            for (String line : org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.File(plugin.getDataFolder(), "messages.yml")).getStringList("help")) sender.sendMessage(plugin.color(line));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vytalcore.admin")) { plugin.sendMessage(sender, "no-permission"); return true; }
            plugin.reloadCore();
            plugin.sendMessage(sender, "reloaded");
            return true;
        }
        if (args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) { plugin.sendMessage(sender, "player-only"); return true; }
            if (!player.hasPermission("vytalcore.menu")) { plugin.sendMessage(sender, "no-permission"); return true; }
            if (args.length < 2) { sender.sendMessage(plugin.color("&cUso: /vc menu <nombre>")); return true; }
            plugin.getMenuManager().open(player, args[1]);
            return true;
        }
        if (args[0].equalsIgnoreCase("editor")) {
            if (!(sender instanceof Player player)) { plugin.sendMessage(sender, "player-only"); return true; }
            if (!player.hasPermission("vytalcore.admin")) { plugin.sendMessage(sender, "no-permission"); return true; }
            if (args.length < 2) { sender.sendMessage(plugin.color("&cUso: /vc editor <nombre>")); return true; }
            plugin.getMenuManager().openEditor(player, args[1]);
            return true;
        }
        if (args[0].equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player player)) { plugin.sendMessage(sender, "player-only"); return true; }
            if (!player.hasPermission("vytalcore.admin")) { plugin.sendMessage(sender, "no-permission"); return true; }
            plugin.getHubManager().setSpawn(player);
            plugin.sendMessage(sender, "spawn-set");
            return true;
        }
        sender.sendMessage(plugin.color("&cSubcomando desconocido. Usa /vc help"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("help", "reload", "menu", "editor", "setspawn")) if (s.startsWith(args[0].toLowerCase())) suggestions.add(s);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("editor"))) {
            // Menu names are intentionally simple; the defaults are included here.
            for (String s : List.of("principal", "modalidades")) if (s.startsWith(args[1].toLowerCase())) suggestions.add(s);
        }
        return suggestions;
    }
          }
                  
