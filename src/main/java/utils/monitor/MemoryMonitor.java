package utils.monitor;

import org.openjdk.jol.info.GraphLayout;
import benchmark.oltp.OLTPClient;
import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
public class MemoryMonitor {
    private final String resultDirName;

    public MemoryMonitor(String resultDirName) {
        this.resultDirName = resultDirName;
    }

    public void monitor() {
        try {
            // 创建文件夹和文件对象
            File file = new File(resultDirName, "memory_usage.csv");
            // 确保文件存在
            if (!file.exists()) {
                file.getParentFile().mkdirs(); // 确保目录存在
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.append("Timestamp,Memory Usage(MB)\n"); // 写入文件头部
                }
            }
            // 定义 FileWriter 在外部，使其在 TimerTask 中可见
            FileWriter out = new FileWriter(file, true); 
            Timer timer = new Timer();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 设置时间格式
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // 获取当前时间戳
                    String timestamp = sdf.format(new Date());
                    // 获取内存使用数据
                    long memoryUsage = GraphLayout.parseInstance(OLTPClient.transactionTraceMap.getTableTraces()).totalSize();
                    // 转换为 MB，基于 1024
                    double memoryUsageMB = bytesToMegabytes(memoryUsage);
                    // 构造一行 CSV 记录
                    String record = String.format("%s,%.2f\n", timestamp, memoryUsageMB);
                    try {
                        // 写入文件
                        out.append(record);
                        out.flush(); // 确保数据即时写入硬盘
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1000); // 0毫秒延迟，每1000毫秒执行一次

            // 定时器任务结束后不要忘记关闭 FileWriter
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            System.err.println("Error setting up file: " + e.getMessage());
        }
    }

    /**
     * 将字节转换为兆字节，使用1024作为转换基数。
     * @param bytes 字节数
     * @return 转换后的兆字节值
     */
    public static double bytesToMegabytes(long bytes) {
        final int BYTES_PER_KB = 1024;
        final int KB_PER_MB = 1024;

        return (double) bytes / (BYTES_PER_KB * KB_PER_MB);
    }
}