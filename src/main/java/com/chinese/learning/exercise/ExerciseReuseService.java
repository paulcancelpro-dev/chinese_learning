package com.chinese.learning.exercise;

import com.chinese.learning.common.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Replays stored content only. No dependency on a generator or the OpenAI budget. */
@Service
@Transactional
public class ExerciseReuseService {
  private static final Set<ExerciseType> PHRASE_TYPES =
      Set.of(ExerciseType.SENTENCE_TRANSLATION, ExerciseType.FILL_IN_THE_BLANK);
  private final ExerciseRepository exercises;
  private final SessionRepository sessions;
  private final Clock clock;

  public ExerciseReuseService(
      ExerciseRepository exercises, SessionRepository sessions, Clock clock) {
    this.exercises = exercises;
    this.sessions = sessions;
    this.clock = clock;
  }

  public record Availability(ExerciseType type, int difficulty, long available) {}

  @Transactional(readOnly = true)
  public List<Availability> available() {
    var originals = exercises.findBySourceExerciseIdIsNullAndTypeIn(PHRASE_TYPES);
    var result = new ArrayList<Availability>();
    for (var type : PHRASE_TYPES.stream().sorted().toList()) {
      for (int difficulty = 0; difficulty <= 5; difficulty++) {
        int level = difficulty;
        long count =
            originals.stream()
                .filter(e -> e.type == type && (level == 0 || e.difficulty == level))
                .map(e -> e.question)
                .distinct()
                .count();
        result.add(new Availability(type, level, count));
      }
    }
    return result;
  }

  public ExerciseService.Batch reuse(ReuseRequest request) {
    if (!PHRASE_TYPES.containsAll(request.types()))
      throw ApiException.bad(
          "La réutilisation est réservée aux traductions de phrases et aux phrases à compléter");
    var candidates =
        new ArrayList<>(
            exercises.findBySourceExerciseIdIsNullAndTypeIn(request.types()).stream()
                .filter(e -> request.difficulty() == null || e.difficulty == request.difficulty())
                .toList());
    Collections.shuffle(candidates);
    // Repeated practice must not grow the content pool or repeat a phrase within a batch.
    var unique = new LinkedHashMap<String, Exercise>();
    candidates.forEach(e -> unique.putIfAbsent(e.type.name() + "\n" + e.question, e));
    if (unique.isEmpty())
      throw ApiException.bad(
          "Aucune phrase enregistrée pour ces critères. Générez d’abord des phrases avec OpenAI ou"
              + " changez les filtres.");
    var session = sessions.save(new TrainingSession(clock.instant(), "reused"));
    var attempts =
        unique.values().stream()
            .limit(request.count())
            .map(
                original -> {
                  var content =
                      new GeneratedExercise(
                          original.wordId,
                          original.type,
                          original.question,
                          List.copyOf(original.choices),
                          original.difficulty,
                          List.copyOf(original.acceptedAnswers),
                          original.explanation,
                          List.copyOf(original.newVocabulary));
                  var attempt = new Exercise(session.id, content);
                  attempt.sourceExerciseId = original.id;
                  return ExerciseView.of(exercises.save(attempt));
                })
            .toList();
    return new ExerciseService.Batch("reused", attempts);
  }
}
