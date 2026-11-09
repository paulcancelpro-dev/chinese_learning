package com.chinese.learning.vocabulary;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {
  private final VocabularyService service;

  public VocabularyController(VocabularyService service) {
    this.service = service;
  }

  @PostMapping("/import")
  public Map<String, Integer> importWords(@RequestBody JsonNode body) {
    return service.importWords(body);
  }

  @GetMapping
  public com.chinese.learning.common.PageResponse<WordView> list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return com.chinese.learning.common.PageResponse.from(service.list(q, page, size));
  }

  @GetMapping("/export")
  public Map<String, List<WordDto>> export() {
    return Map.of("vocabulary", service.export());
  }

  @GetMapping("/{id}")
  public WordView get(@PathVariable UUID id) {
    return service.view(id);
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public WordView create(@Valid @RequestBody WordDto body) {
    return service.create(body);
  }

  @PutMapping("/{id}")
  public WordView update(@PathVariable UUID id, @Valid @RequestBody WordDto body) {
    return service.update(id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
