package ve.nottabaker.nottpay.transaction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import ve.nottabaker.nottpay.NottPay;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages transaction persistence using HikariCP and SQL (SQLite/MySQL).
 */
public class TransactionManager {

    private final NottPay plugin;
    private HikariDataSource dataSource;
    private final String dbType;

    public TransactionManager(NottPay plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        setupPool();
        createTable();
    }

    private void setupPool() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("NottPay-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(5000);

        if (dbType.equals("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String db = plugin.getConfig().getString("database.mysql.database", "nottpay");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // Default to SQLite
            File dbFile = new File(plugin.getDataFolder(), "data/database.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Successfully connected to the database (" + dbType + ").");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize standard database pool!", e);
        }
    }

    private void createTable() {
        if (dataSource == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS nottpay_transactions (" +
                (dbType.equals("SQLITE") ? "id INTEGER PRIMARY KEY AUTOINCREMENT, " : "id INT AUTO_INCREMENT PRIMARY KEY, ") +
                "sender VARCHAR(36) NOT NULL, " +
                "receiver VARCHAR(36) NOT NULL, " +
                "sender_name VARCHAR(16) NOT NULL, " +
                "receiver_name VARCHAR(16) NOT NULL, " +
                "currency VARCHAR(64) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "timestamp BIGINT NOT NULL)";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            if (dbType.equals("SQLITE")) {
                // SQLite supports CREATE INDEX IF NOT EXISTS
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sender ON nottpay_transactions(sender)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_receiver ON nottpay_transactions(receiver)");
            } else {
                // MySQL < 8.0 does not support CREATE INDEX IF NOT EXISTS
                // Use a safe INSERT IGNORE workaround via information_schema
                String checkIndex = "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = 'nottpay_transactions' AND index_name = ?";

                try (PreparedStatement ps = conn.prepareStatement(checkIndex)) {
                    ps.setString(1, "idx_sender");
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("CREATE INDEX idx_sender ON nottpay_transactions(sender)");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(checkIndex)) {
                    ps.setString(1, "idx_receiver");
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("CREATE INDEX idx_receiver ON nottpay_transactions(receiver)");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database tables!", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Records a new transaction asynchronously.
     */
    public void addTransaction(Transaction tx) {
        if (dataSource == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO nottpay_transactions (sender, receiver, sender_name, receiver_name, currency, amount, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, tx.getSender().toString());
                pstmt.setString(2, tx.getReceiver().toString());
                pstmt.setString(3, tx.getSenderName());
                pstmt.setString(4, tx.getReceiverName());
                pstmt.setString(5, tx.getCurrency());
                pstmt.setDouble(6, tx.getAmount());
                pstmt.setLong(7, tx.getTimestamp());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save transaction to database", e);
            }
        });
    }

    /**
     * Get a paginated view of a player's transactions from DB asynchronously.
     * Runs the query off the main thread and fires a callback with the results.
     *
     * @param playerUUID The player's UUID
     * @param page       1-indexed page number
     * @param perPage    Items per page
     * @param callback   Called on the main thread with the resulting list
     */
    public void getTransactionsPaged(UUID playerUUID, int page, int perPage,
                                     java.util.function.Consumer<List<Transaction>> callback) {
        if (dataSource == null) {
            callback.accept(java.util.Collections.emptyList());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Transaction> list = new ArrayList<>();
            int offset = (page - 1) * perPage;
            String uuidStr = playerUUID.toString();
            String sql = "SELECT * FROM nottpay_transactions " +
                    "WHERE sender = ? OR receiver = ? " +
                    "ORDER BY timestamp DESC LIMIT ? OFFSET ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, uuidStr);
                pstmt.setString(2, uuidStr);
                pstmt.setInt(3, perPage);
                pstmt.setInt(4, offset);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Transaction(
                                UUID.fromString(rs.getString("sender")),
                                UUID.fromString(rs.getString("receiver")),
                                rs.getString("sender_name"),
                                rs.getString("receiver_name"),
                                rs.getString("currency"),
                                rs.getDouble("amount"),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error fetching paginated transactions", e);
            }
            // Return to main thread
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(list));
        });
    }

    /**
     * Gets the total number of pages asynchronously using COUNT(*).
     */
    public void getTotalPages(UUID playerUUID, int perPage, java.util.function.Consumer<Integer> callback) {
        if (dataSource == null) {
            callback.accept(1);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuidStr = playerUUID.toString();
            String sql = "SELECT COUNT(*) FROM nottpay_transactions WHERE sender = ? OR receiver = ?";
            int pages = 1;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, uuidStr);
                pstmt.setString(2, uuidStr);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1);
                        pages = Math.max(1, (int) Math.ceil((double) total / perPage));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error counting transaction pages", e);
            }
            final int result = pages;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }
}
