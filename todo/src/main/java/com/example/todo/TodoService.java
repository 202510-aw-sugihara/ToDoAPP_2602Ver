package com.example.todo;

import java.util.List;
import java.util.Optional;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
  private final GroupRepository groupRepository;
  private final ConcurrentHashMap<Long, RecentSubmission> recentSubmissions = new ConcurrentHashMap<>();
  private static final long DUPLICATE_WINDOW_MS = 5000;

  public TodoService(TodoRepository todoRepository, TodoMapper todoMapper,
      CategoryRepository categoryRepository, AppUserRepository appUserRepository,
      AuditLogService auditLogService, MailService mailService,
      TodoAttachmentService todoAttachmentService, GroupRepository groupRepository) {
    this.todoRepository = todoRepository;
    this.todoMapper = todoMapper;
    this.categoryRepository = categoryRepository;
    this.appUserRepository = appUserRepository;
    this.auditLogService = auditLogService;
    this.mailService = mailService;
    this.todoAttachmentService = todoAttachmentService;
    this.groupRepository = groupRepository;
  }

  @Transactional(readOnly = true)
  public List<Todo> findAll(long userId) {
    return todoRepository.findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public Page<Todo> findPage(long userId, String keyword, String sort, String direction,
      Long categoryId, Long groupId, String status,
      Pageable pageable) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    TodoStatus safeStatus = parseStatus(status);
    List<Long> groupIds = resolveGroupFilterIds(groupId);
    List<Long> userGroupIds = resolveUserGroupIds(userId);
    long total = todoMapper.count(safeKeyword, userId, userGroupIds, categoryId, groupIds, safeStatus);
    List<Todo> content = todoMapper.search(
        safeKeyword,
        userId,
        userGroupIds,
        safeSort,
        safeDirection,
        categoryId,
        groupIds,
        safeStatus,
        pageable.getPageSize(),
        (int) pageable.getOffset());
    return new PageImpl<>(content, pageable, total);
  }

  @Transactional(readOnly = true)
  public List<Todo> findForExport(long userId, String keyword, String sort, String direction,
      Long categoryId, Long groupId, String status) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    TodoStatus safeStatus = parseStatus(status);
    List<Long> groupIds = resolveGroupFilterIds(groupId);
    List<Long> userGroupIds = resolveUserGroupIds(userId);
    long total = todoMapper.count(safeKeyword, userId, userGroupIds, categoryId, groupIds, safeStatus);
    if (total <= 0) {
      return List.of();
    }
    int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    return todoMapper.search(safeKeyword, userId, userGroupIds, safeSort, safeDirection, categoryId, groupIds, safeStatus, limit, 0);
  }

  @Transactional(readOnly = true)
  public List<Todo> findForExportByIds(long userId, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    AppUser user = resolveUser(userId);
    List<Todo> todos = todoRepository.findAllById(ids);
    todos.removeIf(todo -> todo.getDeletedAt() != null || !canAccess(todo, user));
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
    return todoRepository.findByIdAndDeletedAtIsNull(id);
  }

  @Transactional(readOnly = true)
  public Optional<Todo> findByIdIncludeDeleted(long id) {
    return todoRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Todo> findDeleted() {
    return todoRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
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
    if (todo.getGroups() != null && !todo.getGroups().isEmpty()) {
      java.util.List<Long> ids = todo.getGroups().stream()
          .filter(g -> g != null && g.getId() != null)
          .map(Group::getId)
          .toList();
      form.setGroupIds(ids);
    }
    form.setStatus(todo.getStatus());
    form.setVersion(todo.getVersion());
    return form;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_CREATE", targetType = "Todo")
  public Todo create(long userId, TodoForm form) {
    if (isDuplicateSubmission(userId, form)) {
      throw new DuplicateSubmissionException("duplicate");
    }
    AppUser user = resolveUser(userId);
    form.setAuthor(user.getUsername());
    Todo todo = toEntity(userId, form);
    Todo saved = todoRepository.save(todo);
    auditLogService.record("TODO_CREATE", "todoId=" + saved.getId() + ", userId=" + userId);
    mailService.sendTodoCreated(saved.getUser(), saved);
    return saved;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_UPDATE", targetType = "Todo")
  public Todo update(long id, TodoForm form) {
    Todo todo = todoRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    todo.setTitle(form.getTitle());
    todo.setDescription(form.getDetail());
    todo.setDueDate(form.getDueDate());
    todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
    todo.setCategory(resolveCategory(form.getCategoryId()));
    todo.setStatus(form.getStatus());
    todo.setGroups(resolveGroups(form.getGroupIds()));
    todo.setVersion(form.getVersion());
    Todo saved = todoRepository.save(todo);
    auditLogService.record("TODO_UPDATE", "todoId=" + saved.getId());
    return saved;
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_DELETE", targetType = "Todo")
  public void deleteById(long id) {
    Todo todo = todoRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    todo.setDeletedAt(java.time.LocalDateTime.now());
    todoRepository.save(todo);
    auditLogService.record("TODO_DELETE", "todoId=" + id);
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_RESTORE", targetType = "Todo")
  public void restoreById(long id) {
    Todo todo = todoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    if (todo.getDeletedAt() == null) {
      return;
    }
    todo.setDeletedAt(null);
    todoRepository.save(todo);
    auditLogService.record("TODO_RESTORE", "todoId=" + id);
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
  @Auditable(action = "TODO_DELETE", targetType = "Todo")
  public void deleteByIdHard(long id) {
    if (!todoRepository.existsById(id)) {
      throw new IllegalArgumentException("Todo not found: " + id);
    }
    todoAttachmentService.deleteByTodoId(id);
    todoRepository.deleteById(id);
    auditLogService.record("TODO_DELETE_HARD", "todoId=" + id);
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

  private Todo toEntity(long userId, TodoForm form) {
    return Todo.builder()
        .author(form.getAuthor())
        .title(form.getTitle())
        .description(form.getDetail())
        .dueDate(form.getDueDate())
        .priority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM)
        .category(resolveCategory(form.getCategoryId()))
        .user(resolveUser(userId))
        .groups(resolveGroupsForUser(form.getGroupIds(), userId))
        .status(form.getStatus())
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

  private java.util.Set<Group> resolveGroups(java.util.List<Long> groupIds) {
    if (groupIds == null || groupIds.isEmpty()) {
      return groupRepository.findByNameIgnoreCaseAndType("個人", GroupType.PROJECT)
          .map(java.util.Set::of)
          .orElse(java.util.Collections.emptySet());
    }
    java.util.List<Group> groups = groupRepository.findAllById(groupIds);
    return new java.util.HashSet<>(groups);
  }

  private java.util.Set<Group> resolveGroupsForUser(java.util.List<Long> groupIds, long userId) {
    if (groupIds == null || groupIds.isEmpty()) {
      AppUser user = resolveUser(userId);
      if (user.getDefaultGroups() != null && !user.getDefaultGroups().isEmpty()) {
        return new java.util.HashSet<>(user.getDefaultGroups());
      }
    }
    return resolveGroups(groupIds);
  }

  private List<Long> resolveUserGroupIds(long userId) {
    AppUser user = resolveUser(userId);
    if (user.getDefaultGroups() == null || user.getDefaultGroups().isEmpty()) {
      return List.of();
    }
    return user.getDefaultGroups().stream()
        .filter(g -> g != null && g.getId() != null)
        .map(Group::getId)
        .toList();
  }

  private boolean canAccess(Todo todo, AppUser user) {
    if (todo == null || user == null) {
      return false;
    }
    if (todo.getUser() != null && todo.getUser().getId() != null
        && todo.getUser().getId().longValue() == user.getId().longValue()) {
      return true;
    }
    if (todo.getGroups() == null || todo.getGroups().isEmpty()) {
      return false;
    }
    if (user.getDefaultGroups() == null || user.getDefaultGroups().isEmpty()) {
      return false;
    }
    for (Group todoGroup : todo.getGroups()) {
      if (todoGroup == null || todoGroup.getId() == null) {
        continue;
      }
      for (Group userGroup : user.getDefaultGroups()) {
        if (userGroup == null || userGroup.getId() == null) {
          continue;
        }
        if (todoGroup.getId().longValue() == userGroup.getId().longValue()) {
          return true;
        }
      }
    }
    return false;
  }

  private List<Long> resolveGroupFilterIds(Long groupId) {
    if (groupId == null) {
      return null;
    }
    Group selected = groupRepository.findById(groupId).orElse(null);
    if (selected == null) {
      return null;
    }
    if (selected.getType() == GroupType.PROJECT) {
      return List.of(selected.getId());
    }
    List<Group> allGroups = groupRepository.findAll();
    Map<Long, List<Group>> children = new HashMap<>();
    for (Group group : allGroups) {
      Long parentId = group.getParentId();
      if (parentId == null) {
        continue;
      }
      children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(group);
    }
    List<Long> projectIds = new ArrayList<>();
    ArrayDeque<Group> stack = new ArrayDeque<>();
    stack.push(selected);
    while (!stack.isEmpty()) {
      Group current = stack.pop();
      List<Group> kids = children.get(current.getId());
      if (kids == null) {
        continue;
      }
      for (Group child : kids) {
        if (child.getType() == GroupType.PROJECT) {
          if (child.getId() != null) {
            projectIds.add(child.getId());
          }
        } else {
          stack.push(child);
        }
      }
    }
    if (projectIds.isEmpty()) {
      return List.of(-1L);
    }
    return projectIds;
  }

  private TodoStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return TodoStatus.valueOf(status);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private boolean userIdEquals(Todo todo, long userId) {
    return todo.getUser() != null && todo.getUser().getId() != null
        && todo.getUser().getId().longValue() == userId;
  }

  private boolean isDuplicateSubmission(long userId, TodoForm form) {
    long now = System.currentTimeMillis();
    int hash = submissionHash(form);
    RecentSubmission previous = recentSubmissions.get(userId);
    if (previous != null && previous.hash == hash && (now - previous.timestamp) <= DUPLICATE_WINDOW_MS) {
      return true;
    }
    recentSubmissions.put(userId, new RecentSubmission(hash, now));
    return false;
  }

  private int submissionHash(TodoForm form) {
    String title = normalize(form.getTitle());
    String detail = normalize(form.getDetail());
    List<Long> groupIds = form.getGroupIds() == null ? new ArrayList<>() : new ArrayList<>(form.getGroupIds());
    Collections.sort(groupIds);
    List<String> attachments = form.getAttachmentStoredFilenames() == null
        ? new ArrayList<>()
        : new ArrayList<>(form.getAttachmentStoredFilenames());
    Collections.sort(attachments);
    return Objects.hash(
        title,
        detail,
        form.getDueDate(),
        form.getPriority(),
        form.getStatus(),
        form.getCategoryId(),
        groupIds,
        attachments
    );
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static final class RecentSubmission {
    private final int hash;
    private final long timestamp;

    private RecentSubmission(int hash, long timestamp) {
      this.hash = hash;
      this.timestamp = timestamp;
    }
  }
}

