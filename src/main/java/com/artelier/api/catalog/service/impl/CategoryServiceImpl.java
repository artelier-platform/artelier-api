package com.artelier.api.catalog.service.impl;

import com.artelier.api.catalog.dto.CategoryRequest;
import com.artelier.api.catalog.dto.CategoryResponse;
import com.artelier.api.catalog.entity.Category;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.catalog.mapper.CategoryMapper;
import com.artelier.api.catalog.repository.CategoryRepository;
import com.artelier.api.catalog.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw ArtelierException.conflict("Slug already in use");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }
}
