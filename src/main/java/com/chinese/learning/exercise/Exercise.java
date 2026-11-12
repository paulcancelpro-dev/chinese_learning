package com.chinese.learning.exercise;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
public class Exercise {
  @Id public UUID id = UUID.randomUUID();

  /** Original exercise supplying this attempt; null for newly generated content. */
  public UUID sourceExerciseId;

  public UUID sessionId;
  public UUID wordId;

  @Enumerated(EnumType.STRING)
  public ExerciseType type;

  @Column(length = 1000)
  public String question;

  public int difficulty;

  @ElementCollection
  @CollectionTable(name = "exercise_choice", joinColumns = @JoinColumn(name = "exercise_id"))
  @OrderColumn(name = "position")
  @Column(name = "value", length = 300)
  public List<String> choices = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "exercise_answer", joinColumns = @JoinColumn(name = "exercise_id"))
  @Column(name = "value", length = 2000)
  public List<String> acceptedAnswers = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "exercise_new_vocabulary",
      joinColumns = @JoinColumn(name = "exercise_id"))
  @Column(name = "value", length = 100)
  public List<String> newVocabulary = new ArrayList<>();

  @Column(length = 2000)
  public String explanation;

  @Column(length = 2000)
  public String submittedAnswer;

  public Boolean correct;
  public Instant answeredAt;

  protected Exercise() {}

  public Exercise(UUID sessionId, GeneratedExercise e) {
    this.sessionId = sessionId;
    wordId = e.wordId();
    type = e.type();
    question = e.question();
    difficulty = e.difficulty();
    choices.addAll(e.choices());
    acceptedAnswers.addAll(e.acceptedAnswers());
    newVocabulary.addAll(e.newVocabulary());
    explanation = e.explanation();
  }
}
