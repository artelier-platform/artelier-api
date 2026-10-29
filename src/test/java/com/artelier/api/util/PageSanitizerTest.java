
package com.artelier.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PageSanitizer Unit Tests")
class PageSanitizerTest {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("id", "name", "createdAt");

    private static final Sort FALLBACK_SORT =
            Sort.by(Sort.Direction.ASC, "id");

    @Nested
    @DisplayName("Page Size Validation")
    class PageSizeValidationTests {

        @Test
        @DisplayName("Should preserve the requested page size when within the allowed range")
        void shouldPreserveValidPageSize() {
            Pageable pageable = PageRequest.of(0, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(20, result.getPageSize());
        }

        @Test
        @DisplayName("Should limit the page size to 100 when it exceeds the maximum")
        void shouldLimitPageSizeToMaximum() {
            Pageable pageable = PageRequest.of(0, 150);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(MAX_PAGE_SIZE, result.getPageSize());
        }

        @Test
        @DisplayName("Should preserve the maximum allowed page size")
        void shouldPreserveMaximumPageSize() {
            Pageable pageable = PageRequest.of(0, MAX_PAGE_SIZE);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(MAX_PAGE_SIZE, result.getPageSize());
        }

        @Test
        @DisplayName("Should preserve a page size of one")
        void shouldPreserveMinimumPageSize() {
            Pageable pageable = PageRequest.of(0, 1);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(1, result.getPageSize());
        }
    }

    @Nested
    @DisplayName("Page Number Validation")
    class PageNumberValidationTests {

        @Test
        @DisplayName("Should preserve the requested page number")
        void shouldPreservePageNumber() {
            Pageable pageable = PageRequest.of(5, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(5, result.getPageNumber());
        }

        @Test
        @DisplayName("Should preserve the first page")
        void shouldPreserveFirstPage() {
            Pageable pageable = PageRequest.of(0, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(0, result.getPageNumber());
        }
    }

    @Nested
    @DisplayName("Sort Property Validation")
    class SortPropertyValidationTests {

        @Test
        @DisplayName("Should preserve allowed sort properties")
        void shouldPreserveAllowedSortProperty() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(Sort.Direction.ASC, "name")
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(
                    Sort.by(Sort.Direction.ASC, "name"),
                    result.getSort()
            );
        }

        @Test
        @DisplayName("Should preserve the direction of an allowed sort property")
        void shouldPreserveSortDirection() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(
                    Sort.by(Sort.Direction.DESC, "createdAt"),
                    result.getSort()
            );
        }

        @Test
        @DisplayName("Should remove disallowed sort properties")
        void shouldRemoveDisallowedSortProperties() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(
                            Sort.Order.asc("name"),
                            Sort.Order.desc("password_hash")
                    )
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(
                    Sort.by(Sort.Order.asc("name")),
                    result.getSort()
            );

            assertFalse(
                    result.getSort().getOrderFor("password_hash") != null
            );
        }

        @Test
        @DisplayName("Should use the fallback sort when all properties are disallowed")
        void shouldUseFallbackWhenAllSortPropertiesAreDisallowed() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(
                            Sort.Direction.ASC,
                            "password_hash"
                    )
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(FALLBACK_SORT, result.getSort());
        }

        @Test
        @DisplayName("Should use the fallback sort when no sort is requested")
        void shouldUseFallbackWhenSortIsUnspecified() {
            Pageable pageable = PageRequest.of(0, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(FALLBACK_SORT, result.getSort());
        }

        @Test
        @DisplayName("Should preserve multiple allowed sort properties and their order")
        void shouldPreserveMultipleAllowedSortProperties() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(
                            Sort.Order.asc("name"),
                            Sort.Order.desc("createdAt")
                    )
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(
                    Sort.by(
                            Sort.Order.asc("name"),
                            Sort.Order.desc("createdAt")
                    ),
                    result.getSort()
            );
        }

        @Test
        @DisplayName("Should retain allowed properties when mixed with disallowed properties")
        void shouldRetainAllowedPropertiesAmongDisallowedProperties() {
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(
                            Sort.Order.desc("password_hash"),
                            Sort.Order.asc("name"),
                            Sort.Order.desc("internalSecret")
                    )
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(
                    Sort.by(Sort.Order.asc("name")),
                    result.getSort()
            );

            assertEquals(1, result.getSort().stream().count());
        }
    }

    @Nested
    @DisplayName("Sanitized Pageable Properties")
    class SanitizedPageablePropertiesTests {

        @Test
        @DisplayName("Should return a non-null pageable")
        void shouldReturnNonNullPageable() {
            Pageable pageable = PageRequest.of(0, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return a paged request")
        void shouldReturnPagedRequest() {
            Pageable pageable = PageRequest.of(0, 20);

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertTrue(result.isPaged());
            assertFalse(result.isUnpaged());
        }

        @Test
        @DisplayName("Should preserve the complete pagination configuration")
        void shouldPreservePaginationConfiguration() {
            Pageable pageable = PageRequest.of(
                    3,
                    25,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Pageable result = PageSanitizer.sanitize(
                    pageable,
                    ALLOWED_SORT_PROPERTIES,
                    FALLBACK_SORT
            );

            assertEquals(3, result.getPageNumber());
            assertEquals(25, result.getPageSize());
            assertEquals(
                    Sort.by(Sort.Direction.DESC, "createdAt"),
                    result.getSort()
            );
        }
    }
}
