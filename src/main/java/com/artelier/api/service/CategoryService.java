package com.artelier.api.service;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse create(CategoryRequest request);
}