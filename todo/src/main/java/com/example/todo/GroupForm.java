package com.example.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupForm {

  @NotBlank(message = "{group.name.required}")
  @Size(max = 100, message = "{group.name.size}")
  private String name;

  @NotNull(message = "{group.type.required}")
  private GroupType type;

  private Long parentId;

  @NotBlank(message = "{group.color.required}")
  private String color;
}
