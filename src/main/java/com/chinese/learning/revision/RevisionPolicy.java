package com.chinese.learning.revision;

import com.chinese.learning.progress.Progress;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class RevisionPolicy {
  private static final int[] DAYS = {0, 1, 3, 7, 14, 30};

  public void apply(Progress p, boolean correct, Instant now) {
    if (correct) {
      p.successes++;
      p.streak++;
      p.level =
          Math.min(5, (p.level == null ? 0 : p.level) + (p.streak >= 2 || p.level == null ? 1 : 0));
    } else {
      p.errors++;
      p.streak = 0;
      p.level = Math.max(0, (p.level == null ? 0 : p.level) - 1);
    }
    p.lastReview = now;
    double reliability = (p.successes + 1.0) / (p.successes + p.errors + 1.0);
    p.nextReview =
        correct
            ? now.plus(Duration.ofHours(Math.max(12, Math.round(DAYS[p.level] * 24 * reliability))))
            : now.plus(Duration.ofMinutes(10));
  }
}
