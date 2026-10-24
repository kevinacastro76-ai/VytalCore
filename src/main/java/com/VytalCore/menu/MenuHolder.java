package com.vytalcore.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {
    private final String menuName;
    private final boolean editor;

    public MenuHolder(String menuName, boolean editor) {
        this.menuName = menuName;
        this.editor = editor;
    }

    public String getMenuName() { return menuName; }
    public boolean isEditor() { return editor; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
