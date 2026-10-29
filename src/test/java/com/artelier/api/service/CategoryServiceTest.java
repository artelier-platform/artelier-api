package com.artelier.api.service;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.CategoryMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryServiceImpl}. Repository and mapper are mocked,
 * so no Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper);
    }

    private static Category category(UUID id, String name, String slug, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return category;
    }

    private static CategoryResponse responseOf(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("maps every stored category through the mapper, keeping the repository order")
        void mapsAllInOrder() {
            Category textiles = category(UUID.randomUUID(), "Textiles", "textiles", null);
            Category ceramics = category(UUID.randomUUID(), "Ceramics", "ceramics", "Handmade ceramic products");
            CategoryResponse textilesResponse = responseOf(textiles);
            CategoryResponse ceramicsResponse = responseOf(ceramics);

            when(categoryRepository.findAll()).thenReturn(List.of(textiles, ceramics));
            when(categoryMapper.toResponse(textiles)).thenReturn(textilesResponse);
            when(categoryMapper.toResponse(ceramics)).thenReturn(ceramicsResponse);

            List<CategoryResponse> result = categoryService.getAll();

            assertEquals(List.of(textilesResponse, ceramicsResponse), result);
            verify(categoryRepository).findAll();
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("returns an empty list and never calls the mapper when there are no categories")
        void emptyRepository() {
            when(categoryRepository.findAll()).thenReturn(List.of());

            List<CategoryResponse> result = categoryService.getAll();

            assertTrue(result.isEmpty());
            verifyNoInteractions(categoryMapper);
        }
    }

    @Nested
    @DisplayName("create(request)")
    class Create {

        @Test
        @DisplayName("persists a new category with the request data and returns the mapped saved entity")
        void createsCategory() {
            CategoryRequest request = new CategoryRequest("Ceramics", "ceramics", "Handmade ceramic products");
            Category saved = category(UUID.randomUUID(), "Ceramics", "ceramics", "Handmade ceramic products");
            CategoryResponse expected = responseOf(saved);

            when(categoryRepository.existsBySlug("ceramics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            when(categoryMapper.toResponse(saved)).thenReturn(expected);

            CategoryResponse result = categoryService.create(request);

            assertSame(expected, result);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            Category persisted = captor.getValue();
            assertNotSame(saved, persisted);
            assertNull(persisted.getId());
            assertEquals("Ceramics", persisted.getName());
            assertEquals("ceramics", persisted.getSlug());
            assertEquals("Handmade ceramic products", persisted.getDescription());
        }

        @Test
        @DisplayName("checks slug availability before saving")
        void checksSlugBeforeSaving() {
            CategoryRequest request = new CategoryRequest("Jewelry", "jewelry", null);
            Category saved = category(UUID.randomUUID(), "Jewelry", "jewelry", null);

            when(categoryRepository.existsBySlug("jewelry")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            when(categoryMapper.toResponse(saved)).thenReturn(responseOf(saved));

            categoryService.create(request);

            InOrder order = inOrder(categoryRepository);
            order.verify(categoryRepository).existsBySlug("jewelry");
            order.verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("accepts a request without description and saves it as null")
        void nullDescription() {
            CategoryRequest request = new CategoryRequest("Textiles", "textiles", null);
            Category saved = category(UUID.randomUUID(), "Textiles", "textiles", null);
            CategoryResponse expected = responseOf(saved);

            when(categoryRepository.existsBySlug("textiles")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            when(categoryMapper.toResponse(saved)).thenReturn(expected);

            CategoryResponse result = categoryService.create(request);

            assertSame(expected, result);
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertNull(captor.getValue().getDescription());
        }

        @Test
        @DisplayName("409 – slug already in use: throws conflict, nothing is saved or mapped")
        void duplicatedSlug() {
            CategoryRequest request = new CategoryRequest("Ceramics 2", "ceramics", "Another one");
            when(categoryRepository.existsBySlug("ceramics")).thenReturn(true);

            ArtelierException exception =
                    assertThrows(ArtelierException.class, () -> categoryService.create(request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
            assertEquals("Slug already in use", exception.getMessage());
            verify(categoryRepository).existsBySlug("ceramics");
            verify(categoryRepository, never()).save(any(Category.class));
            verifyNoMoreInteractions(categoryRepository);
            verifyNoInteractions(categoryMapper);
        }
    }
}
