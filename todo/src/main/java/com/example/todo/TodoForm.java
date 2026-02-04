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

  @NotBlank(message = "作成者は必須です。")
  @Size(max = 50, message = "作成者は50文字以内で入力してください。")
  private String author;

  @NotBlank(message = "タイトルは必須です。")
  @Size(max = 100, message = "タイトルは100文字以内で入力してください。")
  private String title;

  @Size(max = 500, message = "詳細は500文字以内で入力してください。")
  private String detail;

  @NotNull(message = "期限日は必須です。")
  @FutureOrPresent(message = "期限日は今日以降の日付を指定してください。")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dueDate;

  @NotNull(message = "優先度は必須です。")
  private Priority priority;

  private Boolean completed;

  private Long version;
}
