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

  public ExerciseController(ExerciseService service, ExerciseStorage storage) {
    this.service = service;
    this.storage = storage;
  }

  public record Answer(@NotBlank @Size(max = 2000) String answer) {}

  @PostMapping("/generate")
  public ExerciseService.Batch generate(@Valid @RequestBody GenerationRequest request) {
    return service.generate(request);
  }

  @GetMapping("/{id}")
  public ExerciseView get(@PathVariable UUID id) {
    return storage.get(id);
  }

  @PostMapping("/{id}/answers")
  public ExerciseStorage.Correction answer(
      @PathVariable UUID id, @Valid @RequestBody Answer answer) {
    return storage.answer(id, answer.answer());
  }

  @GetMapping("/history")
  public com.chinese.learning.common.PageResponse<ExerciseStorage.Correction> history(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return com.chinese.learning.common.PageResponse.from(storage.history(page, size));
  }
}
