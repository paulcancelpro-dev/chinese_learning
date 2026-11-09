package com.chinese.learning.vocabulary;

import com.chinese.learning.common.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional
public class VocabularyService {
  private final WordRepository words;
  private final JsonImport importer;

  public VocabularyService(WordRepository words, JsonImport importer) {
    this.words = words;
    this.importer = importer;
  }

  public Word get(UUID id) {
    return words.findById(id).orElseThrow(ApiException::missing);
  }

  public WordView view(UUID id) {
    return WordView.of(get(id));
  }

  public Page<WordView> list(String q, int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw ApiException.bad("Pagination : page >= 0, size entre 1 et 100");
    return words
        .search(q, PageRequest.of(page, size, Sort.by("hanzi", "pinyin")))
        .map(WordView::of);
  }

  public List<WordDto> export() {
    return words.findAll().stream()
        .map(w -> new WordDto(w.hanzi, w.pinyin, List.copyOf(w.translations)))
        .toList();
  }

  public Map<String, Integer> importWords(JsonNode json) {
    var entries = importer.parse(json, "vocabulary", WordDto.class);
    int created = 0;
    for (var raw : entries) {
      var d = raw.normalized();
      var found = words.findByHanziAndPinyin(d.hanzi(), d.pinyin());
      if (found.isPresent()) found.get().translations.addAll(d.translations());
      else {
        words.saveAndFlush(new Word(d.hanzi(), d.pinyin(), d.translations()));
        created++;
      }
    }
    return Map.of("created", created, "merged", entries.size() - created);
  }

  public WordView create(WordDto raw) {
    var d = raw.normalized();
    return WordView.of(words.saveAndFlush(new Word(d.hanzi(), d.pinyin(), d.translations())));
  }

  public WordView update(UUID id, WordDto raw) {
    var d = raw.normalized();
    var w = words.lock(id).orElseThrow(ApiException::missing);
    w.hanzi = d.hanzi();
    w.pinyin = d.pinyin();
    w.translations.clear();
    w.translations.addAll(d.translations());
    words.flush();
    return WordView.of(w);
  }

  public void delete(UUID id) {
    words.delete(get(id));
    words.flush();
  }
}
