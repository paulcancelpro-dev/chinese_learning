package com.chinese.learning.progress;

import com.chinese.learning.common.*;
import com.chinese.learning.revision.RevisionPolicy;
import com.chinese.learning.vocabulary.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional
public class ProgressService {
  private final ProgressRepository progress;
  private final ReviewRepository events;
  private final WordRepository words;
  private final RevisionPolicy policy;
  private final JsonImport importer;
  private final Clock clock;

  public ProgressService(
      ProgressRepository progress,
      ReviewRepository events,
      WordRepository words,
      RevisionPolicy policy,
      JsonImport importer,
      Clock clock) {
    this.progress = progress;
    this.events = events;
    this.words = words;
    this.policy = policy;
    this.importer = importer;
    this.clock = clock;
  }

  public record ImportedReview(
      @NotNull UUID id,
      @NotBlank String hanzi,
      @NotBlank String pinyin,
      @NotNull Skill skill,
      @NotNull Boolean correct,
      @NotNull Instant reviewedAt,
      @NotNull @Size(max = 2000) String answer) {}

  /** Timestamp after the word lock: concurrent exercises must follow commit order. */
  public Instant recordAnswer(
      UUID eventId, UUID wordId, Skill skill, boolean correct, String answer) {
    words.lock(wordId).orElseThrow(ApiException::missing);
    Instant time = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var existing = progress.findByWordIdAndSkill(wordId, skill);
    if (existing.isPresent()
        && existing.get().lastReview != null
        && !time.isAfter(existing.get().lastReview)) {
      time = existing.get().lastReview.plusNanos(1000);
    }
    record(eventId, wordId, skill, correct, time, answer);
    return time;
  }

  public void record(
      UUID eventId, UUID wordId, Skill skill, boolean correct, Instant time, String answer) {
    words.lock(wordId).orElseThrow(ApiException::missing);
    if (events.existsById(eventId)) return;
    var p =
        progress.findByWordIdAndSkill(wordId, skill).orElseGet(() -> new Progress(wordId, skill));
    if (p.lastReview != null && time.isBefore(p.lastReview))
      throw ApiException.bad(
          "Une révision antérieure à l'historique existant ne peut pas être ajoutée");
    policy.apply(p, correct, time);
    progress.save(p);
    events.save(new ReviewEvent(eventId, wordId, skill, correct, time, answer));
  }

  public Map<String, Integer> importProgress(JsonNode body) {
    var input =
        importer.parse(body, "progress", ImportedReview.class).stream()
            .sorted(
                Comparator.comparing(ImportedReview::reviewedAt).thenComparing(ImportedReview::id))
            .toList();
    int created = 0;
    for (int i = 0; i < input.size(); i++) {
      var e = input.get(i);
      if (e.reviewedAt().isAfter(clock.instant()))
        throw ApiException.bad("Révision future interdite : " + e.id());
      var w =
          words
              .findByHanziAndPinyin(e.hanzi().strip(), e.pinyin().strip().toLowerCase(Locale.ROOT))
              .orElseThrow(() -> ApiException.bad("Mot inconnu pour événement " + e.id()));
      var old = events.findById(e.id());
      if (old.isPresent()) {
        var o = old.get();
        if (!o.wordId.equals(w.id)
            || o.skill != e.skill()
            || o.correct != e.correct()
            || !o.reviewedAt.equals(e.reviewedAt())
            || !o.answer.equals(e.answer()))
          throw ApiException.bad(
              "Identifiant de révision réutilisé avec un contenu différent : " + e.id());
        continue;
      }
      record(e.id(), w.id, e.skill(), e.correct(), e.reviewedAt(), e.answer());
      created++;
    }
    return Map.of("created", created, "ignored", input.size() - created);
  }

  public List<ProgressView> list(boolean due) {
    Map<String, Progress> index = new HashMap<>();
    progress.findAll().forEach(p -> index.put(p.wordId + ":" + p.skill, p));
    var result = new ArrayList<ProgressView>();
    for (var w : words.findAll())
      for (var skill : Skill.values()) {
        if (skill == Skill.WRITING) continue;
        var p = index.getOrDefault(w.id + ":" + skill, new Progress(w.id, skill));
        if (!due || p.nextReview == null || !p.nextReview.isAfter(clock.instant()))
          result.add(ProgressView.of(p));
      }
    result.sort(
        Comparator.comparing(
                ProgressView::nextReview, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(ProgressView::wordId)
            .thenComparing(ProgressView::skill));
    return result;
  }

  public List<ImportedReview> export() {
    Map<UUID, Word> index = new HashMap<>();
    words.findAll().forEach(w -> index.put(w.id, w));
    return events.findAll().stream()
        .sorted(Comparator.comparing((ReviewEvent e) -> e.reviewedAt).thenComparing(e -> e.id))
        .map(
            e -> {
              var w = index.get(e.wordId);
              return new ImportedReview(
                  e.id, w.hanzi, w.pinyin, e.skill, e.correct, e.reviewedAt, e.answer);
            })
        .toList();
  }
}
