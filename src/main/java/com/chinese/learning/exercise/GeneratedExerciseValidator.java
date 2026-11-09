package com.chinese.learning.exercise;

import com.chinese.learning.common.ApiException;
import com.chinese.learning.vocabulary.Word;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class GeneratedExerciseValidator {
  public void validate(
      List<GeneratedExercise> exercises, GenerationRequest request, List<Word> words) {
    if (exercises == null || exercises.size() != request.count()) fail();
    Map<UUID, Word> allowed = new HashMap<>();
    Set<Integer> known = new HashSet<>();
    words.forEach(
        w -> {
          allowed.put(w.id, w);
          w.hanzi.codePoints().forEach(known::add);
        });
    for (var e : exercises) {
      if (e == null
          || !allowed.containsKey(e.wordId())
          || !request.types().contains(e.type())
          || e.difficulty() != request.difficulty()
          || !valid(e.question(), 1000)
          || !valid(e.explanation(), 2000)
          || e.choices() == null
          || e.choices().size() > 6
          || e.acceptedAnswers() == null
          || e.acceptedAnswers().isEmpty()
          || e.acceptedAnswers().size() > 30
          || e.newVocabulary() == null
          || e.newVocabulary().size() > 30) fail();
      if (e.acceptedAnswers().stream().anyMatch(a -> !valid(a, 2000))
          || e.choices().stream().anyMatch(a -> !valid(a, 300))
          || e.newVocabulary().stream().anyMatch(a -> !valid(a, 100))) fail();
      var w = allowed.get(e.wordId());
      var answers = e.acceptedAnswers().stream().map(AnswerNormalizer::normalize).toList();
      if (answers.stream().anyMatch(String::isBlank)) fail();
      if (e.type() == ExerciseType.HANZI_TO_FRENCH
          && (!e.question().equals(w.hanzi)
              || answers.stream()
                  .anyMatch(
                      a ->
                          w.translations.stream()
                              .map(AnswerNormalizer::normalize)
                              .noneMatch(a::equals)))) fail();
      if (e.type() == ExerciseType.HANZI_TO_PINYIN
          && (!e.question().equals(w.hanzi)
              || answers.stream().anyMatch(a -> !a.equals(AnswerNormalizer.normalize(w.pinyin)))))
        fail();
      if (e.type() == ExerciseType.PINYIN_TO_HANZI
          && (!e.question().equals(w.pinyin + " — " + w.translations.iterator().next())
              || answers.stream().anyMatch(a -> !a.equals(w.hanzi)))) fail();
      if (e.type() == ExerciseType.MULTIPLE_CHOICE) {
        if (e.question().contains(w.hanzi)
            || !e.question().contains(w.pinyin)
            || w.translations.stream().noneMatch(e.question()::contains)) fail();
        for (String choice : e.choices()) {
          if (!choice.equals(w.hanzi)
              && words.stream()
                  .noneMatch(
                      other ->
                          other.hanzi.equals(choice)
                              && Collections.disjoint(other.translations, w.translations))) fail();
        }
        var c = e.choices().stream().map(AnswerNormalizer::normalize).toList();
        if (c.size() < 2
            || new HashSet<>(c).size() != c.size()
            || c.stream().filter(answers::contains).count() != 1
            || answers.size() != 1
            || !answers.getFirst().equals(w.hanzi)) fail();
      } else if (!e.choices().isEmpty()) fail();
      if (e.type() == ExerciseType.SENTENCE_TRANSLATION && !e.question().contains(w.hanzi)) fail();
      if (e.type() == ExerciseType.FILL_IN_THE_BLANK
          && (!e.question().contains("___") || answers.stream().anyMatch(a -> !a.equals(w.hanzi))))
        fail();
      Set<Integer> declared = new HashSet<>(known);
      e.newVocabulary().forEach(s -> s.codePoints().forEach(declared::add));
      if (e.question()
          .codePoints()
          .anyMatch(
              cp ->
                  Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN
                      && !declared.contains(cp))) fail();
    }
  }

  private static boolean valid(String s, int max) {
    return s != null && !s.isBlank() && s.length() <= max;
  }

  private static void fail() {
    throw new ApiException(
        org.springframework.http.HttpStatus.BAD_GATEWAY, "Exercices générés invalides");
  }
}
