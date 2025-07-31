package com.bsruEnderchest.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

public class LuckPermsHook {

    private final LuckPerms luckPerms;
    private final Logger logger;

    public LuckPermsHook(Logger logger) {
        this.logger = logger;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            throw new IllegalStateException("LuckPerms API not found. This should not happen.");
        }
    }

    public int getMaxPagesOffline(OfflinePlayer player) {
        try {
            // โหลดข้อมูล user จาก LuckPerms (อาจจะช้าถ้าฐานข้อมูลช้า)
            // .join() คือการรอให้โหลดเสร็จ ซึ่งจะทำให้โค้ดทำงานแบบ Sync
            User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();

            if (user == null || !user.getCachedData().getPermissionData().checkPermission("bsruenderchest.use").asBoolean()) {
                return 0;
            }
            if (user.getCachedData().getPermissionData().checkPermission("op").asBoolean()) {
                return 10;
            }
            for (int i = 10; i >= 2; i--) {
                if (user.getCachedData().getPermissionData().checkPermission("bsruenderchest.plus." + i).asBoolean()) {
                    return i;
                }
            }
            return 1;
        } catch (Exception e) {
            logger.warning("Could not load offline player permissions from LuckPerms for " + player.getName() + ". Falling back to data check.");
            // ถ้าเกิด Error ให้คืนค่า -1 เพื่อให้โค้ดหลักไปใช้วิธี Fallback
            return -1;
        }
    }
}