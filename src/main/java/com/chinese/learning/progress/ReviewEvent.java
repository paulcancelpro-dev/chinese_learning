package com.chinese.learning.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_event")
public class ReviewEvent {
  @Id public UUID id;

  @Column(nullable = false)
  public UUID wordId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Skill skill;

  public boolean correct;

  @Column(nullable = false)
  public Instant reviewedAt;

  @Column(length = 2000, nullable = false)
  public String answer;

  protected ReviewEvent() {}

  public ReviewEvent(
      UUID id, UUID wordId, Skill skill, boolean correct, Instant time, String answer) {
    this.id = id;
    this.wordId = wordId;
    this.skill = skill;
    this.correct = correct;
    this.reviewedAt = time;
    this.answer = answer;
  }
}
