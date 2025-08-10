import io.nats.client.*;
import io.nats.client.api.*;
import io.nats.client.impl.Headers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NATS Subject监控服务
 * 监控cdc-stream中每个subject的消息变化
 */
public class NATSSubjectMonitor {
    private Connection natsConnection;
    private JetStreamManagement jsm;
    private Map<String, SubjectStats> subjectStats = new ConcurrentHashMap<>();
    
    public NATSSubjectMonitor() throws Exception {
        // 连接到NATS服务器
        Options options = new Options.Builder()
            .server("nats://localhost:4222")
            .connectionTimeout(Duration.ofSeconds(5))
            .build();
            
        natsConnection = Nats.connect(options);
        jsm = natsConnection.jetStreamManagement();
    }
    
    /**
     * 启动监控服务
     */
    public void startMonitoring() throws Exception {
        System.out.println("启动NATS Subject监控服务...");
        
        // 订阅监控流中的所有消息
        JetStream jetStream = natsConnection.jetStream();
        PushSubscribeOptions pushSubscribeOptions = PushSubscribeOptions.builder()
            .stream("monitoring-stream")
            .orderedConsumer()
            .build();
        
        JetStreamSubscription subscription = jetStream.subscribe("monitoring.>", pushSubscribeOptions);
        
        System.out.println("开始监听监控流中的消息...");
        
        while (true) {
            Message msg = subscription.nextMessage(Duration.ofSeconds(1));
            if (msg != null) {
                handleMessageEvent(msg);
                msg.ack();
            }
        }
    }
    
    /**
     * 处理消息事件
     */
    private void handleMessageEvent(Message msg) {
        String subject = msg.getSubject();
        System.out.println("收到消息事件: " + subject);
        
        // 解析原始subject
        // 监控subject格式: monitoring.{originalSubject}
        if (subject.startsWith("monitoring.")) {
            String originalSubject = subject.substring("monitoring.".length());
            
            // 更新统计信息
            SubjectStats stats = subjectStats.computeIfAbsent(originalSubject, 
                k -> new SubjectStats(originalSubject));
            stats.incrementMessageCount();
            
            System.out.println("原始Subject: " + originalSubject + 
                             ", 消息总数: " + stats.getMessageCount());
            
            // 通知上层应用
            notifyApplication(originalSubject, stats.getMessageCount());
        }
    }
    
    /**
     * 通知上层应用
     */
    private void notifyApplication(String subject, long messageCount) {
        // 这里可以实现通知上层应用的逻辑
        // 例如通过HTTP API、WebSocket等方式
        System.out.println("通知应用 - Subject: " + subject + ", 消息数: " + messageCount);
    }
    
    /**
     * Subject统计信息
     */
    static class SubjectStats {
        private String subject;
        private long messageCount = 0;
        
        public SubjectStats(String subject) {
            this.subject = subject;
        }
        
        public synchronized void incrementMessageCount() {
            messageCount++;
        }
        
        public String getSubject() {
            return subject;
        }
        
        public long getMessageCount() {
            return messageCount;
        }
    }
    
    /**
     * 停止监控服务
     */
    public void stop() {
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
            NATSSubjectMonitor monitor = new NATSSubjectMonitor();
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
            
            // 启动监控
            monitor.startMonitoring();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}