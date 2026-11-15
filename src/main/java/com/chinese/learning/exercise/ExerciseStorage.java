package com.chinese.learning.exercise;

import com.chinese.learning.common.ApiException;
import com.chinese.learning.progress.ProgressService;
import com.chinese.learning.vocabulary.WordRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExerciseStorage {
  private final ExerciseRepository exercises;
  private final SessionRepository sessions;
  private final ProgressService progress;
  private final Clock clock;
  private final ExercisePresentation presentation;
  private final WordRepository words;

  public ExerciseStorage(
      ExerciseRepository exercises,
      SessionRepository sessions,
      ProgressService progress,
      Clock clock,
      ExercisePresentation presentation,
      WordRepository words) {
    this.exercises = exercises;
    this.sessions = sessions;
    this.progress = progress;
    this.clock = clock;
    this.presentation = presentation;
    this.words = words;
  }

  public List<ExerciseView> save(List<GeneratedExercise> generated, String source) {
    var session = sessions.save(new TrainingSession(clock.instant(), source));
    return generated.stream()
        .map(e -> presentation.view(exercises.save(new Exercise(session.id, e))))
        .toList();
  }

  public ExerciseView get(UUID id) {
    return presentation.view(exercises.findById(id).orElseThrow(ApiException::missing));
  }

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  public record Hint(String firstLetter, String pinyin) {}

  @Transactional(readOnly = true)
  public Hint hint(UUID id) {
    var exercise = exercises.findById(id).orElseThrow(ApiException::missing);
    if (!exercise.type.simple()) {
      if (exercise.pinyinHint == null)
        throw ApiException.bad(
            "Cette phrase a été enregistrée sans pinyin. Réimportez-la avec un indice pinyin.");
      return new Hint(null, exercise.pinyinHint);
    }
    String answer =
        exercise.acceptedAnswers.stream()
            .map(String::strip)
            .filter(value -> !value.isEmpty())
            .sorted()
            .findFirst()
            .orElseThrow(() -> ApiException.bad("Aucun indice disponible pour cet exercice"));
    var first = java.util.regex.Pattern.compile("\\X").matcher(answer);
    first.find();
    return new Hint(first.group(), null);
  }

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  public record Correction(
      UUID exerciseId,
      boolean correct,
      List<String> acceptedAnswers,
      String explanation,
      Instant answeredAt,
      String pinyin) {}

  private Correction correction(Exercise e) {
    String pinyin = e.pinyinHint;
    if (e.type == ExerciseType.FILL_IN_THE_BLANK && pinyin != null && pinyin.contains("___")) {
      var target = words.findById(e.wordId).orElseThrow(ApiException::missing);
      pinyin = pinyin.replace("___", target.pinyin);
    }
    return new Correction(
        e.id, e.correct, List.copyOf(e.acceptedAnswers), e.explanation, e.answeredAt, pinyin);
  }

  public Correction answer(UUID id, String answer) {
    var e = exercises.lock(id).orElseThrow(ApiException::missing);
    if (e.answeredAt != null) {
      if (!AnswerNormalizer.normalize(e.submittedAnswer, e.type)
          .equals(AnswerNormalizer.normalize(answer, e.type)))
        throw new ApiException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Cet exercice a déjà été corrigé avec une autre réponse");
      return correction(e);
    }
    boolean correct =
        e.acceptedAnswers.stream()
            .map(expected -> AnswerNormalizer.normalize(expected, e.type))
            .anyMatch(AnswerNormalizer.normalize(answer, e.type)::equals);
    Instant reviewedAt = progress.recordAnswer(e.id, e.wordId, e.type.skill(), correct, answer);
    e.correct = correct;
    e.submittedAnswer = answer;
    e.answeredAt = reviewedAt;
    return correction(e);
  }

  public Correction acceptAnswer(UUID id) {
    var exercise = exercises.lock(id).orElseThrow(ApiException::missing);
    if (exercise.answeredAt == null)
      throw ApiException.bad("Soumettez une réponse avant de la valider manuellement.");
    if (!exercise.correct) {
      progress.acceptAnswer(exercise.id, exercise.wordId, exercise.type.skill());
      exercise.correct = true;
    }
    return correction(exercise);
  }

  public Page<Correction> history(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiException.bad("Pagination invalide");
    return exercises
        .findByAnsweredAtIsNotNull(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "answeredAt")))
        .map(this::correction);
  }
}
