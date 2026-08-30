package com.avbooknest.review.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avbooknest.auth.security.*;
import com.avbooknest.auth.service.*;
import com.avbooknest.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {ReviewController.class, AdminReviewController.class},
    properties = "app.cors.allowed-origins=http://localhost:5173")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class ReviewSecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean ReviewService reviews;
  @MockitoBean JwtService jwt;
  @MockitoBean UserSecurityService users;
  @MockitoBean AuthCookieService cookies;
  private static final String VALID =
      "{\"sellerRating\":5,\"descriptionRating\":4,\"conditionRating\":3}";

  @Test
  void fractionalOrStringRatingsAreNotSilentlyCoerced() throws Exception {
    for (String value : new String[] {"4.9", "\"5\"", "true", "{}"}) {
      mvc.perform(
              post("/api/orders/10/items/30/review")
                  .with(user("buyer"))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"sellerRating\":"
                          + value
                          + ",\"descriptionRating\":4,\"conditionRating\":3}"))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(reviews);
  }

  @Test
  void invalidEnumsAndPageParametersAreBadRequestsNotServerErrors() throws Exception {
    mvc.perform(get("/api/sellers/2/reviews").param("page", "invalid"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/admin/reviews").with(user("admin").roles("ADMIN")).param("status", "INVALID"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/reviews/40/moderation")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVALID\",\"reason\":\"test\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(reviews);
  }

  @Test
  void reputationAndPublicReviewsAreAnonymous() throws Exception {
    mvc.perform(get("/api/sellers/2/reputation")).andExpect(status().isOk());
    mvc.perform(get("/api/sellers/2/reviews")).andExpect(status().isOk());
  }

  @Test
  void purchaseDataAndWritesRequireAuthentication() throws Exception {
    mvc.perform(get("/api/orders/10/reviews")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/orders/10/items/30/review")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(reviews);
  }

  @Test
  void reviewCreationRequiresCsrf() throws Exception {
    mvc.perform(
            post("/api/orders/10/items/30/review")
                .with(user("buyer@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID))
        .andExpect(status().isForbidden());
    verifyNoInteractions(reviews);
  }

  @Test
  void validPurchaseRequestReachesServiceAsAuthenticatedBuyer() throws Exception {
    mvc.perform(
            post("/api/orders/10/items/30/review")
                .with(user("buyer@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID))
        .andExpect(status().isCreated());
    verify(reviews).create(eq(10L), eq(30L), eq("buyer@example.com"), any());
  }

  @Test
  void invalidRatingsAndMissingFieldsAreRejected() throws Exception {
    for (String body :
        new String[] {
          "{}",
          "{\"sellerRating\":0,\"descriptionRating\":6,\"conditionRating\":3}",
          "{\"sellerRating\":5,\"descriptionRating\":4}"
        }) {
      mvc.perform(
              post("/api/orders/10/items/30/review")
                  .with(user("buyer"))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(reviews);
  }

  @Test
  void nonAdminCannotReadOrModerateReviews() throws Exception {
    mvc.perform(get("/api/admin/reviews").with(user("buyer"))).andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/admin/reviews/40/moderation")
                .with(user("buyer"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(reviews);
  }

  @Test
  void adminCanModerateButNeedsCsrfAndReason() throws Exception {
    mvc.perform(
            patch("/api/admin/reviews/40/moderation")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/admin/reviews/40/moderation")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"HIDDEN\",\"reason\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/reviews/40/moderation")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
        .andExpect(status().isOk());
    verify(reviews).moderate(eq(40L), eq("admin"), any());
  }
}
