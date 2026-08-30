package com.avbooknest.support;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avbooknest.auth.security.*;
import com.avbooknest.auth.service.*;
import com.avbooknest.contact.controller.ContactController;
import com.avbooknest.contact.service.ContactRateLimitService;
import com.avbooknest.support.controller.*;
import com.avbooknest.support.service.SupportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {SupportController.class, AdminSupportController.class, ContactController.class},
    properties = "app.cors.allowed-origins=http://localhost:5173")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class SupportSecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean SupportService support;
  @MockitoBean ContactRateLimitService limits;
  @MockitoBean JwtService jwt;
  @MockitoBean UserSecurityService users;
  @MockitoBean AuthCookieService cookies;
  static final String CONTACT =
      "{\"name\":\"Visitor\",\"email\":\"visitor@test.ro\",\"topic\":\"GENERAL\",\"subject\":\"Ajutor\",\"message\":\"Am nevoie de ajutor pentru o problemă\",\"privacyAccepted\":true}";

  @Test
  void anonymousCanCreateButCannotReadTickets() throws Exception {
    mvc.perform(
            post("/api/contact")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONTACT))
        .andExpect(status().isOk());
    verify(support).create(any(), isNull());
    mvc.perform(get("/api/support/tickets")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/support/tickets/10/messages")).andExpect(status().isUnauthorized());
  }

  @Test
  void contactUsesAuthenticatedIdentity() throws Exception {
    mvc.perform(
            post("/api/contact")
                .with(user("real@test.ro"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONTACT))
        .andExpect(status().isOk());
    verify(support).create(any(), eq("real@test.ro"));
  }

  @Test
  void honeypotDoesNotPersistAnything() throws Exception {
    mvc.perform(
            post("/api/contact")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    CONTACT.replace(
                        "\"privacyAccepted\":true",
                        "\"privacyAccepted\":true,\"website\":\"spam\"")))
        .andExpect(status().isOk());
    verifyNoInteractions(support);
  }

  @Test
  void writesRequireCsrf() throws Exception {
    mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(CONTACT))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/support/tickets/1/messages")
                .with(user("buyer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Test\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/support/tickets/1/messages/2/retry-email")
                .with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(support);
  }

  @Test
  void ordinaryUsersCannotAccessAnyAdminAction() throws Exception {
    mvc.perform(get("/api/admin/support/tickets").with(user("buyer")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/support/administrators").with(user("buyer")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/support/tickets/1/messages")
                .with(user("buyer"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Test\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(support);
  }

  @Test
  void invalidPayloadsAndEnumsReturn400() throws Exception {
    mvc.perform(
            post("/api/support/tickets/1/messages")
                .with(user("buyer"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/support/tickets/1/status")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVALID\",\"reason\":\"test\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/support/tickets/1/assignment")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"administratorId\":-1,\"reason\":\"test\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(support);
  }

  @Test
  void validAdminReplyUsesPrincipal() throws Exception {
    mvc.perform(
            post("/api/admin/support/tickets/1/messages")
                .with(user("admin@test.ro").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Reply\"}"))
        .andExpect(status().isOk());
    verify(support).adminReply(eq(1L), eq("admin@test.ro"), any());
  }
}
