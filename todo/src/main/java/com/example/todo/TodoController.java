package com.example.todo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TodoController {

  private final TodoService todoService;

  public TodoController(TodoService todoService) {
    this.todoService = todoService;
  }

  // ToDo一覧画面を表示します。
  @GetMapping("/todos")
  public String list(Model model) {
    List<Todo> todos = todoService.findAll();
    model.addAttribute("todos", todos);
    return "todo/list";
  }

  // ToDo新規作成画面を表示します。
  @GetMapping("/todos/new")
  public String newTodo(@ModelAttribute("todoForm") TodoForm form) {
    return "todo/new";
  }

  // フォーム送信を受け取り、確認画面に遷移します。
  @PostMapping("/todos/confirm")
  public String confirm(@Valid @ModelAttribute("todoForm") TodoForm form, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return "todo/new";
    }
    return "todo/confirm";
  }

  // 確認画面から入力画面へ戻る際、入力値を保持してリダイレクトします。
  @PostMapping("/todos/back")
  public String back(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("todoForm", form);
    return "redirect:/todos/new";
  }

  // 確認画面から登録を行い、一覧画面へリダイレクトします。
  @PostMapping("/todos/complete")
  public String complete(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    todoService.create(form);
    redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
    return "redirect:/todos";
  }

  // 指定IDのToDo詳細画面を表示します。
  @GetMapping("/todos/{id}")
  public String detail(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
    Todo todo = todoService.findById(id).orElse(null);
    if (todo == null) {
      redirectAttributes.addFlashAttribute("successMessage", "指定されたToDoが見つかりませんでした。");
      return "redirect:/todos";
    }
    model.addAttribute("todo", todo);
    return "todo/detail";
  }

  // 指定IDのToDo編集画面を表示します。
  @GetMapping("/todos/{id}/edit")
  public String edit(@PathVariable("id") long id, Model model) {
    Todo todo = todoService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("todoForm", todoService.toForm(todo));
    return "todo/edit";
  }

  // 指定IDのToDoを更新し、一覧画面へリダイレクトします。
  @PostMapping("/todos/{id}/update")
  public String update(@PathVariable("id") long id,
      @Valid @ModelAttribute("todoForm") TodoForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "todo/edit";
    }
    try {
      todoService.update(id, form);
    } catch (OptimisticLockingFailureException ex) {
      model.addAttribute("errorMessage", "他のユーザーが更新しています。再読み込みしてやり直してください。");
      return "todo/edit";
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
    return "redirect:/todos";
  }

  // 指定IDのToDoを削除し、一覧画面へリダイレクトします。
  @PostMapping("/todos/{id}/delete")
  public String delete(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
    try {
      todoService.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました。");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました。");
    }
    return "redirect:/todos";
  }

  // 指定IDのToDoの完了状態を反転します。
  @PostMapping("/todos/{id}/toggle")
  public Object toggle(@PathVariable("id") long id, HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    try {
      boolean completed = todoService.toggleCompleted(id);
      boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
      if (ajax) {
        return ResponseEntity.ok().body(java.util.Map.of("completed", completed));
      }
      redirectAttributes.addFlashAttribute("successMessage", "完了状態を更新しました。");
      return "redirect:/todos";
    } catch (IllegalArgumentException ex) {
      boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
      if (ajax) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "not_found"));
      }
      redirectAttributes.addFlashAttribute("errorMessage", "対象のToDoが見つかりませんでした。");
      return "redirect:/todos";
    }
  }
}
