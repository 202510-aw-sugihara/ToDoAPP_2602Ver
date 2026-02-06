package com.example.todo;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper {

  int insert(AuditLog log);

  List<AuditLog> search(@Param("action") String action,
      @Param("username") String username,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("limit") int limit,
      @Param("offset") int offset);

  long count(@Param("action") String action,
      @Param("username") String username,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}
