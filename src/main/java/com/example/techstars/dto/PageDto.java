package com.example.techstars.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class PageDto<T> {
    private final List<T> content;
    private final int currentPage;
    private final int totalPages;
    private final long totalElements;

    public PageDto(List<T> content, org.springframework.data.domain.Page<?> page) {
        this.content = content;
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
    }
}