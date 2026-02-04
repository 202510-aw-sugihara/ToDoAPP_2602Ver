package com.example.todo;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  public String confirm(@ModelAttribute("todoForm") TodoForm form) {
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
}
