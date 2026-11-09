package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chinese.learning.ai.*;
import com.chinese.learning.common.ApiException;
import com.chinese.learning.exercise.*;
import com.chinese.learning.vocabulary.Word;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ExerciseServiceTest {
  @Test
  void upstreamFailureFallsBackOnlyForSimpleExercises() {
    var selection = mock(WordSelection.class);
    var ai = mock(OpenAiExerciseGenerator.class);
    var storage = mock(ExerciseStorage.class);
    var request =
        new GenerationRequest(
            1, List.of(ExerciseType.HANZI_TO_FRENCH), 1, GenerationRequest.Selection.NEW);
    var word = new Word("朋友", "péngyou", List.of("ami"));
    when(selection.select(any())).thenReturn(List.of(word));
    when(ai.enabled()).thenReturn(true);
    when(ai.generate(any(), any()))
        .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "Unavailable"));
    when(storage.save(any(), any())).thenReturn(List.of());
    var service =
        new ExerciseService(
            new GenerationBudget(Clock.systemUTC(), 5),
            selection,
            new LocalExerciseGenerator(),
            ai,
            new GeneratedExerciseValidator(),
            storage);
    assertThat(service.generate(request).source()).isEqualTo("local_fallback");
    verify(storage)
        .save(
            argThat(
                list ->
                    list.size() == 1 && list.getFirst().acceptedAnswers().equals(List.of("ami"))),
            eq("local_fallback"));
    var advanced =
        new GenerationRequest(
            1, List.of(ExerciseType.SENTENCE_TRANSLATION), 1, GenerationRequest.Selection.NEW);
    assertThatThrownBy(() -> service.generate(advanced)).isInstanceOf(ApiException.class);
    verifyNoMoreInteractions(storage);
  }

  @Test
  void budgetCapsRepeatedPaidAttempts() {
    var budget = new GenerationBudget(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 2);
    budget.acquire();
    budget.acquire();
    assertThatThrownBy(budget::acquire)
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Limite");
  }
}
