package com.example.todo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/todos")
public class TodoController {

  private final TodoService todoService;
  private final CategoryRepository categoryRepository;

  public TodoController(TodoService todoService, CategoryRepository categoryRepository) {
    this.todoService = todoService;
    this.categoryRepository = categoryRepository;
  }

  @ModelAttribute("categories")
  public List<Category> categories() {
    return categoryRepository.findAll();
  }

  // ToDo一覧画面を表示します。
  @GetMapping
  public String list(@RequestParam(required = false) String keyword,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) Long categoryId,
      @PageableDefault(size = 10) Pageable pageable,
      Model model) {

    Page<Todo> page = todoService.findPage(keyword, sort, direction, categoryId, pageable);
    model.addAttribute("todos", page.getContent());
    model.addAttribute("page", page);
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("sort", sort == null ? "createdAt" : sort);
    model.addAttribute("direction", direction == null ? "desc" : direction);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("resultCount", page.getTotalElements());

    long total = page.getTotalElements();
    long start = total == 0 ? 0 : (page.getNumber() * (long) page.getSize()) + 1;
    long end = total == 0 ? 0 : Math.min(start + page.getSize() - 1, total);
    model.addAttribute("start", start);
    model.addAttribute("end", end);
    return "index";
  }

  // ToDo新規作成画面を表示します。
  @GetMapping("/new")
  public String newTodo(@ModelAttribute("todoForm") TodoForm form) {
    if (form.getDueDate() == null) {
      form.setDueDate(java.time.LocalDate.now().plusWeeks(1));
    }
    return "todo/new";
  }

  // フォーム送信を受け取り、確認画面に遷移します。
  @PostMapping("/confirm")
  public String confirm(@Valid @ModelAttribute("todoForm") TodoForm form, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return "todo/new";
    }
    return "todo/confirm";
  }

  // 確認画面から入力画面へ戻る際、入力値を保持してリダイレクトします。
  @PostMapping("/back")
  public String back(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("todoForm", form);
    return "redirect:/todos/new";
  }

  // 確認画面から登録を行い、一覧画面へリダイレクトします。
  @PostMapping("/complete")
  public String complete(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    todoService.create(form);
    redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
    return "redirect:/todos";
  }

  // 指定IDのToDo詳細画面を表示します。
  @GetMapping("/{id:\\d+}")
  public String detail(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
    Todo todo = todoService.findById(id).orElse(null);
    if (todo == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "指定されたToDoが見つかりませんでした。");
      return "redirect:/todos";
    }
    model.addAttribute("todo", todo);
    return "todo/detail";
  }

  // 指定IDのToDo編集画面を表示します。
  @GetMapping("/{id:\\d+}/edit")
  public String edit(@PathVariable("id") long id, Model model) {
    Todo todo = todoService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("todoForm", todoService.toForm(todo));
    return "todo/edit";
  }

  // 指定IDのToDoを更新し、一覧画面へリダイレクトします。
  @PostMapping("/{id:\\d+}/update")
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
  @DeleteMapping("/{id:\\d+}")
  public String delete(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
    try {
      todoService.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました。");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました。");
    }
    return "redirect:/todos";
  }

  // 選択したToDoを一括削除し、一覧画面へリダイレクトします。
  @PostMapping("/bulk-delete")
  public String bulkDelete(@RequestParam(name = "ids", required = false) List<Long> ids,
      RedirectAttributes redirectAttributes) {
    int deleted = todoService.deleteByIds(ids);
    if (deleted > 0) {
      redirectAttributes.addFlashAttribute("successMessage", "選択したToDoを削除しました。");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "削除対象が選択されていません。");
    }
    return "redirect:/todos";
  }

  // 指定IDのToDoの完了状態を反転します。
  @PostMapping("/{id:\\d+}/toggle")
  public Object toggle(@PathVariable("id") long id,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {

    boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

    try {
      boolean completed = todoService.toggleCompleted(id);
      if (ajax) {
        return ResponseEntity.ok(Map.of("completed", completed));
      }
      redirectAttributes.addFlashAttribute("successMessage", "完了状態を更新しました。");
      return "redirect:/todos";
    } catch (IllegalArgumentException ex) {
      if (ajax) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
      }
      redirectAttributes.addFlashAttribute("errorMessage", "対象のToDoが見つかりませんでした。");
      return "redirect:/todos";
    }
  }
}
