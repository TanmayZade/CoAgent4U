package com.coagent4u.coordination.application.dto;

import java.util.List;

/**
 * Generic paginated response to avoid Spring dependencies in core.
 */
public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
