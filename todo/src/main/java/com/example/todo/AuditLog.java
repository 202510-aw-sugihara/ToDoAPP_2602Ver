package com.example.todo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(length = 100)
  private String username;

  @Column(length = 100)
  private String targetType;

  @Column(length = 100)
  private String targetId;

  @Column(length = 1000)
  private String detail;

  @Column(length = 4000)
  private String beforeValue;

  @Column(length = 4000)
  private String afterValue;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
