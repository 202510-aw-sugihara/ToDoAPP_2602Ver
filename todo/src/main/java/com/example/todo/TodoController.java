package com.example.todo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("todoForm")
@RequestMapping("/todos")
public class TodoController {

  private final TodoService todoService;
  private final CategoryRepository categoryRepository;
  private final AppUserRepository appUserRepository;
  private final TodoAttachmentService todoAttachmentService;
  private final FileStorageService fileStorageService;
  private final MessageSource messageSource;
  private static final Pattern STORED_NAME_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}.*$");

  public TodoController(TodoService todoService, CategoryRepository categoryRepository,
      AppUserRepository appUserRepository, TodoAttachmentService todoAttachmentService,
      FileStorageService fileStorageService, MessageSource messageSource) {
    this.todoService = todoService;
    this.categoryRepository = categoryRepository;
    this.appUserRepository = appUserRepository;
    this.todoAttachmentService = todoAttachmentService;
    this.fileStorageService = fileStorageService;
    this.messageSource = messageSource;
  }

  private String msg(String code) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(code, null, locale);
  }

  @ModelAttribute("todoForm")
  public TodoForm todoForm() {
    return new TodoForm();
  }

  @ModelAttribute("categories")
  public List<Category> categories() {
    return categoryRepository.findAll();
  }

  @ModelAttribute("categoryLabels")
  public Map<Long, String> categoryLabels() {
    Map<Long, String> labels = new LinkedHashMap<>();
    Locale locale = LocaleContextHolder.getLocale();
    for (Category category : categoryRepository.findAll()) {
      if (category == null || category.getId() == null) {
        continue;
      }
      String fallback = category.getName();
      String label = messageSource.getMessage("category." + category.getId(), null, fallback, locale);
      labels.put(category.getId(), label);
    }
    return labels;
  }

  @GetMapping
  public String list(@RequestParam(required = false) String keyword,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) Long categoryId,
      @PageableDefault(size = 10) Pageable pageable,
      @AuthenticationPrincipal UserDetails userDetails,
      Model model) {

    long userId = requireUserId(userDetails);
    Page<Todo> page = todoService.findPage(userId, keyword, sort, direction, categoryId, pageable);
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

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String keyword,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) List<Long> ids,
      @AuthenticationPrincipal UserDetails userDetails) {
    long userId = requireUserId(userDetails);
    List<Todo> todos = (ids != null && !ids.isEmpty())
        ? todoService.findForExportByIds(userId, ids)
        : todoService.findForExport(userId, keyword, sort, direction, categoryId);
    if (todos.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    StringBuilder csv = new StringBuilder();
    csv.append("ID,タイトル,登録者,ステータス,作成日\r\n");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    for (Todo todo : todos) {
      csv.append(csvCell(todo.getId() == null ? "" : String.valueOf(todo.getId()))).append(",");
      csv.append(csvCell(todo.getTitle())).append(",");
      csv.append(csvCell(todo.getAuthor())).append(",");
      csv.append(csvCell(todo.isCompleted() ? "完了" : "未完了")).append(",");
      String createdAt = todo.getCreatedAt() == null ? "" : todo.getCreatedAt().format(dateFormatter);
      csv.append(csvCell(createdAt)).append("\r\n");
    }

    byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
    byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    byte[] bytes = new byte[bom.length + body.length];
    System.arraycopy(bom, 0, bytes, 0, bom.length);
    System.arraycopy(body, 0, bytes, bom.length, body.length);

    String filename = "todo_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  private String csvCell(String value) {
    if (value == null) {
      return "";
    }
    boolean needsQuote = value.contains(",") || value.contains("\"")
        || value.contains("\r") || value.contains("\n");
    String escaped = value.replace("\"", "\"\"");
    return needsQuote ? "\"" + escaped + "\"" : escaped;
  }

  @GetMapping("/new")
  public String newTodo(@ModelAttribute("todoForm") TodoForm form) {
    if (form.getDueDate() == null) {
      form.setDueDate(java.time.LocalDate.now().plusWeeks(1));
    }
    return "todo/new";
  }

  @PostMapping("/confirm")
  public String confirm(@Valid @ModelAttribute("todoForm") TodoForm form,
      BindingResult bindingResult,
      @RequestParam(name = "files", required = false) List<MultipartFile> files) {
    if (files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty())) {
      if (form.getAttachmentStoredFilenames() != null) {
        for (String stored : form.getAttachmentStoredFilenames()) {
          if (stored != null && !stored.isBlank()) {
            fileStorageService.delete(stored);
          }
        }
      }
      java.util.ArrayList<String> originals = new java.util.ArrayList<>();
      java.util.ArrayList<String> storedNames = new java.util.ArrayList<>();
      java.util.ArrayList<String> types = new java.util.ArrayList<>();
      java.util.ArrayList<Long> sizes = new java.util.ArrayList<>();
      for (MultipartFile file : files) {
        if (file == null || file.isEmpty()) {
          continue;
        }
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        originals.add(stored.originalFilename());
        storedNames.add(stored.storedFilename());
        types.add(stored.contentType());
        sizes.add(stored.size());
      }
      form.setAttachmentOriginalFilenames(originals);
      form.setAttachmentStoredFilenames(storedNames);
      form.setAttachmentContentTypes(types);
      form.setAttachmentSizes(sizes);
    }
    if (bindingResult.hasErrors()) {
      return "todo/new";
    }
    return "todo/confirm";
  }

  @PostMapping("/back")
  public String back(@ModelAttribute("todoForm") TodoForm form, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("todoForm", form);
    return "redirect:/todos/new";
  }

  @PostMapping("/complete")
  public String complete(@ModelAttribute("todoForm") TodoForm form,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails,
      SessionStatus sessionStatus) {
    long userId = requireUserId(userDetails);
    Todo created = todoService.create(userId, form);
    todoAttachmentService.attachStoredList(created, form);
    redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_saved"));
    sessionStatus.setComplete();
    return "redirect:/todos";
  }

  @GetMapping("/{id:\\d+}")
  public String detail(@PathVariable("id") long id, Model model,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(id).orElse(null);
    if (todo == null) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    ensureOwner(todo, requireUserId(userDetails));
    model.addAttribute("attachments", todoAttachmentService.findByTodoId(todo.getId()));
    model.addAttribute("todo", todo);
    return "todo/detail";
  }

  @GetMapping("/{id:\\d+}/edit")
  public String edit(@PathVariable("id") long id, Model model,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    ensureOwner(todo, requireUserId(userDetails));
    model.addAttribute("attachments", todoAttachmentService.findByTodoId(todo.getId()));
    model.addAttribute("todoForm", todoService.toForm(todo));
    return "todo/edit";
  }

  @PostMapping("/{id:\\d+}/update")
  public String update(@PathVariable("id") long id,
      @Valid @ModelAttribute("todoForm") TodoForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam(name = "files", required = false) List<MultipartFile> files) {

    if (bindingResult.hasErrors()) {
      return "todo/edit";
    }

    try {
      Todo existing = todoService.findById(id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      ensureOwner(existing, requireUserId(userDetails));
      todoService.update(id, form);
      if (files != null) {
        for (MultipartFile file : files) {
          if (file == null || file.isEmpty()) {
            continue;
          }
          todoAttachmentService.upload(existing, file);
        }
      }
    } catch (OptimisticLockingFailureException ex) {
      model.addAttribute("errorMessage", msg("msg.concurrent_update"));
      return "todo/edit";
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_updated"));
    return "redirect:/todos/update-complete?title=" + java.net.URLEncoder.encode(form.getTitle(), StandardCharsets.UTF_8);
  }

  @GetMapping("/update-complete")
  public String updateComplete(@RequestParam(name = "title", required = false) String title,
      Model model) {
    model.addAttribute("title", title);
    model.addAttribute("message", msg("msg.success_updated"));
    return "todo/complete";
  }

  @DeleteMapping("/{id:\\d+}")
  public String delete(@PathVariable("id") long id,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {
    try {
      Todo existing = todoService.findById(id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      ensureOwner(existing, requireUserId(userDetails));
      todoService.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_deleted"));
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.not_found"));
    }
    return "redirect:/todos";
  }

  @PostMapping("/{id:\\d+}/attachments")
  public String uploadAttachment(@PathVariable("id") long id,
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "redirect", required = false) String redirect,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(id)
        .orElseThrow(() -> new TodoNotFoundException(msg("msg.not_found")));
    ensureOwner(todo, requireUserId(userDetails));
    if (file == null || file.isEmpty()) {
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.file_select"));
      if ("edit".equalsIgnoreCase(redirect)) {
        return "redirect:/todos/" + id + "/edit";
      }
      return "redirect:/todos/" + id;
    }
    todoAttachmentService.upload(todo, file);
    redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_updated"));
    if ("edit".equalsIgnoreCase(redirect)) {
      return "redirect:/todos/" + id + "/edit";
    }
    return "redirect:/todos/" + id;
  }

  @GetMapping("/{todoId:\\d+}/attachments/{attachmentId:\\d+}")
  public ResponseEntity<Resource> downloadAttachment(@PathVariable("todoId") long todoId,
      @PathVariable("attachmentId") long attachmentId,
      @AuthenticationPrincipal UserDetails userDetails) {
    return attachmentResource(todoId, attachmentId, true, userDetails);
  }

  @GetMapping("/{todoId:\\d+}/attachments/{attachmentId:\\d+}/content")
  public ResponseEntity<Resource> attachmentContent(@PathVariable("todoId") long todoId,
      @PathVariable("attachmentId") long attachmentId,
      @RequestParam(name = "download", defaultValue = "false") boolean download,
      @AuthenticationPrincipal UserDetails userDetails) {
    return attachmentResource(todoId, attachmentId, download, userDetails);
  }

  @GetMapping("/{todoId:\\d+}/attachments/{attachmentId:\\d+}/preview")
  public String previewAttachmentPage(@PathVariable("todoId") long todoId,
      @PathVariable("attachmentId") long attachmentId,
      Model model,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(todoId)
        .orElseThrow(() -> new TodoNotFoundException(msg("msg.not_found")));
    ensureOwner(todo, requireUserId(userDetails));
    TodoAttachment attachment = todoAttachmentService.findById(attachmentId);
    if (attachment == null || attachment.getTodo() == null
        || !attachment.getTodo().getId().equals(todoId)) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    String type = attachment.getContentType() == null || attachment.getContentType().isBlank()
        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
        : attachment.getContentType();
    String previewUrl = "/todos/" + todoId + "/attachments/" + attachmentId + "/content?download=false";
    String downloadUrl = "/todos/" + todoId + "/attachments/" + attachmentId + "/content?download=true";
    model.addAttribute("filename", attachment.getOriginalFilename());
    model.addAttribute("contentType", type);
    model.addAttribute("previewUrl", previewUrl);
    model.addAttribute("downloadUrl", downloadUrl);
    return "todo/attachment_preview";
  }

  @GetMapping("/attachments/temp/{stored}")
  public ResponseEntity<Resource> previewTempAttachment(@PathVariable("stored") String stored,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "contentType", required = false) String contentType,
      @RequestParam(name = "download", defaultValue = "false") boolean download,
      @AuthenticationPrincipal UserDetails userDetails) {
    requireUserId(userDetails);
    if (stored == null || stored.contains("..") || stored.contains("/") || stored.contains("\\")
        || !STORED_NAME_PATTERN.matcher(stored).matches()) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    Resource resource = fileStorageService.loadAsResource(stored);
    String safeName = (name == null || name.isBlank()) ? stored : name;
    String type = (contentType == null || contentType.isBlank())
        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
        : contentType;
    ContentDisposition disposition = download
        ? ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build()
        : ContentDisposition.inline().filename(safeName, StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(type))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(resource);
  }

  @GetMapping("/attachments/temp/{stored}/preview")
  public String previewTempAttachmentPage(@PathVariable("stored") String stored,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "contentType", required = false) String contentType,
      Model model,
      @AuthenticationPrincipal UserDetails userDetails) {
    requireUserId(userDetails);
    if (stored == null || stored.contains("..") || stored.contains("/") || stored.contains("\\")
        || !STORED_NAME_PATTERN.matcher(stored).matches()) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    String safeName = (name == null || name.isBlank()) ? stored : name;
    String type = (contentType == null || contentType.isBlank())
        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
        : contentType;
    String previewUrl = "/todos/attachments/temp/" + stored + "?name=" + java.net.URLEncoder.encode(safeName, StandardCharsets.UTF_8)
        + "&contentType=" + java.net.URLEncoder.encode(type, StandardCharsets.UTF_8)
        + "&download=false";
    String downloadUrl = "/todos/attachments/temp/" + stored + "?name=" + java.net.URLEncoder.encode(safeName, StandardCharsets.UTF_8)
        + "&contentType=" + java.net.URLEncoder.encode(type, StandardCharsets.UTF_8)
        + "&download=true";
    model.addAttribute("filename", safeName);
    model.addAttribute("contentType", type);
    model.addAttribute("previewUrl", previewUrl);
    model.addAttribute("downloadUrl", downloadUrl);
    return "todo/attachment_preview";
  }

  @DeleteMapping("/{todoId:\\d+}/attachments/{attachmentId:\\d+}")
  public String deleteAttachment(@PathVariable("todoId") long todoId,
      @PathVariable("attachmentId") long attachmentId,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {
    Todo todo = todoService.findById(todoId)
        .orElseThrow(() -> new TodoNotFoundException(msg("msg.not_found")));
    ensureOwner(todo, requireUserId(userDetails));
    TodoAttachment attachment = todoAttachmentService.findById(attachmentId);
    if (attachment == null || attachment.getTodo() == null
        || !attachment.getTodo().getId().equals(todoId)) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    todoAttachmentService.delete(attachment);
    redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_deleted"));
    return "redirect:/todos/" + todoId;
  }

  @PostMapping("/bulk-delete")
  public String bulkDelete(@RequestParam(name = "ids", required = false) List<Long> ids,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {
    long userId = requireUserId(userDetails);
    int deleted = todoService.deleteByIds(userId, ids);
    if (deleted > 0) {
      redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_deleted"));
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.bulk_delete_none"));
    }
    return "redirect:/todos";
  }

  @PostMapping("/{id:\\d+}/toggle")
  public Object toggle(@PathVariable("id") long id,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @AuthenticationPrincipal UserDetails userDetails) {

    boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

    try {
      Todo existing = todoService.findById(id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      ensureOwner(existing, requireUserId(userDetails));
      if (!Boolean.TRUE.equals(existing.getCompleted())) {
        if (ajax) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "not_allowed"));
        }
        redirectAttributes.addFlashAttribute("errorMessage", "未完了から完了への変更はできません。");
        return "redirect:/todos";
      }
      boolean completed = todoService.toggleCompleted(id);
      if (ajax) {
        return ResponseEntity.ok(Map.of("completed", completed));
      }
      redirectAttributes.addFlashAttribute("successMessage", msg("msg.success_updated"));
      return "redirect:/todos";
    } catch (ResponseStatusException ex) {
      if (ajax) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", "forbidden"));
      }
      throw ex;
    } catch (IllegalArgumentException ex) {
      if (ajax) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
      }
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.not_found"));
      return "redirect:/todos";
    }
  }

  private long requireUserId(UserDetails userDetails) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return appUserRepository.findByUsername(userDetails.getUsername())
        .map(AppUser::getId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private void ensureOwner(Todo todo, long userId) {
    if (todo.getUser() == null || todo.getUser().getId() == null
        || todo.getUser().getId().longValue() != userId) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  private ResponseEntity<Resource> attachmentResource(long todoId, long attachmentId,
      boolean download, UserDetails userDetails) {
    Todo todo = todoService.findById(todoId)
        .orElseThrow(() -> new TodoNotFoundException(msg("msg.not_found")));
    ensureOwner(todo, requireUserId(userDetails));
    TodoAttachment attachment = todoAttachmentService.findById(attachmentId);
    if (attachment == null || attachment.getTodo() == null
        || !attachment.getTodo().getId().equals(todoId)) {
      throw new TodoNotFoundException(msg("msg.not_found"));
    }
    Resource resource = fileStorageService.loadAsResource(attachment.getStoredFilename());
    String contentType = attachment.getContentType() != null && !attachment.getContentType().isBlank()
        ? attachment.getContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    ContentDisposition disposition = download
        ? ContentDisposition.attachment()
          .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
          .build()
        : ContentDisposition.inline()
          .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
          .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(resource);
  }
}
