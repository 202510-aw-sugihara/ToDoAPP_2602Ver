package com.example.todo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AdminUserController {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminUserController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public String list(Model model) {
    List<AppUser> users = appUserRepository.findAll();
    model.addAttribute("users", users);
    return "admin/users";
  }

  @PostMapping("/create")
  public String create(@RequestParam("username") String username,
      @RequestParam("password") String password,
      @RequestParam("role") String role,
      RedirectAttributes redirectAttributes) {
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      redirectAttributes.addFlashAttribute("errorMessage", "ユーザー名とパスワードは必須です。");
      return "redirect:/admin/users";
    }
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      redirectAttributes.addFlashAttribute("errorMessage", "不正な権限指定です。");
      return "redirect:/admin/users";
    }
    if (appUserRepository.findByUsername(username).isPresent()) {
      redirectAttributes.addFlashAttribute("errorMessage", "このユーザー名は既に登録されています。");
      return "redirect:/admin/users";
    }

    AppUser user = AppUser.builder()
        .username(username.trim())
        .password(passwordEncoder.encode(password))
        .roles(role)
        .build();
    appUserRepository.save(user);
    redirectAttributes.addFlashAttribute("successMessage", "ユーザーを作成しました。");
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/role")
  public String updateRole(@PathVariable("id") long id,
      @RequestParam("role") String role,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "ユーザーが見つかりません。");
      return "redirect:/admin/users";
    }
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      redirectAttributes.addFlashAttribute("errorMessage", "不正な権限指定です。");
      return "redirect:/admin/users";
    }
    boolean isCurrentlyAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
    if (isCurrentlyAdmin && "ROLE_USER".equals(role)) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
        redirectAttributes.addFlashAttribute("errorMessage", "管理者は最低1人必要です。");
        return "redirect:/admin/users";
      }
    }
    Set<String> roles = parseRoles(user.getRoles());
    roles.remove("ROLE_ADMIN");
    roles.remove("ROLE_USER");
    roles.add(role);
    user.setRoles(String.join(",", roles));
    appUserRepository.save(user);
    redirectAttributes.addFlashAttribute("successMessage", "権限を更新しました。");
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable("id") long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(id).orElse(null);
    if (user == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "ユーザーが見つかりません。");
      return "redirect:/admin/users";
    }
    if (authentication != null && authentication.getName().equals(user.getUsername())) {
      redirectAttributes.addFlashAttribute("errorMessage", "自分自身は削除できません。");
      return "redirect:/admin/users";
    }
    if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) {
      long adminCount = appUserRepository.countByRolesContaining("ROLE_ADMIN");
      if (adminCount <= 1) {
        redirectAttributes.addFlashAttribute("errorMessage", "管理者は最低1人必要です。");
        return "redirect:/admin/users";
      }
    }
    appUserRepository.delete(user);
    redirectAttributes.addFlashAttribute("successMessage", "ユーザーを削除しました。");
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
}
