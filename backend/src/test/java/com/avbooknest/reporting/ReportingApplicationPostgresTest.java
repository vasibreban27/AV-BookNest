package com.avbooknest.reporting;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "app.jwt.secret=reporting-test-secret-at-least-32-characters-long",
      "app.stripe.enabled=false",
      "app.sameday.enabled=false",
      "app.mail.enabled=false",
      "app.seed.enabled=false",
      "spring.jpa.show-sql=false",
      "app.moderation.expiry-delay-ms=3600000"
    })
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class ReportingApplicationPostgresTest {
  static final String SCHEMA = "report_app_" + UUID.randomUUID().toString().replace("-", "");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> System.getenv("BOOKNEST_TEST_DATABASE_URL"));
    r.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test"));
    r.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", ""));
    r.add("spring.flyway.schemas", () -> SCHEMA);
    r.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    r.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
  }

  @Test
  void fullApplicationStartsAndAcceptsMultipartThenModeratesThroughHttp() throws Exception {
    jdbc.update(
        """
        INSERT INTO users(id,email,first_name,last_name,password_hash,role_id,email_verified)
        VALUES(501,'reporter@app.test','Ana','Test','test',(SELECT id FROM roles WHERE name='USER'),true),
        (502,'seller@app.test','Ion','Test','test',(SELECT id FROM roles WHERE name='USER'),true),
        (503,'admin@app.test','Admin','Test','test',(SELECT id FROM roles WHERE name='ADMIN'),true)
        """);
    jdbc.update("INSERT INTO categories(id,name,slug) VALUES(5001,'App books','app-books')");
    jdbc.update(
        "INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id) VALUES(5001,'Carte','Autor',20,'GOOD','Română',502,5001)");
    var payload =
        new MockMultipartFile(
            "report",
            "report.json",
            "application/json",
            "{\"targetType\":\"BOOK\",\"targetId\":5001,\"reason\":\"SPAM\",\"description\":\"Publicitate repetată\"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var result =
        mvc.perform(
                multipart("/api/reports")
                    .file(payload)
                    .with(user("reporter@app.test"))
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("status").value("NEW"))
            .andReturn();
    long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(get("/api/reports/" + id).with(user("seller@app.test")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/admin/reports/" + id + "/resolve")
                .with(user("admin@app.test").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"decision\":\"WARN\",\"note\":\"Publicitatea nu este permisă\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("report.status").value("RESOLVED"));
    assertEquals(
        "WARN",
        jdbc.queryForObject("SELECT decision FROM content_reports WHERE id=?", String.class, id));
  }
}
