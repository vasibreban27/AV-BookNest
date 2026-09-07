package com.avbooknest.reporting;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avbooknest.auth.security.*;
import com.avbooknest.auth.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {ReportController.class, AdminReportController.class},
    properties = "app.cors.allowed-origins=http://localhost:5173")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class ReportSecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean ReportingService service;
  @MockitoBean JwtService jwt;
  @MockitoBean UserSecurityService users;
  @MockitoBean AuthCookieService cookies;

  MockMultipartFile payload(String body) {
    return new MockMultipartFile(
        "report",
        "report.json",
        "application/json",
        body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  static final String VALID =
      "{\"targetType\":\"BOOK\",\"targetId\":1,\"reason\":\"SPAM\",\"description\":\"\"}";

  @Test
  void anonymousCannotCreateListOrReadEvidence() throws Exception {
    mvc.perform(multipart("/api/reports").file(payload(VALID)).with(csrf()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/reports")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/reports/1/evidence/2")).andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void writesRequireCsrf() throws Exception {
    mvc.perform(multipart("/api/reports").file(payload(VALID)).with(user("buyer")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/admin/reports/1/claim").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void allAdminRoutesRejectOrdinaryUsers() throws Exception {
    mvc.perform(get("/api/admin/reports").with(user("buyer"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/reports/1").with(user("buyer"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/reports/users/1/history").with(user("buyer")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/admin/reports/1/claim").with(csrf()).with(user("buyer")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/reports/1/resolve")
                .with(csrf())
                .with(user("buyer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void validMultipartUsesAuthenticatedIdentity() throws Exception {
    mvc.perform(
            multipart("/api/reports")
                .file(payload(VALID))
                .with(csrf())
                .with(user("real@report.test")))
        .andExpect(status().isCreated());
    verify(service).create(eq("real@report.test"), any(), isNull());
  }

  @Test
  void invalidEnumsAndMissingFieldsAreRejected() throws Exception {
    mvc.perform(
            multipart("/api/reports")
                .file(payload(VALID.replace("SPAM", "INVALID")))
                .with(csrf())
                .with(user("buyer")))
        .andExpect(status().isBadRequest());
    mvc.perform(multipart("/api/reports").file(payload("{}")).with(csrf()).with(user("buyer")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void invalidDecisionReasonAndDurationAreRejected() throws Exception {
    mvc.perform(
            post("/api/admin/reports/1/resolve")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"SUSPEND\",\"note\":\" \",\"suspensionDays\":91}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
