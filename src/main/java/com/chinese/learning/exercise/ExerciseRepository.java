package com.chinese.learning.exercise;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Exercise e where e.id=:id")
  Optional<Exercise> lock(@Param("id") UUID id);

  Page<Exercise> findByAnsweredAtIsNotNull(Pageable page);
}
