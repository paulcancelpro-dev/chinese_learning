package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;

import com.chinese.learning.ai.OpenAiExerciseGenerator;
import com.chinese.learning.common.ApiException;
import com.chinese.learning.exercise.*;
import com.chinese.learning.vocabulary.Word;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class OpenAiTest {
  HttpServer server;
  final JsonMapper mapper = JsonMapper.builder().build();
  AtomicReference<String> requestBody = new AtomicReference<>();
  OpenAiExerciseGenerator generator;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    generator =
        new OpenAiExerciseGenerator(
            mapper,
            "test-key",
            "test-model",
            "http://127.0.0.1:" + server.getAddress().getPort() + "/responses",
            true,
            1,
            1000);
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  void response(int status, String body) {
    server.createContext(
        "/responses",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
  }

  List<GeneratedExercise> call() {
    return generator.generate(
        new GenerationRequest(
            1, List.of(ExerciseType.HANZI_TO_FRENCH), 1, GenerationRequest.Selection.NEW),
        List.of(new Word("朋友", "péngyou", List.of("ami"))));
  }

  @Test
  void sendsStrictResponsesSchema() {
    response(
        200,
        "{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"exercises\\\":[]}\"}]}]}");
    assertThat(call()).isEmpty();
    var body = mapper.readTree(requestBody.get());
    assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
    assertThat(body.path("store").asBoolean()).isFalse();
    assertThat(body.has("response_format")).isFalse();
  }

  @Test
  void hidesUpstreamBodyAndKey() {
    response(429, "test-key private upstream details");
    assertThatThrownBy(this::call)
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("429")
        .hasMessageNotContaining("test-key")
        .hasMessageNotContaining("private");
  }

  @Test
  void handlesRefusal() {
    response(200, "{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"refusal\"}]}]}");
    assertThatThrownBy(this::call).hasMessageContaining("refusée");
  }

  @Test
  void handlesIncomplete() {
    response(200, "{\"status\":\"incomplete\"}");
    assertThatThrownBy(this::call).hasMessageContaining("incomplète");
  }

  @Test
  void handlesMalformedJson() {
    response(200, "not json");
    assertThatThrownBy(this::call).hasMessageContaining("JSON");
  }

  @Test
  void handlesTimeout() {
    server.createContext(
        "/responses",
        exchange -> {
          try {
            Thread.sleep(1500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });
    assertThatThrownBy(this::call).hasMessageContaining("délai");
  }

  @Test
  void decodesACompleteInternalExercise() {
    var item =
        new GeneratedExercise(
            UUID.randomUUID(),
            ExerciseType.HANZI_TO_FRENCH,
            "朋友",
            List.of(),
            1,
            List.of("ami"),
            "Un ami",
            List.of());
    String output = mapper.writeValueAsString(Map.of("exercises", List.of(item)));
    response(
        200,
        mapper.writeValueAsString(
            Map.of(
                "status",
                "completed",
                "output",
                List.of(
                    Map.of("content", List.of(Map.of("type", "output_text", "text", output)))))));
    assertThat(call()).containsExactly(item);
  }
}
