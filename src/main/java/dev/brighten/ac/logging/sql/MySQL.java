package dev.brighten.ac.logging.sql;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class MySQL {
    private static Connection conn;

    @SneakyThrows
    public static void initH2() {
        File dataFolder = new File(Anticheat.INSTANCE.getDataFolder(), "database.db");
        try {//https://nexus.funkemunky.cc/service/local/repositories/releases/content/com/h2database/h2/1.4.199/h2-1.4.199.jar
            if(dataFolder.createNewFile()) {
                Anticheat.INSTANCE.getLogger().info("Successfully created database.db in Anticheat folder!");
            }
        } catch (IOException e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "File write error: database.db");
        }
        try {
            Class.forName("org.h2.Driver");
            WrappedConstructor jdbcConnection = Reflections.getClass("org.h2.jdbc.JdbcConnection")
                    .getConstructor(String.class, Properties.class, String.class, Object.class, boolean.class);
            conn = new NonClosableConnection(jdbcConnection.newInstance("jdbc:h2:file:" +
                    dataFolder.getAbsolutePath(),
                    new Properties(), "root", "erc5gmv-xvg5CZQ0nzw", false));
            conn.setAutoCommit(true);
            Query.use(conn);
            Bukkit.getLogger().info("Connection to H2 SQlLite has been established.");
        } catch (SQLException ex) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "You need the H2 JBDC library. Google it. Put it in /lib folder.");
        }
    }

    public static void shutdown() {
        try {
            if(conn != null && !conn.isClosed()) {
                if(conn instanceof NonClosableConnection) {
                    ((NonClosableConnection)conn).shutdown();
                } else conn.close();
                conn = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
