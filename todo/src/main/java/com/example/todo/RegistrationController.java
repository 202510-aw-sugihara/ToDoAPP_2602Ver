package com.example.todo;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserDetailsService userDetailsService;

  public RegistrationController(AppUserRepository appUserRepository,
      PasswordEncoder passwordEncoder,
      UserDetailsService userDetailsService) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.userDetailsService = userDetailsService;
  }

  @GetMapping("/register")
  public String showRegister(@ModelAttribute("registrationForm") RegistrationForm form) {
    return "register";
  }

  @PostMapping("/register")
  public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes,
      HttpServletRequest request) {
    if (!form.getPassword().equals(form.getConfirmPassword())) {
      bindingResult.rejectValue("confirmPassword", "mismatch", "パスワードが一致しません。");
    }
    if (appUserRepository.findByUsername(form.getUsername()).isPresent()) {
      bindingResult.rejectValue("username", "duplicate", "このユーザー名は既に登録されています。");
    }

    if (bindingResult.hasErrors()) {
      return "register";
    }

    AppUser user = AppUser.builder()
        .username(form.getUsername())
        .password(passwordEncoder.encode(form.getPassword()))
        .roles("ROLE_USER")
        .build();
    appUserRepository.save(user);

    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    redirectAttributes.addFlashAttribute("successMessage", "ユーザー登録が完了しました。");
    return "redirect:/todos";
  }
}
