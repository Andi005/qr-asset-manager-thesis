package io.github.andi005.qram;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the whole application once against a real PostgreSQL. Starting at all proves that the
 * Flyway migrations run and that Hibernate's {@code validate} accepts every entity against them.
 */
@IntegrationTest
class QrAssetManagerApplicationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("The health endpoint reports UP without a login")
  void healthIsUp() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("Bootstrap and html5-qrcode are served by the app itself, not from a CDN")
  void frontEndLibrariesAreServedLocally() throws Exception {
    mockMvc
        .perform(get("/webjars/bootstrap/css/bootstrap.min.css"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("Bootstrap")));
    mockMvc.perform(get("/webjars/html5-qrcode/html5-qrcode.min.js")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("The login page is in Albanian")
  void loginPageIsAlbanian() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("Kyçuni")));
  }
}
