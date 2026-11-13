package com.artelier.api.order.mapper;

import com.artelier.api.order.dto.OrderResponse;
import com.artelier.api.order.entity.Order;
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