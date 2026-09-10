package com.yuelock.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", true, "message", ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> conflict(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", true, "message", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
    String msg =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .orElse("参数无效");
    return ResponseEntity.badRequest().body(Map.of("error", true, "message", msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> other(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", true, "message", ex.getMessage() == null ? "服务器错误" : ex.getMessage()));
  }
}
