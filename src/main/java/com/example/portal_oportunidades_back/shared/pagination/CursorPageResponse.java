package com.example.portal_oportunidades_back.shared.pagination;

import java.util.List;

/**
 * Generic response envelope for forward cursor-based pagination.
 *
 * @param <T> item type returned by the paginated resource
 */
public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {
    public CursorPageResponse {
        items = List.copyOf(items);
    }
}
