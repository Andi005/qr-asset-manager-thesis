package io.github.andi005.qram.security;

import io.github.andi005.qram.common.BusinessRuleException;
import io.github.andi005.qram.common.Flash;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** User management pages (ADMIN only, see SecurityConfig and UserService). */
@Controller
@RequestMapping("/users")
class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  String list(Model model) {
    model.addAttribute("users", userService.findAll());
    model.addAttribute("roles", Role.values());
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", UserForm.empty());
    }
    return "users/list";
  }

  @PostMapping
  String create(
      @Valid @ModelAttribute("form") UserForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (!binding.hasErrors()) {
      try {
        userService.create(form);
        Flash.success(redirect, "user.created", form.username());
        return "redirect:/users";
      } catch (BusinessRuleException e) {
        e.addTo(binding);
      }
    }
    model.addAttribute("users", userService.findAll());
    model.addAttribute("roles", Role.values());
    return "users/list";
  }

  @PostMapping("/{id}/role")
  String changeRole(@PathVariable long id, @RequestParam Role role, RedirectAttributes redirect) {
    try {
      userService.changeRole(id, role);
      Flash.success(redirect, "user.updated");
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/users";
  }

  @PostMapping("/{id}/active")
  String setActive(
      @PathVariable long id, @RequestParam boolean active, RedirectAttributes redirect) {
    try {
      userService.setActive(id, active);
      Flash.success(redirect, "user.updated");
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/users";
  }

  @PostMapping("/{id}/password")
  String resetPassword(
      @PathVariable long id, @RequestParam String password, RedirectAttributes redirect) {
    try {
      userService.resetPassword(id, password);
      Flash.success(redirect, "user.passwordReset");
    } catch (BusinessRuleException e) {
      Flash.error(redirect, e);
    }
    return "redirect:/users";
  }
}
