package com.example.todo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET /todos: ステータス200とindexビューを返す")
  @WithMockUser(username = "user", roles = "USER")
  void getTodos_returnsIndexView() throws Exception {
    mockMvc.perform(get("/todos"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"));
  }

  @Test
  @DisplayName("POST /todos/confirm: ステータス200と確認ビューを返す")
  @WithMockUser(username = "user", roles = "USER")
  void postConfirm_returnsConfirmView() throws Exception {
    mockMvc.perform(post("/todos/confirm")
            .with(csrf())
            .param("author", "Tester")
            .param("title", "Integration Test")
            .param("detail", "Confirm flow")
            .param("dueDate", LocalDate.now().plusDays(1).toString())
            .param("priority", "MEDIUM"))
        .andExpect(status().isOk())
        .andExpect(view().name("todo/confirm"));
  }
}
