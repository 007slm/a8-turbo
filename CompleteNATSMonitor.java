import io.nats.client.*;
import io.nats.client.api.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 完整的NATS监控方案
 * 监控cdc-stream中每个subject的生产和消费状态
 */
public class CompleteNATSMonitor {
    private Connection natsConnection;
    private JetStreamManagement jsm;
    private ScheduledExecutorService scheduler;
    
    public CompleteNATSMonitor() throws Exception {
        // 连接到NATS服务器
        Options options = new Options.Builder()
            .server("nats://localhost:4222")
            .connectionTimeout(Duration.ofSeconds(5))
            .build();
            
        natsConnection = Nats.connect(options);
        jsm = natsConnection.jetStreamManagement();
        scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 启动完整监控
     */
    public void startCompleteMonitoring() {
        System.out.println("启动完整NATS监控...");
        
        // 启动流状态监控（定期检查）
        scheduler.scheduleAtFixedRate(this::monitorStreamState, 0, 5, TimeUnit.SECONDS);
        
        // 启动消费者状态监控（定期检查）
        scheduler.scheduleAtFixedRate(this::monitorConsumerStates, 0, 3, TimeUnit.SECONDS);
    }
    
    /**
     * 监控流状态
     */
    private void monitorStreamState() {
        try {
            StreamInfo streamInfo = jsm.getStreamInfo("cdc-stream");
            System.out.println("=== 流状态 ===");
            System.out.println("流名称: " + streamInfo.getStreamName());
            System.out.println("消息总数: " + streamInfo.getStreamState().getMsgCount());
            
            // 显示每个subject的消息数
            for (Map.Entry<String, Long> entry : streamInfo.getStreamState().getSubjects().entrySet()) {
                System.out.println("  Subject: " + entry.getKey() + ", 消息数: " + entry.getValue());
            }
            
        } catch (Exception e) {
            System.err.println("监控流状态时出错: " + e.getMessage());
        }
    }
    
    /**
     * 监控消费者状态
     */
    private void monitorConsumerStates() {
        try {
            List<ConsumerInfo> consumers = jsm.getConsumerInfoList("cdc-stream");
            System.out.println("=== 消费者状态 ===");
            
            for (ConsumerInfo consumerInfo : consumers) {
                String consumerName = consumerInfo.getName();
                ConsumerStats stats = consumerInfo.getStats();
                
                System.out.println("消费者: " + consumerName);
                System.out.println("  待处理消息数: " + stats.getNumPending());
                System.out.println("  已确认消息数: " + stats.getAckFloor().getConsumerSeq());
                System.out.println("  最后活跃时间: " + stats.getLastActivity());
                
                // 通知应用消费者状态变化
                notifyConsumerStateChange(consumerName, stats.getNumPending());
            }
            
        } catch (Exception e) {
            System.err.println("监控消费者状态时出错: " + e.getMessage());
        }
    }
    
    /**
     * 通知消费者状态变化
     */
    private void notifyConsumerStateChange(String consumerName, long pendingMessages) {
        // 解析表名（假设消费者命名格式为 flink-cdc-consumer-{tableName}）
        if (consumerName.startsWith("flink-cdc-consumer-")) {
            String tableName = consumerName.substring("flink-cdc-consumer-".length());
            System.out.println("表 " + tableName + " 待处理消息数: " + pendingMessages);
            
            // 这里可以发送通知到你的监控系统
            // 例如通过HTTP API、消息队列等方式
        }
    }
    
    /**
     * 停止监控服务
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (natsConnection != null) {
            try {
                natsConnection.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            CompleteNATSMonitor monitor = new CompleteNATSMonitor();
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
            
            // 启动完整监控
            monitor.startCompleteMonitoring();
            
            System.out.println("NATS监控服务已启动，按Ctrl+C退出...");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}