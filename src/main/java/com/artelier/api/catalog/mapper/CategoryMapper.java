package com.artelier.api.catalog.mapper;

import com.artelier.api.catalog.dto.CategoryResponse;
import com.artelier.api.catalog.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}