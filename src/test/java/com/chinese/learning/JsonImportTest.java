package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;

import com.chinese.learning.common.*;
import com.chinese.learning.vocabulary.WordDto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonImportTest {
  @Test
  void identifiesInvalidEntryBeforePersistence() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var mapper = JsonMapper.builder().build();
      var importer = new JsonImport(mapper, factory.getValidator());
      var root =
          mapper.readTree(
              "[{\"hanzi\":\"朋友\",\"pinyin\":\"péngyou\",\"traductions_fr\":[\"ami\"]},{\"hanzi\":\"\",\"pinyin\":\"\",\"traductions_fr\":[]}]");
      assertThatThrownBy(() -> importer.parse(root, "vocabulary", WordDto.class))
          .isInstanceOf(ApiException.class)
          .hasMessageContaining("entrée 1");
    }
  }

  @Test
  void supportsEnvelope() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var mapper = JsonMapper.builder().build();
      var importer = new JsonImport(mapper, factory.getValidator());
      assertThat(
              importer.parse(mapper.readTree("{\"vocabulary\":[]}"), "vocabulary", WordDto.class))
          .isEmpty();
    }
  }
}
