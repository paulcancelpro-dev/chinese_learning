package com.chinese.learning.exercise;

import com.chinese.learning.common.ApiException;
import com.chinese.learning.vocabulary.Word;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class LocalExerciseGenerator implements ExerciseGenerator {
  public List<GeneratedExercise> generate(GenerationRequest request, List<Word> words) {
    if (request.types().stream().anyMatch(t -> !t.simple()))
      throw ApiException.bad("Les phrases et textes à trous nécessitent OpenAI");
    var result = new ArrayList<GeneratedExercise>();
    for (int i = 0; i < request.count(); i++) {
      var w = words.get(i % words.size());
      var type = request.types().get(i % request.types().size());
      String question =
          switch (type) {
            case PINYIN_TO_HANZI -> w.pinyin + " — " + w.translations.iterator().next();
            case MULTIPLE_CHOICE -> w.translations.iterator().next() + " (" + w.pinyin + ")";
            default -> w.hanzi;
          };
      List<String> answers =
          switch (type) {
            case HANZI_TO_PINYIN -> List.of(w.pinyin);
            case PINYIN_TO_HANZI, MULTIPLE_CHOICE -> List.of(w.hanzi);
            default -> List.copyOf(w.translations);
          };
      var choices = new ArrayList<String>();
      if (type == ExerciseType.MULTIPLE_CHOICE) {
        choices.add(w.hanzi);
        words.stream()
            .filter(
                other ->
                    !other.hanzi.equals(w.hanzi)
                        && Collections.disjoint(other.translations, w.translations))
            .map(other -> other.hanzi)
            .distinct()
            .limit(3)
            .forEach(choices::add);
        if (choices.size() < 2)
          throw ApiException.bad(
              "QCM : au moins deux mots de significations distinctes sont nécessaires");
        Collections.shuffle(choices);
      }
      result.add(
          new GeneratedExercise(
              w.id,
              type,
              question,
              choices,
              request.difficulty(),
              answers,
              w.hanzi + " — " + w.pinyin + " : " + String.join(", ", w.translations),
              List.of()));
    }
    return result;
  }
}
