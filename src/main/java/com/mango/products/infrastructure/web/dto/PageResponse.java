package com.mango.products.infrastructure.web.dto;

import com.mango.products.application.PageInfo;

public record PageResponse(int number, int size, long totalElements, int totalPages) {

    public static PageResponse from(PageInfo page) {
        return new PageResponse(page.number(), page.size(), page.totalElements(), page.totalPages());
    }
}
