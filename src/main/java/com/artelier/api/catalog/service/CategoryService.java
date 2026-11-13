package com.artelier.api.catalog.service;

import com.artelier.api.catalog.dto.CategoryRequest;
import com.artelier.api.catalog.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse create(CategoryRequest request);
}