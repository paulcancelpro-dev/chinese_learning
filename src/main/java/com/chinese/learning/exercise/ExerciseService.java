package com.chinese.learning.exercise;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
  private final WordSelection selection;
  private final LocalExerciseGenerator local;
  private final GeneratedExerciseValidator validator;
  private final ExerciseStorage storage;

  public ExerciseService(
      WordSelection selection,
      LocalExerciseGenerator local,
      GeneratedExerciseValidator validator,
      ExerciseStorage storage) {
    this.selection = selection;
    this.local = local;
    this.validator = validator;
    this.storage = storage;
  }

  public record Batch(String source, List<ExerciseView> exercises) {}

  public Batch generate(GenerationRequest request) {
    var words = selection.select(request);
    var result = local.generate(request, words);
    validator.validate(result, request, words);
    return new Batch("local", storage.save(result, "local"));
  }
}
