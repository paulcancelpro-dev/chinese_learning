package com.chinese.learning.exercise;

import com.chinese.learning.ai.OpenAiExerciseGenerator;
import com.chinese.learning.common.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
  private final com.chinese.learning.ai.GenerationBudget budget;
  private final WordSelection selection;
  private final LocalExerciseGenerator local;
  private final OpenAiExerciseGenerator ai;
  private final GeneratedExerciseValidator validator;
  private final ExerciseStorage storage;
  private final java.util.concurrent.Semaphore generation = new java.util.concurrent.Semaphore(1);

  public ExerciseService(
      com.chinese.learning.ai.GenerationBudget budget,
      WordSelection selection,
      LocalExerciseGenerator local,
      OpenAiExerciseGenerator ai,
      GeneratedExerciseValidator validator,
      ExerciseStorage storage) {
    this.budget = budget;
    this.selection = selection;
    this.local = local;
    this.ai = ai;
    this.validator = validator;
    this.storage = storage;
  }

  public record Batch(String source, List<ExerciseView> exercises) {}

  public Batch generate(GenerationRequest request) {
    if (!generation.tryAcquire())
      throw new ApiException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
          "Une génération est déjà en cours");
    try {
      var words = selection.select(request);
      List<GeneratedExercise> result;
      String source = "local";
      if (ai.enabled()) {
        budget.acquire();
        try {
          result = ai.generate(request, words);
          validator.validate(result, request, words);
          source = "openai";
        } catch (ApiException e) {
          if (request.types().stream().anyMatch(t -> !t.simple())) throw e;
          result = local.generate(request, words);
          source = "local_fallback";
        }
      } else result = local.generate(request, words);
      validator.validate(result, request, words);
      return new Batch(source, storage.save(result, source));
    } finally {
      generation.release();
    }
  }
}
