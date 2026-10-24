package com.vytalcore;

import com.vytalcore.command.CoreCommand;
import com.vytalcore.hub.HubManager;
import com.vytalcore.menu.MenuManager;
import com.vytalcore.menu.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VytalCore extends JavaPlugin {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private MenuManager menuManager;
    private HubManager hubManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("menus/principal.yml");
        saveResourceIfMissing("menus/modalidades.yml");

        this.menuManager = new MenuManager(this);
        this.hubManager = new HubManager(this);
        menuManager.loadMenus();

        PluginCommand command = getCommand("vytalcore");
        if (command != null) {
            CoreCommand executor = new CoreCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(hubManager, this);
        getLogger().info("vytalCore habilitado.");
    }

    @Override
    public void onDisable() {
        if (hubManager != null) hubManager.shutdown();
        getLogger().info("vytalCore deshabilitado.");
    }

    private void saveResourceIfMissing(String path) {
        if (!getDataFolder().toPath().resolve(path).toFile().exists()) {
            saveResource(path, false);
        }
    }

    public void reloadCore() {
        reloadConfig();
        menuManager.loadMenus();
        hubManager.reload();
    }

    public MenuManager getMenuManager() { return menuManager; }
    public HubManager getHubManager() { return hubManager; }

    public String color(String input) {
        if (input == null) return "";
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String message(String key) {
        var messages = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(getDataFolder(), "messages.yml"));
        String prefix = color(messages.getString("prefix", "&8[&dvytalCore&8] &r"));
        return prefix + color(messages.getString(key, "&cMensaje no configurado: " + key));
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String key) {
        sender.sendMessage(message(key));
    }
          }
