package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;

import com.chinese.learning.common.ApiException;
import com.chinese.learning.exercise.*;
import com.chinese.learning.vocabulary.Word;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExerciseRulesTest {
  final Word word = new Word("朋友", "péngyou", List.of("ami"));
  final GenerationRequest request =
      new GenerationRequest(
          1, List.of(ExerciseType.HANZI_TO_FRENCH), 1, GenerationRequest.Selection.NEW);

  @Test
  void normalizesTypographyWithoutLosingTones() {
    assertThat(AnswerNormalizer.normalize("  L’AMI！  ")).isEqualTo("l'ami");
    assertThat(AnswerNormalizer.normalize("PÉNGYOU")).isEqualTo("péngyou");
    assertThat(AnswerNormalizer.normalize("pengyou")).isNotEqualTo("péngyou");
  }

  @Test
  void localOutputPassesValidation() {
    var generated = new LocalExerciseGenerator().generate(request, List.of(word));
    new GeneratedExerciseValidator().validate(generated, request, List.of(word));
    assertThat(generated.getFirst().acceptedAnswers()).containsExactly("ami");
  }

  @Test
  void rejectsUnknownTarget() {
    var e =
        new GeneratedExercise(
            UUID.randomUUID(),
            ExerciseType.HANZI_TO_FRENCH,
            "朋友",
            List.of(),
            1,
            List.of("ami"),
            "Ami",
            List.of());
    assertThatThrownBy(
            () -> new GeneratedExerciseValidator().validate(List.of(e), request, List.of(word)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsWrongCorrection() {
    var e =
        new GeneratedExercise(
            word.id,
            ExerciseType.HANZI_TO_FRENCH,
            "朋友",
            List.of(),
            1,
            List.of("ennemi"),
            "Ami",
            List.of());
    assertThatThrownBy(
            () -> new GeneratedExerciseValidator().validate(List.of(e), request, List.of(word)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void refusesFakeLocalSentence() {
    var r =
        new GenerationRequest(
            1, List.of(ExerciseType.SENTENCE_TRANSLATION), 1, GenerationRequest.Selection.NEW);
    assertThatThrownBy(() -> new LocalExerciseGenerator().generate(r, List.of(word)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void qcmNeedsDistinctMeanings() {
    var r =
        new GenerationRequest(
            1, List.of(ExerciseType.MULTIPLE_CHOICE), 1, GenerationRequest.Selection.NEW);
    assertThatThrownBy(() -> new LocalExerciseGenerator().generate(r, List.of(word)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsUnannouncedCharacters() {
    var r =
        new GenerationRequest(
            1, List.of(ExerciseType.SENTENCE_TRANSLATION), 1, GenerationRequest.Selection.NEW);
    var e =
        new GeneratedExercise(
            word.id,
            ExerciseType.SENTENCE_TRANSLATION,
            "我的朋友",
            List.of(),
            1,
            List.of("mon ami"),
            "Ami",
            List.of());
    assertThatThrownBy(
            () -> new GeneratedExerciseValidator().validate(List.of(e), r, List.of(word)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void allLocalTypesSatisfyBusinessConstraints() {
    var r =
        new GenerationRequest(
            4,
            List.of(
                ExerciseType.HANZI_TO_FRENCH,
                ExerciseType.HANZI_TO_PINYIN,
                ExerciseType.PINYIN_TO_HANZI,
                ExerciseType.MULTIPLE_CHOICE),
            2,
            GenerationRequest.Selection.NEW);
    var words = List.of(word, new Word("水", "shuǐ", List.of("eau")));
    var generated = new LocalExerciseGenerator().generate(r, words);
    new GeneratedExerciseValidator().validate(generated, r, words);
    assertThat(generated).hasSize(4);
  }
}
