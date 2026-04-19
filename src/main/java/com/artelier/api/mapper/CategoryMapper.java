package com.artelier.api.mapper;

import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}