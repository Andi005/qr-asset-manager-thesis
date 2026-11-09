package io.github.andi005.qram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.asset.AssetService;
import io.github.andi005.qram.assignment.AssignmentService;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.license.LicenseForm;
import io.github.andi005.qram.license.LicenseService;
import java.time.Clock;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** The main flows over HTTP, from login to the result page, through every layer. */
@IntegrationTest
@Transactional
class FullFlowTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestData testData;
  @Autowired private AssetService assetService;
  @Autowired private AssignmentService assignmentService;
  @Autowired private LicenseService licenseService;
  @Autowired private Clock clock;

  @AfterEach
  void logout() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpSession loginAsAdmin() throws Exception {
    MvcResult result =
        mockMvc
            .perform(formLogin("/login").user("admin").password("test-admin-password"))
            .andExpect(redirectedUrl("/"))
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  @Test
  @DisplayName("Login → register an asset → assign it to an employee → it shows the holder")
  void createAndAssign() throws Exception {
    TestData.loginAsAdmin();
    Employee employee = testData.employee();
    SecurityContextHolder.clearContext();
    MockHttpSession session = loginAsAdmin();

    MvcResult created =
        mockMvc
            .perform(
                post("/assets")
                    .session(session)
                    .with(csrf())
                    .param("category", "LAPTOP")
                    .param("manufacturer", "Lenovo")
                    .param("model", "ThinkPad T14 Gen 5")
                    .param("serialNumber", "pf-flow-001")
                    .param("purchaseDate", "2026-01-15")
                    .param("warrantyEndDate", "2029-01-15")
                    .param("status", "IN_STOCK"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/assets/*"))
            .andReturn();
    long assetId = Long.parseLong(created.getResponse().getRedirectedUrl().replace("/assets/", ""));
    Asset asset = assetService.get(assetId);
    assertThat(asset.getTag()).matches("IT-\\d{6}");
    assertThat(asset.getSerialNumber()).isEqualTo("PF-FLOW-001");

    mockMvc
        .perform(
            post("/assets/{id}/assign", assetId)
                .session(session)
                .with(csrf())
                .param("employeeId", employee.getId().toString()))
        .andExpect(redirectedUrl("/assets/" + assetId));

    assertThat(assignmentService.currentFor(assetId))
        .get()
        .extracting(a -> a.getEmployee().getId())
        .isEqualTo(employee.getId());
    mockMvc
        .perform(get("/assets/{id}", assetId).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(employee.getFullName())));
  }

  @Test
  @DisplayName("Scanning a label without a session → login → back on the asset's page")
  void labelUrlLeadsThroughLoginToAsset() throws Exception {
    TestData.loginAsAdmin();
    Asset asset = testData.asset();
    SecurityContextHolder.clearContext();

    MvcResult first =
        mockMvc
            .perform(get("/a/{tag}", asset.getTag()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andReturn();
    MockHttpSession session = (MockHttpSession) first.getRequest().getSession();

    // Spring Security remembered the label address and returns there after login.
    mockMvc
        .perform(
            post("/login")
                .session(session)
                .with(csrf())
                .param("username", "admin")
                .param("password", "test-admin-password"))
        .andExpect(redirectedUrlPattern("**/a/" + asset.getTag() + "*"));
    mockMvc
        .perform(get("/a/{tag}", asset.getTag()).session(session))
        .andExpect(redirectedUrl("/assets/" + asset.getId()));
  }

  @Test
  @DisplayName("The expiry view lists a license expiring on day 90, but not one on day 91")
  void expiryViewBoundary() throws Exception {
    TestData.loginAsAdmin();
    LocalDate today = LocalDate.now(clock);
    licenseService.create(
        new LicenseForm("Day-90 License", null, 5, today.plusDays(90), null, null));
    licenseService.create(
        new LicenseForm("Day-91 License", null, 5, today.plusDays(91), null, null));
    licenseService.create(
        new LicenseForm("Yesterday License", null, 5, today.minusDays(1), null, null));
    SecurityContextHolder.clearContext();

    String page =
        mockMvc
            .perform(get("/expiring").session(loginAsAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String soon =
        page.substring(
            page.indexOf("id=\"licenses-soon\""), page.indexOf("id=\"warranties-soon\""));
    assertThat(soon).contains("Day-90 License").doesNotContain("Day-91 License");
    String expired = page.substring(page.indexOf("id=\"licenses-expired\""));
    assertThat(expired).contains("Yesterday License");
  }

  @Test
  @DisplayName("Editing with an old version is refused (concurrent edit detected)")
  void concurrentEditIsDetected() throws Exception {
    TestData.loginAsAdmin();
    Asset asset = testData.asset();
    SecurityContextHolder.clearContext();
    MockHttpSession session = loginAsAdmin();

    mockMvc
        .perform(
            post("/assets/{id}", asset.getId())
                .session(session)
                .with(csrf())
                .param("category", "LAPTOP")
                .param("model", "Changed by someone else")
                .param("status", "IN_STOCK")
                .param("version", String.valueOf(asset.getVersion() + 7)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("u ndryshua nga dikush tjetër")));
    assertThat(assetService.get(asset.getId()).getModel()).isEqualTo("ThinkPad T14");
  }
}
