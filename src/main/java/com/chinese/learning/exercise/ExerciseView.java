package com.chinese.learning.exercise;

import java.util.*;

public record ExerciseView(
    UUID id,
    UUID sessionId,
    ExerciseType type,
    String question,
    List<String> choices,
    int difficulty,
    List<String> newVocabulary) {
  public static ExerciseView of(Exercise e) {
    return new ExerciseView(
        e.id,
        e.sessionId,
        e.type,
        e.question,
        List.copyOf(e.choices),
        e.difficulty,
        List.copyOf(e.newVocabulary));
  }
}
