package com.chinese.learning.exercise;

import jakarta.validation.constraints.*;
import java.util.List;

public record GenerationRequest(
    @Min(1) @Max(20) int count,
    @NotEmpty @Size(max = 6) List<@NotNull ExerciseType> types,
    @Min(1) @Max(5) int difficulty,
    @NotNull Selection strategy) {
  public enum Selection {
    NEW,
    DIFFICULT,
    DUE,
    RANDOM
  }
}
