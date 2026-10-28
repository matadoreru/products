package com.mango.products.application;

import org.springframework.data.domain.Page;

public record PageInfo(int number, int size, long totalElements, int totalPages) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }
}
