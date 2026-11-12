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

  /** Ignore pinyin tone marks only; ü remains distinct from u. */
  public static String normalize(String value, ExerciseType type) {
    String normalized = normalize(value);
    if (type != ExerciseType.HANZI_TO_PINYIN) return normalized;
    return Normalizer.normalize(
        Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replaceAll("[\u0300\u0301\u0304\u030C]", ""),
        Normalizer.Form.NFC);
  }
}
