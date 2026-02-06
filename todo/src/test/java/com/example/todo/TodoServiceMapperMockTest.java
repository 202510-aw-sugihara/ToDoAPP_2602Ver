package com.example.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TodoServiceMapperMockTest {

  @Mock
  private TodoRepository todoRepository;

  @Mock
  private TodoMapper todoMapper;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AppUserRepository appUserRepository;

  @Mock
  private AuditLogService auditLogService;

  @InjectMocks
  private TodoService todoService;

  @Test
  @DisplayName("findPage: TodoMapperの検索結果がPageとして返る")
  void findPage_usesMapper() {
    Pageable pageable = PageRequest.of(0, 5);
    Todo todo1 = Todo.builder()
        .id(1L)
        .author("Alice")
        .title("First")
        .dueDate(LocalDate.now().plusDays(1))
        .priority(Priority.MEDIUM)
        .completed(false)
        .build();
    Todo todo2 = Todo.builder()
        .id(2L)
        .author("Bob")
        .title("Second")
        .dueDate(LocalDate.now().plusDays(2))
        .priority(Priority.HIGH)
        .completed(false)
        .build();

    when(todoMapper.count(null, 1L, null)).thenReturn(2L);
    when(todoMapper.search(null, 1L, "createdAt", "desc", null, 5, 0))
        .thenReturn(List.of(todo1, todo2));

    Page<Todo> page = todoService.findPage(1L, "", "", "", null, pageable);

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).hasSize(2);
    verify(todoMapper).count(null, 1L, null);
    verify(todoMapper).search(null, 1L, "createdAt", "desc", null, 5, 0);
  }

  @Test
  @DisplayName("deleteByIds: TodoMapperの削除結果を返し監査ログを記録する")
  void deleteByIds_recordsAudit() {
    List<Long> ids = List.of(10L, 11L);
    when(todoMapper.deleteByIds(ids, 1L)).thenReturn(2);

    int deleted = todoService.deleteByIds(1L, ids);

    assertThat(deleted).isEqualTo(2);
    verify(todoMapper).deleteByIds(ids, 1L);
    verify(auditLogService).record("TODO_BULK_DELETE", "count=2, userId=1");
  }
}
