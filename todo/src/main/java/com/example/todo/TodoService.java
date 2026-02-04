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

  public TodoService(TodoRepository todoRepository, TodoMapper todoMapper,
      CategoryRepository categoryRepository) {
    this.todoRepository = todoRepository;
    this.todoMapper = todoMapper;
    this.categoryRepository = categoryRepository;
  }

  public List<Todo> findAll() {
    return todoRepository.findAllByOrderByCreatedAtDesc();
  }

  public Page<Todo> findPage(String keyword, String sort, String direction, Long categoryId,
      Pageable pageable) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    long total = todoMapper.count(safeKeyword, categoryId);
    List<Todo> content = todoMapper.search(
        safeKeyword,
        safeSort,
        safeDirection,
        categoryId,
        pageable.getPageSize(),
        (int) pageable.getOffset()
    );
    return new PageImpl<>(content, pageable, total);
  }

  public Optional<Todo> findById(long id) {
    return todoRepository.findById(id);
  }

  public TodoForm toForm(Todo todo) {
    return new TodoForm(
        todo.getId(),
        todo.getAuthor(),
        todo.getTitle(),
        todo.getDescription(),
        todo.getDueDate(),
        todo.getPriority(),
        todo.getCategory() != null ? todo.getCategory().getId() : null,
        todo.getCompleted(),
        todo.getVersion()
    );
  }

  @Transactional
  public Todo create(TodoForm form) {
    Todo todo = toEntity(form);
    return todoRepository.save(todo);
  }

  @Transactional
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
    return todoRepository.save(todo);
  }

  @Transactional
  public void deleteById(long id) {
    if (!todoRepository.existsById(id)) {
      throw new IllegalArgumentException("Todo not found: " + id);
    }
    todoRepository.deleteById(id);
  }

  @Transactional
  public int deleteByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }
    return todoMapper.deleteByIds(ids);
  }

  @Transactional
  public boolean toggleCompleted(long id) {
    Todo todo = todoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    boolean newValue = !Boolean.TRUE.equals(todo.getCompleted());
    todo.setCompleted(newValue);
    todoRepository.save(todo);
    return newValue;
  }

  private Todo toEntity(TodoForm form) {
    return Todo.builder()
        .author(form.getAuthor())
        .title(form.getTitle())
        .description(form.getDetail())
        .dueDate(form.getDueDate())
        .priority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM)
        .category(resolveCategory(form.getCategoryId()))
        .completed(false)
        .build();
  }

  private Category resolveCategory(Long categoryId) {
    if (categoryId == null) {
      return null;
    }
    return categoryRepository.findById(categoryId).orElse(null);
  }
}
