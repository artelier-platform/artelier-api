package com.artelier.api.mapper;

import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {OrderItemMapper.class}
)
public interface OrderMapper {

    @Mapping(source = "items", target = "items")
    OrderResponse toResponse(Order order);
}