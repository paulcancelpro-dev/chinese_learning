package com.chinese.learning.statistics;

import com.chinese.learning.progress.ProgressView;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
  private final StatisticsService service;

  public StatisticsController(StatisticsService service) {
    this.service = service;
  }

  @GetMapping("/overview")
  public StatisticsService.Overview overview() {
    return service.overview();
  }

  @GetMapping("/weak-words")
  public List<ProgressView> weak() {
    return service.weak();
  }

  @GetMapping("/revisions")
  public List<StatisticsService.Daily> revisions() {
    return service.revisions();
  }
}
