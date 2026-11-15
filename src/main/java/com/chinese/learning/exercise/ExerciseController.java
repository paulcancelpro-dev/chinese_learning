package com.chinese.learning.exercise;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
  private final ExerciseService service;
  private final ExerciseStorage storage;
  private final ExerciseReuseService reuseService;
  private final ManualExerciseService manual;

  public ExerciseController(
      ExerciseService service,
      ExerciseStorage storage,
      ExerciseReuseService reuseService,
      ManualExerciseService manual) {
    this.service = service;
    this.storage = storage;
    this.reuseService = reuseService;
    this.manual = manual;
  }

  public record Answer(@NotBlank @Size(max = 2000) String answer) {}

  @PostMapping("/generate")
  public ExerciseService.Batch generate(@Valid @RequestBody GenerationRequest request) {
    return service.generate(request);
  }

  @PostMapping("/prompt")
  public ManualExerciseService.Prompt prompt(@Valid @RequestBody GenerationRequest request) {
    return manual.prompt(request);
  }

  @PostMapping("/import")
  public ExerciseService.Batch importExercises(@RequestBody tools.jackson.databind.JsonNode body) {
    return manual.importExercises(body);
  }

  @PostMapping("/{id}/replay")
  public ExerciseService.Batch replay(@PathVariable UUID id) {
    return reuseService.replay(id);
  }

  @GetMapping("/reusable")
  public java.util.List<ExerciseReuseService.Availability> reusable() {
    return reuseService.available();
  }

  @PostMapping("/reuse")
  public ExerciseService.Batch reuse(@Valid @RequestBody ReuseRequest request) {
    return reuseService.reuse(request);
  }

  @GetMapping("/{id}")
  public ExerciseView get(@PathVariable UUID id) {
    return storage.get(id);
  }

  @GetMapping("/{id}/hint")
  public ExerciseStorage.Hint hint(@PathVariable UUID id) {
    return storage.hint(id);
  }

  @PostMapping("/{id}/answers")
  public ExerciseStorage.Correction answer(
      @PathVariable UUID id, @Valid @RequestBody Answer answer) {
    return storage.answer(id, answer.answer());
  }

  @PostMapping({"/{id}/accept-answer", "/{id}/accept-translation"})
  public ExerciseStorage.Correction acceptAnswer(@PathVariable UUID id) {
    return storage.acceptAnswer(id);
  }

  @GetMapping("/history")
  public com.chinese.learning.common.PageResponse<ExerciseStorage.Correction> history(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return com.chinese.learning.common.PageResponse.from(storage.history(page, size));
  }
}
