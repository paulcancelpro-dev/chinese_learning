package com.chinese.learning.exercise;

import java.text.Normalizer;
import java.util.Locale;

public final class AnswerNormalizer {
  private AnswerNormalizer() {}

  public static String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replace('’', '\'')
        .replaceAll("[\\p{Z}\\s]+", " ")
        .strip()
        .replaceAll("[.!?。！？]+$", "")
        .strip();
  }
}
