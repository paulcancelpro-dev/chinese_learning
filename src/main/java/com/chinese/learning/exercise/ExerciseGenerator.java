package com.chinese.learning.exercise;

import com.chinese.learning.vocabulary.Word;
import java.util.List;

public interface ExerciseGenerator {
  List<GeneratedExercise> generate(GenerationRequest request, List<Word> words);
}
