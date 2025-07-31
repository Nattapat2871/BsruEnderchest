package com.bsruEnderchest;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DatabaseManager {

    private final HikariDataSource dataSource;
    private final Logger logger;

    public DatabaseManager(String host, int port, String dbName, String user, String pass, Logger logger) {
        this.logger = logger;
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true");
        dataSource.setUsername(user);
        dataSource.setPassword(pass);

        // --- ส่วนที่เพิ่มเข้ามาเพื่อแก้ปัญหา Warning และเพิ่มประสิทธิภาพ ---
        dataSource.setMaxLifetime(1800000); // 30 minutes
        dataSource.setKeepaliveTime(30000); // 30 seconds
        dataSource.setConnectionTimeout(10000); // 10 seconds
        // ----------------------------------------------------

        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        logger.info("Database connection pool initialized.");
        createTables();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    private Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Database connection is closed.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }

    private void createTables() {
        String createPagedTableSQL = "CREATE TABLE IF NOT EXISTS paged_enderchests (" +
                "player_uuid VARCHAR(36) NOT NULL," +
                "page_number INT NOT NULL," +
                "inventory_data MEDIUMBLOB NOT NULL," +
                "PRIMARY KEY (player_uuid, page_number));";

        String createSingleTableSQL = "CREATE TABLE IF NOT EXISTS single_enderchests (" +
                "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "inventory_data MEDIUMBLOB NOT NULL);";

        try (Connection conn = getConnection();
             PreparedStatement pstmtPaged = conn.prepareStatement(createPagedTableSQL);
             PreparedStatement pstmtSingle = conn.prepareStatement(createSingleTableSQL)) {
            pstmtPaged.execute();
            pstmtSingle.execute();
            logger.info("Database tables verified/created successfully.");
        } catch (SQLException e) {
            logger.severe("Could not create database tables!");
            e.printStackTrace();
        }
    }

    public boolean hasSinglePageData(UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM single_enderchests WHERE player_uuid = ? LIMIT 1;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasPagedData(UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM paged_enderchests WHERE player_uuid = ? LIMIT 1;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void savePagedData(UUID uuid, int page, ItemStack[] items) throws SQLException, IOException {
        String sql = "INSERT INTO paged_enderchests (player_uuid, page_number, inventory_data) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE inventory_data = VALUES(inventory_data);";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, page);
            pstmt.setBytes(3, toByteArray(items));
            pstmt.executeUpdate();
        }
    }

    public ItemStack[] loadPagedData(UUID uuid, int page) throws SQLException, IOException, ClassNotFoundException {
        String sql = "SELECT inventory_data FROM paged_enderchests WHERE player_uuid = ? AND page_number = ?;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, page);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return fromByteArray(rs.getBytes("inventory_data"));
                }
            }
        }
        return new ItemStack[45];
    }

    public void saveSinglePageData(UUID uuid, ItemStack[] items) throws SQLException, IOException {
        String sql = "INSERT INTO single_enderchests (player_uuid, inventory_data) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE inventory_data = VALUES(inventory_data);";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setBytes(2, toByteArray(items));
            pstmt.executeUpdate();
        }
    }

    public ItemStack[] loadSinglePageData(UUID uuid) throws SQLException, IOException, ClassNotFoundException {
        String sql = "SELECT inventory_data FROM single_enderchests WHERE player_uuid = ?;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return fromByteArray(rs.getBytes("inventory_data"));
                }
            }
        }
        return new ItemStack[54];
    }

    private byte[] toByteArray(ItemStack[] items) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
             BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(gzipOut)) {
            bukkitOut.writeObject(items);
        }
        return byteOut.toByteArray();
    }

    private ItemStack[] fromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        try (GZIPInputStream gzipIn = new GZIPInputStream(byteIn);
             BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(gzipIn)) {
            return (ItemStack[]) bukkitIn.readObject();
        }
    }
}