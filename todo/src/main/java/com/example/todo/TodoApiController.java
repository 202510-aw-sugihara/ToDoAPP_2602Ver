package com.example.todo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
public class TodoApiController {

  private final TodoService todoService;
  private final AppUserRepository appUserRepository;

  public TodoApiController(TodoService todoService, AppUserRepository appUserRepository) {
    this.todoService = todoService;
    this.appUserRepository = appUserRepository;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<Todo>>> list(@AuthenticationPrincipal UserDetails userDetails) {
    long userId = requireUserId(userDetails);
    List<Todo> todos = todoService.findAll(userId);
    return ResponseEntity.ok(ApiResponse.ok("一覧取得に成功しました。", todos));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Todo>> find(@PathVariable("id") long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(id).orElse(null);
    if (todo == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("ToDoが見つかりません。"));
    }
    if (!isOwner(todo, requireUserId(userDetails))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("アクセス権限がありません。"));
    }
    return ResponseEntity.ok(ApiResponse.ok("取得に成功しました。", todo));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Todo>> create(@Valid @RequestBody TodoForm form,
      @AuthenticationPrincipal UserDetails userDetails) {
    long userId = requireUserId(userDetails);
    Todo created = todoService.create(userId, form);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok("作成に成功しました。", created));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Todo>> update(@PathVariable("id") long id,
      @Valid @RequestBody TodoForm form,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo existing = todoService.findById(id).orElse(null);
    if (existing == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("ToDoが見つかりません。"));
    }
    if (!isOwner(existing, requireUserId(userDetails))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("アクセス権限がありません。"));
    }
    Todo updated = todoService.update(id, form);
    return ResponseEntity.ok(ApiResponse.ok("更新に成功しました。", updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo existing = todoService.findById(id).orElse(null);
    if (existing == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("ToDoが見つかりません。"));
    }
    if (!isOwner(existing, requireUserId(userDetails))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("アクセス権限がありません。"));
    }
    todoService.deleteById(id);
    return ResponseEntity.ok(ApiResponse.ok("削除に成功しました。", null));
  }

  private long requireUserId(UserDetails userDetails) {
    if (userDetails == null) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return appUserRepository.findByUsername(userDetails.getUsername())
        .map(AppUser::getId)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private boolean isOwner(Todo todo, long userId) {
    return todo.getUser() != null && todo.getUser().getId() != null
        && todo.getUser().getId().longValue() == userId;
  }
}
