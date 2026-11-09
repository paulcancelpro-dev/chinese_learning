package com.chinese.learning;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class LearningApplicationTests extends BasePostgresScenarios {
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
    r.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("TEST_DATABASE_USER", "chinese"));
    r.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
    r.add("app.openai.enabled", () -> false);
  }
}
