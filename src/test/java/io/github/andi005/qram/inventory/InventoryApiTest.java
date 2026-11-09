package io.github.andi005.qram.inventory;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andi005.qram.IntegrationTest;
import io.github.andi005.qram.TestData;
import io.github.andi005.qram.asset.Asset;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** An inventory session over the JSON API, as the phone page uses it. */
@IntegrationTest
@Transactional
class InventoryApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestData testData;
  @Autowired private InventoryService inventoryService;

  @AfterEach
  void logout() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpServletRequestBuilder scan(long sessionId, String raw) {
    return post("/api/inventory/{id}/scans", sessionId)
        .with(user("it").roles("IT_STAFF"))
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"raw\":\"" + raw + "\",\"manual\":false}");
  }

  @Test
  @DisplayName("Scans are classified, the lists update, and a finished session answers 409")
  void sessionFlow() throws Exception {
    TestData.loginAsAdmin();
    Asset a = testData.asset();
    Asset b = testData.asset();
    Asset elsewhere = testData.asset();
    long id = inventoryService.start("Test", null, a.getTag() + "\n" + b.getTag()).getId();
    SecurityContextHolder.clearContext();

    mockMvc
        .perform(scan(id, "https://old-host.example/a/" + a.getTag()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FOUND"))
        .andExpect(jsonPath("$.state.found.length()").value(1))
        .andExpect(jsonPath("$.state.missing[0].tag").value(b.getTag()));
    mockMvc.perform(scan(id, a.getTag())).andExpect(jsonPath("$.result").value("DUPLICATE"));
    mockMvc
        .perform(scan(id, elsewhere.getTag().toLowerCase()))
        .andExpect(jsonPath("$.result").value("UNEXPECTED"));
    mockMvc
        .perform(scan(id, "not a label"))
        .andExpect(jsonPath("$.result").value("UNKNOWN"))
        .andExpect(jsonPath("$.state.unknown").value(1));

    mockMvc
        .perform(post("/inventory/{id}/finish", id).with(user("it").roles("IT_STAFF")).with(csrf()))
        .andExpect(status().is3xxRedirection());
    mockMvc.perform(scan(id, b.getTag())).andExpect(status().isConflict());

    mockMvc
        .perform(get("/inventory/{id}/summary.csv", id).with(user("v").roles("VIEWER")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(content().string(Matchers.containsString(b.getTag())));
  }

  @Test
  @DisplayName("A VIEWER can't scan into a session")
  void viewerCannotScan() throws Exception {
    TestData.loginAsAdmin();
    Asset a = testData.asset();
    long id = inventoryService.start("Test", null, a.getTag()).getId();
    SecurityContextHolder.clearContext();

    mockMvc
        .perform(
            post("/api/inventory/{id}/scans", id)
                .with(user("v").roles("VIEWER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"raw\":\"" + a.getTag() + "\"}"))
        .andExpect(status().isForbidden());
  }
}
