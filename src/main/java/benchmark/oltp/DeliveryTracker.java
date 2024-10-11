package benchmark.oltp;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class DeliveryTracker {
    // 使用 LinkedHashMap 来保持插入顺序
    private LinkedHashMap<Integer, Transaction> transactionMap;
    public static AtomicInteger executionTimes = new AtomicInteger(1);

    public DeliveryTracker() {
        transactionMap = new LinkedHashMap<>();
    }

    // 同步方法来记录事务，确保线程安全
    public synchronized void logTransaction(int w_id, int ol_d_id, int ol_o_id, Timestamp current_ts) {
        Transaction transaction = new Transaction(w_id, ol_d_id, ol_o_id, current_ts);
        transactionMap.putFirst(w_id, transaction);
    }

    // 获取事务映射
    public synchronized LinkedHashMap<Integer, Transaction> getTransactionMap() {
        return new LinkedHashMap<>(transactionMap);
    }

    public void printTransactions() {
    if (transactionMap.isEmpty()) {
        System.out.println("No transactions to display.");
    } else {
        System.out.println("Displaying all transactions:");
        for (Map.Entry<Integer, DeliveryTracker.Transaction> entry : transactionMap.entrySet()) {
            Integer key = entry.getKey();
            DeliveryTracker.Transaction transaction = entry.getValue();
            System.out.printf("Transaction ID: %d, Details: %s%n", key, transaction.toString());
        }
    }
}

    // Transaction 类
    @ToString
    public static class Transaction {
        public int w_id;
        public int ol_d_id;
        public int ol_o_id;
        public Timestamp current_ts;

        public Transaction(int w_id, int ol_d_id, int ol_o_id, Timestamp current_ts) {
            this.w_id = w_id;
            this.ol_d_id = ol_d_id;
            this.ol_o_id = ol_o_id;
            this.current_ts = current_ts;
        }
    }

}