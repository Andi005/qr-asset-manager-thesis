package io.github.andi005.qram.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Shows the login form. Spring Security itself handles the POST /login and /logout. */
@Controller
class LoginController {

  @GetMapping("/login")
  String login() {
    return "login";
  }
}
