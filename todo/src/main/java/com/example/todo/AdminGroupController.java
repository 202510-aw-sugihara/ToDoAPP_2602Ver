package com.example.todo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGroupController {

  private final GroupRepository groupRepository;

  public AdminGroupController(GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
  }

  @GetMapping
  public String list(@ModelAttribute("groupForm") GroupForm form, Model model) {
    List<Group> groups = groupRepository.findAllByOrderByTypeAscNameAsc();
    model.addAttribute("groups", groups);
    return "admin/groups";
  }

  @PostMapping
  public String create(@Valid @ModelAttribute("groupForm") GroupForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      List<Group> groups = groupRepository.findAllByOrderByTypeAscNameAsc();
      model.addAttribute("groups", groups);
      return "admin/groups";
    }
    Group group = Group.builder()
        .name(form.getName())
        .type(form.getType())
        .color(form.getColor())
        .build();
    groupRepository.save(group);
    redirectAttributes.addFlashAttribute("successMessage", "所属グループを作成しました。");
    return "redirect:/admin/groups";
  }
}
