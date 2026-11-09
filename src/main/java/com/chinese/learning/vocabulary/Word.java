package com.chinese.learning.vocabulary;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "word", uniqueConstraints = @UniqueConstraint(columnNames = {"hanzi", "pinyin"}))
public class Word {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 100)
  public String hanzi;

  @Column(nullable = false, length = 200)
  public String pinyin;

  @ElementCollection
  @CollectionTable(name = "word_translation", joinColumns = @JoinColumn(name = "word_id"))
  @Column(name = "translation", nullable = false, length = 300)
  public Set<String> translations = new TreeSet<>();

  protected Word() {}

  public Word(String hanzi, String pinyin, Collection<String> translations) {
    this.hanzi = hanzi;
    this.pinyin = pinyin;
    this.translations.addAll(translations);
  }
}
