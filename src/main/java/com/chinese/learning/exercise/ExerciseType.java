package com.chinese.learning.exercise;

import com.chinese.learning.progress.Skill;

public enum ExerciseType {
  HANZI_TO_FRENCH,
  HANZI_TO_PINYIN,
  PINYIN_TO_HANZI,
  MULTIPLE_CHOICE,
  SENTENCE_TRANSLATION,
  FILL_IN_THE_BLANK;

  public Skill skill() {
    return switch (this) {
      case HANZI_TO_PINYIN -> Skill.HANZI_TO_PINYIN;
      case PINYIN_TO_HANZI, MULTIPLE_CHOICE, FILL_IN_THE_BLANK -> Skill.HANZI_RECOGNITION;
      default -> Skill.HANZI_TO_FRENCH;
    };
  }

  public boolean simple() {
    return this != SENTENCE_TRANSLATION && this != FILL_IN_THE_BLANK;
  }
}
