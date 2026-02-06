package com.example.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogAdminController {

  private final AuditLogMapper auditLogMapper;

  public AuditLogAdminController(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  @GetMapping
  public String list(@RequestParam(name = "action", required = false) String action,
      @RequestParam(name = "username", required = false) String username,
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      Model model) {

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = safePage * safeSize;

    LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
    LocalDateTime toAt = to == null ? null : to.atTime(LocalTime.MAX);

    long total = auditLogMapper.count(trimOrNull(action), trimOrNull(username), fromAt, toAt);
    List<AuditLog> logs = auditLogMapper.search(trimOrNull(action), trimOrNull(username), fromAt,
        toAt, safeSize, offset);

    model.addAttribute("logs", logs);
    model.addAttribute("action", action == null ? "" : action);
    model.addAttribute("username", username == null ? "" : username);
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);
    model.addAttribute("total", total);

    long start = total == 0 ? 0 : (safePage * (long) safeSize) + 1;
    long end = total == 0 ? 0 : Math.min(start + safeSize - 1, total);
    model.addAttribute("start", start);
    model.addAttribute("end", end);

    return "admin/audit_logs";
  }

  private String trimOrNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
