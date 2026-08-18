package com.avbooknest.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content, long totalElements, int totalPages, int page, int size, boolean hasNext) {

  public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
    return new PageResponse<>(
        source.getContent().stream().map(mapper).toList(),
        source.getTotalElements(),
        source.getTotalPages(),
        source.getNumber(),
        source.getSize(),
        source.hasNext());
  }
}
