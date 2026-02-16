package com.example.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

  @Mock
  private MailService mailService;

  @Mock
  private TodoAttachmentService todoAttachmentService;

  @Mock
  private GroupRepository groupRepository;

  @InjectMocks
  private TodoService todoService;

  @Test
  @DisplayName("findPage: 現行TodoMapperシグネチャで検索結果をPage化する")
  void findPage_usesMapper() {
    // 変更理由: 本体が userGroupIds/categoryId/groupIds/status を受ける実装に追従する。
    Pageable pageable = PageRequest.of(0, 5);
    AppUser user = AppUser.builder().id(1L).defaultGroups(new HashSet<>()).build();

    Todo todo1 = Todo.builder()
        .id(1L)
        .author("Alice")
        .title("First")
        .dueDate(LocalDate.now().plusDays(1))
        .priority(Priority.MEDIUM)
        .status(TodoStatus.IN_PROGRESS)
        .build();
    Todo todo2 = Todo.builder()
        .id(2L)
        .author("Bob")
        .title("Second")
        .dueDate(LocalDate.now().plusDays(2))
        .priority(Priority.HIGH)
        .status(TodoStatus.PLANNED)
        .build();

    when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
    when(todoMapper.count(null, 1L, List.of(), null, null, null)).thenReturn(2L);
    when(todoMapper.search(null, 1L, List.of(), "createdAt", "desc", null, null, null, 5, 0))
        .thenReturn(List.of(todo1, todo2));

    Page<Todo> page = todoService.findPage(1L, "", "", "", null, null, null, pageable);

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).hasSize(2);
    verify(todoMapper).count(null, 1L, List.of(), null, null, null);
    verify(todoMapper).search(null, 1L, List.of(), "createdAt", "desc", null, null, null, 5, 0);
  }

  @Test
  @DisplayName("deleteByIds: TodoMapperの論理削除結果を監査ログへ記録する")
  void deleteByIds_recordsAudit() {
    List<Long> ids = List.of(10L, 11L);
    when(todoMapper.deleteByIds(ids, 1L)).thenReturn(2);

    int deleted = todoService.deleteByIds(1L, ids);

    assertThat(deleted).isEqualTo(2);
    verify(todoMapper).deleteByIds(ids, 1L);
    verify(auditLogService).record("TODO_BULK_DELETE", "count=2, userId=1");
  }

  @Test
  @DisplayName("deleteByIds: IDsが空なら削除も監査も行わない")
  void deleteByIds_emptyIds_returnsZero() {
    // 変更理由: 現行実装の境界条件（null/emptyで即時return）を明示する。
    int deleted = todoService.deleteByIds(1L, List.of());

    assertThat(deleted).isZero();
    verify(todoMapper, never()).deleteByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyLong());
    verify(auditLogService, never()).record(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }
}
