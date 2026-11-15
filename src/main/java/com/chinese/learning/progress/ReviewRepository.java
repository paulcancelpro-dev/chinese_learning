package com.chinese.learning.progress;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEvent, UUID> {
  List<ReviewEvent> findByWordIdAndSkillOrderByReviewedAtAscIdAsc(UUID wordId, Skill skill);
}
