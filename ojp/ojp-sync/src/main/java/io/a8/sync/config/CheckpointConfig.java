package io.a8.sync.config;

/**
 * 检查点配置
 */
public class CheckpointConfig {
    private Long interval;
    private Long timeout;
    private Long minPause;
    private Integer tolerableFailureNum;

    // Getters and Setters
    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getMinPause() {
        return minPause;
    }

    public void setMinPause(Long minPause) {
        this.minPause = minPause;
    }

    public Integer getTolerableFailureNum() {
        return tolerableFailureNum;
    }

    public void setTolerableFailureNum(Integer tolerableFailureNum) {
        this.tolerableFailureNum = tolerableFailureNum;
    }
}
