package com.example.todo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoForm {

  @NotBlank(message = "タイトルは必須です。")
  @Size(max = 100, message = "タイトルは100文字以内で入力してください。")
  private String title;

  @Size(max = 1000, message = "説明は1000文字以内で入力してください。")
  private String description;

  @NotNull(message = "期限日は必須です。")
  @FutureOrPresent(message = "期限日は今日以降の日付を指定してください。")
  private LocalDate dueDate;

  @NotNull(message = "優先度は必須です。")
  @Min(value = 1, message = "優先度は1以上を指定してください。")
  @Max(value = 5, message = "優先度は5以下を指定してください。")
  private Integer priority;
}
