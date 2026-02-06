package com.example.todo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoForm {

  private Long id;

  @NotBlank(message = "{todo.author.required}")
  @Size(max = 50, message = "{todo.author.size}")
  private String author;

  @NotBlank(message = "{todo.title.required}")
  @Size(max = 100, message = "{todo.title.size}")
  private String title;

  @Size(max = 500, message = "{todo.detail.size}")
  private String detail;

  @NotNull(message = "{todo.due_date.required}")
  @FutureOrPresent(message = "{todo.due_date.future}")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dueDate;

  @NotNull(message = "{todo.priority.required}")
  private Priority priority;

  private Long categoryId;

  private java.util.List<Long> groupIds;

  private Boolean completed;

  private Long version;

  private java.util.List<String> attachmentOriginalFilenames;

  private java.util.List<String> attachmentStoredFilenames;

  private java.util.List<String> attachmentContentTypes;

  private java.util.List<Long> attachmentSizes;
}
