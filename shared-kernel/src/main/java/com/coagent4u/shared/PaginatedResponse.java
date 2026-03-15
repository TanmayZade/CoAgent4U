package com.coagent4u.shared;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Lives in shared-kernel so all modules can use it without cross-dependencies.
 */
public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
