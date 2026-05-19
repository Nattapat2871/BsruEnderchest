package com.bsruEnderchest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EnderchestHolder implements InventoryHolder {
    private final UUID ownerUUID;
    private final int page;
    private final boolean isAdminView;

    public EnderchestHolder(UUID ownerUUID, int page, boolean isAdminView) {
        this.ownerUUID = ownerUUID;
        this.page = page;
        this.isAdminView = isAdminView;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getPage() {
        return page;
    }

    public boolean isAdminView() {
        return isAdminView;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null; // Will be set or handled elsewhere
    }
}
