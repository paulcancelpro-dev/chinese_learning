package com.chinese.learning.exercise;

import com.chinese.learning.common.ApiException;
import com.chinese.learning.progress.*;
import com.chinese.learning.vocabulary.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordSelection {
  private final WordRepository words;
  private final ProgressRepository progress;
  private final Clock clock;

  public WordSelection(WordRepository words, ProgressRepository progress, Clock clock) {
    this.words = words;
    this.progress = progress;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<Word> select(GenerationRequest request) {
    var skills =
        request.types().stream()
            .map(ExerciseType::skill)
            .collect(java.util.stream.Collectors.toSet());
    var index = new HashMap<UUID, List<Progress>>();
    progress.findAll().stream()
        .filter(p -> skills.contains(p.skill))
        .forEach(p -> index.computeIfAbsent(p.wordId, k -> new ArrayList<>()).add(p));
    var candidates =
        new ArrayList<>(
            words.findAll().stream()
                .filter(
                    w -> {
                      var p = index.getOrDefault(w.id, List.of());
                      return switch (request.strategy()) {
                        case RANDOM -> true;
                        case NEW ->
                            p.size() < skills.size() || p.stream().anyMatch(x -> x.level == null);
                        case DIFFICULT ->
                            p.stream()
                                .anyMatch(
                                    x ->
                                        x.level != null
                                            && (x.level <= 2 || x.errors > x.successes));
                        case DUE ->
                            p.size() < skills.size()
                                || p.stream()
                                    .anyMatch(
                                        x ->
                                            x.nextReview == null
                                                || !x.nextReview.isAfter(clock.instant()));
                      };
                    })
                .toList());
    if (request.strategy() == GenerationRequest.Selection.RANDOM) Collections.shuffle(candidates);
    else
      candidates.sort(
          Comparator.comparing(
                  (Word w) -> index.getOrDefault(w.id, List.of()).size() >= skills.size())
              .thenComparing(
                  w ->
                      index.getOrDefault(w.id, List.of()).stream()
                          .map(p -> p.nextReview)
                          .filter(Objects::nonNull)
                          .min(Comparator.naturalOrder())
                          .orElse(java.time.Instant.MIN))
              .thenComparing(w -> w.id));
    if (candidates.isEmpty()) throw ApiException.bad("Aucun mot disponible pour cette sélection");
    // Detach a bounded vocabulary snapshot before any network request.
    return candidates.stream()
        .limit(Math.max(request.count(), 4))
        .map(
            w -> {
              var copy = new Word(w.hanzi, w.pinyin, w.translations);
              copy.id = w.id;
              return copy;
            })
        .toList();
  }
}
