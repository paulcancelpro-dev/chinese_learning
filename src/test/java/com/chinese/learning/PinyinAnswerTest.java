package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chinese.learning.exercise.*;
import com.chinese.learning.progress.ProgressService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PinyinAnswerTest {
  @Test
  void removesAllFourToneMarksAndHandlesDecomposedUnicode() {
    assertThat(AnswerNormalizer.normalize("Ā Á Ǎ À", ExerciseType.HANZI_TO_PINYIN))
        .isEqualTo("a a a a");
    assertThat(AnswerNormalizer.normalize("  PE\u0301NGYOU！ ", ExerciseType.HANZI_TO_PINYIN))
        .isEqualTo("pengyou");
    assertThat(AnswerNormalizer.normalize("shuǐ", ExerciseType.HANZI_TO_PINYIN)).isEqualTo("shui");
  }

  @Test
  void preservesUmlautAndFrenchAccents() {
    assertThat(AnswerNormalizer.normalize("lǜ", ExerciseType.HANZI_TO_PINYIN))
        .isEqualTo("lü")
        .isNotEqualTo("lu");
    assertThat(AnswerNormalizer.normalize("été", ExerciseType.HANZI_TO_FRENCH)).isEqualTo("été");
  }

  @Test
  void acceptsTonelessPinyinAndReplaysAccentedAnswerWithoutUpdatingProgressAgain() {
    var exercises = mock(ExerciseRepository.class);
    var progress = mock(ProgressService.class);
    var wordId = UUID.randomUUID();
    var exercise =
        new Exercise(
            UUID.randomUUID(),
            new GeneratedExercise(
                wordId,
                ExerciseType.HANZI_TO_PINYIN,
                "朋友",
                List.of(),
                1,
                List.of("péngyou"),
                "朋友 : péngyou",
                List.of()));
    var now = Instant.parse("2026-11-11T12:00:00Z");
    when(exercises.lock(exercise.id)).thenReturn(Optional.of(exercise));
    when(progress.recordAnswer(eq(exercise.id), eq(wordId), any(), eq(true), eq("pengyou")))
        .thenReturn(now);
    var storage =
        new ExerciseStorage(exercises, mock(SessionRepository.class), progress, Clock.systemUTC());
    var correction = storage.answer(exercise.id, "pengyou");
    assertThat(correction.correct()).isTrue();
    assertThat(correction.acceptedAnswers()).containsExactly("péngyou");
    assertThat(storage.answer(exercise.id, "PÉNGYOU")).isEqualTo(correction);
    verify(progress, times(1))
        .recordAnswer(eq(exercise.id), eq(wordId), any(), eq(true), eq("pengyou"));
    verifyNoMoreInteractions(progress);
  }

  @Test
  void stillRejectsWrongLetters() {
    assertThat(AnswerNormalizer.normalize("pengyau", ExerciseType.HANZI_TO_PINYIN))
        .isNotEqualTo(AnswerNormalizer.normalize("péngyou", ExerciseType.HANZI_TO_PINYIN));
  }
}
