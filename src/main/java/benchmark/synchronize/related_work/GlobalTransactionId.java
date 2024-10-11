package benchmark.synchronize.related_work;


import java.util.concurrent.atomic.AtomicInteger;

public class GlobalTransactionId {
    public static final AtomicInteger currentId = new AtomicInteger(0);
}