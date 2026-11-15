package com.chinese.learning.exercise;

import com.chinese.learning.vocabulary.WordRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ExercisePresentation {
  private final WordRepository words;

  public ExercisePresentation(WordRepository words) {
    this.words = words;
  }

  public ExerciseView view(Exercise exercise) {
    if (exercise.type != ExerciseType.FILL_IN_THE_BLANK) return ExerciseView.of(exercise);

    var random =
        new Random(exercise.id.getMostSignificantBits() ^ exercise.id.getLeastSignificantBits());
    var accepted = exercise.acceptedAnswers.stream().map(AnswerNormalizer::normalize).toList();
    var alternatives =
        new ArrayList<>(
            words.findDistinctHanzi().stream()
                .filter(hanzi -> !accepted.contains(AnswerNormalizer.normalize(hanzi)))
                .toList());
    Collections.shuffle(alternatives, random);
    var choices = new ArrayList<>(alternatives.stream().limit(3).toList());
    choices.add(exercise.acceptedAnswers.getFirst());
    Collections.shuffle(choices, random);
    return new ExerciseView(
        exercise.id,
        exercise.sessionId,
        exercise.type,
        exercise.question,
        List.copyOf(choices),
        exercise.difficulty,
        List.copyOf(exercise.newVocabulary));
  }
}
