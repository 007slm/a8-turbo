package org.openjdbcproxy.grpc.server.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 处理器责任链
 * 负责按顺序同步执行所有已注册的处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessorChain {
    
    private final ProcessorRegistry processorRegistry;
    
    /**
     * 执行处理器链
     */
    public <REQ, RESP> RESP execute(StatementServiceMethodName methodType, REQ request,
                                    ProcessorFunction<REQ, RESP> coreFunction) {
        
        // 创建处理上下文
        ProcessorContext<REQ, RESP> context = ProcessorContext.<REQ, RESP>builder()
                .methodType(methodType)
                .request(request)
                .startTime(System.currentTimeMillis())
                .build();
        
        // 获取支持该方法类型的处理器
        List<StatementServiceProcessor> processors = getProcessorsForMethod(methodType);
        
        try {
            // 1. 执行全局前置处理
            executeGlobalPreProcess(context, processors);
            
            // 2. 执行方法级别前置处理
            executeMethodPreProcess(methodType, context, processors);
            
            // 3. 执行核心业务逻辑
            log.debug("Executing core function for method: {}", methodType);
            RESP response = coreFunction.apply(request);
            context.setResponse(response);
            context.setEndTime(System.currentTimeMillis());
            
            // 4. 执行方法级别后置处理
            executeMethodPostProcess(methodType, context, processors);
            
            // 5. 执行全局后置处理
            executeGlobalPostProcess(context, processors);
            
            return response;
            
        } catch (Exception e) {
            context.setException(e);
            context.setEndTime(System.currentTimeMillis());
            
            // 执行全局异常处理
            executeGlobalExceptionProcess(context, processors);
            
            // 重新抛出异常
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    

    
    /**
     * 获取支持指定方法类型的处理器列表（按优先级排序）
     */
    private List<StatementServiceProcessor> getProcessorsForMethod(StatementServiceMethodName methodType) {
        return processorRegistry.getProcessors().stream()
                .filter(processor -> processor.supports(methodType))
                .sorted(Comparator.comparingInt(StatementServiceProcessor::getOrder))
                .toList();
    }
    
    // ========== 全局级别处理方法 ==========
    
    /**
     * 执行全局前置处理
     */
    private void executeGlobalPreProcess(ProcessorContext<?, ?> context, 
                                        List<StatementServiceProcessor> processors) {
        for (StatementServiceProcessor processor : processors) {
            try {
                processor.preProcess(context);
                log.trace("Executed global preProcess for processor: {}", processor.getName());
            } catch (Exception e) {
                log.error("Error in global preProcess for processor: {}", processor.getName(), e);
                // 前置处理出错不影响主流程，记录日志即可
            }
        }
    }
    
    /**
     * 执行全局后置处理（按相反顺序）
     */
    private void executeGlobalPostProcess(ProcessorContext<?, ?> context, 
                                         List<StatementServiceProcessor> processors) {
        // 后置处理按相反顺序执行
        for (int i = processors.size() - 1; i >= 0; i--) {
            StatementServiceProcessor processor = processors.get(i);
            try {
                processor.postProcess(context);
                log.trace("Executed global postProcess for processor: {}", processor.getName());
            } catch (Exception e) {
                log.error("Error in global postProcess for processor: {}", processor.getName(), e);
                // 后置处理出错不影响主流程，记录日志即可
            }
        }
    }
    
    /**
     * 执行全局异常处理
     */
    private void executeGlobalExceptionProcess(ProcessorContext<?, ?> context, 
                                              List<StatementServiceProcessor> processors) {
        for (StatementServiceProcessor processor : processors) {
            try {
                processor.onException(context);
                log.trace("Executed global onException for processor: {}", processor.getName());
            } catch (Exception e) {
                log.error("Error in global onException for processor: {}", processor.getName(), e);
                // 异常处理出错记录日志，不影响异常抛出
            }
        }
    }
    
    // ========== 方法级别处理方法 ==========
    
    /**
     * 执行方法级别前置处理
     */
    private void executeMethodPreProcess(StatementServiceMethodName methodType, ProcessorContext<?, ?> context,
                                         List<StatementServiceProcessor> processors) {
        for (StatementServiceProcessor processor : processors) {
            try {
                invokePreMethod(methodType, processor, context);
                log.trace("Executed preProcess for processor: {} on method: {}", 
                         processor.getName(), methodType);
            } catch (Exception e) {
                log.error("Error in preProcess for processor: {} on method: {}", 
                         processor.getName(), methodType, e);
                // 前置处理出错不影响主流程，记录日志即可
            }
        }
    }
    
    /**
     * 执行方法级别后置处理（按相反顺序）
     */
    private void executeMethodPostProcess(StatementServiceMethodName methodType, ProcessorContext<?, ?> context,
                                          List<StatementServiceProcessor> processors) {
        // 后置处理按相反顺序执行
        for (int i = processors.size() - 1; i >= 0; i--) {
            StatementServiceProcessor processor = processors.get(i);
            try {
                invokePostMethod(methodType, processor, context);
                log.trace("Executed postProcess for processor: {} on method: {}", 
                         processor.getName(), methodType);
            } catch (Exception e) {
                log.error("Error in postProcess for processor: {} on method: {}", 
                         processor.getName(), methodType, e);
                // 后置处理出错不影响主流程，记录日志即可
            }
        }
    }
    

    
    // ========== 方法调用工具方法 ==========
    /**
     * 调用处理器的前置处理方法
     */
    private void invokePreMethod(StatementServiceMethodName methodType, StatementServiceProcessor processor,
                                 ProcessorContext<?, ?> context) {
        switch (methodType) {
            case CONNECT -> processor.preConnect(context);
            case EXECUTE_UPDATE -> processor.preExecuteUpdate(context);
            case EXECUTE_QUERY -> processor.preExecuteQuery(context);
            case FETCH_NEXT_ROWS -> processor.preFetchNextRows(context);
            case CREATE_LOB -> processor.preCreateLob(context);
            case READ_LOB -> processor.preReadLob(context);
            case TERMINATE_SESSION -> processor.preTerminateSession(context);
            case START_TRANSACTION -> processor.preStartTransaction(context);
            case COMMIT_TRANSACTION -> processor.preCommitTransaction(context);
            case ROLLBACK_TRANSACTION -> processor.preRollbackTransaction(context);
            case CALL_RESOURCE -> processor.preCallResource(context);
            default -> log.warn("Unknown method type for preProcess: {}", methodType);
        }
    }
    
    /**
     * 调用处理器的后置处理方法
     */
    private void invokePostMethod(StatementServiceMethodName methodType, StatementServiceProcessor processor,
                                  ProcessorContext<?, ?> context) {
        switch (methodType) {
            case CONNECT -> processor.postConnect(context);
            case EXECUTE_UPDATE -> processor.postExecuteUpdate(context);
            case EXECUTE_QUERY -> processor.postExecuteQuery(context);
            case FETCH_NEXT_ROWS -> processor.postFetchNextRows(context);
            case CREATE_LOB -> processor.postCreateLob(context);
            case READ_LOB -> processor.postReadLob(context);
            case TERMINATE_SESSION -> processor.postTerminateSession(context);
            case START_TRANSACTION -> processor.postStartTransaction(context);
            case COMMIT_TRANSACTION -> processor.postCommitTransaction(context);
            case ROLLBACK_TRANSACTION -> processor.postRollbackTransaction(context);
            case CALL_RESOURCE -> processor.postCallResource(context);
            default -> log.warn("Unknown method type for postProcess: {}", methodType);
        }
    }
    
    /**
     * 调用处理器的异常处理方法
     * 已移除方法级别的异常处理，仅保留全局异常处理逻辑。
     */
    private void invokeExceptionMethod(StatementServiceMethodName methodType, StatementServiceProcessor processor,
                                       ProcessorContext<?, ?> context) {
        // 方法级别的异常处理已被移除，只保留全局异常处理
    }
    
    /**
     * 获取当前注册的处理器数量
     */
    public int getProcessorCount() {
        return processorRegistry.getProcessors().size();
    }
    
    /**
     * 获取支持指定方法的处理器数量
     */
    public int getProcessorCount(StatementServiceMethodName methodType) {
        return (int) processorRegistry.getProcessors().stream()
                .filter(processor -> processor.supports(methodType))
                .count();
    }
    
    /**
     * 核心业务逻辑函数式接口
     */
    @FunctionalInterface
    public interface ProcessorFunction<REQ, RESP> {
        RESP apply(REQ request) throws Exception;
    }
}