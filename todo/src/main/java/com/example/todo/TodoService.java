package com.example.todo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

  private final TodoRepository todoRepository;

  public TodoService(TodoRepository todoRepository) {
    this.todoRepository = todoRepository;
  }

  public List<Todo> findAll() {
    return todoRepository.findAllByOrderByCreatedAtDesc();
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
