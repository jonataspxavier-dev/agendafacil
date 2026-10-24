package com.agendafacil.backend.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableFactory {

    private PageableFactory() {
    }

    public static Pageable from(Integer page, Integer size, String sort) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, parts[0]));
    }
}
