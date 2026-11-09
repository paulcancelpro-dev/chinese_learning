package com.chinese.learning.progress;

import java.time.Instant;
import java.util.UUID;

public record ProgressView(
    UUID wordId,
    Skill skill,
    Integer level,
    long successes,
    long errors,
    Instant lastReview,
    Instant nextReview) {
  public static ProgressView of(Progress p) {
    return new ProgressView(
        p.wordId, p.skill, p.level, p.successes, p.errors, p.lastReview, p.nextReview);
  }
}
