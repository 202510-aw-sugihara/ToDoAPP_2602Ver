package com.example.todo;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoAttachmentMapper {

  int insert(TodoAttachment attachment);

  TodoAttachment findById(@Param("id") long id);

  List<TodoAttachment> findByTodoId(@Param("todoId") long todoId);

  int deleteById(@Param("id") long id);

  int deleteByTodoId(@Param("todoId") long todoId);
}
