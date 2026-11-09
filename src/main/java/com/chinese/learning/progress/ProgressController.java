package com.chinese.learning.progress;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
  private final ProgressService service;

  public ProgressController(ProgressService service) {
    this.service = service;
  }

  @PostMapping("/import")
  public Map<String, Integer> importProgress(@RequestBody JsonNode body) {
    return service.importProgress(body);
  }

  @GetMapping
  public List<ProgressView> list() {
    return service.list(false);
  }

  @GetMapping("/due")
  public List<ProgressView> due() {
    return service.list(true);
  }

  @GetMapping("/export")
  public Map<String, List<ProgressService.ImportedReview>> export() {
    return Map.of("progress", service.export());
  }
}
