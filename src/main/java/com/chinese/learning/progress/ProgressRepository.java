package com.chinese.learning.progress;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<Progress, UUID> {
  Optional<Progress> findByWordIdAndSkill(UUID wordId, Skill skill);
}
