package io.a8.sync.config;

import java.util.Map;

/**
 * 同步程序总配置类
 */
public class SyncConfig {
    private SourceConfig source;
    private SinkConfig sink;
    private PipelineConfig pipeline;
    private CheckpointConfig checkpoint;

    // Getters and Setters
    public SourceConfig getSource() {
        return source;
    }

    public void setSource(SourceConfig source) {
        this.source = source;
    }

    public SinkConfig getSink() {
        return sink;
    }

    public void setSink(SinkConfig sink) {
        this.sink = sink;
    }

    public PipelineConfig getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineConfig pipeline) {
        this.pipeline = pipeline;
    }

    public CheckpointConfig getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(CheckpointConfig checkpoint) {
        this.checkpoint = checkpoint;
    }
}




