package io.a8.sync.config;

/**
 * 管道配置
 */
public class PipelineConfig {
    private String name;
    private Integer parallelism;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParallelism() {
        return parallelism;
    }

    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }
}