package com.example.todo;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

  private final AppUserRepository appUserRepository;
  private final TodoRepository todoRepository;
  private final GroupRepository groupRepository;

  public ProfileController(AppUserRepository appUserRepository, TodoRepository todoRepository,
      GroupRepository groupRepository) {
    this.appUserRepository = appUserRepository;
    this.todoRepository = todoRepository;
    this.groupRepository = groupRepository;
  }

  @GetMapping("/profile")
  public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    AppUser user = resolveUser(userDetails);
    String name = user != null ? user.getUsername() : "Guest";
    String title = user != null ? normalizeRoles(user.getRoles()) : "Viewer";
    String status = user != null && Boolean.TRUE.equals(user.getEnabled()) ? "Active" : "Disabled";
    String email = user != null ? user.getEmail() : "-";
    String roles = user != null ? user.getRoles() : "-";

    List<Todo> ownedTodos = user != null
        ? todoRepository.findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId())
        : List.of();
    List<Long> groupIds = user != null && user.getDefaultGroups() != null
        ? user.getDefaultGroups().stream()
            .filter(g -> g != null && g.getId() != null)
            .map(Group::getId)
            .toList()
        : List.of();
    List<Todo> sharedTodos = user != null && !groupIds.isEmpty()
        ? todoRepository.findSharedByGroupIds(user.getId(), groupIds)
        : List.of();
    long todoCount = ownedTodos.size();
    List<String> groupNames = user != null && user.getDefaultGroups() != null
        ? user.getDefaultGroups().stream()
            .filter(g -> g != null && g.getName() != null)
            .map(Group::getName)
            .distinct()
            .sorted()
            .toList()
        : List.of();

    model.addAttribute("userName", name);
    model.addAttribute("userTitle", title);
    model.addAttribute("userStatus", status);
    model.addAttribute("userEmail", email);
    model.addAttribute("userRoles", roles);
    model.addAttribute("userGroups", groupNames);
    model.addAttribute("ownedTodos", ownedTodos);
    model.addAttribute("sharedTodos", sharedTodos);

    ProfileForm form = new ProfileForm();
    if (user != null) {
      form.setUsername(user.getUsername());
      form.setEmail(user.getEmail());
      form.setRoles(user.getRoles());
      form.setEnabled(user.getEnabled());
      if (user.getDefaultGroups() != null && !user.getDefaultGroups().isEmpty()) {
        form.setGroupIds(user.getDefaultGroups().stream()
            .filter(g -> g != null && g.getId() != null)
            .map(Group::getId)
            .toList());
      }
    }
    model.addAttribute("profileForm", form);
    model.addAttribute("groupOptions", buildGroupOptions());

    model.addAttribute("stats", List.of(
        Map.of("label", "Followers", "value", "0"),
        Map.of("label", "Following", "value", "0"),
        Map.of("label", "Posts", "value", String.valueOf(todoCount))
    ));

    model.addAttribute("activities", List.of(
        "Reviewed task board updates",
        "Joined Project Apollo discussion",
        "Planned next sprint tasks"
    ));

    model.addAttribute("timeline", List.of(
        Map.of("time", "09:10", "title", "Checked morning tasks", "detail", "Reviewed priorities"),
        Map.of("time", "11:45", "title", "Project sync", "detail", "Shared progress updates"),
        Map.of("time", "15:20", "title", "Task planning", "detail", "Updated upcoming work")
    ));

    return "profile";
  }

  @PostMapping("/profile")
  public String updateProfile(@Valid ProfileForm form, BindingResult bindingResult,
      @AuthenticationPrincipal UserDetails userDetails,
      RedirectAttributes redirectAttributes,
      Model model) {
    AppUser user = resolveUser(userDetails);
    if (user == null) {
      return "redirect:/login";
    }
    if (bindingResult.hasErrors()) {
      model.addAttribute("groupOptions", buildGroupOptions());
      return "profile";
    }
    user.setUsername(form.getUsername());
    user.setEmail(form.getEmail());
    user.setRoles(form.getRoles());
    user.setEnabled(form.getEnabled());
    if (form.getGroupIds() == null || form.getGroupIds().isEmpty()) {
      user.setDefaultGroups(new java.util.HashSet<>());
    } else {
      List<Group> groups = groupRepository.findAllById(form.getGroupIds());
      user.setDefaultGroups(new java.util.HashSet<>(groups));
    }
    appUserRepository.save(user);
    redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
    return "redirect:/profile";
  }

  private AppUser resolveUser(UserDetails userDetails) {
    if (userDetails == null) {
      return null;
    }
    return appUserRepository.findByUsername(userDetails.getUsername()).orElse(null);
  }

  private List<Map<String, Object>> buildGroupOptions() {
    List<Group> groups = groupRepository.findAllByOrderByTypeAscNameAsc();
    List<Map<String, Object>> options = new ArrayList<>();
    for (Group group : groups) {
      if (group == null || group.getId() == null) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", group.getId());
      row.put("name", group.getName());
      row.put("type", group.getType() == null ? null : group.getType().name());
      row.put("parentId", group.getParentId());
      row.put("color", group.getColor());
      options.add(row);
    }
    return options;
  }

  private String normalizeRoles(String roles) {
    if (roles == null || roles.isBlank()) {
      return "Member";
    }
    if (roles.contains("ROLE_ADMIN")) {
      return "Admin";
    }
    if (roles.contains("ROLE_USER")) {
      return "User";
    }
    return roles.replace("ROLE_", "");
  }
}
