package dev.emortal.minestom.marathon.leaderboard;

import dev.emortal.messaging.types.MarathonData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeaderboardDB {

    private static final Logger LOGGER = Logger.getLogger("LeaderboardDB");

    private final String connString;
    private final String user;
    private final String password;
    private @Nullable Connection conn;

    public LeaderboardDB(String connString, String user, String password) {
        this.connString = connString;
        this.user = user;
        this.password = password;
    }

    public void connect() {
        try {
            LOGGER.info("Connecting to database");
            conn = DriverManager.getConnection(connString, user, password);
            LOGGER.info("Connected to database");

            init();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        if (!isConnectionValid()) {
            LOGGER.info("Connecting to database");
            conn = DriverManager.getConnection(connString, user, password);
        }
        return conn;
    }

    private boolean isConnectionValid() {
        if (conn == null) return false;
        try {
            return conn.isValid(5);
        } catch (SQLException _) {
            return false;
        }
    }

    private void init() {
        if (conn == null) return;

//        deleteLeaderboardTable();
        createLeaderboardTable();
        createSettingsTable();

        LOGGER.info("Created SQL tables");
    }

    private void deleteLeaderboardTable() {
        String sql =
                "DROP TABLE IF EXISTS marathon";
        try (var statement = getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private void createLeaderboardTable() {
        String sql = """
                        CREATE TABLE IF NOT EXISTS marathon (
                        uuid UUID not null primary key,
                        name TEXT not null,
                        score INT,
                        ticks BIGINT,
                        submitted_at DATETIME not null
                        )""";
        try (var statement = getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createSettingsTable() {
        String sql = """
                        CREATE TABLE IF NOT EXISTS marathon_settings (
                        uuid UUID not null primary key,
                        time TEXT not null,
                        palette TEXT not null,
                        animation TEXT not null
                        )""";
        try (var statement = getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSettings(Player player, MarathonData settings) {
        String sql = """
                        INSERT INTO marathon_settings (uuid, time, palette, animation) VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                        time = VALUES(time),
                        palette = VALUES(palette),
                        animation = VALUES(animation)
                        """;

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, player.getUuid().toString());
            statement.setString(2, settings.time());
            statement.setString(3, settings.blockPalette());
            statement.setString(4, settings.animation());
            int result = statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable MarathonData getSettings(UUID player) {
        String sql = """
                        SELECT time, palette, animation FROM marathon_settings
                        WHERE uuid = ?
                        """;

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, player.toString());
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return new MarathonData(
                        result.getString("time"),
                        result.getString("palette"),
                        result.getString("animation")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addScore(Player player, int score, long ticks) {
        if (conn == null) return;

        // only insert if score is higher
        String sql = """
                        INSERT INTO marathon (uuid, name, score, ticks, submitted_at) VALUES (?, ?, ?, ?, now())
                        ON DUPLICATE KEY UPDATE
                        name = ?,
                        ticks = IF(VALUES(score) > score, VALUES(ticks), ticks),
                        score = IF(VALUES(score) > score, VALUES(score), score),
                        submitted_at = now()
                        """;

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, player.getUuid().toString());
            statement.setString(2, player.getUsername());
            statement.setInt(3, score);
            statement.setLong(4, ticks);
            statement.setString(5, player.getUsername());
            int result = statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LeaderboardEntry> getTopScores(int top) {
        String sql = """
                        SELECT uuid, name, score, ticks, submitted_at, rn AS position
                        FROM (
                            SELECT uuid, name, score, ticks, submitted_at,
                                   ROW_NUMBER() OVER (ORDER BY score DESC, ticks ASC) AS rn
                            FROM marathon
                        ) ranked
                        LIMIT ?""";

        List<LeaderboardEntry> results = new ArrayList<>();

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, top);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                results.add(new LeaderboardEntry(
                        UUID.fromString(result.getString("uuid")),
                        result.getString("name"),
                        result.getInt("position"),
                        result.getInt("score"),
                        result.getLong("ticks"),
                        result.getTimestamp("submitted_at").getTime()
                ));
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LeaderboardEntry> getTopScores(int top, int days) {
        String sql = """
                        SELECT uuid, name, score, ticks, submitted_at, rn AS position
                        FROM (
                            SELECT uuid, name, score, ticks, submitted_at,
                                   ROW_NUMBER() OVER (ORDER BY score DESC, ticks ASC) AS rn
                            FROM marathon
                            WHERE submitted_at >= NOW() - INTERVAL ? DAY
                        ) ranked
                        LIMIT ?""";

        List<LeaderboardEntry> results = new ArrayList<>();

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, days);
            statement.setInt(2, top);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                results.add(new LeaderboardEntry(
                        UUID.fromString(result.getString("uuid")),
                        result.getString("name"),
                        result.getInt("position"),
                        result.getInt("score"),
                        result.getLong("ticks"),
                        result.getTimestamp("submitted_at").getTime()
                ));
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable LeaderboardEntry getScore(UUID uuid) {
        String sql = """
                        SELECT uuid, name, score, ticks, submitted_at, rn AS position
                        FROM (
                            SELECT uuid, name, score, ticks, submitted_at,
                                   ROW_NUMBER() OVER (ORDER BY score DESC, ticks ASC) AS rn
                            FROM marathon
                        ) ranked
                        WHERE uuid = ?
                        LIMIT 1""";

        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return new LeaderboardEntry(
                        UUID.fromString(result.getString("uuid")),
                        result.getString("name"),
                        result.getInt("position"),
                        result.getInt("score"),
                        result.getLong("ticks"),
                        result.getTimestamp("submitted_at").getTime()
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getHypotheticalPlacement(int score, int days) {
        String sql = """
                        SELECT COUNT(*) + 1 AS position
                        FROM marathon
                        WHERE submitted_at >= NOW() - INTERVAL ? DAY
                          AND score > ?""";
        try (var statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, days);
            statement.setInt(2, score);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getInt(1);
            }

            return 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}