package com.chinese.learning.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable frontend contract, independent of Spring Data internals. */
public record PageResponse<T>(
    List<T> content, int number, int size, long totalElements, int totalPages) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
