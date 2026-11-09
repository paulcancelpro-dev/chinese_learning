package com.chinese.learning.vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

public record WordView(
    UUID id,
    String hanzi,
    String pinyin,
    @JsonProperty("traductions_fr") Set<String> translations) {
  public static WordView of(Word w) {
    return new WordView(w.id, w.hanzi, w.pinyin, Set.copyOf(w.translations));
  }
}
