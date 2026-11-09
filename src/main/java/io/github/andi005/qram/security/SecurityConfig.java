package io.github.andi005.qram.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Who may open which page. Two layers protect every action:
 *
 * <ol>
 *   <li>URL rules here (checked before any controller runs);
 *   <li>{@code @PreAuthorize} on the service methods that change data.
 * </ol>
 *
 * Hiding buttons in the templates ({@code sec:authorize}) is only convenience, never protection.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Pages that only show a form for changing data. Viewers can't use them, so they don't see them.
   */
  private static final String[] EDIT_FORMS = {
    "/assets/new",
    "/assets/*/edit",
    "/assets/*/assign",
    "/employees/new",
    "/employees/*/edit",
    "/licenses/new",
    "/licenses/*/edit",
    "/inventory/new",
    "/labels",
  };

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth ->
            auth.requestMatchers(
                    "/login", "/css/**", "/js/**", "/webjars/**", "/favicon.ico", "/error")
                .permitAll()
                .requestMatchers("/actuator/health")
                .permitAll()
                .requestMatchers("/users/**", "/import/**")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, EDIT_FORMS)
                .hasRole("IT_STAFF")
                // Reading is open to every logged-in user (VIEWER and up).
                .requestMatchers(HttpMethod.GET, "/**")
                .hasRole("VIEWER")
                // Everything else changes data (POST): IT_STAFF and up.
                .anyRequest()
                .hasRole("IT_STAFF"))
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
        // Not logged in: the JSON API answers 401; every page redirects to the login page,
        // whatever the client sends as Accept header (phone camera apps, curl, browsers).
        .exceptionHandling(ex ->
            ex.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE));
    return http.build();
  }

  /** ADMIN can do everything IT_STAFF can, and IT_STAFF everything VIEWER can. */
  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN")
        .implies("IT_STAFF")
        .role("IT_STAFF")
        .implies("VIEWER")
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
