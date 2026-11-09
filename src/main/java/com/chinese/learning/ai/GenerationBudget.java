package com.chinese.learning.ai;

import com.chinese.learning.common.ApiException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Per-process minute window, including failed calls. Provider billing limits remain necessary. */
@Component
public class GenerationBudget {
  private final Clock clock;
  private final int limit;
  private long window = -1;
  private int used;

  public GenerationBudget(Clock clock, @Value("${app.openai.requests-per-minute:5}") int limit) {
    this.clock = clock;
    this.limit = limit;
  }

  public synchronized void acquire() {
    long current = clock.instant().getEpochSecond() / 60;
    if (current != window) {
      window = current;
      used = 0;
    }
    if (used >= limit)
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS, "Limite de générations OpenAI par minute atteinte");
    used++;
  }
}
