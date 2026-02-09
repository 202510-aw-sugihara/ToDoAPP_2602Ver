package com.example.todo;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGroupController {

  private final GroupRepository groupRepository;
  private final MessageSource messageSource;

  public AdminGroupController(GroupRepository groupRepository, MessageSource messageSource) {
    this.groupRepository = groupRepository;
    this.messageSource = messageSource;
  }

  @GetMapping
  public String list(@ModelAttribute("groupForm") GroupForm form, Model model) {
    List<Group> groups = groupRepository.findAllByOrderByTypeAscNameAsc();
    List<Group> parentCandidates = groupRepository.findAllByOrderByTypeAscNameAsc();
    model.addAttribute("groups", groups);
    model.addAttribute("parentCandidates", parentCandidates);
    model.addAttribute("groupLabels", buildGroupLabels());
    return "admin/groups";
  }

  @PostMapping
  public String create(@Valid @ModelAttribute("groupForm") GroupForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    Group parent = resolveParent(form);
    validateParent(form, parent, bindingResult);
    if (bindingResult.hasErrors()) {
      List<Group> groups = groupRepository.findAllByOrderByTypeAscNameAsc();
      List<Group> parentCandidates = groupRepository.findAllByOrderByTypeAscNameAsc();
      model.addAttribute("groups", groups);
      model.addAttribute("parentCandidates", parentCandidates);
      model.addAttribute("groupLabels", buildGroupLabels());
      return "admin/groups";
    }
    Group group = Group.builder()
        .name(form.getName())
        .type(form.getType())
        .parent(parent)
        .color(form.getColor())
        .build();
    groupRepository.save(group);
    redirectAttributes.addFlashAttribute("successMessage", msg("msg.group_created"));
    return "redirect:/admin/groups";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable("id") long id, Model model,
      RedirectAttributes redirectAttributes) {
    Group group = groupRepository.findById(id).orElse(null);
    if (group == null) {
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.not_found"));
      return "redirect:/admin/groups";
    }
    GroupForm form = new GroupForm();
    form.setName(group.getName());
    form.setType(group.getType());
    form.setParentId(group.getParentId());
    form.setColor(group.getColor());
    model.addAttribute("group", group);
    model.addAttribute("groupForm", form);
    model.addAttribute("parentCandidates", groupRepository.findAllByOrderByTypeAscNameAsc());
    model.addAttribute("groupLabels", buildGroupLabels());
    return "admin/group_edit";
  }

  @PostMapping("/{id}/update")
  public String update(@PathVariable("id") long id,
      @Valid @ModelAttribute("groupForm") GroupForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    Group group = groupRepository.findById(id).orElse(null);
    if (group == null) {
      redirectAttributes.addFlashAttribute("errorMessage", msg("msg.not_found"));
      return "redirect:/admin/groups";
    }
    if (form.getParentId() != null && form.getParentId().longValue() == id) {
      bindingResult.rejectValue("parentId", "group.parent.invalid");
    }
    Group parent = resolveParent(form);
    validateParent(form, parent, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute("group", group);
      model.addAttribute("parentCandidates", groupRepository.findAllByOrderByTypeAscNameAsc());
      model.addAttribute("groupLabels", buildGroupLabels());
      return "admin/group_edit";
    }
    group.setName(form.getName());
    group.setType(form.getType());
    group.setParent(parent);
    group.setColor(form.getColor());
    groupRepository.save(group);
    redirectAttributes.addFlashAttribute("successMessage", msg("msg.group_updated"));
    return "redirect:/admin/groups";
  }

  private Group resolveParent(GroupForm form) {
    if (form.getParentId() == null) {
      return null;
    }
    return groupRepository.findById(form.getParentId()).orElse(null);
  }

  private void validateParent(GroupForm form, Group parent, BindingResult bindingResult) {
    if (form.getType() == null) {
      return;
    }
    if (form.getType() == GroupType.COMPANY || form.getType() == GroupType.CLIENT) {
      if (parent != null) {
        bindingResult.rejectValue("parentId", "group.parent.invalid");
      }
      return;
    }
    if (form.getType() == GroupType.DEPARTMENT) {
      if (parent == null || parent.getType() != GroupType.COMPANY) {
        bindingResult.rejectValue("parentId", "group.parent.invalid");
      }
      return;
    }
    if (form.getType() == GroupType.PROJECT) {
      if (parent == null || (parent.getType() != GroupType.DEPARTMENT
          && parent.getType() != GroupType.CLIENT)) {
        bindingResult.rejectValue("parentId", "group.parent.invalid");
      }
    }
  }

  private String msg(String code) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(code, null, locale);
  }

  private Map<Long, String> buildGroupLabels() {
    Map<Long, String> labels = new LinkedHashMap<>();
    for (Group group : groupRepository.findAllByOrderByTypeAscNameAsc()) {
      if (group == null || group.getId() == null) {
        continue;
      }
      labels.put(group.getId(), resolveGroupLabel(group));
    }
    return labels;
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
