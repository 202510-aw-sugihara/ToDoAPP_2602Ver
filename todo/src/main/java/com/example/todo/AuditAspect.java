package com.example.todo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  public AuditAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
    this.auditLogService = auditLogService;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(auditable)")
  public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
    String beforeValue = serialize(joinPoint.getArgs());
    String username = resolveUsername();
    String targetId = resolveTargetId(joinPoint.getArgs());

    try {
      Object result = joinPoint.proceed();
      String afterValue = serialize(result);
      auditLogService.recordAudit(AuditLog.builder()
          .action(auditable.action())
          .username(username)
          .targetType(blankToNull(auditable.targetType()))
          .targetId(targetId)
          .beforeValue(beforeValue)
          .afterValue(afterValue)
          .build());
      return result;
    } catch (Throwable ex) {
      auditLogService.recordAudit(AuditLog.builder()
          .action(auditable.action())
          .username(username)
          .targetType(blankToNull(auditable.targetType()))
          .targetId(targetId)
          .beforeValue(beforeValue)
          .afterValue("EXCEPTION: " + ex.getClass().getSimpleName() + " - " + ex.getMessage())
          .build());
      throw ex;
    }
  }

  private String serialize(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.warn("audit serialize failed: {}", ex.getMessage());
      return String.valueOf(value);
    }
  }

  private String resolveUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    return authentication.getName();
  }

  private String resolveTargetId(Object[] args) {
    if (args == null || args.length == 0) {
      return null;
    }
    return Arrays.stream(args)
        .filter(arg -> arg instanceof Number || arg instanceof String)
        .map(String::valueOf)
        .findFirst()
        .orElse(null);
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
