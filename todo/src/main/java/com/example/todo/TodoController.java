package com.example.todo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TodoController {

  // ToDo一覧画面を表示します。
  @GetMapping("/todos")
  public String list() {
    return "todo/list";
  }

  // ToDo新規作成画面を表示します。
  @GetMapping("/todos/new")
  public String newTodo() {
    return "todo/new";
  }

  // 指定IDのToDo詳細画面を表示します。
  @GetMapping("/todos/{id}")
  public String detail(@PathVariable("id") long id) {
    return "todo/detail";
  }
}
