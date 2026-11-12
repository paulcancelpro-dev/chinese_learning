package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chinese.learning.exercise.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExerciseReuseTest {
  @Test
  void reuseRouteNeverInvokesGeneration() {
    var generator = mock(ExerciseService.class);
    var storage = mock(ExerciseStorage.class);
    var reuse = mock(ExerciseReuseService.class);
    var request = new ReuseRequest(1, List.of(ExerciseType.SENTENCE_TRANSLATION), null);
    var expected = new ExerciseService.Batch("reused", List.of());
    when(reuse.reuse(request)).thenReturn(expected);
    var controller = new ExerciseController(generator, storage, reuse);
    assertThat(controller.reuse(request)).isSameAs(expected);
    verify(reuse).reuse(request);
    verifyNoInteractions(generator, storage);
  }
}
