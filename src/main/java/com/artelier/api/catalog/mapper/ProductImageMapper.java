package com.artelier.api.catalog.mapper;


import com.artelier.api.catalog.dto.ProductImageResponse;
import com.artelier.api.catalog.entity.ProductImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {
    ProductImageResponse toResponse(ProductImage image);
}
