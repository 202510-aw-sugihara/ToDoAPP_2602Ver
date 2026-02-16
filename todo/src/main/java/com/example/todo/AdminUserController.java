package com.example.todo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final GroupRepository groupRepository;
  private final MessageSource messageSource;

  public AdminUserController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
      GroupRepository groupRepository, MessageSource messageSource) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.groupRepository = groupRepository;
    this.messageSource = messageSource;
  }

  @GetMapping
  public String list(Model model) {
    List<AppUser> users = appUserRepository.findAll();
    model.addAttribute("users", users);
    return "admin/users";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable("id") long id, Model model,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    ProfileForm form = new ProfileForm();
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
    model.addAttribute("editUser", user);
    model.addAttribute("editForm", form);
    model.addAttribute("groupOptions", buildGroupOptions());
    return "admin/user_edit";
  }

  @PostMapping("/{id}/update")
  public String update(@PathVariable("id") long id,
      @Valid ProfileForm form,
      BindingResult bindingResult,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (bindingResult.hasErrors()) {
      return "redirect:/admin/users/" + id + "/edit";
    }

    String role = form.getRoles();
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    boolean enabled = form.getEnabled() != null && form.getEnabled();

    boolean isCurrentlyAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
    if (isCurrentlyAdmin && "ROLE_USER".equals(role)) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
        return "redirect:/admin/users";
      }
    }
    if (isCurrentlyAdmin && !enabled) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
        return "redirect:/admin/users";
      }
    }

    if (authentication != null && authentication.getName().equals(user.getUsername()) && !enabled) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }

    String nextUsername = form.getUsername() != null ? form.getUsername().trim() : "";
    if (nextUsername.isBlank()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users/" + id + "/edit";
    }
    if (!nextUsername.equals(user.getUsername())
        && appUserRepository.findByUsername(nextUsername).isPresent()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users/" + id + "/edit";
    }

    String nextEmail = form.getEmail() != null ? form.getEmail().trim() : "";
    if (nextEmail.isBlank()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users/" + id + "/edit";
    }

    Set<String> roles = parseRoles(user.getRoles());
    roles.remove("ROLE_ADMIN");
    roles.remove("ROLE_USER");
    roles.add(role);
    user.setRoles(String.join(",", roles));
    user.setEnabled(enabled);
    user.setUsername(nextUsername);
    user.setEmail(nextEmail);
    if (form.getGroupIds() == null || form.getGroupIds().isEmpty()) {
      user.setDefaultGroups(new java.util.HashSet<>());
    } else {
      List<Group> groups = groupRepository.findAllById(form.getGroupIds());
      user.setDefaultGroups(new java.util.HashSet<>(groups));
    }
    appUserRepository.save(user);
      redirectAttributes.addFlashAttribute("successMessage", "Updated.");
    return "redirect:/admin/users";
  }

  @PostMapping("/create")
  public String create(@RequestParam("username") String username,
      @RequestParam("email") String email,
      @RequestParam("password") String password,
      @RequestParam("role") String role,
      RedirectAttributes redirectAttributes) {
    if (username == null || username.isBlank() || password == null || password.isBlank()
        || email == null || email.isBlank()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (appUserRepository.findByUsername(username).isPresent()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }

    AppUser user = AppUser.builder()
        .username(username.trim())
        .email(email.trim())
        .password(passwordEncoder.encode(password))
        .roles(role)
        .enabled(true)
        .build();
    appUserRepository.save(user);
      redirectAttributes.addFlashAttribute("successMessage", "Updated.");
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/role")
  public String updateRole(@PathVariable("id") long id,
      @RequestParam("role") String role,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    boolean isCurrentlyAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
    if (isCurrentlyAdmin && "ROLE_USER".equals(role)) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
        return "redirect:/admin/users";
      }
    }
    Set<String> roles = parseRoles(user.getRoles());
    roles.remove("ROLE_ADMIN");
    roles.remove("ROLE_USER");
    roles.add(role);
    user.setRoles(String.join(",", roles));
    appUserRepository.save(user);
      redirectAttributes.addFlashAttribute("successMessage", "Updated.");
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable("id") long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (authentication != null && authentication.getName().equals(user.getUsername())) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
      return "redirect:/admin/users";
    }
    if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
      redirectAttributes.addFlashAttribute("errorMessage", "Operation failed.");
        return "redirect:/admin/users";
      }
    }
    appUserRepository.delete(user);
      redirectAttributes.addFlashAttribute("successMessage", "Updated.");
    return "redirect:/admin/users";
  }

  private Set<String> parseRoles(String roles) {
    if (roles == null || roles.isBlank()) {
      return Set.of();
    }
    return Set.of(roles.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toSet());
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
      row.put("label", resolveGroupLabel(group));
      row.put("type", group.getType() == null ? null : group.getType().name());
      row.put("parentId", group.getParentId());
      row.put("color", group.getColor());
      options.add(row);
    }
    return options;
  }

  private String resolveGroupLabel(Group group) {
    if (group == null) {
      return "";
    }
    String name = group.getName();
    if (name == null || name.isBlank()) {
      return "";
    }
    Locale locale = LocaleContextHolder.getLocale();
    String key = groupNameKey(name);
    if (key == null) {
      return name;
    }
    return messageSource.getMessage(key, null, name, locale);
  }

  private String groupNameKey(String name) {
    if (name == null) {
      return null;
    }
    String slug = name.trim()
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    if (!slug.isEmpty()) {
      return "group.name." + slug;
    }
    return "group.name." + name.trim();
  }
}






