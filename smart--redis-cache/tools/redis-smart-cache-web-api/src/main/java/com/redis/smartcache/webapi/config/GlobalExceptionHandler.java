package com.redis.smartcache.webapi.config;

import com.redis.smartcache.webapi.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的异常，返回标准的错误响应格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("参数验证失败: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数验证失败: " + message));
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("参数绑定失败: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数绑定失败: " + message));
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = String.format("缺少必需的请求参数: %s", e.getParameterName());
        
        logger.warn("缺少请求参数: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数类型不匹配: %s 应该是 %s 类型", 
                e.getName(), e.getRequiredType().getSimpleName());
        
        logger.warn("参数类型不匹配: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        String message = "请求体格式错误，请检查JSON格式";
        
        logger.warn("JSON解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("非法参数异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数错误: " + e.getMessage()));
    }

    /**
     * 处理RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统内部错误: " + e.getMessage()));
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        logger.error("未知异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统发生未知错误，请联系管理员"));
    }

    /**
     * 处理Redis连接异常
     */
    @ExceptionHandler({
        org.springframework.data.redis.RedisConnectionFailureException.class,
        io.lettuce.core.RedisConnectionException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleRedisConnectionException(Exception e) {
        logger.error("Redis连接异常", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Redis连接失败，请检查Redis服务状态"));
    }

    /**
     * 处理超时异常
     */
    @ExceptionHandler({
        java.util.concurrent.TimeoutException.class,
        io.lettuce.core.RedisCommandTimeoutException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleTimeoutException(Exception e) {
        logger.error("操作超时", e);
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ApiResponse.error("操作超时，请稍后重试"));
    }
}