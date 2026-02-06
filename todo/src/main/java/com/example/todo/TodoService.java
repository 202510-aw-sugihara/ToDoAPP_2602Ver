package com.example.todo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
public class TodoService {

  private final TodoRepository todoRepository;
  private final TodoMapper todoMapper;
  private final CategoryRepository categoryRepository;
  private final AppUserRepository appUserRepository;
  private final AuditLogService auditLogService;
  private final MailService mailService;
  private final TodoAttachmentService todoAttachmentService;

  public TodoService(TodoRepository todoRepository, TodoMapper todoMapper,
      CategoryRepository categoryRepository, AppUserRepository appUserRepository,
      AuditLogService auditLogService, MailService mailService,
      TodoAttachmentService todoAttachmentService) {
    this.todoRepository = todoRepository;
    this.todoMapper = todoMapper;
    this.categoryRepository = categoryRepository;
    this.appUserRepository = appUserRepository;
    this.auditLogService = auditLogService;
    this.mailService = mailService;
    this.todoAttachmentService = todoAttachmentService;
  }

  @Transactional(readOnly = true)
  public List<Todo> findAll(long userId) {
    return todoRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public Page<Todo> findPage(long userId, String keyword, String sort, String direction,
      Long categoryId,
      Pageable pageable) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    long total = todoMapper.count(safeKeyword, userId, categoryId);
    List<Todo> content = todoMapper.search(
        safeKeyword,
        userId,
        safeSort,
        safeDirection,
        categoryId,
        pageable.getPageSize(),
        (int) pageable.getOffset());
    return new PageImpl<>(content, pageable, total);
  }

  @Transactional(readOnly = true)
  public List<Todo> findForExport(long userId, String keyword, String sort, String direction,
      Long categoryId) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    long total = todoMapper.count(safeKeyword, userId, categoryId);
    if (total <= 0) {
      return List.of();
    }
    int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    return todoMapper.search(safeKeyword, userId, safeSort, safeDirection, categoryId, limit, 0);
  }

  @Transactional(readOnly = true)
  public List<Todo> findForExportByIds(long userId, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    List<Todo> todos = todoRepository.findAllById(ids);
    todos.removeIf(todo -> todo.getUser() == null || !userIdEquals(todo, userId));
    todos.sort((a, b) -> {
      if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
        return 0;
      }
      if (a.getCreatedAt() == null) {
        return 1;
      }
      if (b.getCreatedAt() == null) {
        return -1;
      }
      return b.getCreatedAt().compareTo(a.getCreatedAt());
    });
    return todos;
  }

  @Transactional(readOnly = true)
  public Optional<Todo> findById(long id) {
    return todoRepository.findById(id);
  }

  public TodoForm toForm(Todo todo) {
    TodoForm form = new TodoForm();
    form.setId(todo.getId());
    form.setAuthor(todo.getAuthor());
    form.setTitle(todo.getTitle());
    form.setDetail(todo.getDescription());
    form.setDueDate(todo.getDueDate());
    form.setPriority(todo.getPriority());
    form.setCategoryId(todo.getCategory() != null ? todo.getCategory().getId() : null);
    form.setCompleted(todo.getCompleted());
    form.setVersion(todo.getVersion());
    return form;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_CREATE", targetType = "Todo")
  public Todo create(long userId, TodoForm form) {
    Todo todo = toEntity(userId, form);
    Todo saved = todoRepository.save(todo);
    auditLogService.record("TODO_CREATE", "todoId=" + saved.getId() + ", userId=" + userId);
    mailService.sendTodoCreated(saved.getUser(), saved);
    return saved;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_UPDATE", targetType = "Todo")
  public Todo update(long id, TodoForm form) {
    Todo todo = todoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    todo.setAuthor(form.getAuthor());
    todo.setTitle(form.getTitle());
    todo.setDescription(form.getDetail());
    todo.setDueDate(form.getDueDate());
    todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
    todo.setCategory(resolveCategory(form.getCategoryId()));
    todo.setCompleted(Boolean.TRUE.equals(form.getCompleted()));
    todo.setVersion(form.getVersion());
    Todo saved = todoRepository.save(todo);
    auditLogService.record("TODO_UPDATE", "todoId=" + saved.getId());
    return saved;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_DELETE", targetType = "Todo")
  public void deleteById(long id) {
    if (!todoRepository.existsById(id)) {
      throw new IllegalArgumentException("Todo not found: " + id);
    }
    todoAttachmentService.deleteByTodoId(id);
    todoRepository.deleteById(id);
    auditLogService.record("TODO_DELETE", "todoId=" + id);
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  public int deleteByIds(long userId, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }
    int deleted = todoMapper.deleteByIds(ids, userId);
    auditLogService.record("TODO_BULK_DELETE", "count=" + deleted + ", userId=" + userId);
    return deleted;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  public boolean toggleCompleted(long id) {
    Todo todo = todoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    boolean newValue = !Boolean.TRUE.equals(todo.getCompleted());
    todo.setCompleted(newValue);
    todoRepository.save(todo);
    auditLogService.record("TODO_TOGGLE", "todoId=" + id + ", completed=" + newValue);
    return newValue;
  }

  private Todo toEntity(long userId, TodoForm form) {
    return Todo.builder()
        .author(form.getAuthor())
        .title(form.getTitle())
        .description(form.getDetail())
        .dueDate(form.getDueDate())
        .priority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM)
        .category(resolveCategory(form.getCategoryId()))
        .user(resolveUser(userId))
        .completed(false)
        .build();
  }

  private Category resolveCategory(Long categoryId) {
    if (categoryId == null) {
      return null;
    }
    return categoryRepository.findById(categoryId).orElse(null);
  }

  private AppUser resolveUser(long userId) {
    return appUserRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
  }

  private boolean userIdEquals(Todo todo, long userId) {
    return todo.getUser() != null && todo.getUser().getId() != null
        && todo.getUser().getId().longValue() == userId;
  }
}
