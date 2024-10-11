package benchmark.synchronize.tasks;

import static benchmark.oltp.OLTPClient.transactionTraceMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FreshnessExecutor {
    String tableName = "vodka_order_line";
    String query = "SELECT /*+ read_from_storage(tiflash[vodka_order_line]) */ ol_delivery_d, ol_receipdate, ol_w_id, ol_d_id, ol_o_id, access_version FROM vodka_order_line WHERE vodka_order_line.current_ts BETWEEN ? AND ? ;";
    Connection apConnection;
    List<ColumnVersion> columnVersionList;
    Timestamp lTimestamp, rTimestamp;

    public FreshnessExecutor(Connection apConnection, Timestamp lTimestamp, Timestamp rTimestamp) {
        this.apConnection = apConnection;
        this.lTimestamp = lTimestamp;
        this.rTimestamp = rTimestamp;
    }

    public void organizeResult() throws SQLException {
        List<ColumnVersion> columnVersionList = new ArrayList<>();
        try (PreparedStatement stmt = apConnection.prepareStatement(query)) {
            // 设置查询参数，例如
            stmt.setTimestamp(1, lTimestamp);
            stmt.setTimestamp(2, rTimestamp);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // 读取每个列的数据
                    int wId = rs.getInt("ol_w_id");
                    int dId = rs.getInt("ol_d_id");
                    int oId = rs.getInt("ol_o_id");
                    int version = rs.getInt("access_version");
                    List<Integer> primaryKeys = Arrays.asList(wId, dId, oId);
                    ColumnVersion columnVersion1 = new ColumnVersion(primaryKeys, "ol_delivery_d", version);
                    ColumnVersion columnVersion2 = new ColumnVersion(primaryKeys, "ol_receipdate", version);
                    columnVersionList.add(columnVersion1);
                    columnVersionList.add(columnVersion2);
                }
            }
        }
        this.columnVersionList = columnVersionList;
    }

    public long computeFreshness() throws SQLException {
        organizeResult();
        long freshness = transactionTraceMap.findMaxTimeDifference(tableName, columnVersionList, lTimestamp);
        return freshness;
    }
}