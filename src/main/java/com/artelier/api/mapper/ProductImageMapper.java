package com.artelier.api.mapper;


import com.artelier.api.dto.response.ProductImageResponse;
import com.artelier.api.entity.ProductImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {
    ProductImageResponse toResponse(ProductImage image);
}
