package com.example.mentor.exception;

import com.example.mentor.dto.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：返回 code=409 等业务码；HTTP 状态建议 200 或等同业务码二选一
    // 如需所有业务错误都 http 200：把 HttpStatus.OK 改为 OK。
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<?>> handleBiz(BizException ex) {
        log.warn("BizException: code={}, errorCode={}, msg={}", ex.getCode(), ex.getErrorCode(), ex.getMessage());
        Result<?> body = Result.buildFailure(ex.getCode(), ex.getErrorCode(), ex.getMessage());
        // 业务错误使用 200 更利于前端统一处理；若希望语义化，也可用 HttpStatus.valueOf(ex.getCode())
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    // 参数校验错误（JSR-303）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleMethodArgNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.buildFailure(400, "400", msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<?>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.buildFailure(400, "400", msg));
    }

    // 常见请求错误
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Result<?>> handleBadRequest(Exception ex) {
        log.warn("BadRequest: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.buildFailure(400, "400", ex.getMessage()));
    }

        // 如需 403 处理，可在实际抛出处返回 BizException(403, ...) 或
        // 抛出 org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN)
        // 这里不依赖 spring-security，避免无意引入安全自动配置导致端点被拦截。

    // 兜底
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.buildFailure(500, "500", "服务器错误"));
    }
}
