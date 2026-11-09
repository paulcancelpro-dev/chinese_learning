package com.chinese.learning.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress", uniqueConstraints = @UniqueConstraint(columnNames = {"word_id", "skill"}))
public class Progress {
  @Id public UUID id = UUID.randomUUID();

  @Column(name = "word_id", nullable = false)
  public UUID wordId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Skill skill;

  public Integer level;
  public long successes;
  public long errors;
  public int streak;
  public Instant lastReview;
  public Instant nextReview;

  protected Progress() {}

  public Progress(UUID wordId, Skill skill) {
    this.wordId = wordId;
    this.skill = skill;
  }
}
