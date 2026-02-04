package com.example.todo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TodoController {

  // ToDo一覧画面を表示します。
  @GetMapping("/todos")
  public String list() {
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

  // 確認画面から登録を行い、完了画面へリダイレクトします。
  @PostMapping("/todos/complete")
  public String complete(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("title", form.getTitle());
    return "redirect:/todos/complete";
  }

  // 完了画面を表示します。
  @GetMapping("/todos/complete")
  public String completePage() {
    return "todo/complete";
  }

  // 指定IDのToDo詳細画面を表示します。
  @GetMapping("/todos/{id}")
  public String detail(@PathVariable("id") long id) {
    return "todo/detail";
  }
}
