package benchmark.synchronize.related_work;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import benchmark.oltp.OLTPClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HATtrickFreshness {
    private static String ConnTP, ConnAP;
    private static Properties dbProps;
    private static int THREAD_COUNT; // 你可以根据需要设置线程数量

    public HATtrickFreshness(String ConnTP, String ConnAP, Properties dbProps, int threadCount) {
        THREAD_COUNT = threadCount;
        this.ConnAP = ConnAP;
        this.ConnTP = ConnTP;
        this.dbProps = dbProps;
    }

    public static void parallelCreateTable() {
        for (int i = 1; i <= THREAD_COUNT; i++) {
            int threadId = i;
            new Thread(() -> createTable(threadId)).start();
        }
    }

    private static void createTable(int threadId) {
        Statement stmt = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(ConnTP, dbProps);
            stmt = conn.createStatement();
            String tableName = "freshness_" + threadId;

            // 删除已存在的表
            String sqlDrop = "DROP TABLE IF EXISTS " + tableName;
            stmt.executeUpdate(sqlDrop);

            // 创建新表
            String sqlCreate = "CREATE TABLE " + tableName +
                    " (thread_id INT, transaction_id INT) ";
            stmt.executeUpdate(sqlCreate);
            System.out.println("Table created: " + tableName);
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public Long computeFreshness(long queryStartTime) {
        // 创建 txnIdToCommitTimeMap 的只读快照
        Map<Integer, Long> txnIdToCommitTimeMapSnapshot = new HashMap<>(OLTPClient.txnIdToCommitTimeMap);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        long freshness = 0;
        try {
            conn = DriverManager.getConnection(ConnAP, dbProps);
            stmt = conn.createStatement();
            String unionQuery = buildUnionQuery();
            String query = "SELECT DISTINCT thread_id, transaction_id FROM (" +
                    unionQuery +
                    ") AS TMP1 ORDER BY transaction_id";
            // System.out.println("HATTtrick query is: " + query);
            rs = stmt.executeQuery(query);
            if (rs.next()) {
                int threadId = rs.getInt("thread_id");
                int transactionId = rs.getInt("transaction_id");
                System.out.println("Thread ID: " + threadId + ", Transaction ID: " + transactionId);

                // 查找大于此事务 ID 的最小事务 ID 的提交时间
                Long commitTime = findFirstCommitTimeGreaterThan(transactionId, txnIdToCommitTimeMapSnapshot);
                if (commitTime != null) {
                    freshness = Math.max(0, queryStartTime - commitTime);
                    System.out.println("Freshness for transaction " + transactionId + ": " + freshness + " ms");
                } else {
                    System.out.println("No later transaction found for transaction ID " + transactionId);
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return freshness;
    }

    private Long findFirstCommitTimeGreaterThan(int transactionId, Map<Integer, Long> txnIdToCommitTimeMapSnapshot) {
        // 使用快照来寻找大于指定事务 ID 的最小事务 ID 的提交时间
        return txnIdToCommitTimeMapSnapshot.entrySet().stream()
                .filter(e -> e.getKey() > transactionId)
                .min(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(null); // 如果没有找到，返回 null
    }

    private String buildUnionQuery() {
        StringBuilder unionQuery = new StringBuilder();
        for (int i = 1; i <= THREAD_COUNT; i++) {
            unionQuery.append("SELECT * FROM \"freshness_").append(i).append("\"");
            if (i < THREAD_COUNT) {
                unionQuery.append(" UNION ALL ");
            }
        }
        return unionQuery.toString();
    }
}