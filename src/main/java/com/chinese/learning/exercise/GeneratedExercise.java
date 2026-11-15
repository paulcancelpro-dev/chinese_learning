package com.chinese.learning.exercise;

import java.util.*;

/** Internal DTO: never return this object before correction. */
public record GeneratedExercise(
    UUID wordId,
    ExerciseType type,
    String question,
    List<String> choices,
    int difficulty,
    List<String> acceptedAnswers,
    String explanation,
    List<String> newVocabulary,
    String pinyinHint) {
  public GeneratedExercise(
      UUID wordId,
      ExerciseType type,
      String question,
      List<String> choices,
      int difficulty,
      List<String> acceptedAnswers,
      String explanation,
      List<String> newVocabulary) {
    this(
        wordId,
        type,
        question,
        choices,
        difficulty,
        acceptedAnswers,
        explanation,
        newVocabulary,
        null);
  }
}
