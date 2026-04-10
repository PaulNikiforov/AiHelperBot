package com.nikiforov.aichatbot.exceptionhandler;

import com.nikiforov.aichatbot.domain.exception.FeedbackNotFoundException;
import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.exceptionhandler.exception.CodedException;
import com.nikiforov.aichatbot.exceptionhandler.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private static final String ERROR_CODE = "errorCode"; 
    private static final String MESSAGE = "message";

    @ExceptionHandler(CodedException.class)
    public ResponseEntity<Map<String, Object>> handleCodedException(CodedException ex) {
        log.error("CodedException: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, ex.getErrorCode());
        body.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, Object>> handleLlmException(LlmException ex) {
        log.error("LLM service error: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, "LLM_ERROR");
        body.put(MESSAGE, "AI service temporarily unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFeedbackNotFound(FeedbackNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, "300000");
        body.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleLlmUnavailable(LlmUnavailableException ex) {
        log.error("LLM unavailable: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, "LLM_ERROR");
        body.put(MESSAGE, "AI service temporarily unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, "VALIDATION_FAILED");
        body.put(MESSAGE, "Validation failed");
        body.put("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ERROR_CODE, "INTERNAL_ERROR");
        body.put(MESSAGE, "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
