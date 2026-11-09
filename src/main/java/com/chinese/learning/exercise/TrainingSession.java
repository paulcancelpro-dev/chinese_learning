package com.chinese.learning.exercise;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class TrainingSession {
  @Id public UUID id = UUID.randomUUID();
  public Instant createdAt;
  public String generator;

  protected TrainingSession() {}

  public TrainingSession(Instant now, String generator) {
    this.createdAt = now;
    this.generator = generator;
  }
}
