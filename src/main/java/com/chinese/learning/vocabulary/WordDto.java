package com.chinese.learning.vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.*;

public record WordDto(
    @NotBlank @Size(max = 100) String hanzi,
    @NotBlank @Size(max = 200) String pinyin,
    @JsonProperty("traductions_fr") @NotEmpty @Size(max = 30)
        List<@NotBlank @Size(max = 300) String> translations) {
  public WordDto normalized() {
    return new WordDto(
        hanzi.strip(),
        pinyin.strip().toLowerCase(Locale.ROOT),
        translations.stream().map(String::strip).distinct().toList());
  }
}
