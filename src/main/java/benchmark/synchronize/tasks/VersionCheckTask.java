package benchmark.synchronize.tasks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.Callable;

import java.sql.Connection;
import java.sql.Date;

import benchmark.oltp.DeliveryTracker;
import config.CommonConfig;
import lombok.Getter;
import lombok.Setter;

public class VersionCheckTask implements Callable<Void> {
    private DeliveryTracker.Transaction transaction;
    private Connection tpConnection;
    private Connection apConnection;
    private long freshnessTimeBound;
    private Timestamp currentTime;
    private int taskId;
    private int dbType;
    private String sql;
    private boolean isWeakRead;
    private int weakReadTime;

    public VersionCheckTask(DeliveryTracker.Transaction transaction, Connection tpConnection, Connection apConnection,
            long freshnessTimeBound, Timestamp currentTime, int taskId, int dbType, boolean isWeakRead,
            int weakReadTime) {
        this.transaction = transaction;
        this.tpConnection = tpConnection;
        this.apConnection = apConnection;
        this.freshnessTimeBound = freshnessTimeBound;
        this.currentTime = currentTime;
        this.taskId = taskId;
        this.dbType = dbType;
        this.isWeakRead = isWeakRead;
        this.weakReadTime = weakReadTime;
        initSQLs(dbType);
    }

    private void initSQLs(int dbType) {
        if (dbType == CommonConfig.DB_TIDB) {
            sql = String.format(
                    "SELECT /*+ read_from_storage(tiflash[vodka_order_line]) */ SUM(access_version) AS sum_ver FROM vodka_order_line WHERE ol_w_id = %d AND ol_d_id = %d AND ol_o_id = %d;",
                    transaction.w_id, transaction.ol_d_id, transaction.ol_o_id);

            if (this.isWeakRead) {
                System.out.println("Isweakread for TiDB!!!");
                sql = String.format(
                        "SELECT /*+ read_from_storage(tiflash[vodka_order_line]) */ SUM(access_version) AS sum_ver FROM vodka_order_line as of timestamp NOW() - INTERVAL 5 second WHERE ol_w_id = %d AND ol_d_id = %d AND ol_o_id = %d;",
                        transaction.w_id, transaction.ol_d_id, transaction.ol_o_id);
            }
        }
        if (dbType == CommonConfig.DB_OCEANBASE) {
            sql = String.format(
                    "SELECT SUM(access_version) AS sum_ver FROM vodka_order_line WHERE ol_w_id = %d AND ol_d_id = %d AND ol_o_id = %d;",
                    transaction.w_id, transaction.ol_d_id, transaction.ol_o_id);
            if (this.isWeakRead) {
                System.out.println("Isweakread for OB!!!");
                sql = String.format(
                        "SELECT /*+READ_CONSISTENCY(WEAK) */ SUM(access_version) AS sum_ver FROM vodka_order_line WHERE ol_w_id = %d AND ol_d_id = %d AND ol_o_id = %d;",
                        transaction.w_id, transaction.ol_d_id, transaction.ol_o_id);
            }
        } else {
            sql = String.format(
                    "SELECT SUM(access_version) AS sum_ver FROM vodka_order_line v WHERE v.ol_w_id = %d AND v.ol_d_id = %d AND v.ol_o_id = %d;",
                    transaction.w_id, transaction.ol_d_id, transaction.ol_o_id);
        }
    }

    @Override
    public Void call() throws Exception {
        int tpVersion = sendToEngine(tpConnection, sql);
        if (dbType == CommonConfig.DB_TIDB) {
            Statement setTiflashStatement = apConnection.createStatement();
            setTiflashStatement.execute("set @@session.tidb_isolation_read_engines = 'tiflash';");
            if (isWeakRead) {
                Statement weakStatement = apConnection.createStatement();
                weakStatement.execute("SET @@tidb_read_staleness= \"-5\";");
            }
        }
        long startTime = System.nanoTime();
        int apVersion = sendToEngine(apConnection, sql);
        long endTime = System.nanoTime();
        long syncTime = endTime - startTime;
        updateMaxValues(syncTime);
        startTime = System.nanoTime();
        while (apVersion < tpVersion) {
            apVersion = sendToEngine(apConnection, sql);
        }
        endTime = System.nanoTime();
        syncTime += endTime - startTime;
        updateMaxValues(syncTime);
        System.out.println("Task " + taskId + " completed.");
        return null;
    }

    // private ResultSet sendToEngine(Connection conn, String sql) throws
    // SQLException {
    // Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
    // ResultSet.CONCUR_READ_ONLY);
    // return stmt.executeQuery(sql);
    // }

    private void updateMaxValues(long syncTime) {
        SynchronizeTask.updateMaxValues(syncTime);
    }

    private int sendToEngine(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int sumAccessVersion = rs.getInt(1);
                return sumAccessVersion;
            }
        }
        return 0;
    }

    private DatabaseResponse sendToEngine(String sql, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int sumAccessVersion = rs.getInt(1);
                return new DatabaseResponse(sumAccessVersion, null);
            }
        }
        return new DatabaseResponse(0, null);
    }

    @Getter
    @Setter
    private static class DatabaseResponse {
        public int version;
        public Timestamp avgTs;

        public DatabaseResponse(int version, Timestamp avgTs) {
            this.version = version;
            this.avgTs = avgTs;
        }
    }
}
