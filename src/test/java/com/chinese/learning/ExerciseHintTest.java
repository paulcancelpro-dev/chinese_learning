package com.chinese.learning;

import com.chinese.learning.exercise.*;
import com.chinese.learning.progress.ProgressService;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExerciseHintTest {
  @Test void hintReturnsOnlyFirstGraphemeWithoutRecordingAnswer() {
    var repository = mock(ExerciseRepository.class);
    var progress = mock(ProgressService.class);
    var exercise = mock(Exercise.class);
    exercise.acceptedAnswers = List.of("e\u0301cole", "étude");
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(exercise));
    var storage = new ExerciseStorage(repository, mock(SessionRepository.class), progress, Clock.systemUTC());
    assertThat(storage.hint(id).firstLetter()).isEqualTo("e\u0301");
    assertThat(exercise.answeredAt).isNull();
    verifyNoInteractions(progress);
    verify(repository, never()).lock(any());
    verify(repository, never()).save(any());
  }

  @Test void missingExerciseDoesNotProduceAHint() {
    var repository = mock(ExerciseRepository.class);
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());
    var storage = new ExerciseStorage(repository, mock(SessionRepository.class), mock(ProgressService.class), Clock.systemUTC());
    assertThatThrownBy(() -> storage.hint(id)).isInstanceOf(com.chinese.learning.common.ApiException.class);
  }
}
