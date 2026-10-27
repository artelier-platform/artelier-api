package com.artelier.api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/** Evita ordenar por columnas arbitrarias (p. ej. password_hash) y limita el tamano de pagina. */
public final class PageSanitizer {

    private static final int MAX_SIZE = 100;

    private PageSanitizer() {}

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSort, Sort fallback) {
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(o -> allowedSort.contains(o.getProperty()))
                .toList();
        Sort sort = orders.isEmpty() ? fallback : Sort.by(orders);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
