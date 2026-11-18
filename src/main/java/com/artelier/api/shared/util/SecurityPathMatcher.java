package com.artelier.api.shared.util;

import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for evaluating HTTP request paths against arrays of Ant-style path patterns
 * using Spring's {@link AntPathMatcher}.
 */
public final class SecurityPathMatcher {

    /**
     * Spring utility for matching Ant-style path patterns (e.g., {@code /auth/**}).
     */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SecurityPathMatcher() {
    }

    /**
     * Checks whether the specified path matches any of the provided Ant-style route patterns.
     *
     * @param patterns a collection of Ant-style path patterns to test against
     * @param path     the request path to evaluate
     * @return {@code true} if the path matches at least one pattern; {@code false} otherwise
     */
    public static boolean matchesAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(p -> MATCHER.match(p, path));
    }
}