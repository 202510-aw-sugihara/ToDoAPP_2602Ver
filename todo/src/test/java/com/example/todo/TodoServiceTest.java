package com.example.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TodoServiceTest {

  @Autowired
  private TodoService todoService;

  @Autowired
  private TodoRepository todoRepository;

  @Autowired
  private AppUserRepository appUserRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @MockBean
  private TodoMapper todoMapper;

  @Test
  @DisplayName("create: 保存されたTodoが取得できる")
  void create_persistsTodo() {
    AppUser user = appUserRepository.save(AppUser.builder()
        .username("tester")
        .password("{noop}pass")
        .roles("ROLE_USER")
        .enabled(true)
        .build());
    Category category = categoryRepository.save(new Category(null, "Work", "#000000"));

    TodoForm form = new TodoForm();
    form.setAuthor("Alice");
    form.setTitle("Write tests");
    form.setDetail("basic service test");
    form.setDueDate(LocalDate.now().plusDays(1));
    form.setPriority(Priority.HIGH);
    form.setCategoryId(category.getId());

    Todo saved = todoService.create(user.getId(), form);

    assertThat(saved.getId()).isNotNull();
    Optional<Todo> found = todoRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    assertThat(found.get().getCategory().getId()).isEqualTo(category.getId());
  }

  @Test
  @DisplayName("findById: 保存済みTodoを取得できる")
  void findById_returnsTodo() {
    AppUser user = appUserRepository.save(AppUser.builder()
        .username("finder")
        .password("{noop}pass")
        .roles("ROLE_USER")
        .enabled(true)
        .build());
    Todo todo = Todo.builder()
        .author("Bob")
        .title("Find me")
        .description("desc")
        .dueDate(LocalDate.now().plusDays(3))
        .priority(Priority.MEDIUM)
        .user(user)
        .completed(false)
        .build();
    Todo saved = todoRepository.save(todo);

    Optional<Todo> found = todoService.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("Find me");
  }

  @Test
  @DisplayName("deleteById: 対象Todoが削除される")
  void deleteById_removesTodo() {
    AppUser user = appUserRepository.save(AppUser.builder()
        .username("deleter")
        .password("{noop}pass")
        .roles("ROLE_USER")
        .enabled(true)
        .build());
    Todo todo = Todo.builder()
        .author("Cara")
        .title("Delete me")
        .description("desc")
        .dueDate(LocalDate.now().plusDays(2))
        .priority(Priority.LOW)
        .user(user)
        .completed(false)
        .build();
    Todo saved = todoRepository.save(todo);

    todoService.deleteById(saved.getId());

    assertThat(todoRepository.findById(saved.getId())).isEmpty();
  }
}
