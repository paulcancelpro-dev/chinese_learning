package com.chinese.learning.progress;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEvent, UUID> {}
