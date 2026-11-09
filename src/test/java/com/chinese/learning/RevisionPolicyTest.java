package com.chinese.learning;

import static org.assertj.core.api.Assertions.*;

import com.chinese.learning.progress.*;
import com.chinese.learning.revision.RevisionPolicy;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RevisionPolicyTest {
  final RevisionPolicy policy = new RevisionPolicy();
  final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void unseenIsNotLevelZero() {
    var p = new Progress(UUID.randomUUID(), Skill.HANZI_RECOGNITION);
    assertThat(p.level).isNull();
    assertThat(p.nextReview).isNull();
  }

  @Test
  void failureReturnsInTenMinutes() {
    var p = new Progress(UUID.randomUUID(), Skill.HANZI_RECOGNITION);
    policy.apply(p, false, now);
    assertThat(p.level).isZero();
    assertThat(p.errors).isEqualTo(1);
    assertThat(p.nextReview).isEqualTo(now.plusSeconds(600));
  }

  @Test
  void masteryIsBoundedAndNeverPermanent() {
    var p = new Progress(UUID.randomUUID(), Skill.HANZI_RECOGNITION);
    for (int i = 0; i < 20; i++) policy.apply(p, true, now);
    assertThat(p.level).isEqualTo(5);
    assertThat(p.successes).isEqualTo(20);
    assertThat(p.nextReview).isEqualTo(now.plus(Duration.ofDays(30)));
    policy.apply(p, false, now);
    assertThat(p.level).isEqualTo(4);
    assertThat(p.streak).isZero();
  }

  @Test
  void previousFailuresShortenInterval() {
    var a = new Progress(UUID.randomUUID(), Skill.HANZI_RECOGNITION);
    var b = new Progress(UUID.randomUUID(), Skill.HANZI_RECOGNITION);
    policy.apply(b, false, now);
    policy.apply(a, true, now);
    policy.apply(b, true, now);
    assertThat(b.nextReview).isBefore(a.nextReview);
  }
}
