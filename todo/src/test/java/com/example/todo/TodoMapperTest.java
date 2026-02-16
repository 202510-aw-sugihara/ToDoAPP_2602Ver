package com.example.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@MybatisTest(properties = "spring.sql.init.mode=never")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"/schema-mybatis.sql", "/data-mybatis.sql"})
@Transactional
class TodoMapperTest {

  @Autowired
  private TodoMapper todoMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("未削除ToDoが通常検索で取得できる")
  void search_returnsUndeletedTodos() {
    // 変更理由: TodoMapper.xml の通常検索条件（deleted_at IS NULL）に追従する。
    List<Todo> todos = todoMapper.search(null, 1L, List.of(), "createdAt", "desc", null, null, null, 20, 0);

    assertThat(todos).extracting(Todo::getTitle)
        .containsExactly("Visible task");
  }

  @Test
  @DisplayName("deleted_at入りToDoは通常検索で除外される")
  void search_excludesDeletedTodos() {
    List<Todo> todos = todoMapper.search("Deleted", 1L, List.of(), "createdAt", "desc", null, null, null, 20, 0);

    assertThat(todos).isEmpty();
  }

  @Test
  @DisplayName("削除済みToDoのみ抽出できる（管理画面前提の確認）")
  void deletedOnly_canBeSelected() {
    Long deletedCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM todos WHERE user_id = ? AND deleted_at IS NOT NULL", Long.class, 1L);

    assertThat(deletedCount).isEqualTo(1L);
  }

  @Test
  @DisplayName("復元後は通常検索に戻る")
  void restoredTodo_returnsToNormalSearch() {
    jdbcTemplate.update("UPDATE todos SET deleted_at = NULL WHERE id = ?", 2L);

    List<Todo> todos = todoMapper.search("Deleted", 1L, List.of(), "createdAt", "desc", null, null, null, 20, 0);

    assertThat(todos).hasSize(1);
    assertThat(todos.get(0).getId()).isEqualTo(2L);
  }
}
