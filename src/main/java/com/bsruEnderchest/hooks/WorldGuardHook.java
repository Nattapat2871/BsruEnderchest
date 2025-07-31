package com.bsruEnderchest.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    public WorldGuardHook() {
        // Constructor, can be left empty
    }

    public boolean canDropItems(Player player) {
        try {
            Location loc = player.getLocation();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            // ใช้ WorldGuardPlugin สำหรับแปลง Player เป็น LocalPlayer
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            return query.testState(BukkitAdapter.adapt(loc), localPlayer, Flags.ITEM_DROP);
        } catch (Exception e) {
            e.printStackTrace();
            // ในกรณีที่เกิดข้อผิดพลาดกับ API ของ WorldGuard ให้ถือว่าปลอดภัยไว้ก่อน (อนุญาตให้ทิ้งได้)
            return true;
        }
    }
}
