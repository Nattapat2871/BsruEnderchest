package com.bsruEnderchest;

import com.bsruEnderchest.hooks.LuckPermsHook;
import com.bsruEnderchest.hooks.WorldGuardHook;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class BsruEnderchest extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static boolean IS_FOLIA;

    private final Map<UUID, Integer> playerCurrentPage = new ConcurrentHashMap<>();
    private final Map<UUID, Location> openedPhysicalChests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> adminViewingMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> navigationLock = new ConcurrentHashMap<>();

    private LuckPermsHook luckPermsHook;
    private WorldGuardHook worldGuardHook;

    private boolean useDatabase;
    private DatabaseManager databaseManager;

    private String titleFormat, baseTitleCheck, singlePageTitle, noPermsMsg, adminTitleFormat, adminBaseTitleCheck;
    private String playerDbErrorMessage, adminDbErrorMessage, adminSinglePageTitle, wgDropDenyMessage;
    private Sound soundNavigate, soundFail, soundWgDeny;
    private float navigateVolume, navigatePitch, failVolume, failPitch, wgDenyVolume, wgDenyPitch;
    private ItemStack prevPageEnabled, prevPageDisabled, nextPageEnabled, nextPageDisabled, fillerItem;

    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            IS_FOLIA = true;
        } catch (ClassNotFoundException e) {
            IS_FOLIA = false;
        }

        saveDefaultConfig();
        loadConfigValues();
        setupDatabaseConnection();

        String luckPermsStatus;
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            this.luckPermsHook = new LuckPermsHook(getLogger());
            luckPermsStatus = ChatColor.GREEN + "Enabled";
        } else {
            luckPermsStatus = ChatColor.GRAY + "Disabled (LuckPerms not found)";
        }

        String worldGuardStatus;
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuardHook = new WorldGuardHook();
            worldGuardStatus = ChatColor.GREEN + "Enabled";
        } else {
            worldGuardStatus = ChatColor.GRAY + "Disabled (WorldGuard not found)";
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("enderchest").setExecutor(this);
        getCommand("bsruenderchest").setExecutor(this);
        getCommand("bsruenderchest").setTabCompleter(this);

        String serverType = IS_FOLIA ? "Folia" : "Paper";
        String serverVersion = Bukkit.getBukkitVersion().split("-")[0];
        CommandSender console = Bukkit.getConsoleSender();

        console.sendMessage(ChatColor.YELLOW + "------------------------------------------");
        console.sendMessage(ChatColor.GOLD + " BsruEnderchest v" + this.getDescription().getVersion() + ChatColor.WHITE + " by " + ChatColor.LIGHT_PURPLE + "Nattapat2871");
        console.sendMessage(ChatColor.BLUE + " LuckPerms Hook: " + luckPermsStatus);
        console.sendMessage(ChatColor.AQUA + " WorldGuard Hook: " + worldGuardStatus);
        console.sendMessage(ChatColor.WHITE + " Successfully enabled on " + ChatColor.YELLOW + serverType + " " + serverVersion);
        console.sendMessage(ChatColor.GRAY + " GitHub " + ChatColor.DARK_BLUE + "https://github.com/Nattapat2871/BsruEnderchest");
        console.sendMessage(ChatColor.YELLOW + "------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("BsruEnderchest has been disabled.");
    }

    private void reloadPluginConfig() {
        boolean wasUsingDatabase = this.useDatabase;
        reloadConfig();
        loadConfigValues();
        if (this.useDatabase != wasUsingDatabase) {
            getLogger().info("Database usage setting changed. Re-initializing connection...");
            if (databaseManager != null) {
                databaseManager.close();
                databaseManager = null;
            }
            setupDatabaseConnection();
        }
    }

    private void setupDatabaseConnection() {
        if (useDatabase) {
            if (databaseManager != null) {
                databaseManager.close();
            }
            try {
                FileConfiguration config = getConfig();
                databaseManager = new DatabaseManager(
                        config.getString("database.host"), config.getInt("database.port"),
                        config.getString("database.database"), config.getString("database.username"),
                        config.getString("database.password"), getLogger()
                );
            } catch (Exception e) {
                getLogger().severe("!!! FAILED TO INITIALIZE DATABASE CONNECTION POOL !!!");
                useDatabase = false;
            }
        } else {
            getLogger().info("Database is disabled. Using file-based storage.");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("bsruenderchest")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GRAY + "----------------------------------");
                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "BsruEnderchest " + ChatColor.YELLOW + "v" + this.getDescription().getVersion());
                sender.sendMessage(ChatColor.WHITE + "ผู้สร้าง: " + ChatColor.AQUA + "Nattapat2871");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GRAY + "ปลั๊กอิน Ender Chest หลายหน้าพร้อมระบบฐานข้อมูล");
                sender.sendMessage(ChatColor.DARK_AQUA + "GitHub: " + ChatColor.GRAY + "https://github.com/Nattapat2871/BsruEnderchest");
                sender.sendMessage(ChatColor.GRAY + "----------------------------------");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("bsruenderchest.admin.reload")) {
                    sender.sendMessage(noPermsMsg);
                    return true;
                }
                reloadPluginConfig();
                sender.sendMessage(ChatColor.GREEN + "[BsruEnderchest] Configuration reloaded.");
                return true;
            }
            if (args[0].equalsIgnoreCase("chestsee")) {
                if (!(sender instanceof Player admin)) {
                    sender.sendMessage("This command can only be used by a player.");
                    return true;
                }
                if (!admin.hasPermission("bsruenderchest.admin.chestsee")) {
                    admin.sendMessage(noPermsMsg);
                    return true;
                }
                if (args.length < 2) {
                    admin.sendMessage(ChatColor.RED + "Usage: /bsruenderchest chestsee <player>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    if (!hasData(target.getUniqueId())) {
                        admin.sendMessage(ChatColor.RED + "No Ender Chest data found for player '" + args[1] + "'.");
                        return true;
                    }
                } catch (SQLException e) {
                    handleDatabaseError(e, admin);
                    return true;
                }
                admin.playSound(admin.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.0f);
                adminViewingMap.put(admin.getUniqueId(), target.getUniqueId());
                openInventoryForPlayer(admin);
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("enderchest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is for players only.");
                return true;
            }
            if (!player.hasPermission("bsruenderchest.command.use")) {
                player.sendMessage(noPermsMsg);
                return true;
            }
            if (getMaxPages(player) > 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.0f);
                openInventoryForPlayer(player);
            } else {
                player.sendMessage(noPermsMsg);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("bsruenderchest")) {
            if (args.length == 1) {
                return Arrays.asList("reload", "chestsee").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("chestsee")) {
                List<String> playerNames = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
                return playerNames.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private void openInventoryForPlayer(Player player) {
        UUID targetUUID = adminViewingMap.getOrDefault(player.getUniqueId(), player.getUniqueId());
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
        runTaskAsync(() -> {
            try {
                if (useDatabase && (databaseManager == null || !databaseManager.isConnected())) {
                    throw new IllegalStateException("Database is not connected.");
                }
                int maxPages = getMaxPages(targetPlayer);
                if (maxPages == 0) {
                    runTaskOnCorrectThread(player, () -> {
                        if (adminViewingMap.containsKey(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "No Ender Chest data found for player '" + targetPlayer.getName() + "'.");
                        } else {
                            player.openInventory(player.getEnderChest());
                        }
                    });
                    return;
                }
                if (maxPages == 1) {
                    ItemStack[] items = loadSinglePageData(targetUUID);
                    runTaskOnCorrectThread(player, () -> {
                        String title;
                        if (adminViewingMap.containsKey(player.getUniqueId())) {
                            title = adminSinglePageTitle.replace("{player_name}", Objects.toString(targetPlayer.getName(), "Unknown"));
                        } else {
                            title = singlePageTitle;
                        }
                        Inventory inv = Bukkit.createInventory(null, 54, title);
                        if (items != null) inv.setContents(items);
                        player.openInventory(inv);
                    });
                } else {
                    if (!useDatabase) handleDataMigration(targetUUID);
                    ItemStack[] items = loadPagedData(targetUUID, 1);
                    runTaskOnCorrectThread(player, () -> openPagedEnderChest(player, targetPlayer, 1, items));
                }
            } catch (Exception e) {
                handleDatabaseError(e, player);
            }
        });
    }

    private void handleDatabaseError(Exception e, Player player) {
        getLogger().severe("A database error occurred for player " + (player != null ? player.getName() : "UNKNOWN"));
        e.printStackTrace();
        runTaskOnCorrectThread(player, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage(playerDbErrorMessage);
                player.playSound(player.getLocation(), soundFail, failVolume, failPitch);
            }
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("bsruenderchest.admin.notify")) {
                    admin.sendMessage(adminDbErrorMessage);
                }
            }
        });
    }

    private void runTaskOnCorrectThread(Player player, Runnable task) {
        if (IS_FOLIA) {
            player.getScheduler().run(this, scheduledTask -> task.run(), null);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTask(this);
        }
    }

    private void runTaskAsync(Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTaskAsynchronously(this);
        }
    }

    private void openPagedEnderChest(Player adminOrPlayer, OfflinePlayer targetPlayer, int page, ItemStack[] items) {
        int maxPages = getMaxPages(targetPlayer);
        if (page > maxPages) page = maxPages;
        if (page < 1) page = 1;
        String title;
        if (adminViewingMap.containsKey(adminOrPlayer.getUniqueId())) {
            title = adminTitleFormat.replace("{player_name}", Objects.toString(targetPlayer.getName(), "Unknown"))
                    .replace("{current_page}", String.valueOf(page))
                    .replace("{max_pages}", String.valueOf(maxPages));
        } else {
            title = titleFormat.replace("{current_page}", String.valueOf(page))
                    .replace("{max_pages}", String.valueOf(maxPages));
        }
        Inventory inv = Bukkit.createInventory(null, 54, title);
        if (items != null) {
            ItemStack[] sizedItems = new ItemStack[45];
            System.arraycopy(items, 0, sizedItems, 0, Math.min(items.length, 45));
            inv.setContents(sizedItems);
        }
        setupControlPanel(inv, page, maxPages);
        adminOrPlayer.openInventory(inv);
        playerCurrentPage.put(adminOrPlayer.getUniqueId(), page);
    }

    private void setupControlPanel(Inventory inv, int currentPage, int maxPages) {
        for (int i = 46; i <= 52; i++) {
            inv.setItem(i, fillerItem.clone());
        }
        if (currentPage > 1) {
            inv.setItem(45, formatNavItem(prevPageEnabled.clone(), currentPage, maxPages));
        } else {
            inv.setItem(45, formatNavItem(prevPageDisabled.clone(), currentPage, maxPages));
        }
        if (currentPage < maxPages) {
            inv.setItem(53, formatNavItem(nextPageEnabled.clone(), currentPage, maxPages));
        } else {
            inv.setItem(53, formatNavItem(nextPageDisabled.clone(), currentPage, maxPages));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();
        boolean isAdminView = adminViewingMap.containsKey(player.getUniqueId());
        UUID targetUUID = adminViewingMap.getOrDefault(player.getUniqueId(), player.getUniqueId());
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
        String expectedAdminSingleTitle = isAdminView ? adminSinglePageTitle.replace("{player_name}", Objects.toString(targetPlayer.getName(), "Unknown")) : "";

        boolean isSinglePageView = inventoryTitle.equals(singlePageTitle) || inventoryTitle.equals(expectedAdminSingleTitle);
        boolean isPagedView = inventoryTitle.startsWith(baseTitleCheck) || (isAdminView && inventoryTitle.startsWith(adminBaseTitleCheck));

        if (!isSinglePageView && !isPagedView) return;

        boolean isDroppingItem = event.getClick().equals(ClickType.DROP) || event.getClick().equals(ClickType.CONTROL_DROP);
        boolean isDraggingOut = event.getSlot() == -999 && event.getCursor() != null && event.getCursor().getType() != Material.AIR;

        if (isDroppingItem || isDraggingOut) {
            if (worldGuardHook != null) {
                if (!worldGuardHook.canDropItems(player)) {
                    event.setCancelled(true);
                    player.sendMessage(wgDropDenyMessage);
                    player.playSound(player.getLocation(), soundWgDeny, wgDenyVolume, wgDenyPitch);
                    return;
                }
            }
        }

        if (isSinglePageView) return;

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 45) return;

        event.setCancelled(true);
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 1);
        int maxPages = getMaxPages(targetPlayer);

        if (slot == 45) {
            if (currentPage > 1) {
                navigationLock.put(player.getUniqueId(), navigationLock.getOrDefault(player.getUniqueId(), 0) + 1);
                navigateToPage(player, targetPlayer, currentPage, currentPage - 1);
            } else {
                player.playSound(player.getLocation(), soundFail, failVolume, failPitch);
            }
        } else if (slot == 53) {
            if (currentPage < maxPages) {
                navigationLock.put(player.getUniqueId(), navigationLock.getOrDefault(player.getUniqueId(), 0) + 1);
                navigateToPage(player, targetPlayer, currentPage, currentPage + 1);
            } else {
                player.playSound(player.getLocation(), soundFail, failVolume, failPitch);
            }
        }
    }

    private void navigateToPage(Player player, OfflinePlayer targetPlayer, int currentPage, int targetPage) {
        ItemStack[] itemsToSave = Arrays.copyOfRange(player.getOpenInventory().getTopInventory().getContents(), 0, 45);
        savePagedData(targetPlayer.getUniqueId(), currentPage, itemsToSave);
        player.playSound(player.getLocation(), soundNavigate, navigateVolume, navigatePitch);
        runTaskAsync(() -> {
            try {
                if (useDatabase && (databaseManager == null || !databaseManager.isConnected())) {
                    throw new IllegalStateException("Database is not connected.");
                }
                ItemStack[] newItems = loadPagedData(targetPlayer.getUniqueId(), targetPage);
                runTaskOnCorrectThread(player, () -> openPagedEnderChest(player, targetPlayer, targetPage, newItems));
            } catch (Exception e) {
                navigationLock.computeIfPresent(player.getUniqueId(), (k, v) -> v > 1 ? v - 1 : null);
                handleDatabaseError(e, player);
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        int lockCount = navigationLock.getOrDefault(playerUUID, 0);
        if (lockCount > 0) {
            navigationLock.put(playerUUID, lockCount - 1);
            return;
        }
        String inventoryTitle = event.getView().getTitle();
        boolean wasAdminViewing = adminViewingMap.containsKey(playerUUID);
        UUID targetUUID = adminViewingMap.getOrDefault(playerUUID, playerUUID);
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
        String expectedAdminSingleTitle = wasAdminViewing ? adminSinglePageTitle.replace("{player_name}", Objects.toString(targetPlayer.getName(), "Unknown")) : "";

        boolean isSinglePageView = inventoryTitle.equals(singlePageTitle) || inventoryTitle.equals(expectedAdminSingleTitle);
        boolean isPagedView = inventoryTitle.startsWith(baseTitleCheck) || (wasAdminViewing && inventoryTitle.startsWith(adminBaseTitleCheck));

        if (!isSinglePageView && !isPagedView) return;

        adminViewingMap.remove(playerUUID);
        boolean openedByBlock = openedPhysicalChests.containsKey(playerUUID);
        if (isSinglePageView) {
            saveSinglePageData(targetUUID, event.getInventory().getContents());
        } else if (isPagedView) {
            if (playerCurrentPage.containsKey(playerUUID)) {
                int page = playerCurrentPage.get(playerUUID);
                ItemStack[] items = Arrays.copyOfRange(event.getInventory().getContents(), 0, 45);
                savePagedData(targetUUID, page, items);
                playerCurrentPage.remove(playerUUID);
            }
        }
        if (!openedByBlock) {
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 0.8f, 1.0f);
        }
        if (openedPhysicalChests.containsKey(playerUUID)) {
            Location chestLoc = openedPhysicalChests.remove(playerUUID);
            Block chestBlock = chestLoc.getBlock();
            if (chestBlock.getType() == Material.ENDER_CHEST && chestBlock.getState() instanceof org.bukkit.block.EnderChest) {
                ((org.bukkit.block.EnderChest) chestBlock.getState()).close();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (getMaxPages(player) > 0) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof org.bukkit.block.EnderChest) {
                ((org.bukkit.block.EnderChest) block.getState()).open();
                openedPhysicalChests.put(player.getUniqueId(), block.getLocation());
            }
            openInventoryForPlayer(player);
        } else {
            player.openInventory(player.getEnderChest());
        }
    }

    private int getMaxPages(OfflinePlayer player) {
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (!onlinePlayer.hasPermission("bsruenderchest.use")) return 0;
            if (onlinePlayer.isOp()) return 10;
            for (int i = 10; i >= 2; i--) {
                if (onlinePlayer.hasPermission("bsruenderchest.plus." + i)) {
                    return i;
                }
            }
            return 1;
        }

        if (this.luckPermsHook != null) {
            int pagesFromLp = this.luckPermsHook.getMaxPagesOffline(player);
            if (pagesFromLp != -1) {
                return pagesFromLp;
            }
        }

        try {
            if (hasSinglePageData(player.getUniqueId())) {
                return 1;
            } else if (hasPagedData(player.getUniqueId())) {
                return 10;
            }
        } catch (SQLException e) {
            handleDatabaseError(e, null);
            return 0;
        }
        return 0;
    }

    private boolean hasData(UUID uuid) throws SQLException {
        return hasSinglePageData(uuid) || hasPagedData(uuid);
    }

    private boolean hasSinglePageData(UUID uuid) throws SQLException {
        if (useDatabase) {
            return databaseManager.hasSinglePageData(uuid);
        } else {
            File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
            if (!playerFile.exists()) return false;
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return config.contains("single-inventory");
        }
    }

    private boolean hasPagedData(UUID uuid) throws SQLException {
        if (useDatabase) {
            return databaseManager.hasPagedData(uuid);
        } else {
            File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
            if (!playerFile.exists()) return false;
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return config.contains("paged-inventories");
        }
    }

    private void handleDataMigration(UUID uuid) {
        if (useDatabase) return;
        File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
        if (!playerFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        if (config.contains("single-inventory")) {
            getLogger().info("Found old data for player " + uuid + ". Starting smart migration...");
            List<ItemStack> oldItems = (List<ItemStack>) config.getList("single-inventory", new ArrayList<>());
            oldItems.removeAll(Collections.singleton(null));
            if (oldItems.isEmpty()) {
                config.set("single-inventory", null);
                saveConfig(config, playerFile);
                return;
            }
            int pageSize = 45;
            int pageNumber = 1;
            for (int i = 0; i < oldItems.size(); i += pageSize) {
                List<ItemStack> pageItems = new ArrayList<>(oldItems.subList(i, Math.min(i + pageSize, oldItems.size())));
                config.set("paged-inventories." + pageNumber, pageItems);
                pageNumber++;
            }
            config.set("single-inventory", null);
            saveConfig(config, playerFile);
            getLogger().info("Smart migration successful for " + uuid + ".");
        }
    }

    private void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().severe("Could not save configuration to " + file.getName());
        }
    }

    private void savePagedData(UUID uuid, int page, ItemStack[] items) {
        runTaskAsync(() -> {
            try {
                if (useDatabase) {
                    if (databaseManager == null || !databaseManager.isConnected()) throw new IllegalStateException("Database not connected");
                    databaseManager.savePagedData(uuid, page, items);
                } else {
                    File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                    config.set("paged-inventories." + page, items);
                    saveConfig(config, playerFile);
                }
            } catch (Exception e) {
                handleDatabaseError(e, Bukkit.getPlayer(uuid));
            }
        });
    }

    private ItemStack[] loadPagedData(UUID uuid, int page) throws SQLException, IOException, ClassNotFoundException {
        if (useDatabase) {
            return databaseManager.loadPagedData(uuid, page);
        } else {
            File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
            if (!playerFile.exists()) return new ItemStack[45];
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            List<?> list = config.getList("paged-inventories." + page);
            if (list == null) return new ItemStack[45];
            return list.toArray(new ItemStack[0]);
        }
    }

    private void saveSinglePageData(UUID uuid, ItemStack[] items) {
        runTaskAsync(() -> {
            try {
                if (useDatabase) {
                    if (databaseManager == null || !databaseManager.isConnected()) throw new IllegalStateException("Database not connected");
                    databaseManager.saveSinglePageData(uuid, items);
                } else {
                    File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                    config.set("single-inventory", items);
                    saveConfig(config, playerFile);
                }
            } catch (Exception e) {
                handleDatabaseError(e, Bukkit.getPlayer(uuid));
            }
        });
    }

    private ItemStack[] loadSinglePageData(UUID uuid) throws SQLException, IOException, ClassNotFoundException {
        if (useDatabase) {
            return databaseManager.loadSinglePageData(uuid);
        } else {
            File playerFile = new File(new File(getDataFolder(), "playerdata"), uuid.toString() + ".yml");
            if (!playerFile.exists()) return new ItemStack[54];
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            List<?> list = config.getList("single-inventory");
            if (list == null) return new ItemStack[54];
            return list.toArray(new ItemStack[0]);
        }
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        useDatabase = config.getBoolean("database.enable", false);
        playerDbErrorMessage = ChatColor.translateAlternateColorCodes('&', config.getString("player-database-error-message", "&c[Enderchest] ระบบมีปัญหาชั่วคราว"));
        adminDbErrorMessage = ChatColor.translateAlternateColorCodes('&', config.getString("admin-database-error-message", "&c&l[BSRU Enderchest] &cCRITICAL: Database connection failed!"));
        titleFormat = ChatColor.translateAlternateColorCodes('&', config.getString("inventory-title-format", "&5Ender Chest ({current_page}/{max_pages})"));

        int placeholderIndex = titleFormat.indexOf("{");
        baseTitleCheck = (placeholderIndex != -1) ? titleFormat.substring(0, placeholderIndex) : titleFormat;

        singlePageTitle = ChatColor.translateAlternateColorCodes('&', config.getString("single-page-title", "&5&lEnder Chest (6 Rows)"));

        adminTitleFormat = ChatColor.translateAlternateColorCodes('&', config.getString("admin-inventory-title-format", "&cAdmin View: {player_name} ({current_page}/{max_pages})"));
        int adminPlaceholderIndex = adminTitleFormat.indexOf("{");
        adminBaseTitleCheck = (adminPlaceholderIndex != -1) ? adminTitleFormat.substring(0, adminPlaceholderIndex) : adminTitleFormat;

        adminSinglePageTitle = ChatColor.translateAlternateColorCodes('&', config.getString("admin-single-page-title", "&cAdmin View: {player_name} (6 Rows)"));
        wgDropDenyMessage = ChatColor.translateAlternateColorCodes('&', config.getString("worldguard-drop-deny-message", "&cคุณไม่สามารถทิ้งของในบริเวณนี้ได้"));

        noPermsMsg = ChatColor.translateAlternateColorCodes('&', config.getString("no-permission-command-message", "&cYou don't have permission."));

        try {
            String navigateKey = config.getString("sounds.navigate.name", "ui.button.click").toLowerCase().replace('_', '.');
            navigateVolume = (float) config.getDouble("sounds.navigate.volume", 0.8);
            navigatePitch = (float) config.getDouble("sounds.navigate.pitch", 1.2);
            soundNavigate = Registry.SOUNDS.get(NamespacedKey.minecraft(navigateKey));
            if (soundNavigate == null) {
                getLogger().warning("Invalid sound key for 'navigate': " + navigateKey + ". Using default.");
                soundNavigate = Sound.UI_BUTTON_CLICK;
            }

            String failKey = config.getString("sounds.fail.name", "block.anvil.place").toLowerCase().replace('_', '.');
            failVolume = (float) config.getDouble("sounds.fail.volume", 1.0);
            failPitch = (float) config.getDouble("sounds.fail.pitch", 1.0);
            soundFail = Registry.SOUNDS.get(NamespacedKey.minecraft(failKey));
            if (soundFail == null) {
                getLogger().warning("Invalid sound key for 'fail': " + failKey + ". Using default.");
                soundFail = Sound.BLOCK_ANVIL_PLACE;
            }

            String wgDenyKey = config.getString("sounds.worldguard-deny.name", "entity.villager.no").toLowerCase().replace('_', '.');
            wgDenyVolume = (float) config.getDouble("sounds.worldguard-deny.volume", 1.0);
            wgDenyPitch = (float) config.getDouble("sounds.worldguard-deny.pitch", 0.8);
            soundWgDeny = Registry.SOUNDS.get(NamespacedKey.minecraft(wgDenyKey));
            if (soundWgDeny == null) {
                getLogger().warning("Invalid sound key for 'worldguard-deny': " + wgDenyKey + ". Using default.");
                soundWgDeny = Sound.ENTITY_VILLAGER_NO;
            }
        } catch (Exception e) {
            getLogger().severe("An error occurred while loading sounds.");
            soundNavigate = Sound.UI_BUTTON_CLICK;
            soundFail = Sound.BLOCK_ANVIL_PLACE;
            soundWgDeny = Sound.ENTITY_VILLAGER_NO;
        }

        prevPageEnabled = loadNavItem("control-panel.previous-page.enabled");
        prevPageDisabled = loadNavItem("control-panel.previous-page.disabled");
        nextPageEnabled = loadNavItem("control-panel.next-page.enabled");
        nextPageDisabled = loadNavItem("control-panel.next-page.disabled");
        fillerItem = loadNavItem("control-panel.filler");
    }

    private ItemStack loadNavItem(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) {
            getLogger().severe("Config section missing: " + path + ". Using fallback item.");
            return new ItemStack(Material.STONE);
        }
        Material mat = Material.matchMaterial(section.getString("material", "STONE"));
        if (mat == null) {
            getLogger().warning("Invalid material in config: " + section.getString("material") + " at path " + path);
            mat = Material.STONE;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name", " ")));
            List<String> lore = section.getStringList("lore").stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack formatNavItem(ItemStack item, int currentPage, int maxPages) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int prevPage = currentPage - 1;
            int nextPage = currentPage + 1;
            if (meta.hasDisplayName()) {
                meta.setDisplayName(meta.getDisplayName().replace("{previous_page}", String.valueOf(prevPage)).replace("{current_page}", String.valueOf(currentPage)).replace("{next_page}", String.valueOf(nextPage)).replace("{max_pages}", String.valueOf(maxPages)));
            }
            if (meta.hasLore()) {
                meta.setLore(meta.getLore().stream().map(line -> line.replace("{previous_page}", String.valueOf(prevPage)).replace("{current_page}", String.valueOf(currentPage)).replace("{next_page}", String.valueOf(nextPage)).replace("{max_pages}", String.valueOf(maxPages))).collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}