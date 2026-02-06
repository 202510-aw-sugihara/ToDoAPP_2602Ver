package com.example.todo;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

  List<Todo> search(@Param("keyword") String keyword,
      @Param("userId") Long userId,
      @Param("sort") String sort,
      @Param("direction") String direction,
      @Param("categoryId") Long categoryId,
      @Param("groupIds") List<Long> groupIds,
      @Param("limit") int limit,
      @Param("offset") int offset);

  long count(@Param("keyword") String keyword,
      @Param("userId") Long userId,
      @Param("categoryId") Long categoryId,
      @Param("groupIds") List<Long> groupIds);

  int deleteByIds(@Param("ids") List<Long> ids,
      @Param("userId") Long userId);
}
