package io.github.andi005.qram.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.andi005.qram.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The role table (FR-12, NFR-04): endpoint × role → expected HTTP status. Each row is one check.
 * "302" for an anonymous visitor means "sent to the login page"; 401 is the JSON API's answer.
 */
@IntegrationTest
@Transactional
class RoleAccessTest {

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest(name = "{0} {1} as {2} → {3}")
  @CsvSource({
    // reading: every logged-in role
    "GET,  /,                    ANONYMOUS, 302",
    "GET,  /,                    VIEWER,    200",
    "GET,  /assets,              ANONYMOUS, 302",
    "GET,  /assets,              VIEWER,    200",
    "GET,  /expiring,            VIEWER,    200",
    "GET,  /licenses,            VIEWER,    200",
    "GET,  /employees,           VIEWER,    200",
    "GET,  /inventory,           VIEWER,    200",
    "GET,  /history,             VIEWER,    200",
    "GET,  /scan,                VIEWER,    200",
    "GET,  /a/IT-000001,         ANONYMOUS, 302",
    // forms that change data: IT_STAFF and up
    "GET,  /assets/new,          VIEWER,    403",
    "GET,  /assets/new,          IT_STAFF,  200",
    "GET,  /assets/new,          ADMIN,     200",
    "GET,  /licenses/new,        VIEWER,    403",
    "GET,  /inventory/new,       VIEWER,    403",
    "GET,  /labels,              VIEWER,    403",
    "GET,  /labels,              IT_STAFF,  200",
    "POST, /assets,              VIEWER,    403",
    "POST, /assets,              IT_STAFF,  200",
    "POST, /locations,           VIEWER,    403",
    "POST, /locations,           IT_STAFF,  302",
    // ADMIN only
    "GET,  /users,               IT_STAFF,  403",
    "GET,  /users,               ADMIN,     200",
    "GET,  /import,              VIEWER,    403",
    "GET,  /import,              IT_STAFF,  403",
    "GET,  /import,              ADMIN,     200",
    // the JSON API answers 401, never a redirect
    "GET,  /api/assets/by-tag/IT-000001, ANONYMOUS, 401",
    "GET,  /api/assets/by-tag/IT-999999, VIEWER,    404",
    // open to everyone
    "GET,  /actuator/health,     ANONYMOUS, 200",
    "GET,  /login,               ANONYMOUS, 200",
  })
  void roleTable(String method, String path, String role, int expected) throws Exception {
    MockHttpServletRequestBuilder request =
        method.equals("GET") ? get(path) : post(path).with(csrf()).param("name", "Salla test");
    if (!role.equals("ANONYMOUS")) {
      request.with(user("u-" + role.toLowerCase()).roles(role));
    }
    int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
    assertThat(status).as("%s %s as %s", method, path, role).isEqualTo(expected);
  }

  @ParameterizedTest(name = "POST {0} without a CSRF token → 403")
  @CsvSource({"/assets", "/locations", "/logout"})
  void postWithoutCsrfIsRefused(String path) throws Exception {
    int status =
        mockMvc
            .perform(post(path).with(user("admin-user").roles("ADMIN")))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(403);
  }
}
