package com.chinese.learning.statistics;

import com.chinese.learning.progress.*;
import com.chinese.learning.vocabulary.WordRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatisticsService {
  private final ProgressService progress;
  private final ReviewRepository events;
  private final WordRepository words;
  private final Clock clock;

  public StatisticsService(
      ProgressService progress, ReviewRepository events, WordRepository words, Clock clock) {
    this.progress = progress;
    this.events = events;
    this.words = words;
    this.clock = clock;
  }

  public record Overview(
      long words,
      long evaluatedSkills,
      long masteredSkills,
      long dueSkills,
      long answers,
      long successes) {}

  public Overview overview() {
    var p = progress.list(false);
    var e = events.findAll();
    return new Overview(
        words.count(),
        p.stream().filter(x -> x.level() != null).count(),
        p.stream().filter(x -> x.level() != null && x.level() >= 4).count(),
        progress.list(true).size(),
        e.size(),
        e.stream().filter(x -> x.correct).count());
  }

  public List<ProgressView> weak() {
    return progress.list(false).stream()
        .filter(p -> p.level() != null && p.level() <= 2)
        .sorted(
            Comparator.comparing(ProgressView::level)
                .thenComparing(ProgressView::errors, Comparator.reverseOrder()))
        .toList();
  }

  public record Daily(LocalDate date, long completed, long due) {}

  public List<Daily> revisions() {
    var e = events.findAll();
    var p = progress.list(false);
    var today = LocalDate.now(clock);
    var result = new ArrayList<Daily>();
    for (int i = -7; i <= 30; i++) {
      var day = today.plusDays(i);
      result.add(
          new Daily(
              day,
              e.stream()
                  .filter(x -> x.reviewedAt.atZone(ZoneOffset.UTC).toLocalDate().equals(day))
                  .count(),
              p.stream()
                  .filter(
                      x ->
                          x.nextReview() != null
                              && x.nextReview().atZone(ZoneOffset.UTC).toLocalDate().equals(day))
                  .count()));
    }
    return result;
  }
}
