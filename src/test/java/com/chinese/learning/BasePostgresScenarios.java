package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chinese.learning.exercise.*;
import com.chinese.learning.vocabulary.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

abstract class BasePostgresScenarios {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired ExerciseStorage storage;

  @BeforeEach
  void clear() {
    jdbc.execute("truncate table word, training_session cascade");
  }

  String vocabulary =
      "[{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\"]},{\"hanzi\":\"水\",\"pinyin\":\"shuǐ\",\"traductions_fr\":[\"eau\"]}]";

  void seed() throws Exception {
    mvc.perform(post("/api/vocabulary/import").contentType("application/json").content(vocabulary))
        .andExpect(status().isOk());
  }

  UUID generate() throws Exception {
    var result =
        mvc.perform(
                post("/api/exercises/generate")
                    .contentType("application/json")
                    .content(
                        "{\"count\":1,\"types\":[\"HANZI_TO_PINYIN\"],\"difficulty\":1,\"strategy\":\"NEW\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exercises[0].acceptedAnswers").doesNotExist())
            .andExpect(jsonPath("$.exercises[0].explanation").doesNotExist())
            .andReturn();
    return UUID.fromString(
        mapper
            .readTree(result.getResponse().getContentAsString())
            .path("exercises")
            .get(0)
            .path("id")
            .asText());
  }

  @Test
  void importsAreIdempotentAndHomographsStaySeparate() throws Exception {
    seed();
    seed();
    assertThat(jdbc.queryForObject("select count(*) from word", Integer.class)).isEqualTo(2);
    mvc.perform(
            post("/api/vocabulary")
                .contentType("application/json")
                .content("{\"hanzi\":\"朋友\",\"pinyin\":\"other\",\"traductions_fr\":[\"autre\"]}"))
        .andExpect(status().isCreated());
    assertThat(jdbc.queryForObject("select count(*) from word", Integer.class)).isEqualTo(3);
  }

  @Test
  void invalidImportRollsBackEverything() throws Exception {
    mvc.perform(
            post("/api/vocabulary/import")
                .contentType("application/json")
                .content(
                    "[{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\"]},{}]"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("entrée 1")));
    assertThat(jdbc.queryForObject("select count(*) from word", Integer.class)).isZero();
  }

  @Test
  void endToEndAndReplayProtection() throws Exception {
    seed();
    mvc.perform(get("/api/progress/due"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6));
    var id = generate();
    var expected =
        jdbc.queryForObject(
            "select value from exercise_answer where exercise_id=?", String.class, id);
    var body = mapper.writeValueAsString(Map.of("answer", expected));
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/exercises/" + id + "/answers")
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.correct").value(true));
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select successes from progress", Long.class)).isEqualTo(1);
    mvc.perform(
            post("/api/exercises/" + id + "/answers")
                .contentType("application/json")
                .content("{\"answer\":\"different\"}"))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/statistics/overview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answers").value(1));
    mvc.perform(get("/api/exercises/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void concurrentAnswersCountOnce() throws Exception {
    seed();
    var id = generate();
    var expected =
        jdbc.queryForObject(
            "select value from exercise_answer where exercise_id=?", String.class, id);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> storage.answer(id, expected));
      var b = executor.submit(() -> storage.answer(id, expected));
      assertThat(a.get().correct()).isTrue();
      assertThat(b.get().correct()).isTrue();
    }
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select successes from progress", Long.class)).isEqualTo(1);
  }

  @Test
  void progressExportsRoundTripIdempotently() throws Exception {
    seed();
    var id = generate();
    storage.answer(id, "incorrect");
    var exported =
        mvc.perform(get("/api/progress/export"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(post("/api/progress/import").contentType("application/json").content(exported))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ignored").value(1));
    assertThat(jdbc.queryForObject("select errors from progress", Long.class)).isEqualTo(1);
  }

  @Test
  void progressImportRollsBackOnUnknownWord() throws Exception {
    seed();
    String events =
        mapper.writeValueAsString(
            Map.of(
                "progress",
                List.of(
                    Map.of(
                        "id",
                        UUID.randomUUID(),
                        "hanzi",
                        "朋友",
                        "pinyin",
                        "péngyou",
                        "skill",
                        "HANZI_RECOGNITION",
                        "correct",
                        true,
                        "reviewedAt",
                        "2026-01-01T00:00:00Z",
                        "answer",
                        "朋友"),
                    Map.of(
                        "id",
                        UUID.randomUUID(),
                        "hanzi",
                        "不存在",
                        "pinyin",
                        "x",
                        "skill",
                        "HANZI_RECOGNITION",
                        "correct",
                        true,
                        "reviewedAt",
                        "2026-01-02T00:00:00Z",
                        "answer",
                        "x"))));
    mvc.perform(post("/api/progress/import").contentType("application/json").content(events))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from progress", Integer.class)).isZero();
  }

  @Test
  void validatesRequestsAndMissingResources() throws Exception {
    mvc.perform(get("/api/vocabulary/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/exercises/generate")
                .contentType("application/json")
                .content("{\"count\":200,\"types\":[],\"difficulty\":9,\"strategy\":\"NEW\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/vocabulary?size=10000")).andExpect(status().isBadRequest());
  }

  @Test
  void searchExportAndDeletion() throws Exception {
    seed();
    mvc.perform(get("/api/vocabulary?q=ami"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    var exported =
        mvc.perform(get("/api/vocabulary/export")).andReturn().getResponse().getContentAsString();
    mvc.perform(post("/api/vocabulary/import").contentType("application/json").content(exported))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0));
    var id = generate();
    storage.answer(id, "wrong");
    var wordId = jdbc.queryForObject("select word_id from exercise where id=?", UUID.class, id);
    mvc.perform(delete("/api/vocabulary/" + wordId)).andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class)).isZero();
  }

  @Test
  void swaggerIsAvailable() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/vocabulary']").exists());
  }

  @Test
  void concurrentDistinctExercisesPreserveBothResults() throws Exception {
    seed();
    var first = generate();
    var second = generate();
    var expected =
        jdbc.queryForObject(
            "select value from exercise_answer where exercise_id=?", String.class, first);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> storage.answer(first, expected));
      var b = executor.submit(() -> storage.answer(second, expected));
      assertThat(a.get().correct()).isTrue();
      assertThat(b.get().correct()).isTrue();
    }
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select successes from progress", Long.class)).isEqualTo(2);
  }

  @Test
  void rejectsUnknownInputFields() throws Exception {
    mvc.perform(
            post("/api/vocabulary")
                .contentType("application/json")
                .content(
                    "{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\"],\"level\":5}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/vocabulary/import")
                .contentType("application/json")
                .content("{\"vocabulary\":[],\"extra\":true}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void crudUpdatesAndDatabaseUniqueConstraint() throws Exception {
    seed();
    var id = jdbc.queryForObject("select id from word where hanzi='朋友'", UUID.class);
    mvc.perform(
            put("/api/vocabulary/" + id)
                .contentType("application/json")
                .content(
                    "{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\",\"amie\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.traductions_fr.length()").value(2));
    mvc.perform(
            post("/api/vocabulary")
                .contentType("application/json")
                .content("{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\"]}"))
        .andExpect(status().isConflict());
  }

  private UUID storedPhrase(ExerciseType type, int difficulty, String question) {
    UUID wordId = jdbc.queryForObject("select id from word where hanzi='朋友'", UUID.class);
    return storage
        .save(
            List.of(
                new GeneratedExercise(
                    wordId,
                    type,
                    question,
                    List.of(),
                    difficulty,
                    List.of(type == ExerciseType.SENTENCE_TRANSLATION ? "mon ami" : "朋友"),
                    "朋友 signifie ami.",
                    List.of("我的"))),
            "openai")
        .getFirst()
        .id();
  }

  @Test
  void reusesAnsweredPhrasesAsIndependentAttempts() throws Exception {
    seed();
    UUID original = storedPhrase(ExerciseType.SENTENCE_TRANSLATION, 2, "我的朋友");
    storage.answer(original, "mon ami");
    String request = "{\"count\":10,\"types\":[\"SENTENCE_TRANSLATION\"],\"difficulty\":null}";
    var response =
        mvc.perform(post("/api/exercises/reuse").contentType("application/json").content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("reused"))
            .andExpect(jsonPath("$.exercises.length()").value(1))
            .andExpect(jsonPath("$.exercises[0].question").value("我的朋友"))
            .andExpect(jsonPath("$.exercises[0].acceptedAnswers").doesNotExist())
            .andExpect(jsonPath("$.exercises[0].explanation").doesNotExist())
            .andReturn();
    UUID attempt =
        UUID.fromString(
            mapper
                .readTree(response.getResponse().getContentAsString())
                .path("exercises")
                .get(0)
                .path("id")
                .asText());
    assertThat(attempt).isNotEqualTo(original);
    assertThat(
            jdbc.queryForObject(
                "select source_exercise_id from exercise where id=?", UUID.class, attempt))
        .isEqualTo(original);
    assertThat(
            jdbc.queryForObject(
                "select answered_at is null from exercise where id=?", Boolean.class, attempt))
        .isTrue();
    assertThat(storage.answer(attempt, "mauvaise réponse").correct()).isFalse();
    storage.answer(attempt, "mauvaise réponse");
    assertThat(jdbc.queryForObject("select count(*) from review_event", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject("select correct from exercise where id=?", Boolean.class, original))
        .isTrue();
    assertThat(jdbc.queryForObject("select successes from progress", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select errors from progress", Long.class)).isEqualTo(1);
    mvc.perform(post("/api/exercises/reuse").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises.length()").value(1));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from exercise where source_exercise_id=?",
                Integer.class,
                original))
        .isEqualTo(2);
    mvc.perform(get("/api/exercises/reusable"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.type == 'SENTENCE_TRANSLATION' && @.difficulty == 0)].available")
                .value(org.hamcrest.Matchers.contains(1)));
  }

  @Test
  void reuseFiltersDeduplicatesAndDoesNotGenerateMissingContent() throws Exception {
    seed();
    storedPhrase(ExerciseType.SENTENCE_TRANSLATION, 2, "我的朋友");
    storedPhrase(ExerciseType.SENTENCE_TRANSLATION, 2, "我的朋友");
    storedPhrase(ExerciseType.FILL_IN_THE_BLANK, 3, "我的___");
    mvc.perform(
            post("/api/exercises/reuse")
                .contentType("application/json")
                .content("{\"count\":20,\"types\":[\"SENTENCE_TRANSLATION\"],\"difficulty\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises.length()").value(1));
    int sessions = jdbc.queryForObject("select count(*) from training_session", Integer.class);
    mvc.perform(
            post("/api/exercises/reuse")
                .contentType("application/json")
                .content("{\"count\":1,\"types\":[\"SENTENCE_TRANSLATION\"],\"difficulty\":5}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Aucune phrase")));
    assertThat(jdbc.queryForObject("select count(*) from training_session", Integer.class))
        .isEqualTo(sessions);
    mvc.perform(
            post("/api/exercises/reuse")
                .contentType("application/json")
                .content("{\"count\":1,\"types\":[\"FILL_IN_THE_BLANK\"],\"difficulty\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[0].type").value("FILL_IN_THE_BLANK"));
  }

  @Test
  void reuseValidatesRequestAndEmptyPool() throws Exception {
    for (String body :
        List.of(
            "{\"count\":0,\"types\":[\"SENTENCE_TRANSLATION\"]}",
            "{\"count\":1,\"types\":[\"HANZI_TO_FRENCH\"]}",
            "{\"count\":1,\"types\":[]}",
            "{\"count\":1,\"types\":[\"SENTENCE_TRANSLATION\"],\"difficulty\":6}",
            "{\"count\":1,\"types\":[\"SENTENCE_TRANSLATION\"]}")) {
      mvc.perform(post("/api/exercises/reuse").contentType("application/json").content(body))
          .andExpect(status().isBadRequest());
    }
    assertThat(jdbc.queryForObject("select count(*) from training_session", Integer.class))
        .isZero();
  }
}
