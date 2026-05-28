package com.artelier.api.service.impl;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.CategoryMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.service.CategoryService;
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
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw ArtelierException.conflict("Slug already in use");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }
}
