package com.example.todo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

  private final TodoRepository todoRepository;
  private final TodoMapper todoMapper;

  public TodoService(TodoRepository todoRepository, TodoMapper todoMapper) {
    this.todoRepository = todoRepository;
    this.todoMapper = todoMapper;
  }

  public List<Todo> findAll() {
    return todoRepository.findAllByOrderByCreatedAtDesc();
  }

  public List<Todo> findAll(String keyword, String sort, String direction) {
    String safeSort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
    String safeDirection = (direction == null || direction.isBlank()) ? "desc" : direction;
    String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    return todoMapper.search(safeKeyword, safeSort, safeDirection);
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
    todo.setPriority(form.getPriority());
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
  public boolean toggleCompleted(long id) {
    Todo todo = todoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    boolean newValue = !Boolean.TRUE.equals(todo.getCompleted());
    todo.setCompleted(newValue);
    todoRepository.save(todo);
    return newValue;
  }

  private Todo toEntity(TodoForm form) {
    Integer priority = form.getPriority() != null ? form.getPriority() : 1;
    return Todo.builder()
        .author(form.getAuthor())
        .title(form.getTitle())
        .description(form.getDetail())
        .dueDate(form.getDueDate())
        .priority(priority)
        .completed(false)
        .build();
  }
}
