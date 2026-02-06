package com.example.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"/schema-mybatis.sql", "/data-mybatis.sql"})
@Transactional
class TodoMapperTest {

  @Autowired
  private TodoMapper todoMapper;

  @Test
  @DisplayName("search: 条件に一致するTodoを取得できる")
  void search_returnsMatchingTodos() {
    List<Todo> todos = todoMapper.search("First", 1L, "createdAt", "desc", null, 10, 0);

    assertThat(todos).hasSize(1);
    assertThat(todos.get(0).getTitle()).isEqualTo("First task");
  }

  @Test
  @DisplayName("count: userId条件で件数を取得できる")
  void count_returnsTotal() {
    long total = todoMapper.count(null, 1L, null);

    assertThat(total).isEqualTo(2L);
  }

  @Test
  @DisplayName("deleteByIds: 指定IDのTodoを削除できる")
  void deleteByIds_removesRows() {
    int deleted = todoMapper.deleteByIds(List.of(1L, 2L), 1L);

    assertThat(deleted).isEqualTo(2);
    assertThat(todoMapper.count(null, 1L, null)).isZero();
  }
}
