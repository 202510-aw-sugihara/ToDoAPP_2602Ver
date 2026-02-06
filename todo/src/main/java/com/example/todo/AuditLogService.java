package com.example.todo;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
  private final AuditLogMapper auditLogMapper;

  public AuditLogService(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String action, String detail) {
    AuditLog logEntry = AuditLog.builder()
        .action(action)
        .detail(detail)
        .username(resolveUsername())
        .createdAt(LocalDateTime.now())
        .build();
    auditLogMapper.insert(logEntry);
    log.info("AUDIT action={} detail={}", action, detail);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordAudit(AuditLog logEntry) {
    if (logEntry.getCreatedAt() == null) {
      logEntry.setCreatedAt(LocalDateTime.now());
    }
    auditLogMapper.insert(logEntry);
    log.info("AUDIT action={} username={}", logEntry.getAction(), logEntry.getUsername());
  }

  private String resolveUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    return authentication.getName();
  }
}
