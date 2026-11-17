package com.artelier.api.catalog.mapper;

import com.artelier.api.catalog.dto.ProductResponse;
import com.artelier.api.catalog.entity.Product;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = {CategoryMapper.class, ProductImageMapper.class}
)
public interface ProductMapper {
    ProductResponse toResponse(Product product);
}