package com.artelier.api.service;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.CategoryMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.service.Impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private CategoryMapper mapper;

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void shouldReturnAllCategories() {
        Category category = new Category();
        CategoryResponse response = new CategoryResponse();

        when(repository.findAll()).thenReturn(List.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = service.getAll();

        assertEquals(1, result.size());
        verify(repository).findAll();
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldCreateCategory() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Art");
        request.setSlug("art");

        Category saved = new Category();
        CategoryResponse response = new CategoryResponse();

        when(repository.existsBySlug("art")).thenReturn(false);
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        CategoryResponse result = service.create(request);

        assertNotNull(result);
        verify(repository).save(any(Category.class));
    }

    @Test
    void shouldThrowIfSlugExists() {
        CategoryRequest request = new CategoryRequest();
        request.setSlug("art");

        when(repository.existsBySlug("art")).thenReturn(true);

        assertThrows(ArtelierException.class, () -> service.create(request));
    }
}