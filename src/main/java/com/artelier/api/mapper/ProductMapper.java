package com.artelier.api.mapper;

import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Product;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = {CategoryMapper.class, ProductImageMapper.class}
)
public interface ProductMapper {
    ProductResponse toResponse(Product product);
}