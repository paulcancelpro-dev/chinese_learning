package com.chinese.learning.config;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class TimeConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
