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

  @Transactional
  public Todo create(TodoForm form) {
    Todo todo = toEntity(form);
    return todoRepository.save(todo);
  }

  @Transactional
  public void deleteById(long id) {
    if (!todoRepository.existsById(id)) {
      throw new IllegalArgumentException("Todo not found: " + id);
    }
    todoRepository.deleteById(id);
  }

  private Todo toEntity(TodoForm form) {
    Integer priority = form.getPriority() != null ? form.getPriority() : 1;
    return Todo.builder()
        .title(form.getTitle())
        .description(form.getDescription())
        .dueDate(form.getDueDate())
        .priority(priority)
        .completed(false)
        .build();
  }
}
