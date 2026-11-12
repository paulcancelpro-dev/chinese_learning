package com.chinese.learning.exercise;

import jakarta.validation.constraints.*;
import java.util.List;

public record ReuseRequest(
    @Min(1) @Max(20) int count,
    @NotEmpty @Size(max = 2) List<@NotNull ExerciseType> types,
    @Min(1) @Max(5) Integer difficulty) {}
