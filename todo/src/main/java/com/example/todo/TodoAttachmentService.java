package com.example.todo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TodoAttachmentService {

  private final TodoAttachmentMapper todoAttachmentMapper;
  private final FileStorageService fileStorageService;

  public TodoAttachmentService(TodoAttachmentMapper todoAttachmentMapper,
      FileStorageService fileStorageService) {
    this.todoAttachmentMapper = todoAttachmentMapper;
    this.fileStorageService = fileStorageService;
  }

  @Transactional
  public TodoAttachment upload(Todo todo, MultipartFile file) {
    FileStorageService.StoredFile stored = fileStorageService.store(file);
    TodoAttachment attachment = TodoAttachment.builder()
        .todo(todo)
        .originalFilename(stored.originalFilename())
        .storedFilename(stored.storedFilename())
        .contentType(stored.contentType())
        .size(stored.size())
        .uploadedAt(LocalDateTime.now())
        .build();
    todoAttachmentMapper.insert(attachment);
    return attachment;
  }

  @Transactional
  public void attachStoredList(Todo todo, TodoForm form) {
    if (form.getAttachmentStoredFilenames() == null || form.getAttachmentStoredFilenames().isEmpty()) {
      return;
    }
    int count = form.getAttachmentStoredFilenames().size();
    for (int i = 0; i < count; i++) {
      String stored = form.getAttachmentStoredFilenames().get(i);
      if (stored == null || stored.isBlank()) {
        continue;
      }
      String original = safeListValue(form.getAttachmentOriginalFilenames(), i);
      String contentType = safeListValue(form.getAttachmentContentTypes(), i);
      Long size = safeListValueLong(form.getAttachmentSizes(), i);
      TodoAttachment attachment = TodoAttachment.builder()
          .todo(todo)
          .originalFilename(original != null ? original : stored)
          .storedFilename(stored)
          .contentType(contentType)
          .size(size == null ? 0L : size)
          .uploadedAt(LocalDateTime.now())
          .build();
      todoAttachmentMapper.insert(attachment);
    }
  }

  private String safeListValue(List<String> values, int index) {
    if (values == null || index < 0 || index >= values.size()) {
      return null;
    }
    return values.get(index);
  }

  private Long safeListValueLong(List<Long> values, int index) {
    if (values == null || index < 0 || index >= values.size()) {
      return null;
    }
    return values.get(index);
  }

  @Transactional(readOnly = true)
  public List<TodoAttachment> findByTodoId(long todoId) {
    return todoAttachmentMapper.findByTodoId(todoId);
  }

  @Transactional(readOnly = true)
  public TodoAttachment findById(long id) {
    return todoAttachmentMapper.findById(id);
  }

  @Transactional
  public void delete(TodoAttachment attachment) {
    todoAttachmentMapper.deleteById(attachment.getId());
    fileStorageService.delete(attachment.getStoredFilename());
  }

  @Transactional
  public void deleteByTodoId(long todoId) {
    List<TodoAttachment> attachments = todoAttachmentMapper.findByTodoId(todoId);
    for (TodoAttachment attachment : attachments) {
      if (attachment == null) {
        continue;
      }
      if (attachment.getId() != null) {
        todoAttachmentMapper.deleteById(attachment.getId());
      }
      if (attachment.getStoredFilename() != null && !attachment.getStoredFilename().isBlank()) {
        fileStorageService.delete(attachment.getStoredFilename());
      }
    }
  }
}
