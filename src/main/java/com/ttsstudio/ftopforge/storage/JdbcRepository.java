package com.ttsstudio.ftopforge.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcRepository {

    private final HikariDataSource ds;
    private final Dialect dialect;

    public JdbcRepository(HikariDataSource ds, Dialect dialect) {
        this.ds = ds;
        this.dialect = dialect;
    }

    public Connection connection() throws SQLException {
        return ds.getConnection();
    }

    /** Expose the pooled DataSource for modules that bring their own DAOs (history, forensics, seasons, meta). */
    public DataSource getDataSource() {
        return ds;
    }

    public Dialect dialect() {
        return dialect;
    }

    public void shutdown() {
        if (!ds.isClosed()) ds.close();
    }

    public static JdbcRepository init(JavaPlugin plugin) throws SQLException {
        ConfigurationSection storage = plugin.getConfig().getConfigurationSection("storage");
        String type = storage != null ? storage.getString("type", "sqlite") : "sqlite";
        Dialect dialect = Dialect.byName(type);
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("FTopForge-Hikari");
        if (dialect == Dialect.SQLITE) {
            File db = new File(plugin.getDataFolder(), "ftopforge.db");
            plugin.getDataFolder().mkdirs();
            cfg.setDriverClassName("org.sqlite.JDBC");
            cfg.setJdbcUrl("jdbc:sqlite:" + db.getAbsolutePath());
            cfg.setMaximumPoolSize(1); // SQLite is single-writer
            cfg.setConnectionTestQuery("SELECT 1");
        } else {
            ConfigurationSection my = storage.getConfigurationSection("mysql");
            if (my == null) throw new SQLException("storage.mysql section missing");
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            cfg.setJdbcUrl("jdbc:mysql://" + my.getString("host") + ":" + my.getInt("port") +
                    "/" + my.getString("database") + "?useSSL=" + my.getBoolean("use-ssl", false));
            cfg.setUsername(my.getString("user"));
            cfg.setPassword(my.getString("password", ""));
            cfg.setMaximumPoolSize(5);
        }
        HikariDataSource ds = new HikariDataSource(cfg);
        JdbcRepository repo = new JdbcRepository(ds, dialect);
        repo.bootstrapSchema();
        return repo;
    }

    private void bootstrapSchema() throws SQLException {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.executeUpdate(dialect.recalcsDdl());
            s.executeUpdate(dialect.snapshotsCurrentDdl());
        }
    }
}
