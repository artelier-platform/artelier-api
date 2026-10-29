package com.artelier.api.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SearchTerms Unit Tests")
class SearchTermsTest {

    @Nested
    @DisplayName("LIKE Pattern Normalization")
    class LikePatternNormalizationTests {

        @Test
        @DisplayName("Should return null when the input is null")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(SearchTerms.toLikePattern(null));
        }

        @Test
        @DisplayName("Should return null when the input is empty")
        void shouldReturnNullWhenInputIsEmpty() {
            assertNull(SearchTerms.toLikePattern(""));
        }

        @Test
        @DisplayName("Should return null when the input contains only whitespace")
        void shouldReturnNullWhenInputContainsOnlyWhitespace() {
            assertNull(SearchTerms.toLikePattern("   \t\n  "));
        }

        @Test
        @DisplayName("Should trim whitespace and wrap the term with percent signs")
        void shouldTrimWhitespaceAndWrapTerm() {
            assertEquals(
                    "%artelier%",
                    SearchTerms.toLikePattern("  artelier  ")
            );
        }

        @Test
        @DisplayName("Should convert uppercase characters to lowercase")
        void shouldConvertTextToLowercase() {
            assertEquals(
                    "%artelier%",
                    SearchTerms.toLikePattern("ARTELIER")
            );
        }

        @Test
        @DisplayName("Should remove accents from characters")
        void shouldRemoveAccents() {
            assertEquals(
                    "%camion%",
                    SearchTerms.toLikePattern("camión")
            );
        }

        @Test
        @DisplayName("Should remove multiple accents from a term")
        void shouldRemoveMultipleAccents() {
            assertEquals(
                    "%diseno artesanal%",
                    SearchTerms.toLikePattern("DISEÑO ARTESANÁL")
            );
        }

        @Test
        @DisplayName("Should preserve internal whitespace")
        void shouldPreserveInternalWhitespace() {
            assertEquals(
                    "%arte hecho a mano%",
                    SearchTerms.toLikePattern("  Arte Hecho A Mano  ")
            );
        }

        @Test
        @DisplayName("Should preserve punctuation that does not require escaping")
        void shouldPreserveUnescapedPunctuation() {
            assertEquals(
                    "%hello, world.!-+%",
                    SearchTerms.toLikePattern("Hello, World.!-+")
            );
        }

        @Test
        @DisplayName("Should preserve non-Latin characters")
        void shouldPreserveNonLatinCharacters() {
            assertEquals(
                    "%東京%",
                    SearchTerms.toLikePattern("東京")
            );
        }
    }

    @Nested
    @DisplayName("LIKE Wildcard Escaping")
    class LikeWildcardEscapingTests {

        @Test
        @DisplayName("Should escape percent signs")
        void shouldEscapePercentSigns() {
            assertEquals(
                    "%50\\%%",
                    SearchTerms.toLikePattern("50%")
            );
        }

        @Test
        @DisplayName("Should escape underscores")
        void shouldEscapeUnderscores() {
            assertEquals(
                    "%product\\_name%",
                    SearchTerms.toLikePattern("product_name")
            );
        }

        @Test
        @DisplayName("Should escape backslashes")
        void shouldEscapeBackslashes() {
            assertEquals(
                    "%folder\\\\name%",
                    SearchTerms.toLikePattern("folder\\name")
            );
        }

        @Test
        @DisplayName("Should escape consecutive percent signs")
        void shouldEscapeConsecutivePercentSigns() {
            assertEquals(
                    "%\\%\\%%",
                    SearchTerms.toLikePattern("%%")
            );
        }

        @Test
        @DisplayName("Should escape consecutive underscores")
        void shouldEscapeConsecutiveUnderscores() {
            assertEquals(
                    "%\\_\\_%",
                    SearchTerms.toLikePattern("__")
            );
        }

        @Test
        @DisplayName("Should escape mixed SQL LIKE special characters")
        void shouldEscapeMixedSpecialCharacters() {
            assertEquals(
                    "%a\\%b\\_c\\\\d%",
                    SearchTerms.toLikePattern("a%b_c\\d")
            );
        }

        @Test
        @DisplayName("Should escape backslashes before escaping wildcard characters")
        void shouldEscapeBackslashesBeforeWildcards() {
            assertEquals(
                    "%\\\\\\%\\\\\\_%",
                    SearchTerms.toLikePattern("\\%\\_")
            );
        }

        @Test
        @DisplayName("Should escape special characters after normalization")
        void shouldEscapeSpecialCharactersAfterNormalization() {
            assertEquals(
                    "%cancion\\_50\\%%",
                    SearchTerms.toLikePattern("Canción_50%")
            );
        }

        @Test
        @DisplayName("Should expose the backslash escape character")
        void shouldExposeBackslashEscapeCharacter() {
            assertEquals('\\', SearchTerms.ESCAPE);
        }
    }

    @Nested
    @DisplayName("Criteria Expression Normalization")
    class CriteriaExpressionNormalizationTests {

        @Test
        @DisplayName("Should create a lowercase expression before applying unaccent")
        void shouldApplyLowercaseBeforeUnaccent() {
            CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
            Expression<String> originalExpression = mock(Expression.class);
            Expression<String> lowercaseExpression = mock(Expression.class);
            Expression<String> normalizedExpression = mock(Expression.class);

            when(criteriaBuilder.lower(originalExpression))
                    .thenReturn(lowercaseExpression);

            when(criteriaBuilder.function(
                    "unaccent",
                    String.class,
                    lowercaseExpression
            )).thenReturn(normalizedExpression);

            Expression<String> result = SearchTerms.normalized(
                    criteriaBuilder,
                    originalExpression
            );

            assertSame(normalizedExpression, result);

            verify(criteriaBuilder).lower(originalExpression);
            verify(criteriaBuilder).function(
                    "unaccent",
                    String.class,
                    lowercaseExpression
            );
        }

        @Test
        @DisplayName("Should use the unaccent database function")
        void shouldUseUnaccentDatabaseFunction() {
            CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
            Expression<String> originalExpression = mock(Expression.class);
            Expression<String> lowercaseExpression = mock(Expression.class);
            Expression<String> normalizedExpression = mock(Expression.class);

            when(criteriaBuilder.lower(originalExpression))
                    .thenReturn(lowercaseExpression);

            when(criteriaBuilder.function(
                    "unaccent",
                    String.class,
                    lowercaseExpression
            )).thenReturn(normalizedExpression);

            SearchTerms.normalized(criteriaBuilder, originalExpression);

            verify(criteriaBuilder).function(
                    "unaccent",
                    String.class,
                    lowercaseExpression
            );
        }
    }
}
