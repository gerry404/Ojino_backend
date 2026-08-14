package com.schoolcopilot.auth_service.web.admin.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Enveloppe de pagination.
 *
 * <p>On expose cette forme plutot que le {@code Page} de Spring Data : ce dernier
 * serialise une structure interne, non garantie d'une version a l'autre, alors
 * qu'un contrat d'API doit rester stable.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
